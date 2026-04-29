# Mejoras futuras

Servicios y configuraciones eliminados para simplificar el MVP. Todos estaban parcialmente implementados y pueden reintegrarse más adelante.

## 1. AuditService

Archivos eliminados:

1. `service/AuditService.java`
2. `entity/AuditLog.java`
3. `repository/AuditLogRepository.java`
4. `test/service/AuditServiceTest.java`

Qué hacía: registraba acciones administrativas (crear empresa, eliminar usuario, etc.) en la tabla `audit_logs` con usuario, acción, entidad, IP y timestamp. Se invocaba desde `AdminController`.

Para reintegrarlo:

1. Recrear entidad `AuditLog`, repositorio y servicio.
2. Inyectar `AuditService` en `AdminController`.
3. Llamar a `auditService.logAction()` en `createCompany`, `deleteUser`, etc.
4. Considerar endpoint `GET /api/admin/audit-logs` con paginación.

## 2. EmailService

Archivos eliminados:

1. `service/EmailService.java`
2. `test/service/EmailServiceTest.java`

Qué hacía: envío de correo SMTP (alertas de precio, bienvenida) con plantillas HTML embebidas. Usaba `@ConditionalOnProperty(name = "pricewise.email.enabled")`.

Para reintegrarlo:

1. Añadir `spring-boot-starter-mail` en `pom.xml`.
2. Recrear `EmailService` con `@ConditionalOnProperty`.
3. Configurar SMTP en `application.yml` (host, port, username, password).
4. Conectarlo con `NotificationService` para alertas críticas.

## 3. NotificationService y WebSocket

Archivos eliminados:

1. `service/NotificationService.java`
2. `config/WebSocketConfig.java`
3. `test/service/NotificationServiceTest.java`

Qué hacía: notificaciones en tiempo real con STOMP sobre SockJS, canal `/topic/alerts/{userId}`. `PriceAnalysisService` enviaba notificación al crear alertas. Dependía opcionalmente de `EmailService`.

Para reintegrarlo:

1. Añadir `spring-boot-starter-websocket` en `pom.xml`.
2. Recrear `WebSocketConfig` con `MessageBroker` y endpoints STOMP.
3. Recrear `NotificationService` inyectando `SimpMessagingTemplate`.
4. Llamar a `notificationService.sendAlertNotification()` en `PriceAnalysisService.createAlert()`.
5. En Android, integrar cliente WebSocket (OkHttp o Scarlet).

## 4. DataCleanupService

Archivos eliminados:

1. `service/DataCleanupService.java`
2. `test/service/DataCleanupServiceTest.java`

Qué hacía: job programado que purgaba datos antiguos: alertas leídas con más de X días, recomendaciones aplicadas o descartadas, precios de competencia obsoletos. Configuración en `pricewise.cleanup.alert-retention-days`, etc.

Para reintegrarlo:

1. Recrear el servicio con `@Scheduled` o un job de Quartz.
2. Añadir las propiedades de retención en `application.yml`.
3. Implementar las consultas de borrado por fecha en los repositorios.

## 5. OpenAPI y Swagger

Archivos eliminados:

1. `config/OpenApiConfig.java`

Qué hacía: configuraba Swagger UI en `/swagger-ui.html` con documentación automática de los endpoints REST.

Para reintegrarlo:

1. Añadir `springdoc-openapi-starter-webmvc-ui` en `pom.xml`.
2. Recrear `OpenApiConfig` con `@OpenAPIDefinition`.
3. Añadir las rutas de Swagger a `PUBLIC_URLS` en `SecurityConfig`.
4. Anotar endpoints con `@Operation`, `@ApiResponse`, etc. (opcional).

## 6. SchedulerController

Archivos eliminados:

1. `controller/SchedulerController.java`

Qué hacía: endpoints de admin para gestionar jobs de Quartz manualmente (pausar, reanudar, disparar).

Para reintegrarlo:

1. Recrear el controlador con `POST` para `trigger`, `pause` y `resume`.
2. Proteger con `@PreAuthorize("hasRole('ROLE_ADMIN')")`.
3. Inyectar `org.quartz.Scheduler`.

## 7. Caché Redis

Archivos eliminados:

1. `config/RedisCacheConfig.java`

Anotaciones eliminadas en `ProductService`:

1. `@Cacheable(value = "categories", key = "#companyId")` en `getCategories()`.
2. `@Cacheable(value = "brands", key = "#companyId")` en `getBrands()`.
3. `@CacheEvict(value = {"categories", "brands"}, key = "#companyId")` en `createProduct()` y `updateProduct()`.

Para reintegrarlo:

1. Añadir `spring-boot-starter-cache` y `spring-boot-starter-data-redis`.
2. Recrear `RedisCacheConfig` con TTLs por caché.
3. Configurar Redis en `application.yml`:

   ```yaml
   spring:
     data:
       redis:
         host: ${REDIS_HOST:localhost}
         port: ${REDIS_PORT:6379}
         password: ${REDIS_PASSWORD:}
   ```

4. Volver a anotar `ProductService` con `@Cacheable` y `@CacheEvict`.
5. Considerar caché también en `KeepaService`.

## 8. AsyncConfig

Archivos eliminados:

1. `config/AsyncConfig.java`
2. `test/config/AsyncConfigTest.java`

Qué hacía: habilitaba `@Async` con `@EnableAsync` y un `ThreadPoolTaskExecutor` (`coreSize=2`, `maxSize=5`, `queueCapacity=100`, prefijo `PriceWise-Async-`).

Para reintegrarlo:

1. Recrear `AsyncConfig` con `@EnableAsync` y `ThreadPoolTaskExecutor`.
2. Usar `@Async` en servicios que se beneficien (email, notificaciones, scraping).

## 9. Métricas con Prometheus

Dependencia eliminada de `pom.xml`:

1. `micrometer-registry-prometheus`.

Para reintegrarlo:

1. Añadir la dependencia.
2. Exponer el endpoint en `application.yml`: `management.endpoints.web.exposure.include: health,info,prometheus,metrics`.
3. Configurar Prometheus para scrapear `/actuator/prometheus`.
4. Opcional: dashboard en Grafana.

## Servicios mantenidos en el MVP

1. Google Sign-In: implementado de extremo a extremo, valor crítico para la UX.
2. MetricsConfig: un solo archivo, métricas básicas útiles.
3. RateLimitingFilter: protección contra fuerza bruta.
4. Gestión de usuarios y roles: ya integrada en Android.
5. KeepaService: núcleo del negocio, precios de Amazon.
6. PriceAnalysisService: núcleo, recomendaciones y alertas.
7. Quartz Scheduler: núcleo, jobs de scraping programados.
