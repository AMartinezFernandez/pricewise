# Mejoras futuras — Servicios eliminados del MVP

Servicios y configuraciones eliminados para simplificar la entrega del MVP. Todos estaban parcialmente implementados y pueden reintegrarse en futuras iteraciones.

---

## 1. Sistema de auditoría (AuditService)

**Archivos eliminados:**
- `service/AuditService.java`
- `entity/AuditLog.java`
- `repository/AuditLogRepository.java`
- `test/service/AuditServiceTest.java`

**Qué hacía:**
- Registraba acciones administrativas (crear empresa, eliminar usuario, etc.).
- Guardaba en la tabla `audit_logs`: usuario, acción, entidad, IP, timestamp.
- Se invocaba desde `AdminController`.

**Para reintegrar:**
1. Recrear entidad `AuditLog`, repositorio y servicio.
2. Inyectar `AuditService` en `AdminController`.
3. Llamar `auditService.logAction()` en los endpoints de admin (`createCompany`, `deleteUser`, etc.).
4. Considerar endpoint `GET /api/admin/audit-logs` con paginación.

---

## 2. Servicio de email (EmailService)

**Archivos eliminados:**
- `service/EmailService.java`
- `test/service/EmailServiceTest.java`

**Qué hacía:**
- Enviaba emails vía SMTP (alertas de precio, bienvenida, etc.).
- Usaba `@ConditionalOnProperty(name = "pricewise.email.enabled")` — ya era opcional.
- Plantillas HTML embebidas.

**Para reintegrar:**
1. Añadir dependencia `spring-boot-starter-mail` en `pom.xml`.
2. Recrear `EmailService` con `@ConditionalOnProperty`.
3. Configurar SMTP en `application.yml` (host, port, username, password).
4. Integrar con `NotificationService` para enviar emails en alertas críticas.

---

## 3. Servicio de notificaciones y WebSocket (NotificationService)

**Archivos eliminados:**
- `service/NotificationService.java`
- `config/WebSocketConfig.java`
- `test/service/NotificationServiceTest.java`

**Qué hacía:**
- Notificaciones en tiempo real vía WebSocket (STOMP sobre SockJS).
- `PriceAnalysisService` enviaba notificación al crear alertas.
- Canal: `/topic/alerts/{userId}`.
- Dependía opcionalmente de `EmailService`.

**Para reintegrar:**
1. Añadir dependencia `spring-boot-starter-websocket` en `pom.xml`.
2. Recrear `WebSocketConfig` (MessageBroker, STOMP endpoints).
3. Recrear `NotificationService` con inyección de `SimpMessagingTemplate`.
4. En `PriceAnalysisService.createAlert()`, llamar `notificationService.sendAlertNotification()`.
5. En Android: integrar cliente WebSocket (OkHttp o Scarlet).

---

## 4. Limpieza automática de datos (DataCleanupService)

**Archivos eliminados:**
- `service/DataCleanupService.java`
- `test/service/DataCleanupServiceTest.java`

**Qué hacía:**
- Job programado que limpiaba datos antiguos:
  - Alertas leídas con más de X días.
  - Recomendaciones aplicadas o descartadas antiguas.
  - Precios de competencia obsoletos.
- Configuración: `pricewise.cleanup.alert-retention-days`, etc.

**Para reintegrar:**
1. Recrear servicio con `@Scheduled` o job Quartz.
2. Añadir la configuración de retención en `application.yml`.
3. Consultas de borrado por fecha en los repositorios correspondientes.

---

## 5. Documentación API (OpenAPI / Swagger)

**Archivos eliminados:**
- `config/OpenApiConfig.java`

**Qué hacía:**
- Configuraba Swagger UI en `/swagger-ui.html`.
- Documentación automática de todos los endpoints REST.
- Información del proyecto, versión y contacto.

**Para reintegrar:**
1. Añadir dependencia `springdoc-openapi-starter-webmvc-ui` en `pom.xml`.
2. Recrear `OpenApiConfig` con `@OpenAPIDefinition`.
3. Añadir las URLs de Swagger a `PUBLIC_URLS` en `SecurityConfig`.
4. Anotar endpoints con `@Operation`, `@ApiResponse`, etc. (opcional).

---

## 6. Controlador de Scheduler (SchedulerController)

**Archivos eliminados:**
- `controller/SchedulerController.java`

**Qué hacía:**
- Endpoints de administración para gestionar jobs de Quartz manualmente.
- Pausar, reanudar o disparar jobs vía API REST.

**Para reintegrar:**
1. Recrear el controlador con endpoints `POST` para `trigger`, `pause` y `resume`.
2. Proteger con `@PreAuthorize("hasRole('ROLE_ADMIN')")`.
3. Inyectar `org.quartz.Scheduler`.

---

## 7. Caché con Redis (RedisCacheConfig)

**Archivos eliminados:**
- `config/RedisCacheConfig.java`

**Anotaciones eliminadas de `ProductService`:**
- `@Cacheable(value = "categories", key = "#companyId")` en `getCategories()`.
- `@Cacheable(value = "brands", key = "#companyId")` en `getBrands()`.
- `@CacheEvict(value = {"categories", "brands"}, key = "#companyId")` en `createProduct()` y `updateProduct()`.

**Para reintegrar:**
1. Añadir dependencias `spring-boot-starter-cache` y `spring-boot-starter-data-redis`.
2. Recrear `RedisCacheConfig` con TTLs por caché.
3. Configurar Redis en `application.yml` (host, port, password).
4. Volver a añadir `@Cacheable` / `@CacheEvict` en `ProductService`.
5. Considerar caché para `KeepaService` (rate limiting natural).

---

## 8. Configuración asíncrona (AsyncConfig)

**Archivos eliminados:**
- `config/AsyncConfig.java`
- `test/config/AsyncConfigTest.java`

**Qué hacía:**
- Habilitaba `@Async` con `@EnableAsync`.
- ThreadPool configurado: `coreSize=2`, `maxSize=5`, `queueCapacity=100`.
- Prefijo: `PriceWise-Async-`.

**Para reintegrar:**
1. Recrear `AsyncConfig` con `@EnableAsync` y `ThreadPoolTaskExecutor`.
2. Usar `@Async` en servicios que se beneficien (email, notificaciones, scraping).

---

## 9. Métricas con Prometheus

**Dependencia eliminada de `pom.xml`:**
- `micrometer-registry-prometheus`.

**Para reintegrar:**
1. Añadir la dependencia `micrometer-registry-prometheus`.
2. En `application.yml`, exponer el endpoint: `management.endpoints.web.exposure.include: health,info,prometheus,metrics`.
3. Configurar Prometheus para hacer scrape de `/actuator/prometheus`.
4. Opcional: dashboard en Grafana.

---

## Servicios que se mantuvieron en el MVP

| Servicio | Razón |
|----------|-------|
| Google Sign-In | Ya implementado de extremo a extremo, aporta valor crítico a la UX. |
| MetricsConfig | Un único archivo, métricas básicas útiles. |
| RateLimitingFilter | Seguridad básica contra fuerza bruta. |
| Gestión de usuarios y roles | Ya integrada en Android (CreateUserScreen). |
| KeepaService | Núcleo del negocio — precios de Amazon. |
| PriceAnalysisService | Núcleo — recomendaciones y alertas. |
| Quartz Scheduler | Núcleo — jobs programados de scraping. |
