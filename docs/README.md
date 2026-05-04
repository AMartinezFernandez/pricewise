# PriceWise, API backend

Sistema de comparación y monitorización de precios para PYMEs.

API REST en Spring Boot que permite a las PYMEs gestionar su catálogo y monitorizar precios en Amazon. Incluye autenticación JWT, roles `ADMIN`, `COMPANY_ADMIN` y `EMPLOYEE`, integración con la API de Keepa, reglas de alerta configurables con precio objetivo y análisis automático de precios mediante jobs de Quartz.

Existe una app Android acompañante en Jetpack Compose: ver `../pricewise-android/README.md`.

## Tecnologías

1. Java 17.
2. Spring Boot 3.3.0.
3. Spring Security 6.x.
4. Spring Data JPA 3.x.
5. PostgreSQL 14 o superior.
6. JWT (jjwt 0.12.3).
7. Quartz Scheduler.
8. Keepa API.
9. Lombok.
10. SpringDoc OpenAPI 2.3.0.

## Requisitos

1. Java 17 o superior.
2. PostgreSQL 14 o superior.
3. Maven 3.8 o superior.

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

### 3. Variables de entorno

```bash
cp .env.example .env
```

Editar `.env` con tus valores:

```env
JWT_SECRET=tu_clave_secreta_aqui
DB_URL=jdbc:postgresql://localhost:5432/pricewise_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password_de_bd
SPRING_PROFILES_ACTIVE=dev
```

`SPRING_PROFILES_ACTIVE=dev` es importante: el perfil por defecto de la aplicación es `prod` (con `ddl-auto=validate` y CORS restrictivo), poco práctico para desarrollo local. La carga del `.env` la hace `spring-dotenv` automáticamente al arrancar.

Las API keys de Keepa se configuran por empresa desde la app, en Ajustes e Integración Keepa.

Para generar un `JWT_SECRET` seguro:

```bash
openssl rand -base64 32
```

### 4. Ejecutar

```bash
mvn spring-boot:run
```

O empaquetar y ejecutar:

```bash
mvn clean package -DskipTests
java -jar target/pricewise-1.0.0-SNAPSHOT.jar
```

`-DskipTests` evita ejecutar los 206 tests en el primer build; varios son de integración (`@SpringBootTest`) y requieren la BD configurada. Para correrlos: `mvn test` con la base de datos activa.

Disponible en `http://localhost:9090`.

## URLs útiles

1. `http://localhost:9090/api/health`: estado del servidor.
2. `http://localhost:9090/swagger-ui.html`: documentación interactiva.
3. `http://localhost:9090/api-docs`: OpenAPI JSON.

## Autenticación

La API soporta JWT clásico y Google OAuth2.

### Flujo clásico

1. El usuario obtiene el código de invitación de su empresa.
2. `POST /api/auth/register` con credenciales y código.
3. `POST /api/auth/login` para obtener el token.
4. Incluir en cada petición: `Authorization: Bearer <token>`.

### Flujo Google OAuth2

1. `POST /api/auth/google` con el `idToken` de Google Sign-In.
2. Si el usuario existe, devuelve JWT.
3. Si es nuevo, devuelve `needs_company` con dos opciones:
   1. `POST /api/auth/google/complete-new-company` para crear empresa nueva.
   2. `POST /api/auth/google/complete-join` con `companyCode` para unirse a una existente.

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

## Roles

1. `ADMIN`: super-administrador. Crea empresas y gestiona la plataforma.
2. `COMPANY_ADMIN`: administrador de empresa. Gestiona empleados y productos de su empresa.
3. `EMPLOYEE`: empleado. Gestiona productos de su empresa.

### Rutas públicas

1. `POST /api/auth/register` (requiere `companyCode`).
2. `POST /api/auth/login`.
3. `POST /api/auth/google`.
4. `POST /api/auth/google/complete-new-company`.
5. `POST /api/auth/google/complete-join`.
6. `GET /api/health`.
7. `GET /actuator/health`, `GET /actuator/info`.

### Rutas protegidas

1. `/api/products/*`: todos los roles.
2. `/api/auth/profile`: todos los roles.
3. `/api/auth/change-password`: todos los roles.
4. `/api/auth/create-employee`: `COMPANY_ADMIN` y `ADMIN`.
5. `/api/users/*`: `COMPANY_ADMIN` y `ADMIN`.
6. `/api/api-keys/*`: `COMPANY_ADMIN` y `ADMIN`.
7. `/api/analytics/*`: todos los roles.
8. `/api/alert-rules/*`: todos los roles.
9. `/api/competitors/*`: todos los roles. Incluye `status`, que devuelve el estado de Keepa por empresa.
10. `/api/admin/*`: solo `ADMIN`.

## Endpoints

### Autenticación

```
POST /api/auth/register          Registrarse en empresa
POST /api/auth/login             Iniciar sesión
GET  /api/auth/profile           Ver perfil
POST /api/auth/change-password   Cambiar contraseña
POST /api/auth/create-employee   Crear empleado (COMPANY_ADMIN, ADMIN)
```

### Google OAuth

```
POST /api/auth/google                        Login con Google
POST /api/auth/google/complete-new-company   Completar registro creando empresa
POST /api/auth/google/complete-join          Completar registro uniéndose a empresa
```

### Productos

