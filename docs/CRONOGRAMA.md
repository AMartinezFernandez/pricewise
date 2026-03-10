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

> **Nota:** Las metricas Prometheus/Micrometer se implementaron y probaron, pero se
> retiraron del MVP para simplificar el despliegue. La configuracion base de Actuator
> (health, info) permanece activa. Ver `MEJORAS_FUTURAS.md` para reintegracion.

- OK Metricas custom de Micrometer para peticiones Keepa (exito, fallo, latencia)
- OK `MetricsConfig` con contadores y timers registrados en Prometheus
- OK Exportacion de metricas via `/actuator/prometheus`
- OK Endpoints actuator expuestos: health, info, prometheus, metrics
- RETIRADO del MVP — se mantiene solo Actuator basico (health, info)

---

## Fase 21: Seguridad Avanzada (2026-02-22)

> **Nota:** `AuditService`, `AuditLog` y su migracion V3 se implementaron y probaron,
> pero se retiraron del MVP por complejidad adicional. El rate limiting y OWASP check
> permanecen activos. Ver `MEJORAS_FUTURAS.md` para reintegracion.

- OK Audit log: entidad `AuditLog`, `AuditService`, `AuditLogRepository`
- OK Migracion `V3__create_audit_logs.sql` para tabla audit_logs
- OK Logging automatico en operaciones de `AdminController` (createCompany, deleteUser)
- OK Endpoint `GET /api/admin/audit-logs` con paginacion
- OK 2 tests unitarios para AuditService
- RETIRADO del MVP — AuditService y entidad eliminados, migracion V3 se mantiene
- OK OWASP dependency-check plugin integrado en Maven (failBuildOnCVSS >= 8)
- OK Rate limiting en `/api/auth/login` y `/api/auth/register` (RateLimitingFilter existente)

---

## Fase 22: Cache Distribuida con Redis (2026-02-22)

> **Nota:** Redis se implemento y probo correctamente, pero se retiro del MVP para
> eliminar la dependencia de infraestructura externa. La cache en memoria con
> `ConcurrentHashMap` (Fase 5) cubre las necesidades del MVP.
> Ver `MEJORAS_FUTURAS.md` para reintegracion.

- OK Dependencia `spring-boot-starter-data-redis` (Lettuce client)
- OK `RedisCacheConfig.java` con TTL 1 hora, serializacion JSON, prefix `pricewise::`
- OK Configuracion Redis en `application.yml` (`spring.cache.type: redis`)
- OK Servicio Redis 7 Alpine en `docker-compose.yml` con healthcheck y volumen persistente
- OK Tests aislados de Redis: `@ConditionalOnProperty` + exclusion de auto-config en perfiles test/dev
- RETIRADO del MVP — vuelto a cache en memoria (ConcurrentHashMap)

---

## Fase 23: Reglas de Alerta Configurables (2026-02-22)

### Backend
- OK Entidad `AlertRule` con company, product (opcional), alertType, threshold, enabled
- OK Migracion `V5__create_alert_rules.sql` con tabla e indices
- OK `AlertRuleRepository` con queries por empresa y reglas aplicables
- OK `AlertRuleService` con CRUD completo y multi-tenancy
- OK `AlertRuleController` en `/api/alert-rules` (GET, POST, PUT, DELETE, toggle)
- OK `AlertRuleDTOs` con request/response

### Motor de Analisis
- OK `PriceAnalysisService` consulta reglas configuradas por empresa/producto
- OK Fallback a umbrales por defecto si no hay reglas
- OK Precedencia determinista: reglas globales (product=NULL) antes que especificas

### Android
- OK Modelos `AlertRuleResponse`, `CreateAlertRuleRequest`, `UpdateAlertRuleRequest`
- OK 5 endpoints en `PriceWiseApi.kt`
- OK Metodos en `AnalyticsRepository.kt`
- OK `AlertsScreen` con dos tabs: "Mis alertas" (reglas CRUD) + "Historial" (alertas generadas)
- OK Dialogo de creacion con selector de producto, tipo y umbral

