# Registro de Bugs y Soluciones - PriceWise Backend

Este documento registra todos los errores encontrados durante el desarrollo de PriceWise Backend,
junto con sus causas raiz y soluciones aplicadas.

> Periodo cubierto: 2026-01-25 a 2026-03-08

---

## Indice de Bugs

| #  | Titulo                                            | Fecha      | Estado |
|----|---------------------------------------------------|------------|--------|
| 1  | DDL auto destruye datos al cambiar entidad        | 2026-01-26 | OK     |
| 2  | Fuga de entidad User en respuesta de login        | 2026-01-27 | OK     |
| 3  | SKU duplicado entre usuarios distintos            | 2026-01-28 | OK     |
| 4  | Precio negativo en PriceHistory al actualizar     | 2026-01-29 | OK     |
| 5  | CORS bloquea peticiones desde localhost en dev    | 2026-02-01 | OK     |
| 6  | JWT_SECRET invalido en produccion sin aviso claro | 2026-02-01 | OK     |
| 7  | N+1 queries en listado de productos               | 2026-02-02 | OK     |
| 8  | Cache no se invalida al borrar producto           | 2026-02-03 | OK     |
| 9  | KeepaService bloquea hilo HTTP durante 30s        | 2026-02-04 | OK     |
| 10 | Race condition en inicializacion de Amazon        | 2026-02-04 | OK     |
| 11 | Scheduler no se reanuda tras caida del servidor   | 2026-02-05 | OK     |
| 12 | Paginacion devuelve elementos de otros usuarios   | 2026-02-05 | OK     |
| 13 | getMargin() lanza ArithmeticException             | 2026-02-06 | OK     |
| 14 | CompetitorPrice.getPriceDifferencePercentage() NullPointerException | 2026-02-06 | OK |
| 15 | Endpoint DELETE no hace soft delete               | 2026-02-06 | OK     |
| 16 | Admin puede desactivarse a si mismo               | 2026-02-07 | OK     |
| 17 | JWT expira pero el cliente no recibe 401          | 2026-02-07 | OK     |
| 18 | PriceMonitorJob no para ante error de Keepa       | 2026-02-08 | OK     |
| 19 | BCrypt hash expuesto en log de debug              | 2026-02-08 | OK     |
| 20 | Productos inactivos aparecen en busqueda          | 2026-02-09 | OK     |
| 21 | Connection pool agotado bajo carga moderada       | 2026-02-09 | OK     |
| 22 | ChangeType INITIAL sobreescrito al actualizar     | 2026-02-10 | OK     |
| 23 | NPE en login tras migracion multi-tenancy         | 2026-02-12 | OK     |
| 24 | AnalyticsController usa userId en vez de companyId| 2026-02-13 | OK     |
| 25 | Sin endpoint para crear empleados en empresa      | 2026-02-13 | OK     |
| 26 | Column "company_code" does not exist in login     | 2026-02-13 | OK     |
| 27 | Error 400 Login: emailOrUsername obligatorio      | 2026-02-14 | OK     |
| 28 | Compilation Error: competitorRepository missing   | 2026-02-13 | OK     |
| 29 | JSON Syntax Error en Postman Collection           | 2026-02-13 | OK     |
| 30 | Confusión con archivo index.json (OpenAPI stale)  | 2026-02-14 | OK     |
| 31 | StackOverflowError (Recursion) en JSON response   | 2026-02-14 | OK     |
| 32 | LazyInitializationException en Admin Dashboard    | 2026-02-14 | OK     |
| 33 | DatabaseSeeder omite empresas si una existe       | 2026-02-14 | OK     |
| 34 | ASIN no se autofinancia al buscar en Keepa        | 2026-02-16 | OK     |
| 35 | Mensaje error login generico (401)                | 2026-02-16 | OK     |
| 36 | Lista de productos no refresca tras añadir/borrar | 2026-02-16 | OK     |
| 37 | Dashboard no actualiza contadores al entrar       | 2026-02-16 | OK     |
| 38 | Creación de usuarios no permitía elegir empresa   | 2026-02-16 | OK     |
| 39 | Error creación usuario - Campos faltantes en DTO  | 2026-02-16 | OK     |
| 40 | Texto invisible en campos de login (tema claro)   | 2026-02-23 | OK     |
| 41 | Acentos bloqueados en campo de usuario del login  | 2026-02-23 | OK     |
| 42 | Crash por SecurityException ACCESS_NETWORK_STATE  | 2026-02-23 | OK     |
| 43 | lintDebug falla con AGP 8.5.2 + Java 25           | 2026-02-23 | OK     |
| 44 | EditAlertDialog no incluye campo targetPrice       | 2026-03-08 | OK     |
| 45 | NullPointerException potencial en lambda editingRule | 2026-03-08 | OK   |
| 46 | Carga 500 productos sin cache en dialogo alertas   | 2026-03-08 | OK     |
| 47 | Doble carga de datos en AlertsScreen               | 2026-03-08 | OK     |
| 48 | isRefreshing inconsistente en AlertsViewModel       | 2026-03-08 | OK     |
| 49 | performNormalSearch("") carga todos los productos  | 2026-03-08 | OK     |


---

## Bug #34: ASIN no se autofinancia al buscar en Keepa

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- Al buscar un producto en Keepa y pulsar "Añadir", el campo ASIN en la pantalla de creación aparecía vacío.

### Causa Raiz
- El mapeo del objeto `ProductResponse` temporal (con ID -1) no estaba asignando el campo `asin` correctamente desde el resultado de Keepa.

### Solucion
- Actualizar `ProductViewModel.kt` para asignar `asin = data.asin` al crear el objeto temporal.

### Archivos Modificados
- `ProductViewModel.kt`

---

## Bug #35: Mensaje error login generico (401)

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- Al introducir credenciales incorrectas, el usuario veía "Session expired" en lugar de "Credenciales incorrectas".

### Causa Raiz
- El manejo de errores en `Result.kt` trataba todos los 401 como expiración de sesión.

### Solucion
- Diferenciar el mensaje de error para 401 en la capa de UI/Repository o generalizar el mensaje a "Credenciales incorrectas o sesión expirada".

### Archivos Modificados
- `Result.kt`

---

## Bug #36: Lista de productos no refresca tras añadir/borrar

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- El usuario añadía o borraba un producto y la lista principal no reflejaba los cambios inmediatamente.

### Causa Raiz
- La lista de productos se cargaba solo al iniciar el ViewModel y no escuchaba cambios globales o no se forzaba la recarga al volver a la pantalla.

### Solucion
- Añadir un botón de refresco manual en la `TopAppBar` de `ProductListScreen`.
- (Mejora futura: Implementar flujo de eventos o `SharedFlow` para refresco automático).

