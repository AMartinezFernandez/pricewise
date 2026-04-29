# Informe de seguridad del backend PriceWise

Fecha: 2026-03-09. Versión: 1.2.

## 1. Autenticación

### JWT con HMAC-SHA256

1. Algoritmo HS256 (HMAC con SHA-256).
2. Expiración 24 horas (86.400.000 ms).
3. Claims: `userId`, `username`, `email`, `roles`, `iat`, `exp`.
4. En cada petición se valida firma, formato y expiración.

### Gestión del secreto JWT

El secreto se carga desde la variable de entorno `JWT_SECRET`. Mínimo 32 caracteres, validado al arrancar. En producción, si no está configurado, la aplicación falla. En desarrollo arranca con warning.

```java
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

### Sesión stateless

`SessionCreationPolicy.STATELESS`: el servidor no guarda sesiones, lo que permite escalar horizontalmente sin estado compartido. Sin CSRF necesario, los JWT en header `Authorization` no son vulnerables a CSRF en APIs REST puras.

### Token expirado

Devuelve 401 con mensaje descriptivo. Distingue token expirado (401 con mensaje específico) de token inválido (401 genérico). El cliente Android lo detecta vía `AuthInterceptor`, limpia el token cacheado y notifica a `SessionManager`. `NavGraph` recoge el evento y redirige a Login.

### Google OAuth2

1. El cliente Android obtiene un `idToken` de Google Sign-In y lo envía a `POST /api/auth/google`.
2. `GoogleTokenService` valida el token contra `https://oauth2.googleapis.com/tokeninfo`. Verifica firma, audience (client ID), expiración y email verificado.
3. Si el usuario ya existe, se emite el JWT. Si es nuevo, se devuelve `needs_company` con un `tempToken` corto que solo sirve para `/google/complete-new-company` o `/google/complete-join`.
4. El campo `auth_provider` en `users` distingue `LOCAL` y `GOOGLE` (migración V4).
5. Los usuarios de Google no almacenan contraseña real, solo una cadena aleatoria hasheada con BCrypt.

### Cifrado de API keys (Keepa)

1. Las API keys de Keepa se almacenan cifradas con AES-256 en `company_api_keys`.
2. `CompanyApiKeyService` cifra y descifra con clave derivada de variable de entorno.
3. Cada empresa tiene su propia key, aislada por `company_id`.
4. La key nunca se expone en texto plano. La respuesta devuelve `maskedKey` con asteriscos.
5. Constraint UNIQUE `(company_id, provider)` impide duplicados por proveedor.
6. Las keys se pueden activar o desactivar sin eliminarlas (campo `enabled`).
7. Solo `COMPANY_ADMIN` y `ADMIN` pueden gestionar API keys.

## 2. Autorización

### Roles y multi-tenancy

1. `ADMIN`: super-admin de la plataforma. Control total sobre usuarios, empresas y datos.
2. `COMPANY_ADMIN`: administrador de una empresa. Gestiona productos, empleados y datos de su empresa.
3. `EMPLOYEE`: empleado de una empresa. Lectura y operaciones básicas dentro de su empresa.

Aplicado con `@PreAuthorize` a nivel de controlador y método. El aislamiento de datos se garantiza con el `companyId` del JWT.

```java
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }

@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'EMPLOYEE', 'ADMIN')")
public class AnalyticsController { ... }

@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
public ResponseEntity<?> createEmployee(...) { ... }
```

### Aislamiento por empresa

1. Cada usuario pertenece a una `Company` por foreign key.
2. Productos, alertas y recomendaciones filtran por `companyId`, no por `userId`.
3. El `companyId` se extrae del JWT en cada petición vía `UserPrincipal`.
4. Acceder a datos de otra empresa devuelve 404.
5. Alertas se consultan con `product.company.id` en JPQL para garantizar aislamiento.

```java
ProductResponse response = productService.getProduct(userPrincipal.getCompanyId(), id);

@Query("SELECT a FROM Alert a WHERE a.product.company.id = :companyId")
Page<Alert> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
```

### Gestión de empleados

1. `COMPANY_ADMIN` puede crear empleados de su empresa vía `POST /api/auth/create-employee`.
2. Los empleados se asignan automáticamente a la empresa del admin que los crea.
3. Solo `COMPANY_ADMIN` y `ADMIN` pueden crear empleados.

### Acciones admin protegidas

1. Un admin no puede desactivar su propia cuenta.
2. Un admin no puede eliminar su propio usuario.
3. El rol `ADMIN` solo lo puede asignar otro `ADMIN`.

## 3. Almacenamiento de datos

### Contraseñas

1. BCrypt con salt automático.
2. Nunca en texto plano.
3. No se loguan en debug.
4. El campo `password` en `User` lleva `@JsonIgnore` para evitar serialización.

```java
@JsonIgnore
@Column(nullable = false)
private String password;
```

### PostgreSQL