---

## Fase 24: Limpieza de Codigo Muerto (2026-02-23)

### Android
- OK Eliminados 5 archivos muertos: AlertRulesScreen, AlertRulesViewModel, RecommendationsScreen, RecommendationsViewModel, ProductListScreen
- OK Limpieza de rutas obsoletas en NavGraph (Screen.Products, Screen.AddProduct, Screen.Recommendations, Screen.ProductDetail)
- OK Eliminacion de helpers sin uso en Utils.kt (priorityLabel, recommendationTypeLabel, alertTypeLabel)
- OK Limpieza de imports y dependencias sin uso en DashboardScreen y TrackingScreen

### Backend
- OK Eliminados metodos muertos en 9 repositorios (~40 metodos userId-based y sin uso)
- OK Eliminado `loadUserById()` de CustomUserDetailsService
- OK Eliminados DTOs sin uso: PriceHistoryPoint, ProductPriceComparison, ProductSearchCriteria
- OK Eliminado metodo duplicado `getUnreadAlertsByCompany()` en PriceAnalysisService
- OK Limpieza de imports sin uso en AdminController

### Documentacion
- OK README.md actualizado (SchedulerController eliminado, roles actualizados)
- OK CRONOGRAMA.md con notas sobre fases retiradas del MVP

---

## Fase 25: Correcciones Criticas MVP (2026-02-23)

### Android
- OK Splash Screen con logica de autenticacion (navega a Login o Main segun token)
- OK Offline Banner con NetworkObserver (ConnectivityManager + callbackFlow)
- OK Permiso ACCESS_NETWORK_STATE en AndroidManifest.xml
- OK ProGuard/R8 habilitado para release (minify + shrink resources)
- OK Reglas ProGuard para Retrofit, Moshi, Hilt, Compose, DataStore, Google Sign-In
- OK Tema oscuro forzado (darkTheme = true) — evita texto invisible en modo claro del sistema
- OK Mejora contraste DarkColorScheme: onSurface blanco, onSurfaceVariant #CFD8DC, outline #90A4AE
- OK Permitir acentos en campo de usuario del login (KeyboardType.Text en vez de Email)

### Backend
- OK Paginacion validada con @Min(0)/@Min(1)/@Max(100) en ProductController y AnalyticsController
- OK JWT sin default en perfil prod (app falla si JWT_SECRET no configurado)
- OK AdminController optimizado: count queries en vez de findAll().stream() para stats
- OK Tests de AdminController actualizados para nuevos count methods

---

## Fase 26: Correcciones MVP Pendientes (2026-02-23)

