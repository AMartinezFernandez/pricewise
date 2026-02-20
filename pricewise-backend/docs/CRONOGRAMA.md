# Cronograma de Desarrollo - PriceWise Backend

## Timeline del Proyecto

Este documento registra cronologicamente todas las fases de desarrollo de PriceWise Backend.

---

## Fase 1: Configuracion Inicial (2026-01-25)

### Estructura del Proyecto
- OK Creacion del proyecto Spring Boot con Java 17
- OK Configuracion de Maven con pom.xml y gestion de versiones
- OK Setup de Hilt... de Spring IoC para inyeccion de dependencias
- OK Configuracion de PostgreSQL con Docker Compose
- OK Setup de Lombok, SpringDoc OpenAPI, DevTools

### Archivos Creados
```
src/main/
├── PriceWiseApplication.java           # @SpringBootApplication, @EnableScheduling, @EnableCaching
├── config/SecurityConfig.java          # Cadena de filtros Spring Security (esqueleto)
├── config/OpenApiConfig.java           # Configuracion Swagger
└── resources/application.yml          # Configuracion de servidor, BD, JWT
docker-compose.yml                      # PostgreSQL 14 en contenedor
.env.example                            # Variables de entorno de ejemplo
```

---

## Fase 2: Capa de Datos - Entidades y Repositorios (2026-01-26 - 2026-01-28)

### Entidades JPA
- OK `User` con roles COMPANY_ADMIN/EMPLOYEE/ADMIN y constraints de unicidad
- OK `Company` con nombre, tipo de negocio, plan y limites
- OK `Product` con SKU, EAN, precio, coste y campo monitoringEnabled
- OK `PriceHistory` con tipos INITIAL, INCREASE, DECREASE, NO_CHANGE
- OK `Competitor` con sourceType API/SCRAPING/MANUAL
- OK `CompetitorPrice` con precio, disponibilidad y calculo de diferencia
- OK `Alert` con tipos y severidades
- OK `PriceRecommendation` con prioridades y estados

### Repositorios Spring Data JPA
- OK `UserRepository` con queries por email, username
- OK `ProductRepository` con paginacion, busqueda y filtros
- OK `PriceHistoryRepository` con queries por rango de fechas
- OK `CompetitorRepository`
- OK `CompetitorPriceRepository` con queries por producto y competidor
- OK `AlertRepository` con queries de no leidas por usuario
- OK `PriceRecommendationRepository` con queries de pendientes

### Correccion de Bug
- Bug #1: DDL auto cambiado de create-drop a update para no perder datos

---

## Fase 3: Autenticacion con JWT (2026-01-27)

### Seguridad
- OK `JwtService` con generacion y validacion de tokens HMAC-SHA256
- OK `JwtAuthenticationFilter` que intercepta cada peticion HTTP
- OK `UserPrincipal` como objeto de autenticacion en SecurityContext
- OK `UserDetailsServiceImpl` que carga usuarios desde BD
- OK `SecurityConfig` con cadena de filtros completa, CORS y BCrypt

### AuthService y AuthController
- OK Registro de usuarios con código de invitación (`companyCode`)
- OK Validación de código de empresa y asignación automática
- OK Endpoints de creación de empresas (Admin) y empleados (Company Admin)
- OK Login con generación de JWT de 24 horas
- OK DTOs actualizados para flujo de invitación

### Correcciones
- Bug #2: Fuga de entidad User en respuesta de login, creado `AuthResponse` DTO
- Bug #6: Validacion de JWT_SECRET al arrancar con warning en dev y error en prod

---

## Fase 4: CRUD de Productos (2026-01-28 - 2026-01-29)

### ProductService y ProductController
- OK Crear producto con registro automatico de PriceHistory INITIAL
- OK Listar productos con paginacion y ordenacion
- OK Detalle de producto por ID
- OK Actualizar con deteccion de cambio de precio y registro INCREASE/DECREASE
- OK Borrado logico (soft delete, campo active = false)
- OK Busqueda con filtros por nombre, categoria, marca
- OK Endpoints de categorias y marcas del usuario
- OK Endpoint de conteo total

### DTOs
- OK `ProductRequest` / `ProductUpdateRequest` con validaciones Jakarta
- OK `ProductResponse` con campo calculado margin
- OK `SearchRequest` con parametros de filtro y paginacion
- OK `PageResponse` para respuestas paginadas estandar