1. Sin `rawQuery()` ni `execSQL()`. Prevención de SQL injection por diseño.
2. Todas las queries usan parámetros nombrados vía Spring Data JPA.
3. Constraints de unicidad en BD además de validación en código.
4. La tabla `companies` centraliza datos de empresa con plan y tipo de negocio.

### Variables de entorno

1. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
2. `JWT_SECRET`.
3. API keys de Keepa cifradas en `company_api_keys`, configurables desde Ajustes.
4. `.env.example` solo con valores de ejemplo, no reales.
5. `.gitignore` excluye `.env`.
6. Ninguna credencial está hardcodeada en `application.yml`.

## 4. Comunicaciones de red

### HTTPS

1. API de Keepa: `https://api.keepa.com/`.
2. No hay llamadas a URLs `http://`.
3. La aplicación no configura `usesCleartextTraffic`.

### CORS

En desarrollo (`SPRING_PROFILES_ACTIVE=dev`):

```yaml
cors:
  allow-all: true
```

Acepta cualquier origen, `allowCredentials` deshabilitado.

En producción (`SPRING_PROFILES_ACTIVE=prod`):

```yaml
cors:
  allow-all: false
  allowed-origins:
    - https://miapp.com
    - https://www.miapp.com
```

`allowCredentials: true` solo con orígenes específicos. Headers permitidos: `Authorization`, `Content-Type`, `Accept`, `Origin`, `X-Requested-With`.

### CSRF

Deshabilitado explícitamente en `SecurityConfig`. Es la práctica correcta para APIs REST stateless con JWT en header.

## 5. Endpoints y control de acceso

Mapa de acceso por endpoint:

1. `GET /`: público, bienvenida del API.
2. `POST /api/admin/companies`: ADMIN. Crea empresa, COMPANY_ADMIN y código auto.
3. `POST /api/auth/register`: público. Requiere `companyCode` para unirse a una empresa.
4. `POST /api/auth/login`: público. Devuelve JWT con `companyId` y `userId`.
5. `POST /api/auth/google`: público. Devuelve JWT o `needs_company`.
6. `POST /api/auth/google/complete-new-company`: público. Completa registro Google y crea empresa.
7. `POST /api/auth/google/complete-join`: público. Completa registro Google y une a empresa.
8. `GET /api/health`: público, no expone datos sensibles.
9. `GET /actuator/health`: público.
10. `GET /actuator/info`: público.
11. `GET /api/competitors/status`: EMPLOYEE, COMPANY_ADMIN, ADMIN. Estado Keepa según `companyId` del JWT.
12. `GET /api/auth/profile`: autenticado. Datos del usuario y su empresa.
13. `POST /api/auth/create-employee`: COMPANY_ADMIN, ADMIN.
14. `POST /api/products`: autenticado. Crea producto en la empresa del usuario.
15. `GET /api/products/**`: autenticado. Solo productos de la empresa.
16. `GET /api/analytics/**`: COMPANY_ADMIN, EMPLOYEE, ADMIN.
17. `GET /api/competitors/amazon/**`: autenticado.
18. `* /api/alert-rules/**`: autenticado. CRUD de reglas por empresa.
19. `* /api/users/**`: COMPANY_ADMIN, ADMIN.
20. `* /api/api-keys/**`: COMPANY_ADMIN, ADMIN. CRUD de API keys cifradas.
21. `GET /api/admin/**`: ADMIN. Gestión global.

### Notas sobre Actuator

Solo `/actuator/health` y `/actuator/info` están en `SecurityConfig.PUBLIC_URLS`, sin wildcard. `management.endpoints.web.exposure.include` limita los actuators expuestos a `health, info`. Si se exponen más, sustituir por lista explícita y restringir a `ADMIN` o red interna, sobre todo en `/actuator/env` o `/actuator/beans`.

## 6. Logging

### HTTP

1. En producción no se loguea cuerpo de peticiones ni respuestas.
2. En desarrollo, logging detallado solo en consola local.

```java
HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
interceptor.setLevel(isProduction()
    ? HttpLoggingInterceptor.Level.NONE
    : HttpLoggingInterceptor.Level.BODY);
```

### SQL

1. `show-sql: false` en producción.
2. En desarrollo, `show-sql: true` solo muestra la query, no los parámetros (configurable con `logging.level.org.hibernate.type: WARN`).

### Datos sensibles

1. Las contraseñas no se loguan (`@JsonIgnore`).
2. Las API keys no se loguan en INFO o superior.
3. Las excepciones loguan el mensaje, no el stack trace completo en producción.

## 7. Validación de entradas

Bean Validation (Jakarta) aplicada con `@Valid` en los controladores. Errores de validación devuelven 400 con detalle por campo.

Campos validados en `ProductRequest`:

1. `name`: `@NotBlank`, `@Size(max = 200)`.
2. `currentPrice`: `@NotNull`, `@DecimalMin("0.01")`, `@Digits(integer = 10, fraction = 2)`.
3. `costPrice`: `@DecimalMin("0.00")` si se envía.
4. `sku`: `@Size(max = 50)` si se envía.

