# Mejoras Futuras — Servicios eliminados del MVP

Servicios y configuraciones eliminados para simplificar la entrega del MVP.
Todos estaban parcialmente implementados y pueden reintegrarse en futuras iteraciones.

---

## 1. Sistema de Auditoria (AuditService)

**Archivos eliminados:**
- `service/AuditService.java`
- `entity/AuditLog.java`
- `repository/AuditLogRepository.java`
- `test/service/AuditServiceTest.java`

**Que hacia:**
- Registraba acciones administrativas (crear empresa, eliminar usuario, etc.)
- Guardaba en tabla `audit_logs`: usuario, accion, entidad, IP, timestamp
- Se invocaba desde `AdminController`

**Para reintegrar:**
1. Recrear entidad `AuditLog`, repositorio y servicio
2. Inyectar `AuditService` en `AdminController`
3. Llamar `auditService.logAction()` en endpoints de admin (createCompany, deleteUser, etc.)
4. Considerar endpoint GET `/api/admin/audit-logs` con paginacion

---

## 2. Servicio de Email (EmailService)

**Archivos eliminados:**
- `service/EmailService.java`
- `test/service/EmailServiceTest.java`

**Que hacia:**
- Enviaba emails via SMTP (alertas de precio, bienvenida, etc.)
- Usaba `@ConditionalOnProperty(name = "pricewise.email.enabled")` — ya era opcional
- Templates HTML embebidos

**Para reintegrar:**
1. Añadir dependencia `spring-boot-starter-mail` en `pom.xml`
2. Recrear `EmailService` con `@ConditionalOnProperty`
3. Configurar SMTP en `application.yml` (host, port, username, password)
4. Integrar con `NotificationService` para enviar emails en alertas criticas

---

## 3. Servicio de Notificaciones y WebSocket (NotificationService)

**Archivos eliminados:**
- `service/NotificationService.java`
- `config/WebSocketConfig.java`
- `test/service/NotificationServiceTest.java`

**Que hacia:**
- Notificaciones en tiempo real via WebSocket (STOMP sobre SockJS)
- `PriceAnalysisService` enviaba notificacion al crear alertas
- Canal: `/topic/alerts/{userId}`
- Dependia opcionalmente de `EmailService`

**Para reintegrar:**
1. Añadir dependencia `spring-boot-starter-websocket` en `pom.xml`
2. Recrear `WebSocketConfig` (MessageBroker, STOMP endpoints)
3. Recrear `NotificationService` con inyeccion de `SimpMessagingTemplate`
4. En `PriceAnalysisService.createAlert()`, llamar `notificationService.sendAlertNotification()`
5. En Android: integrar cliente WebSocket (OkHttp o Scarlet)

---

## 4. Limpieza automatica de datos (DataCleanupService)

**Archivos eliminados:**
- `service/DataCleanupService.java`
- `test/service/DataCleanupServiceTest.java`

**Que hacia:**
- Job programado que limpiaba datos antiguos:
  - Alertas leidas con mas de X dias
  - Recomendaciones aplicadas/descartadas antiguas
  - Precios de competencia obsoletos
- Configuracion: `pricewise.cleanup.alert-retention-days`, etc.

**Para reintegrar:**
1. Recrear servicio con `@Scheduled` o job Quartz
2. Añadir configuracion de retencion en `application.yml`
3. Queries de borrado por fecha en los repositorios correspondientes

---

## 5. Documentacion API (OpenAPI/Swagger)

**Archivos eliminados:**
- `config/OpenApiConfig.java`

**Que hacia:**
- Configuraba Swagger UI en `/swagger-ui.html`
- Documentacion automatica de todos los endpoints REST
- Info del proyecto, version, contacto

**Para reintegrar:**
1. Añadir dependencia `springdoc-openapi-starter-webmvc-ui` en `pom.xml`
2. Recrear `OpenApiConfig` con `@OpenAPIDefinition`
3. Añadir URLs de Swagger a `PUBLIC_URLS` en `SecurityConfig`
4. Anotar endpoints con `@Operation`, `@ApiResponse`, etc. (opcional)

---

## 6. Controlador de Scheduler (SchedulerController)

**Archivos eliminados:**
- `controller/SchedulerController.java`

**Que hacia:**
- Endpoints admin para gestionar jobs de Quartz manualmente
- Pausar/reanudar/disparar jobs via API REST

**Para reintegrar:**
1. Recrear controlador con endpoints POST para trigger/pause/resume
2. Proteger con `@PreAuthorize("hasRole('ROLE_ADMIN')")`
3. Inyectar `org.quartz.Scheduler`

---

## 7. Cache con Redis (RedisCacheConfig)

**Archivos eliminados:**
- `config/RedisCacheConfig.java`

**Anotaciones eliminadas de ProductService:**
- `@Cacheable(value = "categories", key = "#companyId")` en `getCategories()`
- `@Cacheable(value = "brands", key = "#companyId")` en `getBrands()`
- `@CacheEvict(value = {"categories", "brands"}, key = "#companyId")` en `createProduct()` y `updateProduct()`

**Para reintegrar:**
1. Añadir dependencias `spring-boot-starter-cache` y `spring-boot-starter-data-redis`
2. Recrear `RedisCacheConfig` con TTLs por cache
3. Configurar Redis en `application.yml` (host, port, password)
4. Re-añadir `@Cacheable`/`@CacheEvict` en `ProductService`
5. Considerar cache para `KeepaService` (rate limiting natural)

---

## 8. Configuracion Async (AsyncConfig)

**Archivos eliminados:**
- `config/AsyncConfig.java`
- `test/config/AsyncConfigTest.java`

**Que hacia:**
- Habilitaba `@Async` con `@EnableAsync`
- ThreadPool configurado: coreSize=2, maxSize=5, queueCapacity=100
- Prefix: `PriceWise-Async-`

**Para reintegrar:**
1. Recrear `AsyncConfig` con `@EnableAsync` y `ThreadPoolTaskExecutor`
2. Usar `@Async` en servicios que se beneficien (email, notificaciones, scraping)

---

## 9. Metricas con Prometheus

**Dependencia eliminada de pom.xml:**
- `micrometer-registry-prometheus`

**Para reintegrar:**
1. Añadir dependencia `micrometer-registry-prometheus`
2. En `application.yml`, exponer endpoint: `management.endpoints.web.exposure.include: health,info,prometheus,metrics`
3. Configurar Prometheus para scrapear `/actuator/prometheus`
4. Opcional: dashboard Grafana

---

## Servicios que se MANTUVIERON en el MVP

| Servicio | Razon |
|----------|-------|
| Google Sign-In | Ya implementado end-to-end, valor critico para UX |
| MetricsConfig | 1 archivo, metricas basicas utiles |
| RateLimitingFilter | Seguridad basica contra fuerza bruta |
| Gestion de usuarios y roles | Ya integrada en Android (CreateUserScreen) |
| KeepaService | Core del negocio — precios de Amazon |
| PriceAnalysisService | Core — recomendaciones y alertas |
| Quartz Scheduler | Core — jobs programados de scraping |
