# PriceWise

Sistema de comparación y monitorización de precios de productos para PYMEs. Trabajo Fin de Ciclo del grado DAM (curso 2025-2026).

Ofrece a cada empresa un espacio aislado (multi-tenancy) donde registrar su catálogo, seguir precios de competidores en Amazon y recibir alertas configurables cuando se desvían de los umbrales definidos.

## Arquitectura

El repositorio contiene dos aplicaciones independientes que se comunican por una API REST autenticada con JWT:

| Módulo | Descripción | Tecnología principal |
|---|---|---|
| [`pricewise-backend/`](pricewise-backend/) | API REST, autenticación, persistencia, jobs y análisis de precios | Spring Boot 3.3, Java 17, PostgreSQL 15, Flyway, Quartz |
| [`pricewise-android/`](pricewise-android/) | Aplicación móvil que consume la API | Kotlin, Jetpack Compose, Hilt, Retrofit, Moshi, DataStore |

El backend está desplegado en Railway (plan Pro) con PostgreSQL gestionado. La aplicación móvil se distribuye como APK firmado en [GitHub Releases](https://github.com/AMartinezFernandez/pricewise/releases).

## Arranque rápido

Requisitos mínimos: Java 17, Maven 3.8+, PostgreSQL 14, Android Studio con SDK 36.

Pasos previos: crear la base de datos (`createdb pricewise_db` o `CREATE DATABASE pricewise_db;` desde `psql`) y editar `.env` con credenciales reales. Para el módulo Android, `local.properties` debe definir `sdk.dir` (lo genera Android Studio al abrir el proyecto; si compilas solo desde CLI, créalo a mano).

```bash
# Backend
cd pricewise-backend
cp .env.example .env          # editar credenciales locales
mvn spring-boot:run           # http://localhost:9090

# Android (en otra terminal)
cd pricewise-android
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
```

La guía completa del backend está en [`docs/README.md`](docs/README.md) (instalación, configuración, endpoints). La del módulo Android en [`pricewise-android/README.md`](pricewise-android/README.md).

## Documentación

Toda la documentación técnica se encuentra en [`docs/`](docs/):

- [`docs/README.md`](docs/README.md) — guía detallada del backend (instalación, perfiles, endpoints).
- [`ARQUITECTURA.md`](docs/ARQUITECTURA.md) — capas, patrones, justificaciones técnicas.
- [`SEGURIDAD.md`](docs/SEGURIDAD.md) — JWT, OAuth2, cifrado AES-256 de API keys, RBAC.
- [`FLYWAY.md`](docs/FLYWAY.md) — migraciones de base de datos (V1-V8).
- [`MEJORAS_FUTURAS.md`](docs/MEJORAS_FUTURAS.md) — líneas de evolución previstas.

La colección Postman completa está en [`postman/`](postman/).

La API está disponible además como documentación interactiva en Swagger UI sobre el despliegue de producción:
[https://backend-production-5a519.up.railway.app/swagger-ui/index.html](https://backend-production-5a519.up.railway.app/swagger-ui/index.html)

## Autor

Álvaro Martínez Fernández — TFC DAM, curso 2025-2026.

## Licencia

Publicado bajo [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](https://creativecommons.org/licenses/by-nc/4.0/). Uso no comercial con atribución al autor. Texto completo en [`LICENSE`](LICENSE).
