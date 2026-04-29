# Patrones internos y reglas críticas

Decisiones arquitectónicas, convenciones y errores recurrentes ya resueltos que no son evidentes leyendo el código. Para arquitectura general, ver `ARQUITECTURA.md`.

## 1. Multi-tenancy estricto

Todos los datos de negocio están particionados por empresa (`company_id`). Cualquier consulta o mutación debe respetar esa frontera para evitar filtraciones entre tenants.

Reglas:

1. Todo `Repository` que acceda a entidades con `company_id` filtra por `companyId` y por `active = true`. Ejemplos: `findBySkuAndCompanyIdAndActiveTrue(sku, companyId)`, `findByCompanyIdAndActiveTrue(companyId, pageable)`.
2. Nunca usar versiones globales como `findAll()` o `findBySku(sku)` como acceso primario.
3. En servicios, obtener el `companyId` desde `UserPrincipal` autenticado con `userPrincipal.requireCompanyId()`.
4. En operaciones que reciben un `id` (apply, dismiss, delete), validar siempre la propiedad de la entidad respecto al `companyId` del usuario.

Implementación:

1. `User.role = ADMIN` puede no tener `companyId`. `UserPrincipal.create(...)` admite `null` y `requireCompanyId()` lanza `BadRequestException` si se invoca en un contexto que requiere empresa.
2. Los endpoints `/api/admin/**` no aplican filtro por `companyId`, lo aplican por rol.

## 2. Soft-delete con índice único parcial

Los productos no se borran físicamente, se marcan con `active = false`. Todas las consultas de negocio filtran `active = true`.

La unicidad de `(sku, company_id)` se garantiza solo entre productos activos para que la misma empresa pueda recrear un producto previamente borrado lógicamente. Hibernate no modela índices únicos parciales con `@UniqueConstraint`, por eso se gestiona en migración Flyway:

```sql
CREATE UNIQUE INDEX uix_product_sku_company_active
    ON products (sku, company_id) WHERE active = true;
```

Reglas:

1. No usar `@Column(unique = true)` sobre `sku`: crearía un índice global y rompería el multi-tenancy.
2. No usar `@UniqueConstraint(columnNames = {"sku","company_id"})` sin filtro de `active`: bloquearía recreaciones tras soft-delete.
3. Mantener un check defensivo en el servicio (`findBySkuAndCompanyIdAndActiveTrue`) antes de insertar para devolver un error de negocio claro en lugar de dejar reventar el índice.
4. Si la BD ya tenía constraints heredados, ejecutar `ALTER TABLE ... DROP CONSTRAINT` antes de aplicar la migración correcta. `ddl-auto=update` no elimina constraints viejos.

`DataIntegrityViolationException` se traduce en `GlobalExceptionHandler` a un mensaje genérico ("Ya existe un registro con esos datos"). Si aparece de forma inesperada, revisar los índices únicos en BD.

## 3. ASIN como identificador canónico

El proyecto se usa con productos de Amazon. El identificador real es el ASIN, pero `Product` mantiene también `sku` por compatibilidad y para permitir el índice único parcial.

Reglas:

1. ASIN es el identificador semántico de un producto. La búsqueda por código es ASIN-only.
2. `@PrePersist` y `@PreUpdate` sincronizan `sku = asin` antes de cada escritura. No modificar `sku` manualmente desde el servicio.
3. En la API pública el campo del precio es `currentPrice`, no `salePrice`. `CreateProductRequest` y `UpdateProductRequest` exponen `currentPrice`, `costPrice`, `asin`, `name`, etc.

## 4. Roles y autorización

Tres roles definidos en `User.Role`:

1. `ADMIN`: superusuario global, sin `companyId`.
2. `COMPANY_ADMIN`: administrador de una empresa.
3. `EMPLOYEE`: usuario operativo.

Reglas:

1. Usar `@PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN','EMPLOYEE')")` con prefijo `ROLE_` implícito.
2. Validar siempre el `companyId` del principal además del rol. El rol da capacidad, el `companyId` da pertenencia.
3. `User.Role.valueOf(...)` debe ir en try/catch y mapearse a `BadRequestException` si llega un valor desconocido.

Endpoints públicos (sin JWT). Cualquier otro requiere autenticación:

```
/                                              GET
/api/auth/register                             POST
/api/auth/login                                POST
/api/auth/google                               POST
/api/auth/google/complete-new-company          POST
/api/auth/google/complete-join                 POST
/api/health                                    GET
/actuator/health                               GET
/actuator/info                                 GET
```

No usar el wildcard `/api/auth/**` en `PUBLIC_URLS`: expondría endpoints autenticados (`/profile`, `/change-password`, `/create-employee`).

`/actuator/**` se mantiene con wildcard solo porque `management.endpoints.web.exposure.include` está limitado a `health, info`. Si se expone otro actuator, sustituir por lista explícita.

## 5. JWT, 401 vs 403 y rehidratación de sesión

`SecurityConfig` declara `AuthenticationEntryPoint(HttpStatusEntryPoint(UNAUTHORIZED))` para que los accesos sin autenticación devuelvan 401 en vez del 403 por defecto de Spring.

El cliente Android distingue:

1. `401`: token ausente, expirado o incorrecto. Limpia el token y navega a Login.
2. `403`: autenticado pero sin permisos. Muestra "No tienes permisos".

Si el backend devolviese 403 ante peticiones sin token, la app mostraría un mensaje incorrecto durante la rehidratación inicial.

Implementación cliente:

