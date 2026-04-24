# Informe de Seguridad - PriceWise Backend

**Fecha:** 2026-03-09
**Versión:** 1.2

---

## Resumen Ejecutivo

| Categoría           | Estado    | Nivel |
|---------------------|-----------|-------|
| Autenticación       | Seguro    | Alto  |
| Google OAuth2       | Seguro    | Alto  |
| Autorización        | Seguro    | Alto  |
| Almacenamiento      | Seguro    | Alto  |
| Cifrado API Keys    | Seguro    | Alto  |
| Comunicaciones      | Seguro    | Alto  |
| Codigo fuente       | Seguro    | Alto  |
| Permisos de API     | Adecuados | Alto  |
| Logging             | Seguro    | Alto  |
| Validación          | Seguro    | Alto  |

---

## 1. Autenticación

### JWT (JSON Web Token) con HMAC-SHA256
- **Estado:** Seguro
- Algoritmo: HS256 (HMAC con SHA-256)
- Expiración: 24 horas (86.400.000 ms)
- El token incluye: userId, username, email, roles, iat, exp
- Validación en cada petición: firma, formato y expiración

### Gestión del Secreto JWT
- **Estado:** Seguro
- El secreto se carga desde la variable de entorno `JWT_SECRET`
- Mínimo 32 caracteres exigido en validación al arrancar
- Si no esta configurado en producción: la aplicación falla al iniciar con error fatal
- En desarrollo sin configurar: arranca con warning visible en logs

```java
// JwtService.java - validación al arrancar
@PostConstruct
public void validateSecretKey() {
    if (secret.equals("default-secret-change-in-production")) {
        if (isProduction()) {
            throw new IllegalStateException("FATAL: JWT_SECRET no configurado en producción.");
        }
        log.warn("ATENCIÓN: Usando JWT_SECRET por defecto. No usar en producción.");
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
- Distingue expirado (401 + mensaje específico) de token inválido (401 genérico)
- **Android**: `AuthInterceptor` detecta 401, limpia token cacheado y notifica `SessionManager` vía `SharedFlow`. `NavGraph` recoge el evento y navega automáticamente a Login.

### Google OAuth2
- **Estado:** Seguro
- Flujo: el cliente Android obtiene un `idToken` de Google Sign-In y lo envía a `POST /api/auth/google`
- `GoogleTokenService` valida el token contra Google API (`https://oauth2.googleapis.com/tokeninfo`)
- Se verifica: firma, audience (client ID), expiración y email verificado
- Si el usuario ya existe: se genera JWT directamente
- Si es nuevo: se devuelve estado `needs_company` con `tempToken` temporal para completar registro
- El `tempToken` tiene expiración corta y solo sirve para `/google/complete-new-company` o `/google/complete-join`
- Campo `auth_provider` en tabla `users` distingue `LOCAL` vs `GOOGLE` (migración V4)
- Usuarios Google no tienen contraseña almacenada (campo password = cadena aleatoria hasheada con BCrypt)

### Cifrado de API Keys (Keepa)
- **Estado:** Seguro
- Las API keys de terceros (Keepa) se almacenan cifradas con AES-256 en tabla `company_api_keys`
- `CompanyApiKeyService` gestiona cifrado/descifrado usando clave derivada de variable de entorno
- Cada empresa tiene su propia API key (aislamiento por `company_id`)
- La key nunca se expone en texto plano en respuestas API (se devuelve `maskedKey` con asteriscos)
- Constraint UNIQUE `(company_id, provider)` impide duplicados por proveedor
- Las keys se pueden activar/desactivar sin eliminarlas (campo `enabled`)
- Solo `COMPANY_ADMIN` y `ADMIN` pueden gestionar API keys

---

## 2. Autorización

### Control de Acceso por Roles (Multi-Tenancy)
- **Estado:** Seguro
- Tres roles disponibles:

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| `ADMIN` | Super-admin de la plataforma | Control total: todos los usuarios, empresas, datos |
| `COMPANY_ADMIN` | Admin de una empresa | Gestiona productos, empleados y datos de su empresa |
| `EMPLOYEE` | Empleado de una empresa | Lectura y operaciones básicas dentro de su empresa |