### Archivos Modificados
- `ProductListScreen.kt`

---

## Bug #37: Dashboard no actualiza contadores al entrar

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- Los contadores del Dashboard (productos, alertas) no se actualizaban si se cambiaban datos en otras pantallas y se volvía al Dashboard.

### Causa Raiz
- `DashboardViewModel` cargaba datos solo en `init {}`.

### Solucion
- Añadir `LaunchedEffect(Unit) { viewModel.refresh() }` en `DashboardScreen` para recargar al entrar.

### Archivos Modificados
- `DashboardScreen.kt`
- `DashboardViewModel.kt`

---

## Bug #38: Creación de usuarios no permitía elegir empresa

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- El administrador global no podía asignar una empresa específica al crear un usuario.
- El administrador de empresa no veía feedback de a qué empresa estaba añadiendo el usuario.

### Causa Raiz
- Falta de implementación de selectores de empresa en `CreateUserScreen`.

### Solucion
- Implementar lógica condicional en `CreateUserScreen`: Dropdown para ADMIN, Texto fijo para COMPANY_ADMIN.
- Cargar lista de empresas desde `AdminRepository`.

### Archivos Modificados
- `CreateUserScreen.kt`
- `CreateUserViewModel.kt` (Mejora: soporte "ROLE_ADMIN")

---

## Bug #39: Error creación usuario - Campos faltantes en petición

**Fecha:** 2026-02-16
**Estado:** OK

### Sintomas
- Al pulsar "Crear Usuario", la petición fallaba o creaba el usuario sin asignar empresa/rol correctamente (backend recibía nulls).

### Causa Raiz
- El modelo `CreateEmployeeRequest` en `ApiModels.kt` no incluía los campos `companyId` ni `role`.

### Solucion
- Añadir campos `companyId` y `role` al data class `CreateEmployeeRequest`.

### Archivos Modificados
- `ApiModels.kt`

- `CreateUserViewModel.kt`

---

## Bug #40: Texto invisible en campos de login (tema claro del sistema)

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- En dispositivos con modo claro del sistema, los campos de texto del login eran completamente invisibles (texto oscuro sobre fondo DarkNavy).

### Causa Raiz
- `isSystemInDarkTheme()` retornaba `false` en modo claro → Compose cargaba `LightColorScheme` con colores de texto oscuros.
- Pero `themes.xml` define DarkNavy como fondo de la actividad, creando texto oscuro sobre fondo oscuro = invisible.

### Solucion
- Forzar `darkTheme = true` en `PriceWiseTheme` (siempre tema oscuro).
- Mejorar contraste de campos de texto en modo oscuro con colores explicitos.

### Archivos Modificados
- `Theme.kt`
- `LoginScreen.kt`

---

## Bug #41: Acentos bloqueados en campo de usuario del login

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- No se podian escribir caracteres acentuados (e, a, u, etc.) en el campo de nombre de usuario del login.

### Causa Raiz
- El campo usaba `KeyboardType.Email` que restringe los caracteres permitidos, bloqueando acentos y caracteres especiales del español.

### Solucion
- Cambiar a `KeyboardType.Text` ya que el campo acepta tanto email como username.

### Archivos Modificados
- `LoginScreen.kt`

---

## Bug #42: Crash por SecurityException ACCESS_NETWORK_STATE

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- La aplicacion se cerraba con `SecurityException` al abrirla, antes de llegar al login.

### Causa Raiz
- `NetworkObserver` usa `ConnectivityManager.registerNetworkCallback()` que requiere el permiso `ACCESS_NETWORK_STATE`.
- El permiso no estaba declarado en `AndroidManifest.xml`.

### Solucion
- Declarar `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>` en el manifiesto.

### Archivos Modificados
- `AndroidManifest.xml`

---

## Bug #43: lintDebug falla con AGP 8.5.2 + Java 25

**Fecha:** 2026-02-23
**Estado:** OK (workaround)

### Sintomas
- `./gradlew assembleDebug` fallaba en la tarea `lintDebug` con un crash en `AndroidLintWorkAction`.

### Causa Raiz
- Incompatibilidad entre Android Gradle Plugin 8.5.2, `compileSdk 35` y Java 25.
- El worker de lint no puede ejecutarse correctamente con esta combinacion de versiones.

### Solucion
- Desactivar lint como tarea bloqueante: `abortOnError = false`, `checkReleaseBuilds = false`.
- Solucion temporal hasta que AGP se actualice a version compatible.

### Archivos Modificados
- `build.gradle.kts`

---

## Bug #44: EditAlertDialog no incluye campo targetPrice

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- Al editar una regla de alerta, no se podia ver ni modificar el precio objetivo (targetPrice).
- El campo se habia añadido al backend (V6 migration, entity, DTO) pero faltaba en Android.

### Causa Raiz
- `UpdateAlertRuleRequest` en `ApiModels.kt` no incluia el campo `targetPrice`.
- `EditAlertDialog` no tenia UI para editar el precio objetivo.
- El callback `onSave` pasaba siempre `targetPrice = null`.

### Solucion
- Añadir `targetPrice` a `UpdateAlertRuleRequest`.
- Añadir `OutlinedTextField` de precio objetivo al `EditAlertDialog`, precargado con el valor existente.
- Conectar el campo en `AlertsViewModel.updateRule()`.

### Archivos Modificados
- `ApiModels.kt`
- `AlertsScreen.kt`
- `AlertsViewModel.kt`

---

## Bug #45: NullPointerException potencial en lambda editingRule

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- Posible crash al guardar cambios en el dialogo de edicion de alertas.

### Causa Raiz
- `uiState.editingRule!!.id` dentro del callback `onSave` podia causar NPE si el estado cambiaba entre recomposiciones de Compose.

### Solucion
- Capturar `editingRule` en variable local `val` antes del bloque condicional, evitando el operador `!!` en la lambda.

### Archivos Modificados
- `AlertsScreen.kt`

---

## Bug #46: Carga de 500 productos sin cache en dialogo de alertas

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- Cada apertura del dialogo de crear alerta realizaba una peticion de red cargando hasta 500 productos.

### Causa Raiz
- `showCreateDialog()` llamaba a `loadProducts()` incondicionalmente.

### Solucion
- Cachear la lista: solo cargar si `products.isEmpty()`. La lista se mantiene durante el ciclo de vida del ViewModel.

### Archivos Modificados
- `AlertsViewModel.kt`

---

## Bug #47: Doble carga de datos en AlertsScreen

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- Al entrar a la pantalla de alertas, los datos se cargaban dos veces (doble peticion de red).