### Correcciones
- Bug #3: Unicidad de SKU por usuario, no global
- Bug #4: Validacion de precio > 0 en actualizacion
- Bug #7: N+1 queries resuelto con FetchType.LAZY y EntityGraph
- Bug #13: getMargin() protegido ante division por cero
- Bug #15: DELETE cambiado a soft delete
- Bug #20: Productos inactivos excluidos de todas las queries de busqueda
- Bug #22: ChangeType INITIAL no sobreescrito en actualizaciones sin cambio de precio

---

## Fase 5: Cache y Rendimiento (2026-02-03)

### Cache en Memoria
- OK Spring Cache con `ConcurrentHashMap` para categorias y marcas por usuario
- OK `@Cacheable` en `getProductCategories()` y `getProductBrands()`
- OK `@CacheEvict` en create, update y delete de productos
- OK Tests de que la cache se invalida correctamente

### Correcciones
- Bug #8: Cache no se invalidaba al borrar producto, anadido @CacheEvict en delete

---

## Fase 6: Integracion Keepa API (2026-02-04)

### KeepaService
- OK Configuracion mediante `KeepaConfig` con propiedades desde YAML
- OK Llamada asincrona a Keepa API con `CompletableFuture`
- OK Semaforo de 3 peticiones concurrentes maximas
- OK Backoff exponencial: espera 1s, 2s, 4s entre reintentos
- OK Soporte para locales ES, US, DE, FR, UK, IT, CA, JP
- OK Creacion automatica del competidor Amazon en BD al primer uso
- OK Guardado de `CompetitorPrice` con precio, disponibilidad y URL

### CompetitorController
- OK `GET /api/competitors/status` para verificar disponibilidad de Keepa (publico)
- OK `GET /api/competitors/amazon/price/{asin}` para consultar precio por ASIN
- OK `POST /api/competitors/amazon/sync/{productId}` para sincronizar producto

### AsyncConfig
- OK Executor dedicado "keepaExecutor" con pool de 3 a 10 hilos
- OK `@EnableAsync` en la configuracion

### Correcciones
- Bug #9: Sync de Amazon rediseado para responder inmediatamente (async)
- Bug #10: Race condition en creacion de Amazon Competitor resuelto con @PostConstruct
- Bug #14: NPE en getPriceDifferencePercentage() corregido con null checks

---

## Fase 7: Motor de Analisis de Precios (2026-02-05)

### PriceAnalysisService
- OK Umbral HIGH_PRICE_THRESHOLD = 10% para detectar precio propio caro
- OK Umbral LOW_PRICE_THRESHOLD = 10% para detectar oportunidad de margen
- OK Umbral SUDDEN_CHANGE_THRESHOLD = 15% para alertas de cambio brusco
- OK Generacion de `PriceRecommendation` con tipo, precio sugerido y prioridad
- OK Generacion de `Alert` con severidad INFO/WARNING/CRITICAL
- OK Analisis por lotes de todos los productos de un usuario

### Integracion con Scheduler
- OK PriceMonitorJob llama a analisis tras actualizar precios de competidores
- OK Analisis solo se ejecuta si hubo cambios en precios de competencia

### Correcciones
- Bug #11: Quartz configurado con misfireHandlingInstructionFireAndProceed
- Bug #12: Todas las queries garantizan filtro por userId

---

## Fase 8: Scheduler Quartz (2026-02-05)

### PriceMonitorJob
- OK Cron expression: cada 6 horas (`0 0 */6 * * ?`)
- OK Filtra productos con monitoringEnabled = true y SKU formato ASIN (B0...)
- OK Procesamiento en lotes de 50 con 1 segundo de pausa entre lotes
- OK CompletableFuture.allOf() para procesar lote en paralelo
- OK Estadisticas de ejecucion en logs (productos procesados, errores)

### SchedulerConfig
- OK JobDetail y Trigger configurados como Spring Beans
- OK Exposicion de estado del scheduler via `/api/admin/scheduler/*`
- OK Endpoints para pausar, reanudar y ejecutar manualmente

