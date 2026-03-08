# Informe de Seguridad - PriceWise Backend

**Fecha:** 2026-02-16
**Version:** 1.1

---

## Resumen Ejecutivo

| Categoria           | Estado    | Nivel |
|---------------------|-----------|-------|
| Autenticacion       | Seguro    | Alto  |
| Autorizacion        | Seguro    | Alto  |
| Almacenamiento      | Seguro    | Alto  |
| Comunicaciones      | Seguro    | Alto  |
| Codigo fuente       | Seguro    | Alto  |
| Permisos de API     | Adecuados | Alto  |
| Logging             | Seguro    | Alto  |
| Validacion          | Seguro    | Alto  |

---

## 1. Autenticacion

### JWT (JSON Web Token) con HMAC-SHA256
- **Estado:** Seguro
- Algoritmo: HS256 (HMAC con SHA-256)
- Expiracion: 24 horas (86.400.000 ms)
- El token incluye: userId, username, email, roles, iat, exp
- Validacion en cada peticion: firma, formato y expiracion

### Gestion del Secreto JWT
- **Estado:** Seguro
- El secreto se carga desde la variable de entorno `JWT_SECRET`
- Minimo 32 caracteres exigido en validacion al arrancar
- Si no esta configurado en produccion: la aplicacion falla al iniciar con error fatal
- En desarrollo sin configurar: arranca con warning visible en logs

```java
// JwtService.java - validacion al arrancar
@PostConstruct
public void validateSecretKey() {
    if (secret.equals("default-secret-change-in-production")) {
        if (isProduction()) {
            throw new IllegalStateException("FATAL: JWT_SECRET no configurado en produccion.");
        }
        log.warn("ATENCION: Usando JWT_SECRET por defecto. No usar en produccion.");
    }
    if (secret.length() < 32) {
        throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
    }
}
```

### Tokens Stateless
- **Estado:** Seguro
- `SessionCreationPolicy.STATELESS`: el servidor no guarda sesiones
- Permite escalar horizontalmente sin estado compartido entre instancias
- Sin CSRF necesario (los tokens JWT no son vulnerables a CSRF en APIs REST puras)

### Respuesta ante Token Expirado
- **Estado:** Seguro
- Devuelve HTTP 401 Unauthorized con mensaje descriptivo
- Distingue expirado (401 + mensaje especifico) de token invalido (401 generico)
- **Android**: `AuthInterceptor` detecta 401, limpia token cacheado y notifica `SessionManager` via `SharedFlow`. `NavGraph` recoge el evento y navega automaticamente a Login.

---

## 2. Autorizacion

### Control de Acceso por Roles (Multi-Tenancy)
- **Estado:** Seguro
- Tres roles disponibles:

| Rol | Descripcion | Acceso |
|-----|-------------|--------|
| `ADMIN` | Super-admin de la plataforma | Control total: todos los usuarios, empresas, datos |
| `COMPANY_ADMIN` | Admin de una empresa | Gestiona productos, empleados y datos de su empresa |
| `EMPLOYEE` | Empleado de una empresa | Lectura y operaciones basicas dentro de su empresa |

- Aplicado con `@PreAuthorize` a nivel de controlador y metodo
- El aislamiento de datos se garantiza mediante `companyId` en el JWT

```java
// AdminController: solo ADMIN de plataforma
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }

// AnalyticsController: COMPANY_ADMIN, EMPLOYEE y ADMIN
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
public class AnalyticsController { ... }

// Crear empleados: solo COMPANY_ADMIN y ADMIN
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
public ResponseEntity<?> createEmployee(...) { ... }
```

### Aislamiento de Datos por Empresa
- **Estado:** Seguro
- Cada usuario pertenece a una `Company` via foreign key
- Todos los endpoints (productos, alertas, recomendaciones) filtran por `companyId`, no por `userId`
- El `companyId` se extrae del JWT en cada peticion via `UserPrincipal`
- Si un usuario intenta acceder a datos de otra empresa recibe 404
- Las alertas se consultan via `product.company.id` en JPQL para garantizar aislamiento

