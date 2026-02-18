PriceWise Backend

Sistema de comparacion y monitorizacion de precios para PYMEs.


DESCRIPCION

PriceWise es una API REST desarrollada con Spring Boot que permite a las PYMEs gestionar sus productos y monitorear precios de la competencia. Incluye autenticacion JWT, sistema de roles USER/ADMIN, integracion con Keepa API para precios de Amazon, y documentacion Swagger.


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
KEEPA_API_KEY=tu_api_key_de_keepa

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

La API usa JWT para autenticacion.

Flujo:
1. El usuario obtiene un codigo de invitacion de su empresa.
2. POST /api/auth/register con credenciales y codigo.
3. POST /api/auth/login para obtener token.
4. Incluir en headers: Authorization: Bearer <token>

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
- POST /api/auth/register (requiere code)
- POST /api/auth/login
- GET /api/health
- GET /api/competitors/status

Rutas protegidas (requieren token):
- /api/products/* (Todos los roles)
- /api/auth/profile (Todos los roles)
- /api/admin/* (solo ADMIN)


ENDPOINTS PRINCIPALES

Autenticacion:
POST /api/auth/register          Registrarse en empresa
POST /api/auth/login             Iniciar sesion
GET  /api/auth/profile           Ver perfil

Productos (requieren autenticacion):
POST   /api/products             Crear producto
GET    /api/products             Listar productos (paginado)
GET    /api/products/{id}        Obtener producto
PUT    /api/products/{id}        Actualizar producto
DELETE /api/products/{id}        Eliminar producto
GET    /api/products/search      Buscar productos
GET    /api/products/categories  Listar categorias
GET    /api/products/brands      Listar marcas
GET    /api/products/count       Contar productos

Competencia (Keepa):
GET  /api/competitors/status                   Estado de Keepa API
GET  /api/competitors/amazon/price/{asin}      Precio por ASIN
POST /api/competitors/amazon/sync/{productId}  Sincronizar producto

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

Scheduler (solo ADMIN):
GET  /api/admin/scheduler/status       Estado del scheduler
POST /api/admin/scheduler/trigger-now  Ejecutar job ahora
POST /api/admin/scheduler/pause        Pausar scheduler
POST /api/admin/scheduler/resume       Reanudar scheduler


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

- Autenticacion JWT stateless
- Contraseñas hasheadas con BCrypt
- CORS configurable por perfil (restrictivo en produccion)
- CSRF deshabilitado (apropiado para APIs REST)
- Proteccion de endpoints por rol
- Rate limiting en login y registro (10 intentos/minuto por IP)
- Todas las credenciales externalizadas a variables de entorno

Recomendaciones:
- No subir .env a Git
- Usar JWT_SECRET unico en produccion
- Contraseñas de minimo 6 caracteres
- Configurar origenes CORS en produccion


BASE DE DATOS

Tablas:
- users: Usuarios del sistema
- products: Productos de cada usuario
- price_history: Historial de precios
- competitors: Competidores configurados
- competitor_prices: Precios de la competencia


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


AUTOR

Alvaro Martinez
- **Email**: admin@pricewise.io


LICENCIA

Proyecto privado de uso exclusivo para PriceWise.