- Aplicado con `@PreAuthorize` a nivel de controlador y método
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
- Cada usuario pertenece a una `Company` vía foreign key
- Todos los endpoints (productos, alertas, recomendaciones) filtran por `companyId`, no por `userId`
- El `companyId` se extrae del JWT en cada petición vía `UserPrincipal`
- Si un usuario intenta acceder a datos de otra empresa recibe 404
- Las alertas se consultan vía `product.company.id` en JPQL para garantizar aislamiento

```java
// ProductService.java — aislamiento por empresa
ProductResponse response = productService.getProduct(
    userPrincipal.getCompanyId(), id);

// AlertRepository.java — alertas aisladas por empresa
@Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId")
Page<Alert> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
```

### Gestión de Empleados
- COMPANY_ADMIN puede crear empleados para su propia empresa vía `POST /api/auth/create-employee`
- Los empleados se asignan automáticamente a la empresa del admin que los crea
- Solo COMPANY_ADMIN y ADMIN pueden crear empleados

### Protección de Acciones Admin
- Los admins no pueden desactivar su propia cuenta (Bug #16 resuelto)
- Los admins no pueden eliminar su propio usuario
- El rol ADMIN solo se puede asignar desde otro ADMIN

---

## 3. Almacenamiento de Datos

### Contraseñas
- **Estado:** Seguro
- Algoritmo: BCrypt con salt automático
- Las contraseñas no se almacenan en texto plano en ningún momento
- No se loguan en debug (Bug #19 resuelto)
- El campo `password` en `User` tiene `@JsonIgnore` para prevenir serialización

```java
@JsonIgnore
@Column(nullable = false)
private String password;
```

### Base de Datos PostgreSQL
- **Estado:** Seguro
- Sin uso de `rawQuery()` ni `execSQL()`: prevención de SQL Injection por diseño
- Todas las queries usan parámetros nombrados vía Spring Data JPA
- Constraints de unicidad en BD además de validación en código
- La tabla `companies` centraliza datos de empresa con plan y tipo de negocio

### Variables de Entorno
- **Estado:** Seguro
- URL de BD vía variable `DB_URL`
- Username de BD vía variable `DB_USERNAME`
- Credenciales de BD vía variable `DB_PASSWORD`
- JWT secret vía variable `JWT_SECRET`
- API keys de Keepa gestionadas por empresa (cifradas AES-256 en tabla `company_api_keys`, configurables desde Ajustes)
- El `.env.example` no contiene valores reales, solo ejemplos
- El `.gitignore` excluye `.env` del control de versiones
- Ninguna credencial esta hardcodeada en `application.yml`

---

## 4. Comunicaciones de Red

### HTTPS Exclusivo
- **Estado:** Seguro
- API Keepa: `https://api.keepa.com/`
- No hay llamadas a URLs `http://` en el código
- La aplicación no configura `usesCleartextTraffic`

### CORS
- **Estado:** Seguro según entorno

En desarrollo (`SPRING_PROFILES_ACTIVE=dev`):
```yaml
cors:
  allow-all: true  # Acepta cualquier origen, credentials deshabilitado
```

En producción (`SPRING_PROFILES_ACTIVE=prod`):
```yaml
cors:
  allow-all: false
  allowed-origins:
    - https://miapp.com
    - https://www.miapp.com
```

- En desarrollo: `allowCredentials: false` con `allowedOriginPatterns: *` (seguro)
- En producción: `allowCredentials: true` solo con origenes específicos
- Headers permitidos restringidos: `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`

### CSRF
- **Estado:** Adecuado
- CSRF deshabilitado explícitamente en `SecurityConfig`
- Es la práctica correcta para APIs REST stateless autenticadas con JWT
- Los tokens JWT en header `Authorization` no son vulnerables a CSRF

---

## 5. Endpoints y Control de Acceso

### Mapa de Acceso por Endpoint

| Endpoint                               | Acceso         | Notas                                      |
|----------------------------------------|----------------|--------------------------------------------|
| POST /api/admin/companies              | ADMIN          | Crea empresa + COMPANY_ADMIN + código auto |
| POST /api/auth/register                | Público        | Requiere `companyCode` para unirse a empresa|
| POST /api/auth/login                   | Público        | Devuelve JWT con companyId y userId         |
| GET  /api/health                       | Público        | No expone datos sensibles                  |
| GET  /api/competitors/status           | Público        | Solo boolean de disponibilidad             |
| GET  /actuator/**                      | Público        | Métricas básicas de Spring                 |
| GET  /api/auth/profile                 | Autenticado    | Datos del usuario + empresa                |
| POST /api/auth/create-employee         | COMPANY_ADMIN/ADMIN | Crea empleado en la empresa del admin |
| POST /api/products                     | Autenticado    | Crea producto para su empresa              |
| GET  /api/products/**                  | Autenticado    | Solo productos de su empresa               |
| GET  /api/analytics/**                 | COMPANY_ADMIN/EMPLOYEE/ADMIN | Métricas de empresa      |
| GET  /api/competitors/amazon/**        | Autenticado    | Consultas Keepa autenticadas               |
| *    /api/alert-rules/**               | Autenticado    | CRUD reglas de alerta por empresa          |
| *    /api/users/**                     | COMPANY_ADMIN/ADMIN | Gestión usuarios de la empresa        |
| *    /api/api-keys/**                  | COMPANY_ADMIN/ADMIN | CRUD API keys cifradas por empresa     |
| GET  /api/admin/**                     | ADMIN          | Gestión global de la plataforma            |

### Notas sobre Actuator
El endpoint `/actuator/**` esta público actualmente. En producción se recomienda:
- Restringir a `ADMIN` o a una red interna
- Deshabilitar endpoints sensibles (`/actuator/env`, `/actuator/beans`)

---

## 6. Logging y Depuración

### HTTP Logging Condicional
- **Estado:** Seguro
- En producción: sin logging del cuerpo de peticiones/respuestas HTTP
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
- `show-sql: false` en producción
- En desarrollo `show-sql: true` solo muestra queries, no valores de parámetros
  (configurable con `logging.level.org.hibernate.type: WARN`)

### Datos Sensibles en Logs
- **Estado:** Seguro
- Contraseñas no se loguan (campo `@JsonIgnore`)
- API keys no se loguan en nivel INFO o superior
- Excepciones loguan el mensaje pero no el stack trace completo en producción

---

## 7. Validación de Entradas

### Bean Validation (Jakarta)
- **Estado:** Seguro
- Todos los DTOs de entrada tienen validaciones `@Valid` activadas en los controladores
- Los errores de validación devuelven 400 con detalle por campo

Campos validados en `ProductRequest`:
- `name`: @NotBlank, @Size(max = 200)
- `currentPrice`: @NotNull, @DecimalMin("0.01"), @Digits(integer=10, fraction=2)
- `costPrice`: @DecimalMin("0.00") si se envía
- `sku`: @Size(max = 50) si se envía

Campos validados en `RegisterRequest`:
- `email`: @NotBlank, @Email
- `username`: @NotBlank, @Size(min=3, max=50)
- `password`: @NotBlank, @Size(min=6)

### Validaciones de Negocio
- Unicidad de email y username al registrar
- Unicidad de SKU por usuario (no global)
- Propiedad de recursos antes de cualquier modificación
- El Admin no puede autodesactivarse

---

## 8. Integridad de Datos

### Transacciones
- **Estado:** Seguro
- Los métodos de servicio que modifican múltiples entidades usan `@Transactional`
- Si una parte falla, toda la operación se revierte automáticamente
- Ejemplo: crear producto + registrar PriceHistory es una sola transacción

### Concurrencia en Keepa
- **Estado:** Seguro
- `Semaphore(3)`: máximo 3 peticiones concurrentes a Keepa
- Inicialización de Amazon Competitor en `@PostConstruct` (hilo único)
- Double-checked locking para casos edge en inicialización

---

## 9. Dependencias y Vulnerabilidades

### Dependencias Principales

| Dependencia         | Versión  | Notas                                   |
|---------------------|----------|-----------------------------------------|
| Spring Boot         | 3.2.1    | LTS activa, parches de seguridad al dia |
| Spring Security     | 6.x      | Incluido en Spring Boot BOM             |
| jjwt                | 0.12.3   | Versión actual, sin CVEs conocidos      |
| PostgreSQL Driver   | 42.x     | Incluido en Spring Boot BOM             |
| Lombok              | 1.18.40  | Solo compile time, sin runtime exposure |
| Jsoup               | 1.17.2   | Versión actual                          |

### Recomendación
Ejecutar `mvn dependency:check` o integrar OWASP Dependency Check en el pipeline
para detectar CVEs en dependencias transitivas.

---

## 10. Configuración del Servidor

### application.yml en Producción
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
- Las variables de entorno se pasan en tiempo de ejecución (`--env-file .env`)
- El usuario del contenedor no debe ser root (pendiente configurar en Dockerfile)

---

## Acciones Recomendadas

### Pendientes (Prioridad Media)

1. **Certificate Pinning para Keepa API**
   Implementar `CertificatePinner` de OkHttp para prevenir ataques man-in-the-middle
   contra la conexión con Keepa.

2. **Restricción de Actuator en Producción**
   Mover `/actuator/**` a solo `ADMIN` o a red interna.

3. **Audit Log**
   Registrar en tabla separada todas las operaciones ADMIN (quien, que, cuando).

4. **OWASP Dependency Check**
   Integrar en el build de Maven para detectar CVEs automáticamente en cada PR.

### Pendientes (Prioridad Baja)

1. **Root Detection** si se desarrolla cliente móvil
2. **2FA para cuentas ADMIN**
3. **Rotación de JWT_SECRET** con periodo de gracia para tokens existentes

### Completadas

- JWT_SECRET validado al arrancar con error fatal en producción
- BCrypt para contraseñas con salt automático
- CORS configurado por perfil (restrictivo en producción)
- CORS: `allowCredentials` deshabilitado con wildcard origins (fix seguridad)
- CORS: headers permitidos restringidos a whitelist explícita
- CSRF deshabilitado correctamente para API REST stateless
- Verificación de propiedad de recursos en capa de servicio
- Soft delete sin exponer datos borrados en queries
- Admin no puede desactivarse a sí mismo
- Token expirado devuelve 401 claro (no 403 genérico)
- Logs de BD sin parámetros sensibles en producción
- Variables de entorno para todos los secretos (incluidos DB_URL y DB_USERNAME)
- Rate limiting en `/api/auth/login` y `/api/auth/register` (10 intentos/minuto por IP)
- Dependencia Keepa fijada a versión concreta (2.04) en lugar de LATEST
- Multi-tenancy: alertas corregidas para filtrar por `companyId` vía JPQL en lugar de `userId`
- Prevención N+1: `default_batch_fetch_size: 16` + JOIN FETCH en consultas críticas
- Login optimizado: eliminada query redundante a `userRepository.findByEmail()`, datos extraídos de `UserPrincipal`
- `UserPrincipal` enriquecido con `companyName`, `role` y `displayUsername` para evitar re-queries
- JWT sin valor por defecto en perfil prod (`${JWT_SECRET}` sin fallback — app falla al arrancar si no esta configurado)
- Paginación validada con `@Min(0)` en page y `@Min(1) @Max(100)` en size en ProductController y AnalyticsController
- Permiso `ACCESS_NETWORK_STATE` declarado en AndroidManifest para NetworkObserver (evita SecurityException)
- AdminController optimizado: usa count queries en vez de `findAll().stream()` (evita cargar entidades completas en memoria)
- Google OAuth2: validación de idToken vía Google API con verificación de audience, firma y expiración
- API keys cifradas con AES-256 en BD (nunca expuestas en texto plano en respuestas)
- Guardia de auto-eliminación en UserService: usuario no puede eliminarse a sí mismo (check temprano antes de consultar BD)

---

## Conclusión

El proyecto tiene una base de seguridad sólida apropiada para un entorno de producción
inicial. Las vulnerabilidades de mayor riesgo (contraseñas en texto plano, secrets en
código, SQL injection, fuga de datos entre usuarios) estan cubiertas.

Las acciones pendientes son de prioridad media-baja y no representan vulnerabilidades
explotables de forma trivial en el estado actual del sistema.

---

## Documentación Relacionada

- [ARQUITECTURA.md](ARQUITECTURA.md) — Guía completa de arquitectura y justificaciones técnicas
- [README.md](README.md) — Endpoints REST, instalación y configuración
- [FLYWAY.md](FLYWAY.md) — Migraciones de base de datos (esquema de seguridad en V1, V5, V8)
- [CRONOGRAMA.md](CRONOGRAMA.md) — Timeline de desarrollo por fases