### Correcciones
- Bug #18: Circuit breaker manual: aborta el job tras 5 errores consecutivos
- Bug #21: Pool de conexiones aumentado a 20 y transacciones divididas por producto

---

## Fase 9: Panel de Administracion (2026-02-06)

### AdminController y AdminService
- OK `GET /api/admin/stats` - estadisticas globales del sistema
- OK `GET /api/admin/dashboard` - metricas con desglose por categoria
- OK CRUD de usuarios: listar, detalle, actualizar, cambiar password, rol, estado
- OK `DELETE /api/admin/users/{id}` - borrado fisico permanente

### DTOs Admin
- OK `AdminStatsResponse` con totales de usuarios, productos, alertas
- OK `UserDetailResponse` con historial de actividad
- OK `UserUpdateRequest` con validaciones

### Correcciones
- Bug #16: Admin no puede desactivar su propia cuenta

---

## Fase 10: Manejo de Excepciones y Validaciones (2026-02-06)

### GlobalExceptionHandler
- OK `BadRequestException` -> 400 Bad Request
- OK `ResourceNotFoundException` -> 404 Not Found
- OK `MethodArgumentNotValidException` -> 400 con mapa de errores por campo
- OK `AccessDeniedException` -> 403 Forbidden
- OK Excepciones no controladas -> 500 con mensaje generico

### Formato Estandar de Respuesta
- OK `ApiResponse<T>` con campos success, message, data, timestamp
- OK `PageResponse<T>` con campos de paginacion estandar

### Correcciones
- Bug #17: JWT expirado devuelve 401 con mensaje claro en lugar de 403 generico

---

## Fase 11: Seguridad y Hardening (2026-02-07 - 2026-02-08)

### Mejoras de Seguridad
- OK JWT_SECRET validado al arrancar: warning en dev, error fatal en prod
- OK CORS configurado por perfil: permisivo en dev, lista blanca en prod
- OK `allowBackup` desactivado (no aplica a backend, aplicado en cliente si lo hubiera)
- OK HTTP logging de OkHttp condicional a BuildConfig / perfil activo

### Correcciones
- Bug #19: BCrypt hash no se loguea en SQL de debug

---

## Fase 12: Documentacion y OpenAPI (2026-02-09)

### OpenAPI / Swagger
- OK `OpenApiConfig.java` con titulo, version, descripcion y contacto
- OK Anotaciones `@Operation`, `@ApiResponse` en endpoints principales
- OK `@Schema` en DTOs principales
- OK Coleccion Postman exportada: `PriceWise_API.postman_collection.json`
- OK README.md con instalacion, configuracion y ejemplos de uso

### Documentacion tecnica
- OK `docs/ARQUITECTURA.md` - guia completa de arquitectura
- OK `docs/BUGS_Y_SOLUCIONES.md` - registro de incidencias
- OK `docs/CRONOGRAMA.md` - timeline de desarrollo
- OK `docs/SEGURIDAD.md` - informe de seguridad

---

## Fase 13: Multi-Tenancy y RBAC (2026-02-12 - 2026-02-13)

### Modelo Multi-Empresa
- OK Entidad `Company` con nombre, tipo de negocio, plan y limites
- OK Migracion de datos: tabla `companies`, FK `company_id` en `users` y `products`
- OK Aislamiento de datos por `companyId` en vez de `userId`
- OK JWT incluye `companyId` ademas de `userId` y roles
- OK `UserPrincipal` con metodo `getCompanyId()` para acceso al contexto de empresa

### Sistema de Roles (RBAC)
- OK Tres roles: `ADMIN` (super-admin), `COMPANY_ADMIN` (admin de empresa), `EMPLOYEE`
- OK `AdminController` restringido a `ADMIN`
- OK `AnalyticsController` restringido a `COMPANY_ADMIN`, `EMPLOYEE` y `ADMIN`
- OK `ProductController` accesible a cualquier usuario autenticado (aislado por empresa)

### Gestion de Empleados
- OK Endpoint `POST /api/auth/create-employee` para crear empleados
- OK Restringido a `COMPANY_ADMIN` y `ADMIN` via `@PreAuthorize`
- OK DTO `CreateEmployeeRequest` con validaciones
- OK `AuthService.createEmployee()` asigna rol EMPLOYEE y empresa del admin

