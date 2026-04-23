# PriceWise Android

Aplicación móvil que consume la API de PriceWise. Desarrollada con Jetpack Compose, inyección de dependencias con Hilt y arquitectura MVVM.

## Requisitos

- Android Studio Koala o superior.
- JDK 17.
- Android SDK 36 (compileSdk), build-tools 36.0.0.
- `minSdk = 26`, `targetSdk = 35`.

Versiones principales (ver [`gradle/libs.versions.toml`](gradle/libs.versions.toml)):

- AGP 8.13.2, Kotlin 2.0.20, KSP 2.0.20-1.0.24.
- Compose BOM 2026.02.00, Navigation Compose 2.9.7.
- Hilt 2.51.1, Retrofit 2.11.0, Moshi 1.15.1, DataStore 1.2.0.

## Configuración inicial

Antes del primer build debes crear dos archivos locales fuera de Git:

### 1. `local.properties`

Añade el Client ID de Google Sign-In (se obtiene en Google Cloud Console):

```properties
GOOGLE_WEB_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
```

Si lo omites, la app compila pero el botón «Entrar con Google» queda inoperativo.

### 2. `keystore.properties` (solo para build release)

En la raíz del módulo (mismo nivel que `settings.gradle.kts`):

```properties
storeFile=/ruta/absoluta/al/pricewise-release.jks
storePassword=...
keyAlias=pricewise
keyPassword=...
```

Si el archivo no existe, Gradle salta la firma y el APK release se genera sin `signingConfig` (sirve para probar el proceso, no para distribución).

## Compilar y ejecutar

```bash
# APK de desarrollo (BASE_URL → http://10.0.2.2:9090, emulador apunta al backend local)
./gradlew assembleDebug

# APK release firmado (BASE_URL → backend desplegado en Railway)
./gradlew assembleRelease
```

El APK resultante queda en `app/build/outputs/apk/{debug,release}/`.

Para instalar en un dispositivo o emulador conectado:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.alvaro.pricewise -c android.intent.category.LAUNCHER 1
```

Si reinstalas un release sobre una versión debug previa, primero desinstala:

```bash
adb uninstall com.alvaro.pricewise
```

## Tests

```bash
./gradlew test          # tests unitarios (SearchViewModel, Result, etc.)
./gradlew lintDebug     # análisis estático Android Lint
```

## Estructura de paquetes

```
com.alvaro.pricewise
├── data/               # modelos API, Retrofit, repositorios, DataStore
├── di/                 # módulos Hilt (AppModule, NetworkModule, …)
├── ui/                 # pantallas Compose organizadas por feature
│   ├── auth/           # login, registro, Google Sign-In
│   ├── products/       # listado, detalle, edición
│   ├── search/         # búsqueda por ASIN
│   ├── tracking/       # productos monitorizados
│   ├── alerts/         # reglas de alerta e histórico
│   ├── dashboard/      # métricas del usuario
│   ├── admin/          # panel solo para rol ADMIN
│   └── settings/       # perfil, tema, logout
├── util/               # NetworkObserver, SessionManager, helpers
├── MainActivity.kt
└── PriceWiseApp.kt     # Application con @HiltAndroidApp
```

## Documentación relacionada

- [`../README.md`](../README.md) — visión general del proyecto.
- [`../docs/ARQUITECTURA.md`](../docs/ARQUITECTURA.md) — capas y patrones del backend y la app.
- [`../docs/SEGURIDAD.md`](../docs/SEGURIDAD.md) — autenticación y rehidratación de token en el cliente.