### Android
- OK Colores Switch (cyan PwCyan) y papelera (rojo coral #EF5350) en AlertsScreen
- OK Lint desactivado como tarea bloqueante (workaround AGP 8.5.2 + Java 25)
- OK TrackingScreen filtra solo productos monitorizados (usa nuevo endpoint /api/products/monitored)
- OK Eliminado codigo muerto: MainViewModel.logout() y parametro onLogout de MainScreen
- OK NavGraph actualizado para no pasar onLogout a MainScreen

### Backend
- OK Nuevo endpoint GET /api/products/monitored (company-scoped, paginado)
- OK ProductRepository: findByCompanyIdAndMonitoringEnabledTrueAndActiveTrue()
- OK ProductService.getMonitoredProducts() con paginacion
- OK Eliminado H2Dialect explicito de application-test.yml (auto-detectado por Hibernate)

---

## Fase 27: Edicion de Productos y Recomendaciones (2026-03-04)

### Android
- OK Pantalla de edicion de producto (EditProductScreen) con formulario pre-rellenado
- OK ProductDetailViewModel y ProductFormViewModel separados
- OK Navegacion: detalle → editar → guardar → volver a detalle
- OK RecommendationsScreen con lista de recomendaciones por producto

### Backend
- OK Endpoint PUT /api/products/{id} con deteccion de cambio de precio
- OK PriceHistory INCREASE/DECREASE registrado automaticamente al actualizar precio

---

## Fase 28: Google OAuth2 y API Keys por Empresa (2026-03-07 - 2026-03-09)

### Backend — Google OAuth2
- OK GoogleTokenService con validacion de ID tokens de Google
- OK 3 endpoints: /api/auth/google, /api/auth/google/complete-new-company, /api/auth/google/complete-join
- OK Campo authProvider (LOCAL/GOOGLE) en entidad User
- OK Password aleatoria BCrypt para usuarios Google (no usable para login local)

### Backend — API Keys por Empresa
- OK Entidad CompanyApiKey con cifrado AES-256-GCM (ApiKeyEncryptionService)
- OK Migracion V8: tabla company_api_keys con UNIQUE(company_id, provider)
- OK CRUD completo: guardar, listar (enmascaradas), toggle, eliminar
- OK KeepaService lee API key desde CompanyApiKeyService en vez de config global

### Android
- OK Google Sign-In con Credential Manager
- OK Pantalla de configuracion de empresa (nueva empresa o unirse con codigo)
- OK Pantalla de ajustes con gestion de API key Keepa (guardar, ver enmascarada, toggle)
- OK Selector de moneda y tema oscuro en ajustes

---

## Fase 29: Reorganizacion de Documentacion (2026-03-09)

- OK Migracion de coleccion Postman a formato YAML (archivos individuales por request)
- OK Documentos movidos de subdirectorios a `docs/` raiz
- OK Eliminacion de archivo duplicado arquitectura_1.md (fusionado en ARQUITECTURA.md)
- OK FLYWAY.md actualizado con migraciones V7 y V8
- OK README.md actualizado: Google OAuth, API Keys, usuarios, analytics, productos monitorizados
- OK SEGURIDAD.md v1.2: Google OAuth2, cifrado API keys AES-256

---

## Fase 30: Revision de Arquitectura (2026-03-10)

### Correcciones Criticas
- OK Filtro `active=true` faltante en queries de categorias y marcas (soft-delete leak)

### Optimizaciones (N+1)
- OK AdminService: JOIN FETCH en findAllWithUsers/findByIdWithUsers
- OK PriceMonitorJob: JOIN FETCH company en findMonitoredProductsWithCompany

### Refactorizacion SRP
- OK ProductViewModel dividido en SearchViewModel, ProductDetailViewModel, ProductFormViewModel
- OK GoogleAuthService extraido de AuthService (3 metodos Google OAuth + helper)
- OK ProductResponse inmutable con factory method fromEntity(Product, CompetitorPrice)

### Testing
- OK 34 nuevos tests: AlertRuleServiceTest (12), CompanyApiKeyServiceTest (12), ApiKeyEncryptionServiceTest (10)
- OK Total backend: 204 tests, 0 fallos

---

## Futuro (Diferido)

### Funcionalidades
- [ ] Integracion con email (Spring Mail) para alertas CRITICAL
- [ ] Comparativa historica precio propio vs competencia (graficos)
- [ ] Scraping con Jsoup para competidores sin API
- [ ] Soporte para multiples locales de Amazon por empresa

### Escalabilidad
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

| Metrica                  | Valor               |
|--------------------------|---------------------|
| Entidades JPA            | 10                  |
| Repositorios             | 10                  |
| Servicios                | 11                  |
| Controladores            | 8                   |
| DTOs                     | ~32                 |
| Endpoints REST           | ~55                 |
| APIs externas            | 1 (Keepa) + Google OAuth2 |
| Tests backend            | 204                 |
| Bugs resueltos           | 39+                 |
| Roles de usuario         | 3                   |
| Migraciones Flyway       | 8                   |
| Cache                    | En memoria (ConcurrentHashMap) |
| Version BD (DDL)         | validate            |
