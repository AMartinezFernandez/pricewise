# PriceWise — API Backend

Sistema de comparación y monitorización de precios para PYMEs.

## Descripción

PriceWise es una API REST desarrollada con Spring Boot que permite a las PYMEs gestionar su catálogo y monitorizar precios de la competencia en Amazon. Incluye autenticación JWT, sistema de roles ADMIN / COMPANY_ADMIN / EMPLOYEE, integración con Keepa API, reglas de alerta configurables con precio objetivo y análisis automático de precios mediante jobs Quartz.

Existe una aplicación Android acompañante construida con Jetpack Compose, cuya documentación está en [`../pricewise-android/README.md`](../pricewise-android/README.md).

## Tecnologías

- Java 17
- Spring Boot 3.2.1
- Spring Security 6.x
- Spring Data JPA 3.x
- PostgreSQL 14+
- JWT (jjwt 0.12.3)
- Quartz Scheduler
- Keepa API
- Lombok
- SpringDoc OpenAPI 2.3.0

## Requisitos

- Java 17 o superior.
- PostgreSQL 14 o superior.
- Maven 3.8+.

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd pricewise-backend
```

### 2. Configurar la base de datos

```bash
psql -U postgres
CREATE DATABASE pricewise_db;
\q
```

### 3. Configurar variables de entorno

```bash
cp .env.example .env
```

Edita el archivo `.env` con tus valores:

```env
JWT_SECRET=tu_clave_secreta_aqui
DB_URL=jdbc:postgresql://localhost:5432/pricewise_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password_de_bd
SPRING_PROFILES_ACTIVE=dev
```

> Las API keys de Keepa se configuran por empresa desde la app (Ajustes → Integración Keepa).

Para generar un `JWT_SECRET` seguro:

```bash
openssl rand -base64 32
```

### 4. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

O compilar y ejecutar:

```bash
mvn clean package
java -jar target/pricewise-1.0.0-SNAPSHOT.jar
```

La aplicación estará disponible en: `http://localhost:9090`.

## URLs importantes

| URL | Descripción |
|---|---|
| `http://localhost:9090/api/health` | Estado del servidor |
| `http://localhost:9090/swagger-ui.html` | Documentación interactiva |
| `http://localhost:9090/api-docs` | OpenAPI JSON |

## Autenticación

La API soporta dos métodos de autenticación: JWT clásico y Google OAuth2.

### Flujo clásico

1. El usuario obtiene un código de invitación de su empresa.
2. `POST /api/auth/register` con credenciales y código.
3. `POST /api/auth/login` para obtener token.
4. Incluir en headers: `Authorization: Bearer <token>`.

### Flujo Google OAuth2

1. `POST /api/auth/google` con el `idToken` de Google Sign-In.
2. Si el usuario ya existe: devuelve JWT directamente.
3. Si es nuevo: devuelve estado `needs_company` con dos opciones:
   - `POST /api/auth/google/complete-new-company` para crear empresa nueva.
   - `POST /api/auth/google/complete-join` con `companyCode` para unirse a empresa existente.

El token expira en 24 horas.

### Obtener token

```http
POST /api/auth/login
Content-Type: application/json

{
    "emailOrUsername": "tu_email@ejemplo.com",
    "password": "tu_contraseña"
}
```

## Roles y permisos

| Rol | Descripción |
|---|---|
| `ADMIN` | Super-administrador: crea empresas y gestiona la plataforma. |
| `COMPANY_ADMIN` | Administrador de empresa: gestiona sus empleados y productos. |
| `EMPLOYEE` | Empleado: gestiona productos de su empresa. |

### Rutas públicas (sin autenticación)

- `POST /api/auth/register` (requiere `companyCode`).
- `POST /api/auth/login`.
- `POST /api/auth/google`.
- `POST /api/auth/google/complete-new-company`.
- `POST /api/auth/google/complete-join`.
- `GET /api/health`.
- `GET /actuator/health`, `GET /actuator/info`.

### Rutas protegidas (requieren token)

