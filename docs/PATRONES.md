# Patrones internos y reglas críticas

Este documento recoge las decisiones arquitectónicas internas, convenciones y *gotchas* que no son evidentes leyendo el código y que son necesarias para mantener la coherencia del proyecto al iterar. Está pensado como referencia rápida para cualquier persona que continúe el desarrollo o evalúe el proyecto.

Los detalles de arquitectura general están en [`ARQUITECTURA.md`](ARQUITECTURA.md). Aquí solo se recogen reglas operativas y errores recurrentes ya resueltos.

---

## 1. Multi-tenancy estricto

Todos los datos de negocio están particionados por empresa (`company_id`). Cualquier consulta o mutación debe respetar esta frontera para evitar filtraciones entre tenants.

### Reglas

- Todo `Repository` que acceda a entidades con `company_id` debe filtrar por `companyId` y por `active = true`. Ejemplos correctos:
  - `findBySkuAndCompanyIdAndActiveTrue(sku, companyId)`
  - `findByCompanyIdAndActiveTrue(companyId, pageable)`
- Nunca usar versiones globales sin filtro: `findAll()`, `findBySku(sku)` están prohibidas como acceso primario porque son globales.
- En servicios, obtener `companyId` desde `UserPrincipal` autenticado: `userPrincipal.requireCompanyId()`.
- En operaciones que reciben un `id` (apply/dismiss/delete...), validar siempre la propiedad de la entidad respecto al `companyId` del usuario.

### Implementación

- `User.role = ADMIN` puede no tener `companyId` (es global). `UserPrincipal.create(...)` admite `null` y `requireCompanyId()` lanza `BadRequestException` si se invoca en contexto que requiere empresa.
- Los endpoints administrativos (`/api/admin/**`) no aplican filtro por `companyId`; lo deben filtrar por rol.

---

## 2. Soft-delete con índice único parcial

Los productos no se borran físicamente. Se marcan con `active = false` y todas las consultas de negocio deben filtrar `active = true`.

### Por qué un índice único parcial

La unicidad de `(sku, company_id)` se quiere garantizar **solo entre productos activos**, para que la misma empresa pueda recrear un producto que previamente borró lógicamente. Hibernate no puede modelar índices únicos parciales con `@UniqueConstraint`, por lo que se gestiona en la migración Flyway:

```sql
CREATE UNIQUE INDEX uix_product_sku_company_active
    ON products (sku, company_id) WHERE active = true;
```

### Reglas

- No usar `@Column(unique = true)` sobre `sku`: crearía un índice único global y rompería el multi-tenancy.
- No usar `@UniqueConstraint(columnNames = {"sku","company_id"})` sin filtro de `active`: bloquearía recreaciones tras soft-delete.
- Mantener un check defensivo a nivel de servicio (`findBySkuAndCompanyIdAndActiveTrue`) antes de insertar, para devolver un error de negocio claro en lugar de dejar que reviente el índice.
- Si la BD ya tenía constraints heredados, hay que `ALTER TABLE ... DROP CONSTRAINT` antes de aplicar la migración correcta. `ddl-auto=update` no elimina constraints viejos.

### Errores típicos detectados

- `DataIntegrityViolationException` se traduce en `GlobalExceptionHandler` a un mensaje genérico ("Ya existe un registro con esos datos"). Si aparece de forma inesperada, revisar el estado de los índices únicos en BD.

---

## 3. ASIN como identificador canónico

El proyecto se usa fundamentalmente con productos de Amazon. El identificador real es el ASIN, pero la entidad `Product` mantiene también un campo `sku` por compatibilidad con flujos legacy y por permitir el índice único parcial sobre una columna estable.

### Reglas

- ASIN es el identificador semántico de un producto. La búsqueda de productos por código es ASIN-only.
- `@PrePersist` y `@PreUpdate` sincronizan `sku = asin` antes de cualquier escritura. **No** modificar `sku` manualmente desde el servicio.
- En la API pública el campo del precio es `currentPrice`, no `salePrice`. Los DTOs `CreateProductRequest` y `UpdateProductRequest` exponen `currentPrice`, `costPrice`, `asin`, `name`, etc.

---

