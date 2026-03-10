PriceWise Backend

Sistema de comparacion y monitorizacion de precios para PYMEs.


DESCRIPCION

PriceWise es una API REST desarrollada con Spring Boot que permite a las PYMEs gestionar sus productos y monitorear precios de la competencia. Incluye autenticacion JWT, sistema de roles ADMIN/COMPANY_ADMIN/EMPLOYEE, integracion con Keepa API para precios de Amazon, sistema de alertas configurables con precio objetivo, y aplicacion Android con Jetpack Compose.


TECNOLOGIAS

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


REQUISITOS

- Java 17 o superior
- PostgreSQL 14 o superior
- Maven 3.8+


INSTALACION

1. Clonar el repositorio

git clone <url-del-repositorio>
cd pricewise-backend


2. Configurar la base de datos

psql -U postgres
CREATE DATABASE pricewise_db;
\q


3. Configurar variables de entorno

cp .env.example .env

Editar el archivo .env con tus valores:

JWT_SECRET=tu_clave_secreta_aqui
DB_URL=jdbc:postgresql://localhost:5432/pricewise_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password_de_bd
SPRING_PROFILES_ACTIVE=dev

> Las API keys de Keepa se configuran por empresa desde la app (Ajustes > Integracion Keepa).

Para generar un JWT_SECRET seguro: openssl rand -base64 32


4. Ejecutar la aplicacion

mvn spring-boot:run

O compilar y ejecutar:

mvn clean package
java -jar target/pricewise-1.0.0-SNAPSHOT.jar

La aplicacion estara disponible en: http://localhost:9090


URLS IMPORTANTES

http://localhost:9090/api/health         Estado del servidor
http://localhost:9090/swagger-ui.html    Documentacion interactiva
http://localhost:9090/api-docs           OpenAPI JSON


AUTENTICACION

La API soporta dos metodos de autenticacion: JWT clasico y Google OAuth2.

Flujo clasico:
1. El usuario obtiene un codigo de invitacion de su empresa.
2. POST /api/auth/register con credenciales y codigo.
3. POST /api/auth/login para obtener token.
4. Incluir en headers: Authorization: Bearer <token>

Flujo Google OAuth2:
1. POST /api/auth/google con el idToken de Google Sign-In.
2. Si el usuario ya existe: devuelve JWT directamente.
3. Si es nuevo: devuelve estado needs_company con opciones:
   a. POST /api/auth/google/complete-new-company para crear empresa nueva.
   b. POST /api/auth/google/complete-join con companyCode para unirse a empresa existente.

El token expira en 24 horas.


Obtener Token:

POST /api/auth/login
Content-Type: application/json

{
    "emailOrUsername": "tu_email@ejemplo.com",
    "password": "tu_contraseña"
}


ROLES Y PERMISOS

ADMIN         - Super-admin, crea empresas y gestiona la plataforma.
COMPANY_ADMIN - Admin de empresa, gestiona sus empleados y productos.
EMPLOYEE      - Empleado, gestiona productos de su empresa.

Rutas publicas:
- POST /api/auth/register (requiere companyCode)
- POST /api/auth/login
- POST /api/auth/google
- POST /api/auth/google/complete-new-company
- POST /api/auth/google/complete-join
- GET /api/health
- GET /api/competitors/status