Campos validados en `RegisterRequest`:

1. `email`: `@NotBlank`, `@Email`.
2. `username`: `@NotBlank`, `@Size(min = 3, max = 50)`.
3. `password`: `@NotBlank`, `@Size(min = 6)`.

Validaciones de negocio:

1. Unicidad de email y username al registrar.
2. Unicidad de SKU por empresa, no global.
3. Verificación de propiedad de recursos antes de modificarlos.
4. El admin no puede autodesactivarse.

## 8. Integridad de datos

### Transacciones

1. Métodos de servicio que tocan varias entidades usan `@Transactional`.
2. Si una parte falla, toda la operación se revierte.
3. Crear producto y registrar `PriceHistory` van en una sola transacción.

### Concurrencia con Keepa

1. `Semaphore(3)`: máximo 3 peticiones concurrentes.
2. Inicialización del competidor Amazon en `@PostConstruct` (hilo único).
3. Double-checked locking para casos edge en la inicialización.

## 9. Dependencias

Versiones principales:

1. Spring Boot 3.3.0 (LTS, parches de seguridad al día).
2. Spring Security 6.x (incluido en el BOM de Spring Boot).
3. jjwt 0.12.3 (sin CVEs conocidos).
4. PostgreSQL Driver 42.x (incluido en el BOM).
5. Lombok 1.18.40 (solo compile time).
6. Jsoup 1.17.2.

Recomendación: ejecutar `mvn dependency:check` o integrar OWASP Dependency Check en el pipeline para detectar CVEs en dependencias transitivas.

## 10. Configuración de servidor

`application.yml` en producción:

```yaml
server:
  port: 9090

spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        default_batch_fetch_size: 16

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

Docker:

1. La imagen no incluye `.env`.
2. Las variables se inyectan en runtime con `--env-file .env`.
3. El usuario del contenedor no debería ser root (pendiente en Dockerfile).

## Pendientes

Prioridad media:

1. Certificate pinning con `CertificatePinner` de OkHttp para la conexión con Keepa.
2. Restringir `/actuator/**` en producción a ADMIN o red interna.
3. Audit log: registrar en tabla aparte todas las operaciones de admin (quién, qué, cuándo).
4. OWASP Dependency Check integrado en el build.

Prioridad baja:

1. Root detection en cliente móvil.
2. 2FA para cuentas ADMIN.
3. Rotación de `JWT_SECRET` con periodo de gracia para tokens existentes.

## Completadas

1. `JWT_SECRET` validado al arrancar con error fatal en producción.
2. BCrypt con salt automático para contraseñas.
3. CORS por perfil, restrictivo en producción.
4. CORS: `allowCredentials` deshabilitado con wildcard origins.
5. CORS: headers permitidos restringidos a whitelist explícita.
6. CSRF deshabilitado para API REST stateless.
7. Verificación de propiedad de recursos en capa de servicio.
8. Soft-delete sin exponer datos borrados en queries.
9. Admin no puede desactivarse a sí mismo.
10. Token expirado devuelve 401 claro, no 403.
11. Logs de BD sin parámetros sensibles en producción.
12. Variables de entorno para todos los secretos (incluidos `DB_URL` y `DB_USERNAME`).
13. Rate limiting en `/api/auth/login` y `/api/auth/register` (10 intentos por minuto y por IP).
14. Dependencia de Keepa fijada a versión concreta (2.04) en lugar de LATEST.
15. Multi-tenancy: alertas filtran por `companyId` vía JPQL en lugar de `userId`.
16. Prevención N+1: `default_batch_fetch_size: 16` y JOIN FETCH en consultas críticas.
17. Login optimizado: eliminada query redundante a `userRepository.findByEmail()`, datos extraídos de `UserPrincipal`.
18. `UserPrincipal` enriquecido con `companyName`, `role` y `displayUsername`.
19. JWT sin valor por defecto en perfil prod (`${JWT_SECRET}` sin fallback).
20. Paginación validada con `@Min(0)` en page y `@Min(1) @Max(100)` en size en `ProductController` y `AnalyticsController`.
21. `ACCESS_NETWORK_STATE` declarado en `AndroidManifest` para `NetworkObserver`.
22. `AdminController` optimizado con count queries en lugar de `findAll().stream()`.
23. Google OAuth2: validación de `idToken` con verificación de audience, firma y expiración.
24. API keys cifradas con AES-256, nunca expuestas en texto plano.
25. Guardia de auto-eliminación en `UserService`: el usuario no puede eliminarse a sí mismo.

## Documentación relacionada

1. `ARQUITECTURA.md`: arquitectura y justificaciones técnicas.
2. `README.md`: endpoints REST, instalación y configuración.
3. `FLYWAY.md`: migraciones de BD, esquema de seguridad en V1, V5 y V8.