### Causa Raiz
- `LifecycleEventObserver(ON_RESUME)` y `LaunchedEffect(Unit)` ambos llamaban a `viewModel.loadAll()`. `ON_RESUME` ya cubre la primera entrada.

### Solucion
- Eliminar `LaunchedEffect(Unit)` redundante. `ON_RESUME` cubre tanto entrada inicial como retorno a la pantalla.

### Archivos Modificados
- `AlertsScreen.kt`

---

## Bug #48: isRefreshing inconsistente en AlertsViewModel

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- El indicador de pull-to-refresh desaparecia antes de que todas las cargas terminaran, o se quedaba visible demasiado tiempo.

### Causa Raiz
- `loadAll()` lanzaba `loadRules()` y `loadAlerts()` en paralelo, pero solo `loadRules()` reseteaba `isRefreshing`.

### Solucion
- Extraer logica a funciones `suspend` internas (`loadRulesInternal()`, `loadAlertsInternal()`).
- `loadAll()` lanza ambas en paralelo con `launch` + `join()` y resetea `isRefreshing` solo cuando ambas terminan.

### Archivos Modificados
- `AlertsViewModel.kt`

---

## Bug #49: performNormalSearch("") carga todos los productos

**Fecha:** 2026-03-08
**Estado:** OK

### Sintomas
- Llamar a `performNormalSearch("")` ejecutaba `repository.getProducts(0)`, cargando todos los productos en `_listState`.

### Causa Raiz
- La rama para query vacia delegaba en `getProducts(0)` en vez de limpiar resultados. La UI ya protegia este caso, pero la funcion en si era vulnerable.

### Solucion
- Guard defensivo: si `trimmedQuery.isBlank()`, llamar `clearSearchResults()` y return. Eliminada la rama que cargaba todos los productos.

### Archivos Modificados
- `ProductViewModel.kt`

---

## Bug #26: Column "company_code" does not exist in login

**Fecha:** 2026-02-13
**Estado:** OK

### Sintomas
- Login fallaba con `500 Internal Server Error`.
- Logs: `PSQLException: ERROR: column c1_0.company_code does not exist`.

### Causa Raiz
- Se añadió el campo `companyCode` a la entidad `Company` como `nullable = false`.
- Hibernate con `ddl-auto: update` no actualizó la tabla existente correctamente o falló al intentar añadir una columna no nula a registros existentes sin valor por defecto.

### Solucion
- Cambiar temporalmente a `ddl-auto: create` para recrear el esquema desde cero.
- Restaurar a `ddl-auto: update`.
- Seeding de datos iniciales vía `DatabaseSeeder`.

### Archivos Modificados
- `application.yml`

---

## Bug #27: Error 400 Login: emailOrUsername obligatorio

**Fecha:** 2026-02-14
**Estado:** OK

### Sintomas
- Al intentar login desde Postman: `400 Bad Request`.
- Respuesta: `{"errors": ["emailOrUsername: El email o username es obligatorio"]}`.

### Causa Raiz
- La colección de Postman enviaba el cuerpo con el campo `email`.
- El DTO `AuthDTOs.LoginRequest` en el backend espera `emailOrUsername`.

### Solucion
- Corregir el body de la request en Postman para usar `emailOrUsername`.

### Archivos Modificados
- `PriceWise_API.postman_collection.json`

---

## Bug #28: Compilation Error: competitorRepository missing

**Fecha:** 2026-02-13
**Estado:** OK

### Sintomas
- Error de compilación en `AdminController`.
- Mensaje: `competitorRepository cannot be resolved`.

### Causa Raiz
- Se eliminó accidentalmente la inyección de `CompetitorRepository` durante un refactor para limpiar imports no usados, pero seguía usándose en el método `getStats`.

### Solucion
- Re-inyectar `CompetitorRepository` en el constructor de `AdminController`.

### Archivos Modificados
- `AdminController.java`

---

## Bug #29: JSON Syntax Error en Postman Collection

**Fecha:** 2026-02-13
**Estado:** OK

### Sintomas
- Postman fallaba al importar la colección modificada.

### Causa Raiz
- La descripción de la colección contenía comillas dobles no escapadas dentro de un string JSON: `"description": "API... "Productos" ..."`.

### Solucion
- Escapar las comillas internas: `\"Productos\"`.

### Archivos Modificados
- `PriceWise_API.postman_collection.json`

---

## Bug #30: Confusión con archivo index.json (OpenAPI stale)

**Fecha:** 2026-02-14
**Estado:** OK

### Sintomas
- El usuario intentaba abrir `index.json` pensando que era la colección de Postman actualizada y no veía los nuevos cambios.

### Causa Raiz
- Existía un archivo `index.json` antiguo en la raíz del proyecto (posiblemente una exportación OpenAPI previa) que no se estaba actualizando.

### Solucion
- Eliminar `index.json`.
- Confirmar que la colección correcta está en `pricewise-backend/PriceWise_API.postman_collection.json`.

### Archivos Modificados
- `index.json` (Eliminado)

---

## Bug #1: DDL auto destruye datos al cambiar entidad

**Fecha:** 2026-01-26
**Estado:** OK

### Sintomas
- Al cambiar el nombre de un campo en una entidad JPA y reiniciar la app, la
  tabla se borraba y recreaba vacia.
- Todos los datos de desarrollo se perdian.

### Causa Raiz
`spring.jpa.hibernate.ddl-auto: create-drop` en el perfil dev. Esta estrategia
borra y recrea el esquema completo en cada inicio.

### Solucion
Cambiar a `ddl-auto: update` en dev y `ddl-auto: validate` en prod:

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update   # dev: actualiza sin borrar
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate # prod: solo verifica, no modifica
```

### Archivos Modificados
- `application.yml`

---

## Bug #2: Fuga de entidad User en respuesta de login

**Fecha:** 2026-01-27
**Estado:** OK

### Sintomas
- La respuesta de `POST /api/auth/login` incluia el campo `password` (hash BCrypt)
  y las listas de productos del usuario en el JSON de respuesta.

### Causa Raiz
`AuthService.login()` devuelvia directamente la entidad `User` en lugar de un DTO.
Jackson serializaba todos los campos publicos incluyendo los sensibles.

### Solucion
Crear `AuthResponse` DTO que incluye solo los campos necesarios:

```java
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String businessName;
    private String role;
    private String token;
    private long expiresIn;
}
```

Y mapear la entidad al DTO en el servicio antes de devolver.

### Archivos Modificados
- `AuthService.java`
- `AuthResponse.java` (nuevo)

---

## Bug #3: SKU duplicado entre usuarios distintos

**Fecha:** 2026-01-28
**Estado:** OK

### Sintomas
- Un usuario recibia error al crear un producto con SKU "ABC-001" si otro usuario
  ya tenia un producto con ese mismo SKU.
- El error era confuso: "SKU ya en uso" cuando en realidad el SKU era libre para
  ese usuario.

### Causa Raiz
La constraint `@Column(unique = true)` en el campo `sku` de `Product` era global
(a nivel de tabla), sin tener en cuenta el `user_id`. El SKU debe ser unico solo
dentro del catalogo de cada usuario.

### Solucion
Eliminar la constraint de columna y mover la unicidad a un indice compuesto:

```java
@Entity
@Table(name = "products",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sku", "user_id"})  // Unico por usuario
    })