1. `AuthInterceptor` (OkHttp) inyecta `Authorization: Bearer <token>` y, si recibe 401, limpia el token y notifica a `SessionManager`.
2. `SessionManager` expone un `SharedFlow` de un solo disparo (`sessionExpired`) que `NavGraph` escucha para redirigir a Login limpiando el back stack.
3. `TokenRepository` cachea el token en memoria y lo persiste en `DataStore`. Al arrancar se rehidrata desde disco; `getCachedToken()` espera hasta 2 segundos con un `CountDownLatch` para evitar la race condition entre la primera petición HTTP y la lectura asíncrona del token.

## 6. Reglas de alerta vs alertas generadas

Existen dos entidades distintas:

1. `AlertRule` (`alert_rules`): reglas configuradas por el usuario, scoped por empresa, opcionalmente por producto. Tienen `alertType`, `threshold`, `enabled`, `name`, `targetPrice`.
2. `Alert` (`alerts`): alertas generadas automáticamente por `PriceAnalysisService` cuando el análisis detecta condiciones que cumplen una regla.

Reglas:

1. `PriceAnalysisService` consulta `AlertRuleRepository.findApplicableRules(companyId, productId)` para conocer los umbrales activos. Si no hay reglas, aplica los valores por defecto (10%, 10%, 15%).
2. Las reglas con `product_id = NULL` son globales para la empresa. Las reglas específicas de un producto tienen precedencia.
3. Tipos de regla: `COMPETITOR_PRICE_DROP`, `COMPETITOR_PRICE_RISE`, `COMPETITOR_OUT_OF_STOCK`, `PRICE_BELOW_COST`, `HIGH_MARGIN_OPPORTUNITY`, `PRICE_MATCH_NEEDED`.

## 7. Persistencia de precios de Amazon

Los precios obtenidos de Keepa se guardan en `competitor_prices`, no en `products`. El campo `Product.amazonPrice` no existe en BD: se calcula al construir `ProductResponse`.

1. `ProductService.getProduct(...)` consulta `competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(...)` y rellena `amazonPrice`, `amazonProductTitle`, `amazonPriceUpdatedAt`.
2. Al llamar a `POST /api/competitors/amazon/sync/{productId}`, `KeepaService` persiste un nuevo `CompetitorPrice` y la siguiente lectura de `getProduct(...)` lo recoge.
3. El endpoint correcto es `POST /api/competitors/amazon/sync/{productId}`. No existe `/api/products/{id}/sync-amazon`.

## 8. Convenciones del cliente Android

Los flujos de productos están repartidos en tres ViewModels para evitar contaminación de estado:

1. `SearchViewModel`: búsqueda por ASIN o nombre. Los resultados temporales pueden tener `id = -1L`.
2. `ProductDetailViewModel`: detalle, sincronización con Amazon, historial, borrado.
3. `ProductFormViewModel`: alta y edición. Aislado del resto, no toca listas de otras pantallas.

`EditProductScreen` inyecta los dos últimos.

Reglas de pantalla:

1. `SearchScreen` limpia sus resultados con `DisposableEffect.onDispose` para que los productos temporales no escapen a otras pantallas.
2. `TrackingScreen` filtra defensivamente `it.id != -1L` como cinturón de seguridad.
3. Tras crear un producto desde búsqueda, navegar a la tab Tracking, no hacer `popBackStack` a Search.
4. `TrackingScreen` consume `GET /api/products/monitored`, no `GET /api/products`.

Carga al volver a pantalla:

1. Usar `LifecycleEventObserver(ON_RESUME)` para recargar al volver. No combinar con `LaunchedEffect(Unit)`: provoca cargas duplicadas.

Estado en diálogos:

1. Cuando se accede a campos opcionales del estado dentro del callback de un diálogo (`uiState.editingRule!!.id`), capturar antes en una `val` local. El estado puede cambiar entre recomposiciones y un `!!` en lambda revienta.

Refresh centralizado:

1. En ViewModels que cargan varias fuentes en paralelo (p.ej. `AlertsViewModel`), gestionar `isRefreshing` solo desde `loadAll()` con `launch + join()`. Los `loadXxxInternal()` no tocan `isRefreshing`.

## 9. Permisos y configuración Android

1. `ACCESS_NETWORK_STATE` es obligatorio en el manifest. `NetworkObserver` usa `ConnectivityManager.registerNetworkCallback(...)` y sin el permiso lanza `SecurityException`.
2. `MainViewModel.isOffline` aplica `debounce(1500ms)` solo en transiciones a offline para evitar falsos positivos durante el arranque cuando `ConnectivityManager` emite "unvalidated" antes de confirmar internet.
3. `KeyboardType.Email` bloquea caracteres acentuados. En el login usar `KeyboardType.Text` porque el campo acepta email o nombre de usuario.
4. `URLEncoder.encode(...)` sustituye espacios por `+`. Decodificar siempre con `URLDecoder.decode(...)` al consumir argumentos de navegación URL-encoded.

## 10. Gradle, `local.properties` y secretos

`Project.findProperty(...)` no lee valores de `local.properties`, solo de `gradle.properties` global o de proyecto. Para secretos como `GOOGLE_WEB_CLIENT_ID`, cargar manualmente:

```kotlin
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val googleClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID")
buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleClientId ?: ""}\"")
```

`local.properties` está fuera de control de versiones. En CI el valor se inyecta desde variables de entorno.

## 11. Cuándo actualizar este documento

1. Cuando aparezca un gotcha recurrente que haya costado al menos una sesión depurar.
2. Cuando se establezca una nueva regla de coherencia entre backend y Android.
3. Cuando cambie un patrón ya documentado: modificar la entrada existente, no añadir una contradictoria.
