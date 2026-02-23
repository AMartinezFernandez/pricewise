plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.alvaro.pricewise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alvaro.pricewise"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google OAuth Web Client ID (from Google Cloud Console)
        // Configurar en local.properties: GOOGLE_WEB_CLIENT_ID=tu-client-id.apps.googleusercontent.com
        val googleClientId = project.findProperty("GOOGLE_WEB_CLIENT_ID")?.toString()
        if (googleClientId.isNullOrBlank() || googleClientId == "placeholder") {
            logger.warn("AVISO: GOOGLE_WEB_CLIENT_ID no configurado. Google Sign-In no funcionará.")
            logger.warn("       Añade GOOGLE_WEB_CLIENT_ID=xxx a local.properties")
        }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",
            "\"${googleClientId ?: ""}\"")
    }

    buildTypes {
        debug {
            // Emulador: 10.0.2.2 es localhost desde el emulador de Android
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9090/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: Reemplazar con la URL real de producción (HTTPS)
            buildConfigField("String", "BASE_URL", "\"https://api.pricewise.example.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // AGP 8.5.2 + compileSdk 35 + Java 25 causa fallos en AndroidLintWorkAction.
        // Desactivar lint como tarea bloqueante hasta que se actualice AGP.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt - Inyeccion de dependencias
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp - Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi - Serialización JSON
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // DataStore - Persistencia del token
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Coil - Carga de imágenes
    implementation(libs.coil.compose)

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    debugImplementation(libs.androidx.ui.tooling)
}