- `/api/products/*` — todos los roles.
- `/api/auth/profile` — todos los roles.
- `/api/auth/change-password` — todos los roles.
- `/api/auth/create-employee` — `COMPANY_ADMIN` / `ADMIN`.
- `/api/users/*` — `COMPANY_ADMIN` / `ADMIN`.
- `/api/api-keys/*` — `COMPANY_ADMIN` / `ADMIN`.
- `/api/analytics/*` — todos los roles.
- `/api/alert-rules/*` — todos los roles.
- `/api/competitors/*` — todos los roles (incluye `status`, que devuelve el estado de Keepa por empresa).
- `/api/admin/*` — solo `ADMIN`.

## Endpoints principales

### Autenticación

```
POST /api/auth/register          Registrarse en empresa
POST /api/auth/login             Iniciar sesión
GET  /api/auth/profile           Ver perfil
POST /api/auth/change-password   Cambiar contraseña
POST /api/auth/create-employee   Crear empleado (COMPANY_ADMIN / ADMIN)
```

### Google OAuth

```
POST /api/auth/google                        Login con Google
POST /api/auth/google/complete-new-company   Completar registro creando empresa
POST /api/auth/google/complete-join          Completar registro uniéndose a empresa
```

### Productos (requieren autenticación)

```
POST   /api/products             Crear producto
GET    /api/products             Listar productos (paginado)
GET    /api/products/monitored   Listar productos monitorizados
GET    /api/products/{id}        Obtener producto
PUT    /api/products/{id}        Actualizar producto
DELETE /api/products/{id}        Eliminar producto (soft-delete)
GET    /api/products/search      Buscar productos
GET    /api/products/categories  Listar categorías
GET    /api/products/brands      Listar marcas
GET    /api/products/count       Contar productos
```

### Competencia (Keepa, requieren autenticación)

```
GET  /api/competitors/status                   Estado de Keepa API
GET  /api/competitors/amazon/price/{asin}      Precio por ASIN
POST /api/competitors/amazon/sync/{productId}  Sincronizar producto
```

### Reglas de alerta (requieren autenticación)

```
GET    /api/alert-rules                  Listar reglas de alerta
POST   /api/alert-rules                  Crear regla de alerta
PUT    /api/alert-rules/{id}             Actualizar regla (threshold, name, targetPrice)
DELETE /api/alert-rules/{id}             Eliminar regla
POST   /api/alert-rules/{id}/toggle      Activar / desactivar regla
```

### Administración (solo `ADMIN`)

```
GET    /api/admin/stats                  Estadísticas
GET    /api/admin/users                  Listar usuarios
POST   /api/admin/companies              Crear empresa
GET    /api/admin/users/{id}             Ver usuario
PUT    /api/admin/users/{id}             Editar usuario
PUT    /api/admin/users/{id}/password    Cambiar contraseña
PUT    /api/admin/users/{id}/role        Cambiar rol
PUT    /api/admin/users/{id}/status      Activar / desactivar
DELETE /api/admin/users/{id}             Eliminar usuario
```

### Usuarios (`COMPANY_ADMIN` / `ADMIN`)

```
GET    /api/users                Listar usuarios de la empresa
GET    /api/users/count          Contar usuarios
DELETE /api/users/{userId}       Eliminar usuario de la empresa
```

### API keys (`COMPANY_ADMIN` / `ADMIN`)

```
GET    /api/api-keys             Listar API keys de la empresa
POST   /api/api-keys             Guardar API key (cifrada AES-256)
POST   /api/api-keys/{id}/toggle Activar / desactivar API key
DELETE /api/api-keys/{id}        Eliminar API key
```

### Analytics (requieren autenticación)

```
GET    /api/analytics/dashboard                       Dashboard con métricas
GET    /api/analytics/recommendations                 Listar recomendaciones
POST   /api/analytics/recommendations/{id}/apply      Aplicar recomendación
POST   /api/analytics/recommendations/{id}/dismiss    Descartar recomendación
GET    /api/analytics/alerts                          Listar alertas generadas
POST   /api/analytics/alerts/{id}/read                Marcar alerta como leída
POST   /api/analytics/alerts/read-all                 Marcar todas como leídas
POST   /api/analytics/analyze                         Ejecutar análisis de precios
```

> El `SchedulerController` fue retirado del MVP. Los jobs de Quartz se ejecutan automáticamente sin interfaz REST. Ver [`MEJORAS_FUTURAS.md`](MEJORAS_FUTURAS.md) para el plan de reintegración.

## Formato de respuestas

Éxito:

```json
{
    "success": true,
    "message": "Operación exitosa",
    "data": { },
    "timestamp": "2026-01-26T19:00:00"
}
```

Error:

```json
{
    "success": false,
    "message": "Descripción del error",
    "timestamp": "2026-01-26T19:00:00"
}
```

Códigos HTTP empleados: `200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `500 Internal Server Error`.

## Perfiles de configuración

### Desarrollo (`dev`)

- CORS permisivo.
- Logs detallados.
- SQL queries visibles.
- `ddl-auto: update` sobre PostgreSQL local.

### Producción (`prod`)

- CORS restrictivo (definido por `CORS_ORIGINS`).
- Logs mínimos.
- `ddl-auto: validate` (Flyway gestiona el esquema).

## Seguridad

- Autenticación JWT stateless (clásica y Google OAuth2).
- Google OAuth2: validación del `idToken` contra la API de Google.
- Contraseñas almacenadas con BCrypt.
- API keys de Keepa cifradas con AES-256 en BD (tabla `company_api_keys`).
- CORS configurable por perfil (restrictivo en producción).
- CSRF deshabilitado (apropiado para APIs REST stateless).
- Protección de endpoints por rol con `@PreAuthorize`.
- Rate limiting en login y registro (10 intentos / minuto por IP).
- Todas las credenciales externalizadas en variables de entorno.
- Multi-tenancy: datos aislados por `companyId` en cada petición.

Ver [`SEGURIDAD.md`](SEGURIDAD.md) para el análisis detallado.

Recomendaciones operativas:

- No subir `.env` a Git.
- Usar un `JWT_SECRET` único por entorno.
- Contraseñas de mínimo 6 caracteres.
- Configurar orígenes CORS explícitos en producción.

## Base de datos

### Tablas principales

| Tabla | Descripción |
|---|---|
| `users` | Usuarios del sistema (soporta auth clásica y Google OAuth). |
| `companies` | Empresas con plan y código de invitación. |
| `products` | Productos de cada empresa (soft-delete con campo `active`). |
| `price_history` | Historial de precios propios. |
| `competitors` | Competidores configurados. |
| `competitor_prices` | Precios de Amazon obtenidos vía Keepa. |
| `alerts` | Alertas generadas por el análisis automático. |
| `alert_rules` | Reglas de alerta configuradas por el usuario (threshold, targetPrice). |
| `company_api_keys` | API keys cifradas AES-256 por empresa (Keepa). |
| `audit_logs` | Logs de auditoría (retirada del MVP, tabla existe pero no se usa). |

Ver [`FLYWAY.md`](FLYWAY.md) para el detalle de migraciones (V1-V8).

## Uso con Postman

1. Importar la colección desde `http://localhost:9090/api-docs` (OpenAPI) o desde [`../postman/`](../postman/).
2. Hacer login y guardar el token devuelto.
3. Usar el token en `Authorization: Bearer {{token}}`.

## Docker

```bash
docker build -t pricewise-backend .
docker run -p 9090:9090 --env-file .env pricewise-backend
```

## Comandos útiles

```bash
mvn spring-boot:run                   # Ejecutar en desarrollo
mvn clean package                     # Compilar
mvn test                              # Ejecutar tests
psql -U postgres -d pricewise_db      # Conectar a PostgreSQL
```

## Documentación relacionada

- [`ARQUITECTURA.md`](ARQUITECTURA.md) — Guía completa de arquitectura y justificaciones técnicas.
- [`SEGURIDAD.md`](SEGURIDAD.md) — Informe de seguridad (JWT, OAuth2, cifrado AES-256, RBAC).
- [`FLYWAY.md`](FLYWAY.md) — Migraciones de base de datos V1-V8.
- [`CRONOGRAMA.md`](CRONOGRAMA.md) — Cronograma de desarrollo (30 fases).
- [`MEJORAS_FUTURAS.md`](MEJORAS_FUTURAS.md) — Servicios retirados del MVP y plan de reintegración.

## Autor

Álvaro Martínez Fernández — TFC del ciclo DAM (curso 2025-2026).

## Licencia

Publicado bajo [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](https://creativecommons.org/licenses/by-nc/4.0/). Uso no comercial con atribución al autor. Texto completo en [`../LICENSE`](../LICENSE).