public class Product { ... }
```

Y actualizar la validacion en el servicio:

```java
// Antes (incorrecto):
if (productRepository.existsBySku(request.getSku()))

// Despues (correcto):
if (productRepository.existsBySkuAndUserId(request.getSku(), userId))
```

### Archivos Modificados
- `Product.java`
- `ProductService.java`
- `ProductRepository.java`

---

## Bug #4: Precio negativo en PriceHistory al actualizar

**Fecha:** 2026-01-29
**Estado:** OK

### Sintomas
- Al actualizar un producto con precio 0.00, se registraba una entrada en
  PriceHistory con `price = 0.00` y el ChangeType calculado como DECREASE.
- El historial mostraba una caida de precio al 0 cuando en realidad era un
  error de validacion.

### Causa Raiz
La validacion `@DecimalMin("0.01")` en `ProductRequest` se saltaba al hacer
PUT porque el campo `currentPrice` era nullable en el DTO de actualizacion.
Un precio 0 o null pasaba la validacion y llegaba al servicio.

### Solucion
Aplicar validaciones estrictas en el DTO de actualizacion:

```java
public class ProductUpdateRequest {
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentPrice;  // Si se envia, debe ser >= 0.01
}
```

Y en el servicio, ignorar el campo si viene null (no actualizar el precio
si no se envia en la peticion).

### Archivos Modificados
- `ProductUpdateRequest.java`
- `ProductService.java`

---

## Bug #5: CORS bloquea peticiones desde localhost en dev

**Fecha:** 2026-02-01
**Estado:** OK

### Sintomas
- El frontend en `http://localhost:3000` recibia error CORS al llamar a la API
  en `http://localhost:9090`.
- El error en el navegador: "No 'Access-Control-Allow-Origin' header".

### Causa Raiz
La configuracion de CORS en `SecurityConfig` no contemplaba localhost en
desarrollo. El perfil dev no tenia habilitado el modo permisivo.

### Solucion
Configurar CORS segun perfil:

```java
// SecurityConfig.java
CorsConfiguration config = new CorsConfiguration();
if (corsProperties.isAllowAll()) {
    config.addAllowedOriginPattern("*");  // Dev: cualquier origen
} else {
    config.setAllowedOrigins(corsProperties.getAllowedOrigins());  // Prod: lista blanca
}
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

```yaml
# application.yml (dev)
cors:
  allow-all: true

# application-prod.yml
cors:
  allow-all: false
  allowed-origins:
    - https://miapp.com
    - https://www.miapp.com
```

### Archivos Modificados
- `SecurityConfig.java`
- `application.yml`

---

## Bug #6: JWT_SECRET invalido en produccion sin aviso claro

**Fecha:** 2026-02-01
**Estado:** OK

### Sintomas
- En produccion, si `JWT_SECRET` no estaba configurado, la aplicacion arrancaba
  con un secreto por defecto inseguro sin ningun error visible.
- Los tokens se firmaban con `"default-secret-change-in-production"` y eran
  validos pero completamente inseguros.

### Causa Raiz
La propiedad `jwt.secret` tenia un valor por defecto en `application.yml`. No
habia ninguna validacion al arrancar de que el secreto cumplia requisitos de
seguridad.

### Solucion
Validar el secreto en la inicializacion de `JwtService`:

```java
@PostConstruct
public void validateSecretKey() {
    if (secret.equals("default-secret-change-in-production")) {
        if (isProduction()) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET no configurado en produccion. " +
                "Configura la variable de entorno JWT_SECRET con minimo 32 caracteres.");
        }
        log.warn("ATENCION: Usando JWT_SECRET por defecto. " +
                 "Configura JWT_SECRET antes de desplegar en produccion.");
    }
    if (secret.length() < 32) {
        throw new IllegalStateException(
            "JWT_SECRET debe tener al menos 32 caracteres.");
    }
}
```

### Archivos Modificados
- `JwtService.java`

---

## Bug #7: N+1 queries en listado de productos

**Fecha:** 2026-02-02
**Estado:** OK

### Sintomas
- Al listar 20 productos, los logs mostraban 21 queries SQL:
  1 para obtener los productos + 1 por cada producto para cargar el usuario.
- Tiempo de respuesta de 200-400ms para solo 20 productos.

### Causa Raiz
La relacion `@ManyToOne` entre `Product` y `User` usaba `FetchType.EAGER` por
defecto. Al serializar cada `ProductResponse`, Jackson intentaba acceder a
`product.getUser()` lo que disparaba una query lazy por cada producto.

### Solucion
1. Cambiar a `FetchType.LAZY` en la relacion:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

2. Usar `@EntityGraph` en la query de listado cuando se necesita el usuario:

```java
@EntityGraph(attributePaths = {"user"})
Page<Product> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
```

3. En el mapeo a DTO, no acceder a `product.getUser()` directamente.
   El `userId` ya esta disponible como parametro del metodo de servicio.

### Archivos Modificados
- `Product.java`
- `ProductRepository.java`
- `ProductService.java`

---

## Bug #8: Cache no se invalida al borrar producto

**Fecha:** 2026-02-03
**Estado:** OK

### Sintomas
- Despues de borrar (soft delete) un producto de la categoria "Electronica",
  el endpoint `/api/products/categories` seguia devolviendo "Electronica" incluso
  si era el unico producto de esa categoria.
- La cache solo se invalidaba en create y update, no en delete.

### Causa Raiz
El metodo `deleteProduct()` en `ProductService` no tenia la anotacion
`@CacheEvict`. La cache de categorias y marcas quedaba desactualizada.

### Solucion

```java
// Antes:
public void deleteProduct(Long productId, Long userId) {
    product.setActive(false);
    productRepository.save(product);
}

