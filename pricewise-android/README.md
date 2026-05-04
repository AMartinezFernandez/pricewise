# PriceWise Android

App móvil que consume la API de PriceWise. Jetpack Compose, Hilt y arquitectura MVVM.

## Requisitos

1. Android Studio Koala o superior.
2. JDK 17.
3. Android SDK 36 (`compileSdk`), build-tools 36.0.0.
4. `minSdk = 26`, `targetSdk = 35`.

Versiones principales en `gradle/libs.versions.toml`:

- AGP 8.13.2, Kotlin 2.0.20, KSP 2.0.20-1.0.24.
- Compose BOM 2026.02.00, Navigation Compose 2.9.7.
- Hilt 2.51.1, Retrofit 2.11.0, Moshi 1.15.1, DataStore 1.2.0.

## Configuración inicial

Antes del primer build hay que crear dos archivos locales fuera de Git.

### local.properties

Android Studio genera este archivo automáticamente al abrir el proyecto, con `sdk.dir` apuntando al SDK local. Si compilas solo desde CLI sin haber abierto Studio, créalo a mano:

```properties
sdk.dir=/ruta/absoluta/al/Android/sdk
GOOGLE_WEB_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
```

`sdk.dir` es obligatorio para que Gradle encuentre el SDK. `GOOGLE_WEB_CLIENT_ID` es el Client ID de Google Sign-In obtenido en Google Cloud Console; si se omite, la app compila pero el botón «Entrar con Google» queda inoperativo.

### keystore.properties

Solo necesario para build release firmado. En la raíz del módulo, mismo nivel que `settings.gradle.kts`:

```properties
storeFile=/ruta/absoluta/al/pricewise-release.jks
storePassword=...
keyAlias=pricewise
keyPassword=...
```

Si no existe, `./gradlew assembleRelease` falla con `Keystore file not set for signing config release`. El APK debug (`./gradlew assembleDebug`) se genera sin necesidad de keystore.

## Compilar y ejecutar

```bash
# APK debug, BASE_URL apunta a http://10.0.2.2:9090 (backend local desde el emulador)
./gradlew assembleDebug

# APK release firmado, BASE_URL apunta al backend de Railway
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/{debug,release}/` con el patrón de nombre `PriceWise-v<versionName>-<buildType>.apk` (configurado en `app/build.gradle.kts`).

Instalación en dispositivo o emulador conectado:

```bash
adb install app/build/outputs/apk/debug/*.apk
adb shell monkey -p com.alvaro.pricewise -c android.intent.category.LAUNCHER 1
```

Si se reinstala un release sobre una versión debug previa, primero desinstalar:

```bash
adb uninstall com.alvaro.pricewise
```

## Tests

```bash
./gradlew test
./gradlew lintDebug
```

## Estructura de paquetes

```
com.alvaro.pricewise
├── data/               modelos API, Retrofit, repositorios, DataStore
├── di/                 módulos Hilt
├── ui/                 pantallas Compose por feature
│   ├── auth/           login, registro, Google Sign-In
│   ├── products/       listado, detalle, edición
│   ├── search/         búsqueda por ASIN
│   ├── tracking/       productos monitorizados
│   ├── alerts/         reglas de alerta e histórico
│   ├── dashboard/      métricas del usuario
│   ├── admin/          panel solo ADMIN
│   └── settings/       perfil, tema, logout
├── util/               NetworkObserver, SessionManager, helpers
├── MainActivity.kt
└── PriceWiseApp.kt     Application con @HiltAndroidApp
```

## Documentación relacionada

1. `../README.md`, visión general del proyecto.
2. `../docs/ARQUITECTURA.md`, capas y patrones del backend y la app.
3. `../docs/SEGURIDAD.md`, autenticación y rehidratación de token.
