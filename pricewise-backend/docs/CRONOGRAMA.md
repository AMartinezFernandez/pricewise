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
- OK `User` con roles USER/ADMIN y constraints de unicidad
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
- OK Registro con validacion de unicidad de email y username
- OK Login con generacion de JWT de 24 horas
- OK Endpoint de perfil autenticado
- OK DTOs: `LoginRequest`, `RegisterRequest`, `AuthResponse`

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

## Pendiente

### Fase 13: Testing Completo

#### Tests Unitarios
- [ ] `AuthServiceTest` - registro, login, validaciones de unicidad
- [ ] `ProductServiceTest` - CRUD, historial, calculo de margen
- [ ] `KeepaServiceTest` - manejo de errores, backoff, semaforo
- [ ] `PriceAnalysisServiceTest` - calculo de umbrales y generacion de alertas
- [ ] `JwtServiceTest` - generacion, validacion, expiracion de tokens

#### Tests de Integracion
- [ ] `ProductControllerIntegrationTest` - flujo completo con MockMvc y H2
- [ ] `AuthControllerIntegrationTest` - registro, login, perfil
- [ ] `AdminControllerIntegrationTest` - gestion de usuarios con rol ADMIN
- [ ] Flujo completo: registro -> crear producto -> sync Amazon -> verificar alerta

#### Tests de Repositorio
- [ ] `ProductRepositoryTest` - queries con filtros, paginacion, unicidad SKU
- [ ] `PriceHistoryRepositoryTest` - queries por rango de fecha
- [ ] `AlertRepositoryTest` - queries de alertas no leidas

#### CI/CD
- [ ] Configurar GitHub Actions para builds automatizados en PR
- [ ] Ejecutar tests en cada push a main
- [ ] Analisis de cobertura con JaCoCo

---

### Fase 14: Migraciones de Base de Datos con Flyway

- [ ] Integrar Flyway para gestion de migraciones versionadas
- [ ] Migrar de `ddl-auto: update` a scripts SQL versionados
- [ ] Scripts iniciales V1__create_users.sql, V2__create_products.sql, etc.
- [ ] Documentar proceso de rollback de migraciones

---

### Fase 15: Mejoras de Rendimiento

- [ ] Paginacion en historial de precios (endpoint `GET /api/products/{id}/history`)
- [ ] Limite de registros devueltos por CompetitorPrice (actualmente sin limite)
- [ ] Indices adicionales en `price_history.recorded_at` para queries de rango de fecha
- [ ] Caducidad de datos historicos: TTL configurable para purgar registros antiguos

---

### Fase 16: Nuevas Funcionalidades

#### Alertas y Notificaciones
- [ ] Endpoint `GET /api/alerts` para listar alertas del usuario
- [ ] Endpoint `PUT /api/alerts/{id}/read` para marcar como leida
- [ ] Endpoint `PUT /api/alerts/read-all` para marcar todas como leidas
- [ ] Integracion con email (Spring Mail + plantilla HTML) para alertas CRITICAL

#### Recomendaciones
- [ ] Endpoint `GET /api/recommendations` para listar recomendaciones pendientes
- [ ] Endpoint `PUT /api/recommendations/{id}/apply` para aplicar precio sugerido
- [ ] Endpoint `PUT /api/recommendations/{id}/dismiss` para descartar

#### Historial
- [ ] Endpoint `GET /api/products/{id}/history` con paginacion y filtro por fechas
- [ ] Graficos de evolucion de precio (datos para el frontend)
- [ ] Comparativa historica precio propio vs competencia

#### Competidores Adicionales
- [ ] Implementar scraping con Jsoup para competidores sin API
- [ ] UI de configuracion de competidores (MANUAL, SCRAPING)
- [ ] Soporte para multiples locales de Amazon por usuario

---

### Fase 17: Escalabilidad

#### Cache Distribuida
- [ ] Migrar Simple Cache a Redis para persistir cache entre reinicios
- [ ] TTL configurable por tipo de cache
- [ ] Cache de respuestas de Keepa para no repetir misma consulta en 1 hora

#### Rate Limiting
- [ ] Limitar peticiones por usuario a endpoints costosos (Keepa sync)
- [ ] Bucket4j o similar para rate limiting por IP y por usuario

#### Observabilidad
- [ ] Metricas custom de Micrometer para peticiones Keepa (exito, fallo, latencia)
- [ ] Exportacion de metricas a Prometheus
- [ ] Dashboard Grafana con KPIs del sistema
- [ ] Tracing distribuido con Zipkin

---

### Fase 18: Seguridad Avanzada

- [ ] Certificate pinning para la API de Keepa con OkHttp CertificatePinner
- [ ] Audit log: registrar todas las operaciones ADMIN en tabla separada
- [ ] 2FA opcional para cuentas ADMIN
- [ ] Rotacion automatica de JWT_SECRET con periodo de gracia
- [ ] OWASP dependency check integrado en el build de Maven

---

## Estadisticas del Proyecto

| Metrica                  | Valor    |
|--------------------------|----------|
| Lineas de codigo Java    | ~5.200   |
| Archivos .java           | ~47      |
| Entidades JPA            | 7        |
| Repositorios             | 7        |
| Servicios                | 4        |
| Controladores            | 6        |
| DTOs                     | ~20      |
| Endpoints REST           | ~30      |
| APIs externas            | 1 (Keepa)|
| Bugs resueltos           | 22       |
| Version BD (DDL)         | update   |