### Correcciones
- Bug #23: NPE en login por lazy-loading de Company, resuelto con LEFT JOIN FETCH
- Bug #24: AnalyticsController usaba userId en vez de companyId en 3 metodos
- Bug #25: Creado endpoint de creacion de empleados que no existia

### Archivos Creados / Modificados
```
entity/Company.java                    # [MOD] Added companyCode
repository/CompanyRepository.java      # [MOD] findByCompanyCode
dto/auth/AuthDTOs.java                 # [MOD] RegisterRequest, CreateCompanyRequest
service/AuthService.java               # [MOD] register linked to company, createCompany
controller/AdminController.java        # [MOD] POST /api/admin/companies
```

---

## Fase 14: Registro con Código de Empresa y Testing (2026-02-13)

### Sistema de Invitación por Código
- OK Campo `companyCode` (UUID 8 chars) en entidad `Company`
- OK Endpoint `POST /api/admin/companies` para crear empresas (Solo ADMIN)
- OK Registro de usuarios requiere `companyCode` para unirse a empresa existente
- OK Asignación automática de rol `EMPLOYEE` al registrarse
- OK Validación de código de empresa existente y activo

### Actualización Android
- OK `RegisterScreen` solicita código de empresa obligatorio
- OK Validación de formato de código en tiempo real
- OK Eliminado campo "Nombre de negocio" y "Tipo" del registro móvil
- OK Conexión con nueva API de registro

### Testing Completo (Backend)
- OK Ejecución exitosa de 139 tests unitarios e integración
- OK `AuthServiceTest`: Cobertura de registro con código, login, creación empleados
- OK `AuthControllerTest`: Validación de endpoints de auth
- OK `AdminControllerTest`: Gestión de usuarios y empresas
- OK Mocking de dependencias (`AuthService`) en tests de controladores

---

---

## Fase 15: Estadísticas y Datos de Prueba (2026-02-14)

### Dashboard Admin
- OK Endpoint `GET /api/admin/dashboard` mejorado con conteos de empresas y usuarios
- OK `DashboardStatsDTO` incluye desglose de empresas activas y total de empleados
- OK Integración con `CompanyRepository.countByActive(true)`

### Datos de Prueba (Seeding)
- OK `DatabaseSeeder` genera 3 empresas ficticias si no existen:
  - Tech Solutions (TECH001)
  - Global Retail (RETAIL01)
  - Consulting Pro (CONSULT1)
- OK Cada empresa creada con 1 Company Admin y 3 Empleados
- OK Contraseñas predefinidas (`password123`) para facilitar pruebas manuales

### Correcciones
- Bug #26: Error de columna `company_code` faltante resuelto con recreación de esquema
- Bug #27: Error 400 en Login desde Postman por nombre de campo incorrecto
- Bug #28: Error de compilación en `AdminController` por dependencia faltante
- Bug #29: Error de sintaxis JSON en colección Postman
- Bug #30: Eliminación de archivo `index.json` obsoleto para evitar confusión

---


---

## Fase 16: Refinamiento de UI y Mejora de UX (2026-02-16)

### Mejoras en Cliente Android
- OK **Refresco Manual:** Botón de actualizar en listado de productos.
- OK **Auto-Refresco Dashboard:** Actualización de métricas al entrar en la pantalla.
- OK **Creación de Usuarios:**
    - Selector de empresas para Global Admin.
    - Visualización de empresa para Company Admin.
    - **Fix:** Modelo de datos `CreateEmployeeRequest` corregido (`companyId`, `role`).
- OK **Feedback Autenticación:** Mensajes de error más claros (401 vs sesión expirada).
- OK **Correcciones:** Autofill de ASIN en creación de producto desde búsqueda Keepa.

### Estado
- Bugs resueltos: 39 (Total acumulado)
- Documentación actualizada.

---

## Fase 17: Migraciones de Base de Datos con Flyway (2026-02-22)

- OK Integrar Flyway para gestion de migraciones versionadas
- OK Migrar de `ddl-auto: update` a `ddl-auto: validate` con scripts SQL
- OK `V1__baseline.sql` con DDL completo de las 8 tablas e indices
- OK `baseline-on-migrate: true` para bases de datos existentes
- OK Flyway deshabilitado en perfil test (H2 con create-drop)
- OK Documentado en `docs/FLYWAY.md`