```
POST   /api/products             Crear producto
GET    /api/products             Listar productos paginado
GET    /api/products/monitored   Listar productos monitorizados
GET    /api/products/{id}        Obtener producto
PUT    /api/products/{id}        Actualizar producto
DELETE /api/products/{id}        Eliminar producto (soft-delete)
GET    /api/products/search      Buscar productos
GET    /api/products/categories  Listar categorías
GET    /api/products/brands      Listar marcas
GET    /api/products/count       Contar productos
```

### Competencia (Keepa)

```
GET  /api/competitors/status                   Estado de Keepa
GET  /api/competitors/amazon/price/{asin}      Precio por ASIN
POST /api/competitors/amazon/sync/{productId}  Sincronizar producto
```

### Reglas de alerta

```
GET    /api/alert-rules                  Listar reglas
POST   /api/alert-rules                  Crear regla
PUT    /api/alert-rules/{id}             Actualizar regla (threshold, name, targetPrice)
DELETE /api/alert-rules/{id}             Eliminar regla
POST   /api/alert-rules/{id}/toggle      Activar o desactivar regla
```

### Administración (solo ADMIN)

```
GET    /api/admin/stats                  Estadísticas
GET    /api/admin/users                  Listar usuarios
POST   /api/admin/companies              Crear empresa
GET    /api/admin/users/{id}             Ver usuario
PUT    /api/admin/users/{id}             Editar usuario
PUT    /api/admin/users/{id}/password    Cambiar contraseña
PUT    /api/admin/users/{id}/role        Cambiar rol
PUT    /api/admin/users/{id}/status      Activar o desactivar
DELETE /api/admin/users/{id}             Eliminar usuario
```

### Usuarios (COMPANY_ADMIN, ADMIN)

```
GET    /api/users                Listar usuarios de la empresa
GET    /api/users/count          Contar usuarios
DELETE /api/users/{userId}       Eliminar usuario de la empresa
```

### API keys (COMPANY_ADMIN, ADMIN)

```
GET    /api/api-keys             Listar API keys de la empresa
POST   /api/api-keys             Guardar API key (cifrada AES-256)
POST   /api/api-keys/{id}/toggle Activar o desactivar API key
DELETE /api/api-keys/{id}        Eliminar API key
```

### Analytics

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

Los jobs de Quartz se ejecutan automáticamente en segundo plano sin interfaz REST.

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

Códigos HTTP usados: 200, 201, 400, 401, 403, 404, 500.

## Perfiles

Desarrollo (`dev`):

1. CORS permisivo.
2. Logs detallados.
3. SQL queries visibles.
4. `ddl-auto: update` sobre PostgreSQL local.

Producción (`prod`):

1. CORS restrictivo definido por `CORS_ORIGINS`.
2. Logs mínimos.
3. `ddl-auto: validate`. Flyway gestiona el esquema.

## Seguridad

1. JWT stateless, clásico y Google OAuth2.
2. Google OAuth2: validación del `idToken` contra la API de Google.
3. Contraseñas con BCrypt.
4. API keys de Keepa cifradas con AES-256 en `company_api_keys`.
5. CORS configurable por perfil, restrictivo en producción.
6. CSRF deshabilitado, apropiado para APIs REST stateless.
7. Protección por rol con `@PreAuthorize`.
8. Rate limiting en login y registro (10 intentos por minuto y por IP).
9. Credenciales externalizadas en variables de entorno.
10. Multi-tenancy: datos aislados por `companyId` en cada petición.

Análisis detallado en `SEGURIDAD.md`.

Recomendaciones operativas:

1. No subir `.env` a Git.
2. Usar un `JWT_SECRET` único por entorno.
3. Contraseñas mínimo 6 caracteres.
4. Configurar orígenes CORS explícitos en producción.

## Base de datos

Tablas principales:

1. `users`: usuarios del sistema. Soporta auth clásica y Google OAuth.
2. `companies`: empresas con plan y código de invitación.
3. `products`: productos de cada empresa, soft-delete con `active`.
4. `price_history`: historial de precios propios.
5. `competitors`: competidores configurados.
6. `competitor_prices`: precios de Amazon obtenidos por Keepa.
7. `alerts`: alertas generadas por el análisis automático.
8. `alert_rules`: reglas configuradas por el usuario (threshold, targetPrice).
9. `company_api_keys`: API keys cifradas AES-256 por empresa.
10. `audit_logs`: tabla creada en BD pero no utilizada por el código actual.

Detalle de migraciones V1 a V8 en `FLYWAY.md`.

## Uso con Postman

1. Importar la colección desde `http://localhost:9090/api-docs` (OpenAPI) o desde `../postman/`.
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

1. `ARQUITECTURA.md`: arquitectura y justificaciones técnicas.
2. `SEGURIDAD.md`: informe de seguridad (JWT, OAuth2, cifrado, RBAC).
3. `PATRONES.md`: patrones internos y reglas para evitar regresiones.
4. `FLYWAY.md`: migraciones V1 a V8.
5. `MEJORAS_FUTURAS.md`: líneas de evolución previstas para iteraciones posteriores.

## Autor

Álvaro Martínez Fernández. TFC del ciclo DAM, curso 2025-2026.

## Licencia

Publicado bajo Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0). Uso no comercial con atribución al autor. Texto completo en `../LICENSE`.