## 4. Roles y autorización

Tres roles definidos en `User.Role`:

- `ADMIN`: superusuario global, sin `companyId`.
- `COMPANY_ADMIN`: administrador de una empresa.
- `EMPLOYEE`: usuario operativo.

### Reglas

- Usar `@PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN','EMPLOYEE')")` con prefijo `ROLE_` implícito (no escribir `ROLE_`).
- Validar siempre el `companyId` del principal **además** del rol. El rol da capacidad, el `companyId` da pertenencia.
- `User.Role.valueOf(...)` debe envolverse en try/catch y mapearse a `BadRequestException` si llega un valor desconocido (típico desde formularios).

### Endpoints públicos vs protegidos

Solo estas rutas son públicas (sin JWT). Cualquier otra requiere autenticación:

```
/                                              GET   (RootController)
/api/auth/register                             POST
/api/auth/login                                POST
/api/auth/google                               POST
/api/auth/google/complete-new-company          POST
/api/auth/google/complete-join                 POST
/api/health                                    GET
/actuator/health                               GET
/actuator/info                                 GET
```

En particular, **no** usar el wildcard `/api/auth/**` en `PUBLIC_URLS` porque expondría endpoints autenticados (`/profile`, `/change-password`, `/create-employee`).

`/actuator/**` se mantiene con wildcard solo porque `management.endpoints.web.exposure.include` está limitado a `health, info`. Si en el futuro se expone otro actuator, sustituir por lista explícita.

---

## 5. JWT, 401 vs 403 y rehidratación de sesión

`SecurityConfig` declara un `AuthenticationEntryPoint(HttpStatusEntryPoint(UNAUTHORIZED))` para que los accesos sin autenticación devuelvan **401**, no el 403 por defecto de Spring.

### Por qué importa

El cliente Android distingue:

- **401** → token ausente/expirado/incorrecto. La app limpia el token y navega a Login.
- **403** → autenticado pero sin permisos. La app muestra "No tienes permisos".

Si el backend devolviera 403 ante peticiones sin token, la app mostraría un mensaje incorrecto durante la rehidratación inicial del token.

### Implementación cliente

- `AuthInterceptor` (OkHttp) inyecta el header `Authorization: Bearer <token>` y, si recibe 401, limpia el token y notifica a `SessionManager`.
- `SessionManager` expone un `SharedFlow` de un solo disparo (`sessionExpired`) que la `NavGraph` escucha para redirigir a Login limpiando el back stack.
- `TokenRepository` cachea el token en memoria y lo persiste en `DataStore`. Al arrancar la app se rehidrata desde disco; `getCachedToken()` espera hasta 2 segundos a que se complete con un `CountDownLatch`, evitando una *race condition* entre la primera petición HTTP y la lectura asíncrona del token.

---

## 6. Reglas de alerta vs alertas generadas

Existen dos entidades distintas:

- `AlertRule` (`alert_rules`): reglas configuradas por el usuario, scoped por empresa, opcionalmente por producto. Tienen `alertType`, `threshold`, `enabled`, `name`, `targetPrice`.
- `Alert` (`alerts`): alertas generadas automáticamente por `PriceAnalysisService` cuando el análisis detecta condiciones que cumplen una regla.

### Reglas

- `PriceAnalysisService` consulta `AlertRuleRepository.findApplicableRules(companyId, productId)` para conocer los umbrales activos. Si no hay reglas, aplica unos valores por defecto (`10%`, `10%`, `15%`).
- Las reglas con `product_id = NULL` son globales para la empresa. Las reglas específicas de un producto tienen precedencia.
- Tipos de regla: `COMPETITOR_PRICE_DROP`, `COMPETITOR_PRICE_RISE`, `COMPETITOR_OUT_OF_STOCK`, `PRICE_BELOW_COST`, `HIGH_MARGIN_OPPORTUNITY`, `PRICE_MATCH_NEEDED`.

---

## 7. Persistencia de precios de Amazon

Los precios obtenidos de Keepa se guardan en `competitor_prices` (no en `products`). El campo `Product.amazonPrice` no existe en la BD: se calcula al construir `ProductResponse`.

### Cómo se enriquecen los `ProductResponse`