Rutas protegidas (requieren token):
- /api/products/* (Todos los roles)
- /api/auth/profile (Todos los roles)
- /api/auth/change-password (Todos los roles)
- /api/auth/create-employee (COMPANY_ADMIN/ADMIN)
- /api/users/* (COMPANY_ADMIN/ADMIN)
- /api/api-keys/* (COMPANY_ADMIN/ADMIN)
- /api/analytics/* (COMPANY_ADMIN/EMPLOYEE/ADMIN)
- /api/alert-rules/* (Todos los roles)
- /api/admin/* (solo ADMIN)


ENDPOINTS PRINCIPALES

Autenticacion:
POST /api/auth/register          Registrarse en empresa
POST /api/auth/login             Iniciar sesion
GET  /api/auth/profile           Ver perfil
POST /api/auth/change-password   Cambiar contrasena
POST /api/auth/create-employee   Crear empleado (COMPANY_ADMIN/ADMIN)

Google OAuth:
POST /api/auth/google                    Login con Google (devuelve estado: existing_user, new_user, needs_company)
POST /api/auth/google/complete-new-company   Completar registro Google creando empresa
POST /api/auth/google/complete-join          Completar registro Google uniendose a empresa

Productos (requieren autenticacion):
POST   /api/products             Crear producto
GET    /api/products             Listar productos (paginado)
GET    /api/products/monitored   Listar productos monitorizados
GET    /api/products/{id}        Obtener producto
PUT    /api/products/{id}        Actualizar producto
DELETE /api/products/{id}        Eliminar producto (soft-delete)
GET    /api/products/search      Buscar productos
GET    /api/products/categories  Listar categorias
GET    /api/products/brands      Listar marcas
GET    /api/products/count       Contar productos

Competencia (Keepa):
GET  /api/competitors/status                   Estado de Keepa API
GET  /api/competitors/amazon/price/{asin}      Precio por ASIN
POST /api/competitors/amazon/sync/{productId}  Sincronizar producto

Reglas de Alertas (requieren autenticacion):
GET    /api/alert-rules                  Listar reglas de alerta
POST   /api/alert-rules                  Crear regla de alerta
PUT    /api/alert-rules/{id}             Actualizar regla (threshold, name, targetPrice)
DELETE /api/alert-rules/{id}             Eliminar regla
POST   /api/alert-rules/{id}/toggle      Activar/desactivar regla

Administracion (solo ADMIN):
GET    /api/admin/stats                  Estadisticas
GET    /api/admin/users                  Listar usuarios
POST   /api/admin/companies              Crear empresa
GET    /api/admin/users/{id}             Ver usuario
PUT    /api/admin/users/{id}             Editar usuario
PUT    /api/admin/users/{id}/password    Cambiar contraseña
PUT    /api/admin/users/{id}/role        Cambiar rol
PUT    /api/admin/users/{id}/status      Activar/desactivar
DELETE /api/admin/users/{id}             Eliminar usuario

Usuarios (COMPANY_ADMIN/ADMIN):
GET    /api/users                Listar usuarios de la empresa
GET    /api/users/count          Contar usuarios
DELETE /api/users/{userId}       Eliminar usuario de la empresa

API Keys (COMPANY_ADMIN/ADMIN):
GET    /api/api-keys             Listar API keys de la empresa
POST   /api/api-keys             Guardar API key (cifrada AES-256)
POST   /api/api-keys/{id}/toggle Activar/desactivar API key
DELETE /api/api-keys/{id}        Eliminar API key

Analytics (requieren autenticacion):
GET    /api/analytics/dashboard            Dashboard con metricas
GET    /api/analytics/recommendations      Listar recomendaciones
POST   /api/analytics/recommendations/{id}/apply    Aplicar recomendacion
POST   /api/analytics/recommendations/{id}/dismiss  Descartar recomendacion
GET    /api/analytics/alerts               Listar alertas generadas
POST   /api/analytics/alerts/{id}/read     Marcar alerta como leida
POST   /api/analytics/alerts/read-all      Marcar todas como leidas
POST   /api/analytics/analyze              Ejecutar analisis de precios

Nota: El SchedulerController fue eliminado del MVP.
Los jobs de Quartz se ejecutan automaticamente sin interfaz REST.
Ver MEJORAS_FUTURAS.md para reintegracion.


FORMATO DE RESPUESTAS

Exito:
{
    "success": true,
    "message": "Operacion exitosa",
    "data": { ... },
    "timestamp": "2026-01-26T19:00:00"
}

Error:
{
    "success": false,
    "message": "Descripcion del error",
    "timestamp": "2026-01-26T19:00:00"
}

Codigos HTTP: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Server Error


PERFILES DE CONFIGURACION

Desarrollo (dev):
- CORS permisivo
- Logs detallados
- SQL queries visibles

Produccion (prod):
- CORS restrictivo
- Logs minimos
- DDL en modo validate


SEGURIDAD

- Autenticacion JWT stateless (clasica y Google OAuth2)
- Google OAuth2: validacion de idToken via Google API
- Contraseñas hasheadas con BCrypt
- API keys de Keepa cifradas con AES-256 en BD (tabla company_api_keys)
- CORS configurable por perfil (restrictivo en produccion)
- CSRF deshabilitado (apropiado para APIs REST)
- Proteccion de endpoints por rol con @PreAuthorize
- Rate limiting en login y registro (10 intentos/minuto por IP)
- Todas las credenciales externalizadas a variables de entorno
- Multi-tenancy: datos aislados por companyId en cada peticion

Ver SEGURIDAD.md para analisis detallado.

Recomendaciones:
- No subir .env a Git
- Usar JWT_SECRET unico en produccion
- Contraseñas de minimo 6 caracteres
- Configurar origenes CORS en produccion


BASE DE DATOS

Tablas:
- users: Usuarios del sistema (soporta auth clasica y Google OAuth)
- companies: Empresas con plan y codigo de invitacion
- products: Productos de cada empresa (soft-delete con campo active)
- price_history: Historial de precios propios
- competitors: Competidores configurados
- competitor_prices: Precios de Amazon via Keepa
- alerts: Alertas generadas automaticamente por analisis de precios
- alert_rules: Reglas de alerta configuradas por usuario (threshold, targetPrice)
- company_api_keys: API keys cifradas AES-256 por empresa (Keepa)
- audit_logs: Logs de auditoria (retirada del MVP, tabla existe pero no se usa)

Ver FLYWAY.md para detalle de migraciones (V1-V8).


USO CON POSTMAN

1. Importar coleccion desde http://localhost:9090/api-docs
2. Hacer login y guardar el token
3. Usar el token en Authorization: Bearer {{token}}


DOCKER

docker build -t pricewise-backend .
docker run -p 9090:9090 --env-file .env pricewise-backend


COMANDOS UTILES

mvn spring-boot:run          Ejecutar en desarrollo
mvn clean package            Compilar
mvn test                     Ejecutar tests
psql -U postgres -d pricewise_db         Conectar a PostgreSQL


DOCUMENTACION RELACIONADA

- [ARQUITECTURA.md](ARQUITECTURA.md) — Guia completa de arquitectura y justificaciones tecnicas
- [SEGURIDAD.md](SEGURIDAD.md) — Informe de seguridad (JWT, OAuth2, cifrado AES-256, RBAC)
- [FLYWAY.md](FLYWAY.md) — Migraciones de base de datos V1-V8
- [CRONOGRAMA.md](CRONOGRAMA.md) — Timeline de desarrollo (30 fases)
- [BUGS_Y_SOLUCIONES.md](BUGS_Y_SOLUCIONES.md) — Registro de incidencias resueltas
- [MEJORAS_FUTURAS.md](MEJORAS_FUTURAS.md) — Servicios retirados del MVP y plan de reintegracion


AUTOR

Alvaro Martinez
- **Email**: admin@pricewise.io


LICENCIA

Proyecto privado de uso exclusivo para PriceWise.