---

## Fase 18: Mejoras de Rendimiento (2026-02-22)

- OK Indices compuestos en `competitor_prices(product_id, scraped_at)` y `price_history(product_id, recorded_at)` via `V2__add_composite_indexes.sql`
- OK `DataCleanupService` con limpieza TTL programada (diario a las 3 AM)
- OK Retencion configurable: competitor_prices 365 dias, price_history 730 dias
- OK 2 tests unitarios para DataCleanupService

---

## Fase 19: Nuevas Funcionalidades (2026-02-22)

### Alertas y Notificaciones
- OK Endpoint `GET /api/analytics/alerts` para listar alertas de la empresa
- OK Endpoint `POST /api/analytics/alerts/{id}/read` para marcar como leida
- OK Endpoint `POST /api/analytics/alerts/read-all` para marcar todas como leidas
- [ ] Integracion con email (Spring Mail + plantilla HTML) para alertas CRITICAL

### Recomendaciones
- OK Endpoint `GET /api/analytics/recommendations` para listar recomendaciones pendientes
- OK Endpoint `POST /api/analytics/recommendations/{id}/apply` para aplicar precio sugerido
- OK Endpoint `POST /api/analytics/recommendations/{id}/dismiss` para descartar

### Historial de Precios
- OK `PriceHistoryController` con 3 endpoints:
  - `GET /api/products/{id}/history` con paginacion y filtro por fechas
  - `GET /api/products/{id}/history/recent` ultimas 10 entradas
- OK `PriceHistoryDTOs` con mapeo desde entidad
- OK 6 tests unitarios para PriceHistoryController

---

## Fase 20: Escalabilidad — Observabilidad (2026-02-22)

### Observabilidad
- OK Metricas custom de Micrometer para peticiones Keepa (exito, fallo, latencia)
- OK `MetricsConfig` con contadores y timers registrados en Prometheus
- OK Exportacion de metricas via `/actuator/prometheus`
- OK Endpoints actuator expuestos: health, info, prometheus, metrics

---

## Fase 21: Seguridad Avanzada (2026-02-22)

- OK Audit log: entidad `AuditLog`, `AuditService`, `AuditLogRepository`
- OK Migracion `V3__create_audit_logs.sql` para tabla audit_logs
- OK Logging automatico en operaciones de `AdminController` (createCompany, deleteUser)
- OK Endpoint `GET /api/admin/audit-logs` con paginacion
- OK 2 tests unitarios para AuditService
- OK OWASP dependency-check plugin integrado en Maven (failBuildOnCVSS >= 8)
- OK Rate limiting en `/api/auth/login` y `/api/auth/register` (RateLimitingFilter existente)

---

## Futuro (Diferido)

### Funcionalidades
- [ ] Integracion con email (Spring Mail) para alertas CRITICAL
- [ ] Comparativa historica precio propio vs competencia (graficos)
- [ ] Scraping con Jsoup para competidores sin API
- [ ] Soporte para multiples locales de Amazon por empresa

### Escalabilidad
- [ ] Migrar Simple Cache a Redis para persistir cache entre reinicios
- [ ] Cache de respuestas de Keepa (1 hora TTL)
- [ ] Bucket4j para rate limiting avanzado por IP y usuario
- [ ] Dashboard Grafana con KPIs del sistema
- [ ] Tracing distribuido con Zipkin

### Seguridad
- [ ] Certificate pinning para la API de Keepa
- [ ] 2FA opcional para cuentas ADMIN y COMPANY_ADMIN
- [ ] Rotacion automatica de JWT_SECRET con periodo de gracia

---

## Estadisticas del Proyecto

| Metrica                  | Valor      |
|--------------------------|------------|
| Lineas de codigo Java    | ~7.100     |
| Archivos .java           | ~55        |
| Entidades JPA            | 9          |
| Repositorios             | 9          |
| Servicios                | 7          |
| Controladores            | 7          |
| DTOs                     | ~28        |
| Endpoints REST           | ~40        |
| APIs externas            | 1 (Keepa)  |
| Tests unitarios          | 155        |
| Bugs resueltos           | 39         |
| Roles de usuario         | 3          |
| Migraciones Flyway       | 3          |
| Version BD (DDL)         | validate   |