// Despues:
@CacheEvict(value = {"categories", "brands"}, key = "#userId")
public void deleteProduct(Long productId, Long userId) {
    product.setActive(false);
    productRepository.save(product);
}
```

### Archivos Modificados
- `ProductService.java`

---

## Bug #9: KeepaService bloquea hilo HTTP durante 30s

**Fecha:** 2026-02-04
**Estado:** OK

### Sintomas
- El endpoint `POST /api/competitors/amazon/sync/{productId}` bloqueaba el hilo
  de la peticion HTTP hasta 30 segundos mientras esperaba la respuesta de Keepa.
- Bajo carga concurrente, el pool de hilos de Tomcat se agotaba.

### Causa Raiz
`KeepaService.syncAmazonPrice()` llamaba a `future.get()` directamente en el
hilo del controlador, convirtiendo una operacion async en sincrona.

### Solucion
Redisenar el endpoint para devolver respuesta inmediata y procesar en background:

```java
// Controller: responde inmediatamente
@PostMapping("/amazon/sync/{productId}")
public ResponseEntity<ApiResponse<String>> syncAmazonPrice(@PathVariable Long productId) {
    keepaService.syncAmazonPriceAsync(productId);  // No espera resultado
    return ResponseEntity.accepted()
        .body(ApiResponse.success("Sincronizacion iniciada en segundo plano"));
}

// Service: procesa de forma asincrona
public void syncAmazonPriceAsync(Long productId) {
    CompletableFuture.runAsync(() -> {
        try {
            // Obtener precio y guardar
            getAmazonPrice(asin, productId)
                .thenAccept(price -> price.ifPresent(this::saveCompetitorPrice));
        } catch (Exception e) {
            log.error("Error sincronizando precio para producto {}: {}", productId, e.getMessage());
        }
    }, keepaExecutor);
}
```

### Archivos Modificados
- `CompetitorController.java`
- `KeepaService.java`

---

## Bug #10: Race condition en inicializacion de Amazon Competitor

**Fecha:** 2026-02-04
**Estado:** OK

### Sintomas
- En el primer arranque, cuando dos peticiones concurrentes de sync llegaban
  a la vez, se creaban dos registros `Competitor` para "Amazon ES" en la BD.
- Violaba la constraint unique del campo `name`.

### Causa Raiz
`KeepaService.getOrCreateAmazonCompetitor()` no era thread-safe. Dos hilos
podian pasar la comprobacion `findByName()` simultaneamente antes de que
ninguno hubiera guardado el registro.

### Solucion
Implementar double-checked locking con inicializacion en `@PostConstruct`:

```java
@Service
public class KeepaService {

    private volatile Competitor amazonCompetitor;

    @PostConstruct
    private void initAmazonCompetitor() {
        // Se ejecuta en un solo hilo al arrancar
        amazonCompetitor = competitorRepository
            .findByCode("amazon_es")
            .orElseGet(this::createAmazonCompetitor);
    }