- `ProductService.getProduct(...)` consulta `competitorPriceRepository.findTopByProductIdOrderByScrapedAtDesc(...)` y rellena `amazonPrice`, `amazonProductTitle`, `amazonPriceUpdatedAt`.
- Cuando se llama a `POST /api/competitors/amazon/sync/{productId}`, `KeepaService` persiste un nuevo `CompetitorPrice` y la siguiente lectura de `getProduct(...)` lo recoge automáticamente.

### Endpoint correcto

Usar `POST /api/competitors/amazon/sync/{productId}`. **No** existe `/api/products/{id}/sync-amazon`.

---

## 8. Convenciones del cliente Android

### Separación de ViewModels

Los flujos de productos están deliberadamente repartidos en tres ViewModels para evitar contaminación de estado entre pantallas:

- `SearchViewModel` — búsqueda por ASIN o por nombre. Resultados temporales pueden tener `id = -1L`.
- `ProductDetailViewModel` — detalle, sincronización con Amazon, historial, borrado.
- `ProductFormViewModel` — alta y edición. Aislado del resto, no toca listas de otras pantallas.

`EditProductScreen` inyecta los dos últimos.

### Reglas de pantalla

- `SearchScreen` limpia sus resultados con `DisposableEffect.onDispose` para evitar que productos temporales (`id = -1L`) aparezcan en otras pantallas.
- `TrackingScreen` filtra defensivamente `it.id != -1L` como cinturón de seguridad por si algún resultado temporal escapa.
- Tras crear un producto desde búsqueda, navegar a la tab Tracking (no hacer `popBackStack` a Search).
- `TrackingScreen` consume `GET /api/products/monitored`, no `GET /api/products`.

### Carga al volver a la pantalla

- Usar `LifecycleEventObserver(ON_RESUME)` para recargar al volver. **No** combinar con `LaunchedEffect(Unit)`: provoca cargas duplicadas.

### Estado en diálogos

- Cuando se accede a campos opcionales del estado dentro de un callback de diálogo (p. ej. `uiState.editingRule!!.id`), capturar antes en una `val` local. El estado puede cambiar entre recomposiciones y un `!!` en lambda puede explotar.

### Refresh centralizado

- En ViewModels que cargan varias fuentes en paralelo (p. ej. `AlertsViewModel` con reglas + alertas + productos), gestionar `isRefreshing` solo desde el método `loadAll()` con `launch + join()`. Los métodos internos `loadXxxInternal()` no tocan `isRefreshing`.

---

## 9. Permisos y configuración Android

- `ACCESS_NETWORK_STATE` es obligatorio en el manifest: `NetworkObserver` usa `ConnectivityManager.registerNetworkCallback(...)` y sin el permiso lanza `SecurityException`.
- `MainViewModel.isOffline` aplica `debounce(1500ms)` solo en transiciones a offline para evitar falsos positivos durante el arranque (cuando `ConnectivityManager` emite "unvalidated" antes de confirmar internet).
- `KeyboardType.Email` restringe caracteres acentuados. En el campo de login usar `KeyboardType.Text` porque el campo acepta tanto email como nombre de usuario.
- `URLEncoder.encode(...)` reemplaza espacios por `+`. Decodificar siempre con `URLDecoder.decode(...)` al consumir argumentos de navegación que vinieron URL-encoded.

---

## 10. Configuración de Gradle: `local.properties` y secretos

`Project.findProperty(...)` no lee valores de `local.properties` (solo lee `gradle.properties` global y de proyecto). Para secretos como `GOOGLE_WEB_CLIENT_ID`, cargar manualmente:

```kotlin
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val googleClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID")
buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleClientId ?: ""}\"")
```

`local.properties` está fuera de control de versiones; en CI se inyecta el valor desde variables de entorno.

---

## 11. Cuándo actualizar este documento

Actualizar `PATRONES.md` cuando:

- Se identifique un *gotcha* recurrente que ya costó al menos una sesión depurar.
- Se establezca una nueva regla de coherencia entre módulos (backend ↔ Android).
- Se cambie un patrón documentado aquí (entonces hay que **modificar** la entrada existente, no añadir una nueva contradictoria).