```java
// ProductService.java — aislamiento por empresa
ProductResponse response = productService.getProduct(
    userPrincipal.getCompanyId(), id);

// AlertRepository.java — alertas aisladas por empresa
@Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId")
Page<Alert> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
```

### Gestion de Empleados
- COMPANY_ADMIN puede crear empleados para su propia empresa via `POST /api/auth/create-employee`
- Los empleados se asignan automaticamente a la empresa del admin que los crea
- Solo COMPANY_ADMIN y ADMIN pueden crear empleados

### Proteccion de Acciones Admin
- Los admins no pueden desactivar su propia cuenta (Bug #16 resuelto)
- Los admins no pueden eliminar su propio usuario
- El rol ADMIN solo se puede asignar desde otro ADMIN

---

## 3. Almacenamiento de Datos

### Contrasenas
- **Estado:** Seguro
- Algoritmo: BCrypt con salt automatico
- Las contrasenas no se almacenan en texto plano en ningun momento
- No se loguan en debug (Bug #19 resuelto)
- El campo `password` en `User` tiene `@JsonIgnore` para prevenir serializacion

```java
@JsonIgnore
@Column(nullable = false)
private String password;
```

### Base de Datos PostgreSQL
- **Estado:** Seguro
- Sin uso de `rawQuery()` ni `execSQL()`: prevention de SQL Injection por diseno
- Todas las queries usan parametros nombrados via Spring Data JPA
- Constraints de unicidad en BD ademas de validacion en codigo
- La tabla `companies` centraliza datos de empresa con plan y tipo de negocio

### Variables de Entorno
- **Estado:** Seguro
- URL de BD via variable `DB_URL`
- Username de BD via variable `DB_USERNAME`
- Credenciales de BD via variable `DB_PASSWORD`
- JWT secret via variable `JWT_SECRET`
- API key de Keepa via variable `KEEPA_API_KEY`
- El `.env.example` no contiene valores reales, solo ejemplos
- El `.gitignore` excluye `.env` del control de versiones
- Ninguna credencial esta hardcodeada en `application.yml`

---

## 4. Comunicaciones de Red

### HTTPS Exclusivo
- **Estado:** Seguro
- API Keepa: `https://api.keepa.com/`
- No hay llamadas a URLs `http://` en el codigo
- La aplicacion no configura `usesCleartextTraffic`

### CORS
- **Estado:** Seguro segun entorno

En desarrollo (`SPRING_PROFILES_ACTIVE=dev`):
```yaml
cors:
  allow-all: true  # Acepta cualquier origen, credentials deshabilitado
```

En produccion (`SPRING_PROFILES_ACTIVE=prod`):
```yaml
cors:
  allow-all: false
  allowed-origins:
    - https://miapp.com
    - https://www.miapp.com
```

- En desarrollo: `allowCredentials: false` con `allowedOriginPatterns: *` (seguro)
- En produccion: `allowCredentials: true` solo con origenes especificos
- Headers permitidos restringidos: `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`

### CSRF
- **Estado:** Adecuado
- CSRF deshabilitado explicitamente en `SecurityConfig`
- Es la practica correcta para APIs REST stateless autenticadas con JWT
- Los tokens JWT en header `Authorization` no son vulnerables a CSRF

---

## 5. Endpoints y Control de Acceso

### Mapa de Acceso por Endpoint

| Endpoint                               | Acceso         | Notas                                      |
|----------------------------------------|----------------|--------------------------------------------|
| POST /api/admin/companies              | ADMIN          | Crea empresa + COMPANY_ADMIN + código auto |
| POST /api/auth/register                | Publico        | Requiere `companyCode` para unirse a empresa|
| POST /api/auth/login                   | Publico        | Devuelve JWT con companyId y userId         |
| GET  /api/health                       | Publico        | No expone datos sensibles                  |
| GET  /api/competitors/status           | Publico        | Solo boolean de disponibilidad             |
| GET  /swagger-ui.html                  | Publico        | Documentacion de la API                    |
| GET  /api-docs                         | Publico        | Especificacion OpenAPI                     |
| GET  /actuator/**                      | Publico        | Metricas basicas de Spring                 |
| GET  /api/auth/profile                 | Autenticado    | Datos del usuario + empresa                |
| POST /api/auth/create-employee         | COMPANY_ADMIN/ADMIN | Crea empleado en la empresa del admin |
| POST /api/products                     | Autenticado    | Crea producto para su empresa              |
| GET  /api/products/**                  | Autenticado    | Solo productos de su empresa               |
| GET  /api/analytics/**                 | COMPANY_ADMIN/EMPLOYEE/ADMIN | Metricas de empresa      |
| GET  /api/competitors/amazon/**        | Autenticado    | Consultas Keepa autenticadas               |
| *    /api/alert-rules/**               | Autenticado    | CRUD reglas de alerta por empresa          |
| GET  /api/admin/**                     | ADMIN          | Gestion global de la plataforma            |

### Notas sobre Actuator
El endpoint `/actuator/**` esta publico actualmente. En produccion se recomienda:
- Restringir a `ADMIN` o a una red interna
- Deshabilitar endpoints sensibles (`/actuator/env`, `/actuator/beans`)

---

## 6. Logging y Depuracion

### HTTP Logging Condicional
- **Estado:** Seguro
- En produccion: sin logging del cuerpo de peticiones/respuestas HTTP
- En desarrollo: logging detallado solo en consola local

```java
// AppModule / OkHttpClient builder
HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
interceptor.setLevel(isProduction()
    ? HttpLoggingInterceptor.Level.NONE
    : HttpLoggingInterceptor.Level.BODY);
```

### SQL Logging
- **Estado:** Seguro
- `show-sql: false` en produccion
- En desarrollo `show-sql: true` solo muestra queries, no valores de parametros
  (configurable con `logging.level.org.hibernate.type: WARN`)

### Datos Sensibles en Logs
- **Estado:** Seguro
- Contrasenas no se loguan (campo `@JsonIgnore`)
- API keys no se loguan en nivel INFO o superior
- Excepciones loguan el mensaje pero no el stack trace completo en produccion

---

## 7. Validacion de Entradas

### Bean Validation (Jakarta)
- **Estado:** Seguro
- Todos los DTOs de entrada tienen validaciones `@Valid` activadas en los controladores
- Los errores de validacion devuelven 400 con detalle por campo

Campos validados en `ProductRequest`:
- `name`: @NotBlank, @Size(max = 200)
- `currentPrice`: @NotNull, @DecimalMin("0.01"), @Digits(integer=10, fraction=2)
- `costPrice`: @DecimalMin("0.00") si se envia
- `sku`: @Size(max = 50) si se envia

Campos validados en `RegisterRequest`:
- `email`: @NotBlank, @Email
- `username`: @NotBlank, @Size(min=3, max=50)
- `password`: @NotBlank, @Size(min=6)

### Validaciones de Negocio
- Unicidad de email y username al registrar
- Unicidad de SKU por usuario (no global)
- Propiedad de recursos antes de cualquier modificacion
- El Admin no puede autodesactivarse

---

## 8. Integridad de Datos

### Transacciones
- **Estado:** Seguro
- Los metodos de servicio que modifican multiples entidades usan `@Transactional`
- Si una parte falla, toda la operacion se revierte automaticamente
- Ejemplo: crear producto + registrar PriceHistory es una sola transaccion

### Concurrencia en Keepa
- **Estado:** Seguro
- `Semaphore(3)`: maximo 3 peticiones concurrentes a Keepa
- Inicializacion de Amazon Competitor en `@PostConstruct` (hilo unico)
- Double-checked locking para casos edge en inicializacion

---

## 9. Dependencias y Vulnerabilidades

### Dependencias Principales

| Dependencia         | Version  | Notas                                   |
|---------------------|----------|-----------------------------------------|
| Spring Boot         | 3.2.1    | LTS activa, parches de seguridad al dia |
| Spring Security     | 6.x      | Incluido en Spring Boot BOM             |
| jjwt                | 0.12.3   | Version actual, sin CVEs conocidos      |
| PostgreSQL Driver   | 42.x     | Incluido en Spring Boot BOM             |
| Lombok              | 1.18.40  | Solo compile time, sin runtime exposure |
| Jsoup               | 1.17.2   | Version actual                          |

### Recomendacion
Ejecutar `mvn dependency:check` o integrar OWASP Dependency Check en el pipeline
para detectar CVEs en dependencias transitivas.

---

## 10. Configuracion del Servidor

### application.yml en Produccion
```yaml
server:
  port: 9090

spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Solo verifica, no modifica esquema
    show-sql: false
    properties:
      hibernate:
        default_batch_fetch_size: 16  # Previene N+1 en relaciones LAZY

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/pricewise}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20

cors:
  allow-all: false
  allowed-origins:
    - https://tudominio.com
```

### Docker
- La imagen no incluye ficheros `.env`
- Las variables de entorno se pasan en tiempo de ejecucion (`--env-file .env`)
- El usuario del contenedor no debe ser root (pendiente configurar en Dockerfile)

---

## Acciones Recomendadas

### Pendientes (Prioridad Media)

2. **Certificate Pinning para Keepa API**
   Implementar `CertificatePinner` de OkHttp para prevenir ataques man-in-the-middle
   contra la conexion con Keepa.

3. **Restriccion de Actuator en Produccion**
   Mover `/actuator/**` a solo `ADMIN` o a red interna.

4. **Audit Log**
   Registrar en tabla separada todas las operaciones ADMIN (quien, que, cuando).

5. **OWASP Dependency Check**
   Integrar en el build de Maven para detectar CVEs automaticamente en cada PR.

### Pendientes (Prioridad Baja)

6. **Root Detection** si se desarrolla cliente movil
7. **2FA para cuentas ADMIN**
8. **Rotacion de JWT_SECRET** con periodo de gracia para tokens existentes

### Completadas

- JWT_SECRET validado al arrancar con error fatal en produccion
- BCrypt para contrasenas con salt automatico
- CORS configurado por perfil (restrictivo en produccion)
- CORS: `allowCredentials` deshabilitado con wildcard origins (fix seguridad)
- CORS: headers permitidos restringidos a whitelist explicita
- CSRF deshabilitado correctamente para API REST stateless
- Verificacion de propiedad de recursos en capa de servicio
- Soft delete sin exponer datos borrados en queries
- Admin no puede desactivarse a si mismo
- Token expirado devuelve 401 claro (no 403 generico)
- Logs de BD sin parametros sensibles en produccion
- Variables de entorno para todos los secretos (incluidos DB_URL y DB_USERNAME)
- Rate limiting en `/api/auth/login` y `/api/auth/register` (10 intentos/minuto por IP)
- Dependencia Keepa fijada a version concreta (2.04) en lugar de LATEST
- Multi-tenancy: alertas corregidas para filtrar por `companyId` via JPQL en lugar de `userId`
- Prevencion N+1: `default_batch_fetch_size: 16` + JOIN FETCH en consultas criticas
- Login optimizado: eliminada query redundante a `userRepository.findByEmail()`, datos extraidos de `UserPrincipal`
- `UserPrincipal` enriquecido con `companyName`, `role` y `displayUsername` para evitar re-queries
- JWT sin valor por defecto en perfil prod (`${JWT_SECRET}` sin fallback — app falla al arrancar si no esta configurado)
- Paginacion validada con `@Min(0)` en page y `@Min(1) @Max(100)` en size en ProductController y AnalyticsController
- Permiso `ACCESS_NETWORK_STATE` declarado en AndroidManifest para NetworkObserver (evita SecurityException)
- AdminController optimizado: usa count queries en vez de `findAll().stream()` (evita cargar entidades completas en memoria)

---

## Conclusion

El proyecto tiene una base de seguridad solida apropiada para un entorno de produccion
inicial. Las vulnerabilidades de mayor riesgo (contrasenas en texto plano, secrets en
codigo, SQL injection, fuga de datos entre usuarios) estan cubiertas.

Las acciones pendientes son de prioridad media-baja y no representan vulnerabilidades
explotables de forma trivial en el estado actual del sistema.