    private synchronized Competitor createAmazonCompetitor() {
        // Double-check: otro hilo puede haber creado mientras esperabamos
        return competitorRepository.findByCode("amazon_es")
            .orElseGet(() -> {
                Competitor amazon = Competitor.builder()
                    .name("Amazon ES")
                    .code("amazon_es")
                    .sourceType(SourceType.API)
                    .active(true)
                    .build();
                return competitorRepository.save(amazon);
            });
    }
}
```

### Archivos Modificados
- `KeepaService.java`

---

## Bug #11: Scheduler no se reanuda tras caida del servidor

**Fecha:** 2026-02-05
**Estado:** OK

### Sintomas
- Si el servidor caia mientras `PriceMonitorJob` estaba ejecutandose,
  tras reiniciar el scheduler no lanzaba el job hasta el siguiente ciclo de 6h.
- En escenarios de crash frecuente, los precios podian desactualizarse por
  horas sin que hubiera ningun aviso.

### Causa Raiz
Quartz estaba configurado en modo `RAMJobStore` (sin persistencia). Al reiniciar,
perdia el estado de los jobs y triggers pendientes.

### Solucion
Configurar `misfire-threshold` y politica de recuperacion en el trigger:

```java
@Bean
public Trigger priceMonitorTrigger(JobDetail jobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 */6 * * ?")
                .withMisfireHandlingInstructionFireAndProceed()  // Ejecuta si se perdio
        )
        .build();
}
```

Con `withMisfireHandlingInstructionFireAndProceed()`, si el servidor estuvo caido
y se perdio una ejecucion, Quartz ejecuta el job inmediatamente al reiniciar.

### Archivos Modificados
- `SchedulerConfig.java`

---

## Bug #12: Paginacion devuelve elementos de otros usuarios

**Fecha:** 2026-02-05
**Estado:** OK

### Sintomas
- Con pagination offset alto (pagina 5+), algunas respuestas incluian productos
  de otros usuarios si el total de productos del usuario era pequeno.

### Causa Raiz
La query de busqueda usaba `findAll(pageable)` sin filtrar por `userId` en el
repositorio cuando no se proporcionaban criterios de busqueda.

### Solucion
Garantizar que TODAS las queries de producto incluyen filtro por `userId`:

```java
// Siempre incluir userId en los filtros
@Query("SELECT p FROM Product p WHERE p.user.id = :userId AND p.active = true")
Page<Product> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
```

Y nunca exponer endpoints que devuelvan productos sin filtro de usuario
(salvo endpoints ADMIN que tienen su propia seguridad).

### Archivos Modificados
- `ProductRepository.java`
- `ProductService.java`

---

## Bug #13: getMargin() lanza ArithmeticException

**Fecha:** 2026-02-06
**Estado:** OK

### Sintomas
- Al llamar a `GET /api/products/{id}` para un producto con `costPrice = 0`,
  la respuesta era un 500 Internal Server Error.
- Stack trace: `java.lang.ArithmeticException: Division by zero`.

### Causa Raiz
El metodo `getMargin()` en `Product.java` no manejaba el caso `costPrice = 0`.
La division `currentPrice / costPrice` lanzaba excepcion.

### Solucion
Verificar ambas condiciones antes de dividir:

```java
public BigDecimal getMargin() {
    if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) <= 0) {
        return null;  // Sin coste configurado o coste 0: no se puede calcular margen
    }
    return currentPrice.subtract(costPrice)
            .divide(costPrice, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
}
```

### Archivos Modificados
- `Product.java`

---

## Bug #14: CompetitorPrice.getPriceDifferencePercentage() NullPointerException

**Fecha:** 2026-02-06
**Estado:** OK

### Sintomas
- El endpoint de analytics crasheaba con NPE al calcular diferencias de precio
  para productos que aun no tenian precio propio configurado.

### Causa Raiz
`getPriceDifferencePercentage()` calculaba `(competitorPrice - ourPrice) / ourPrice * 100`
sin verificar si `ourPrice` (el precio del producto) era null.

### Solucion
Verificar nulls antes del calculo y devolver null si no es calculable:

```java
public BigDecimal getPriceDifferencePercentage() {
    if (product == null || product.getCurrentPrice() == null
            || product.getCurrentPrice().compareTo(BigDecimal.ZERO) == 0
            || this.price == null) {
        return null;
    }
    return this.price.subtract(product.getCurrentPrice())
            .divide(product.getCurrentPrice(), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
}
```

### Archivos Modificados
- `CompetitorPrice.java`

---

## Bug #15: Endpoint DELETE no hace soft delete

**Fecha:** 2026-02-06
**Estado:** OK

### Sintomas
- `DELETE /api/products/{id}` borraba el registro de la BD de forma permanente.
- Se perdia todo el historial de precios del producto (cascade = ALL).

### Causa Raiz
`ProductService.deleteProduct()` llamaba a `productRepository.delete(product)`
en lugar de marcar el campo `active = false`.

### Solucion
Implementar soft delete:

```java
public void deleteProduct(Long productId, Long userId) {
    Product product = productRepository.findByIdAndUserId(productId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

    // Soft delete: marcar inactivo, no borrar fisicamente
    product.setActive(false);
    productRepository.save(product);
}
```

Para borrado fisico (admin) se mantiene un metodo separado con nombre
explicito: `permanentlyDeleteProduct()`.

### Archivos Modificados
- `ProductService.java`

---

## Bug #16: Admin puede desactivarse a si mismo

**Fecha:** 2026-02-07
**Estado:** OK

### Sintomas
- Un administrador podia llamar a `PUT /api/admin/users/{suPropiId}/status`
  con `active = false` y desactivar su propia cuenta.
- Quedaba completamente bloqueado sin poder reactivarse.

### Causa Raiz
`AdminController.updateUserStatus()` no verificaba si el usuario objetivo era
el mismo que el solicitante.

### Solucion
Añadir validacion en el servicio:

```java
public void updateUserStatus(Long targetUserId, boolean active, Long requestingAdminId) {
    if (targetUserId.equals(requestingAdminId)) {
        throw new BadRequestException("No puedes desactivar tu propia cuenta de administrador");
    }
    // ... resto de la logica
}
```

### Archivos Modificados
- `AdminService.java`

---

## Bug #17: JWT expirado no devuelve 401

**Fecha:** 2026-02-07
**Estado:** OK

### Sintomas
- Al enviar un token expirado, el servidor devolvio 403 Forbidden en lugar
  de 401 Unauthorized.
- El cliente no podia distinguir entre "no tienes permiso" y "tu sesion expiro".

### Causa Raiz
`JwtAuthenticationFilter` capturaba todas las excepciones de JWT y las trataba
igual, dejando que Spring Security devolviera 403 generico.

### Solucion
Distinguir el tipo de error JWT y enviar respuesta clara:

```java
try {
    username = jwtService.extractUsername(token);
} catch (ExpiredJwtException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{\"error\": \"Token expirado. Inicia sesion de nuevo.\"}");
    return;
} catch (JwtException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{\"error\": \"Token invalido.\"}");
    return;
}
```

### Archivos Modificados
- `JwtAuthenticationFilter.java`

---

## Bug #18: PriceMonitorJob no detiene procesamiento ante error de Keepa

**Fecha:** 2026-02-08
**Estado:** OK

### Sintomas
- Cuando Keepa API devolvio error 429 (rate limit), `PriceMonitorJob` seguia
  intentando procesar los 200+ productos restantes, lanzando 200+ intentos
  fallidos en rafaga.
- Los logs se llenaban de errores y se gastaba la quota diaria de Keepa.

### Causa Raiz
El job no tenia logica de circuit breaker. Cuando el primer lote fallaba por
rate limit, el siguiente lote se intentaba igualmente.

### Solucion
Implementar circuit breaker manual en el job:

```java
int consecutiveErrors = 0;
final int MAX_CONSECUTIVE_ERRORS = 5;

for (List<Product> batch : batches) {
    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
        log.warn("Demasiados errores consecutivos. Abortando job para evitar " +
                 "saturar Keepa. Se reintentara en el siguiente ciclo.");
        break;
    }
    try {
        processBatch(batch);
        consecutiveErrors = 0;  // Reset en exito
    } catch (Exception e) {
        consecutiveErrors++;
        log.error("Error en lote {}: {}", batchIndex, e.getMessage());
    }
}
```

### Archivos Modificados
- `PriceMonitorJob.java`

---

## Bug #19: BCrypt hash expuesto en log de debug

**Fecha:** 2026-02-08
**Estado:** OK

### Sintomas
- Con el perfil dev activo (`show-sql: true`), la query de INSERT al crear un
  usuario mostraba el hash BCrypt de la contrasena en los logs.
- Aunque el hash es unidireccional, su presencia en logs supone un riesgo.

### Causa Raiz
Hibernate con `show-sql: true` loguea todos los parametros de las queries,
incluyendo el campo `password`.

### Solucion
1. En dev, usar `logging.level.org.hibernate.type: TRACE` solo para queries
   que no involucren la tabla `users`.
2. Marcar el campo password con `@JsonIgnore` para prevenir serializacion
   accidental:

```java
@JsonIgnore
@Column(nullable = false)
private String password;
```

3. En la configuracion de logs dev, excluir queries de autenticacion:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: WARN  # No loguear valores de parametros
```

### Archivos Modificados
- `User.java`
- `application.yml`

---

## Bug #20: Productos inactivos aparecen en busqueda

**Fecha:** 2026-02-09
**Estado:** OK

### Sintomas
- El endpoint `GET /api/products/search?name=ibuprofeno` devolvio productos
  con `active = false` que habian sido borrados (soft delete).

### Causa Raiz
La query de busqueda con JPQL no incluia el filtro `AND p.active = true`.
Solo el listado principal filtraba por activo.

### Solucion
Anadir `AND p.active = true` a todas las queries de busqueda:

```java
@Query("SELECT p FROM Product p WHERE p.user.id = :userId " +
       "AND p.active = true " +   // Siempre excluir borrados logicamente
       "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
       "AND (:category IS NULL OR p.category = :category)")
Page<Product> searchProducts(...);
```

Adicionalmente, anadir test que verifique que los borrados logicamente
nunca aparecen en ninguna query de busqueda.

### Archivos Modificados
- `ProductRepository.java`

---

## Bug #21: Connection pool agotado bajo carga moderada

**Fecha:** 2026-02-09
**Estado:** OK

### Sintomas
- Con 15 usuarios concurrentes, la app empezaba a devolver errores:
  `Unable to acquire JDBC Connection`.
- El tiempo de espera de conexion (30s) se agotaba.

### Causa Raiz
`PriceMonitorJob` abria transacciones de larga duracion durante el procesamiento
de cada lote de productos, manteniendo conexiones del pool ocupadas durante
varios minutos.

El pool de 10 conexiones por defecto era insuficiente cuando el job estaba
corriendo y habia usuarios usando la app simultaneamente.

### Solucion
1. Aumentar el pool a 20 conexiones en `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

2. Dividir la transaccion del job en transacciones mas cortas por producto:

```java
// En lugar de una transaccion para todo el lote,
// usar @Transactional(propagation = REQUIRES_NEW) por producto
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processSingleProduct(Product product, CompetitorPrice newPrice) {
    // Guardado rapido, libera la conexion inmediatamente
}
```

### Archivos Modificados
- `application.yml`
- `PriceMonitorJob.java`

---

## Bug #22: ChangeType INITIAL sobreescrito al actualizar

**Fecha:** 2026-02-10
**Estado:** OK

### Sintomas
- El historial de un producto que nunca habia cambiado de precio mostraba
  entradas de tipo INITIAL repetidas en lugar de mostrar una sola INITIAL
  y luego NO_CHANGE.

### Causa Raiz
`ProductService.updateProduct()` siempre registraba una nueva entrada en
PriceHistory, incluso cuando el precio no habia cambiado. Ademas, en esas
entradas sin cambio asignaba incorrectamente `ChangeType.INITIAL`.

### Solucion
Solo registrar historial cuando el precio realmente cambia:

```java
public ProductResponse updateProduct(Long productId, ProductRequest request, Long userId) {
    // ... cargar y verificar producto

    BigDecimal oldPrice = product.getCurrentPrice();
    BigDecimal newPrice = request.getCurrentPrice();

    if (newPrice != null && newPrice.compareTo(oldPrice) != 0) {
        // Solo registrar si hay cambio real
        ChangeType changeType = newPrice.compareTo(oldPrice) > 0
            ? ChangeType.INCREASE
            : ChangeType.DECREASE;

        PriceHistory history = PriceHistory.builder()
            .product(product)
            .price(newPrice)
            .previousPrice(oldPrice)
            .changeType(changeType)
            .recordedAt(LocalDateTime.now())
            .build();
        priceHistoryRepository.save(history);
    }
    // ... actualizar y guardar producto
}
```

### Archivos Modificados
- `ProductService.java`

---

## Bug #23: NPE en login tras migracion multi-tenancy

**Fecha:** 2026-02-12
**Estado:** OK

### Sintomas
- Tras añadir la entidad `Company` y el campo `company_id` a `User`, el login
  devolvía un 500 Internal Server Error.
- Stack trace: `NullPointerException` en `UserPrincipal.create()` al acceder
  a `user.getCompany().getId()`.

### Causa Raiz
La relación `@ManyToOne(fetch = FetchType.LAZY)` entre `User` y `Company`
causaba que `user.getCompany()` fuese un proxy no inicializado fuera de la
transacción de carga del usuario. Al intentar acceder a `.getId()` fuera
del contexto transaccional, lanzaba NPE.

### Solucion
Usar `LEFT JOIN FETCH` en la query de `UserRepository` para cargar
eagerly la relación User→Company en un solo SQL:

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

@Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);
```

### Archivos Modificados
- `UserRepository.java`

---

## Bug #24: AnalyticsController usa userId en vez de companyId

**Fecha:** 2026-02-13
**Estado:** OK

### Sintomas
- Los endpoints de analytics (`/api/analytics/recommendations`,
  `/api/analytics/alerts`, `/api/analytics/analyze`) devolvían datos
  incorrectos o vacíos.
- Un usuario veía recomendaciones de otros usuarios en lugar de datos
  de su empresa.

### Causa Raiz
Tres métodos en `AnalyticsController` usaban `userPrincipal.getId()` (el ID
del usuario individual) cuando debían usar `userPrincipal.getCompanyId()` para
filtrar datos a nivel de empresa. Esto rompía el aislamiento multi-tenant.

### Solucion
Reemplazar `userPrincipal.getId()` por `userPrincipal.getCompanyId()` en:

```java
// Antes (incorrecto):
priceAnalysisService.getPendingRecommendations(userPrincipal.getId(), pageable);
priceAnalysisService.getUnreadAlerts(userPrincipal.getId(), pageable);
priceAnalysisService.analyzeAllProductsForUser(userPrincipal.getId());

// Después (correcto):
priceAnalysisService.getPendingRecommendations(userPrincipal.getCompanyId(), pageable);
priceAnalysisService.getUnreadAlerts(userPrincipal.getCompanyId(), pageable);
priceAnalysisService.analyzeAllProductsForUser(userPrincipal.getCompanyId());
```

### Archivos Modificados
- `AnalyticsController.java`

---

## Bug #25: Sin endpoint para crear empleados en empresa

**Fecha:** 2026-02-13
**Estado:** OK

### Sintomas
- Tras implementar el modelo multi-tenancy con roles COMPANY_ADMIN y EMPLOYEE,
  no existía ningún endpoint para que un COMPANY_ADMIN pudiera crear usuarios
  de tipo EMPLOYEE dentro de su empresa.
- La única vía de registro creaba siempre un COMPANY_ADMIN con una nueva empresa.

### Causa Raiz
El endpoint `/api/auth/register` fue diseñado como registro público (modelo SaaS)
que crea una empresa y un COMPANY_ADMIN. No se implementó un flujo separado para
añadir empleados a una empresa existente.

### Solucion
Crear un nuevo endpoint `POST /api/auth/create-employee` restringido a
COMPANY_ADMIN y ADMIN:

```java
// AuthController.java
@PostMapping("/create-employee")
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
public ResponseEntity<ApiResponse<AuthResponse>> createEmployee(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody CreateEmployeeRequest request) {
    AuthResponse response = authService.createEmployee(
        userPrincipal.getCompanyId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Empleado creado exitosamente"));
}
```

Nuevo DTO `CreateEmployeeRequest` con username, email y password.
Nuevo método `AuthService.createEmployee()` que crea el usuario con rol EMPLOYEE
y lo asocia a la empresa del admin autenticado.

### Archivos Modificados
- `AuthController.java`
- `AuthService.java`
- `AuthDTOs.java` (nuevo DTO `CreateEmployeeRequest`)

---

## Advertencias Pendientes (No criticas)

| Advertencia | Descripcion | Accion Recomendada |
|-------------|-------------|-------------------|
| DDL update en dev | Hibernate update no gestiona renombrados de columnas | Usar Flyway para migraciones en prod |
| Simple Cache | La cache en memoria se pierde al reiniciar | Migrar a Redis en produccion |
| No paginacion en historial | PriceHistory puede crecer sin limite | Anadir paginacion y TTL de datos historicos |
| Jsoup sin usar | Dependencia incluida pero sin implementacion | Implementar o eliminar |

---

## Plantilla para Nuevos Bugs

```markdown
## Bug #X: [Titulo descriptivo]

**Fecha:** YYYY-MM-DD
**Estado:** Abierto / En progreso / OK

### Sintomas
[Descripcion de lo que el usuario observa]

### Causa Raiz
[Explicacion tecnica del problema]

### Solucion
[Codigo o pasos para resolver]

### Archivos Modificados
- [lista de archivos]
```

---

## Bug #31: StackOverflowError (Recursion) en JSON response

**Fecha:** 2026-02-14
**Estado:** OK

### Sintomas
- Peticiones a endpoints que devuelven `User` o `Company` fallaban con `500 Internal Server Error` sin mensaje claro en la respuesta HTTP.
- Logs mostraban `StackOverflowError` o timeouts por serialización infinita.

### Causa Raiz
- Relación bidireccional `Company <-> User` y `User <-> Product`.
- Jackson intentaba serializar `Company -> users -> Company -> users...` infinitamente.

### Solucion
- Añadir `@JsonIgnore` en las relaciones inversas (`@OneToMany`) en las entidades.
- `Company.users`, `Company.products`, `User.createdProducts`.

### Archivos Modificados
- `Company.java`
- `User.java`

---

## Bug #32: LazyInitializationException en Admin Dashboard

**Fecha:** 2026-02-14
**Estado:** OK

### Sintomas
- `GET /api/admin/dashboard` retornaba `500 Internal Server Error`.
- Logs: `org.hibernate.LazyInitializationException: could not initialize proxy - no Session`.

### Causa Raiz
- El controlador accedía a `user.getCompany().getName()` fuera de una transacción.
- Al ser una relación `LAZY`, Hibernate intentaba cargar la empresa pero la sesión ya estaba cerrada.

### Solucion
- Añadir `@Transactional(readOnly = true)` a los métodos del `AdminController`.

### Archivos Modificados
- `AdminController.java`

---

## Bug #33: DatabaseSeeder omite empresas si una existe

**Fecha:** 2026-02-14
**Estado:** OK

### Sintomas
- Al borrar y recrear la BD, si existía la empresa "Tech Solutions", el seeder no creaba "Global Retail" ni "Consulting Pro" a pesar de ser solicitadas.

### Causa Raiz
- Lógica de comprobación global: `if (exists("TECH001")) return;`.
- Si la primera empresa existía, abortaba todo el proceso de seed de pruebas.

### Solucion
- Refactorizar a comprobación individual por empresa (`createCompanyIfNotExists`).

### Archivos Modificados
- `DatabaseSeeder.java`

---

## Nota: Widgets del Dashboard sin contadores numericos

**Fecha:** 2026-02-23
**Estado:** Decisión de diseño (no es un bug)

### Contexto
Los widgets del `DashboardScreen` (Android) muestran solo icono + titulo, sin contadores
numericos. Esto es **intencional**: el dashboard funciona como menu de navegacion rapida
a las secciones principales (Usuarios, Productos, Alertas, Administracion).
Los contadores se eliminaron para simplificar la UI y evitar llamadas adicionales al backend
en cada carga del dashboard. La informacion detallada esta disponible en cada pantalla
individual al navegar a ella.

---

## Bug #34: Texto invisible en campos de login (tema claro del sistema)

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- Los campos de email/usuario y contrasena en LoginScreen eran invisibles: no se veia el texto escrito ni las labels de los campos.
- Solo ocurria cuando el dispositivo estaba en modo claro del sistema.

### Causa Raiz
- `themes.xml` fuerza fondo DarkNavy en todas las pantallas.
- Pero `PriceWiseTheme` usaba `isSystemInDarkTheme()` para elegir el esquema de colores.
- Si el telefono estaba en modo claro, Compose cargaba `LightColorScheme` (texto oscuro sobre fondo oscuro = invisible).
- Ademas, los colores del `DarkColorScheme` (`onSurface`, `onSurfaceVariant`, `outline`) tenian poco contraste.

### Solucion
1. Forzar `darkTheme: Boolean = true` en `PriceWiseTheme` (nunca usar `isSystemInDarkTheme()`).
2. Mejorar contraste en DarkColorScheme: `onSurface = Color.White`, `onSurfaceVariant = Color(0xFFCFD8DC)`, `outline = Color(0xFF90A4AE)`.

### Archivos Modificados
- `Theme.kt`

---

## Bug #35: Acentos bloqueados en campo de usuario del login

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- El campo de email/usuario en LoginScreen no permitia escribir caracteres con acento (e, a, i, etc.).

### Causa Raiz
- El campo usaba `KeyboardType.Email` que restringe los caracteres disponibles en el teclado virtual.
- Como el campo acepta tanto email como nombre de usuario, `KeyboardType.Email` es demasiado restrictivo.

### Solucion
- Cambiar a `KeyboardType.Text` en el campo de email/usuario de LoginScreen.
- RegisterScreen mantiene `KeyboardType.Email` porque su campo es exclusivamente para email.

### Archivos Modificados
- `LoginScreen.kt`

---

## Bug #36: Crash por SecurityException al abrir la app (ACCESS_NETWORK_STATE)

**Fecha:** 2026-02-23
**Estado:** OK

### Sintomas
- La app crasheaba inmediatamente al abrirla con `java.lang.SecurityException: ConnectivityService: Neither user nor current process has android.permission.ACCESS_NETWORK_STATE`.

### Causa Raiz
- La clase `NetworkObserver` (creada para el offline banner) usa `ConnectivityManager.registerNetworkCallback()` que requiere el permiso `ACCESS_NETWORK_STATE`.
- El permiso no estaba declarado en `AndroidManifest.xml`.

### Solucion
- Anadir `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` al AndroidManifest.xml.

### Archivos Modificados
- `AndroidManifest.xml`

---

## Bug #37: lintDebug falla con AGP 8.5.2 + Java 25

**Fecha:** 2026-02-23
**Estado:** OK (workaround)

### Sintomas
- `./gradlew lintDebug` fallaba con errores internos de `AndroidLintWorkAction` y `Already disposed: MessageBus`.

### Causa Raiz
- Incompatibilidad entre AGP 8.5.2, compileSdk 35, Gradle 9.1 y Java 25.
- El motor de lint interno no soporta esta combinacion de versiones.

### Solucion (workaround)
- Desactivar lint como tarea bloqueante: `abortOnError = false`, `checkReleaseBuilds = false`.
- Desactivar todas las tareas lint: `tasks.matching { it.name.startsWith("lint") }.configureEach { enabled = false }`.
- Pendiente de resolver definitivamente al actualizar AGP a una version compatible.

### Archivos Modificados
- `build.gradle.kts`
