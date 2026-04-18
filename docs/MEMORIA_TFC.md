# PriceWise
## Sistema de Monitorización de Precios para PYMEs

**Trabajo de Fin de Ciclo**
**Desarrollo de Aplicaciones Multiplataforma**

---

**Alumno:** Álvaro Martínez
**Tutor:**
**Centro:**
**Curso:** 2025-2026

---

## Índice

- Abstract
1. Resumen y Objetivos
2. Antecedentes
3. Análisis y Especificación de Requisitos
4. Propuesta de Solución
   - 4.1 Visión general de la arquitectura
   - 4.2 Arquitectura del backend
   - 4.3 Modelo de datos (9 tablas, Flyway V1–V8)
   - 4.4 Diseño de la API REST
   - 4.5 Diseño de seguridad
   - 4.6 Justificaciones de decisiones técnicas
5. Plan de Trabajo
   - 5.1 Metodología de desarrollo
   - 5.2 Planificación por fases (estimada vs. real)
   - 5.3 Presupuesto
   - 5.4 Herramientas utilizadas
6. Desarrollo de la Solución
   - 6.1 Tecnologías y versiones
   - 6.2 Migraciones de base de datos con Flyway
   - 6.3 Estructura del proyecto
   - 6.4 Módulo de autenticación
   - 6.5 Módulo de productos
   - 6.6 Integración con Keepa API
   - 6.7 Motor de análisis de precios
   - 6.8 Monitorización periódica
   - 6.9 Gestión de excepciones
   - 6.10 Configuración y entornos
   - 6.11 Cliente móvil Android
   - 6.12 Pruebas
7. Despliegue e Instalación
8. Evolución y Trabajo Futuro
9. Bibliografía

---

---

# Abstract

Small and medium-sized businesses that sell physical products often struggle to keep their prices competitive against Amazon. Checking competitor prices manually is time-consuming and error-prone, and by the time a price gap is noticed, sales may already have been lost.

PriceWise is a price monitoring system built to address this problem. It consists of a REST API backend developed with Spring Boot 3 and a native Android client built with Kotlin and Jetpack Compose. The backend integrates with the Keepa API to retrieve real-time and historical Amazon prices for any product identified by its ASIN. Prices are stored in a PostgreSQL database managed through Flyway migrations, and a scheduled job running every six hours keeps competitor data up to date for monitored products.

The system supports multi-company operation: each company manages its own product catalogue, and all data is isolated by company identifier extracted from the JWT authentication token. Three user roles are defined — ADMIN, COMPANY_ADMIN, and EMPLOYEE — with access controlled through Spring Security.

A price analysis engine compares each product's own price against the latest Amazon price and generates prioritised recommendations when the difference exceeds configurable thresholds. The Android application covers the core MVP flows: authentication, product management, Amazon price lookup, and alert review.

The project delivers a functional and deployable MVP, validated with 206 unit and integration tests covering the business logic and HTTP layer.

---

# 1. Resumen y Objetivos

## 1.1 Contexto del Problema

Las pequeñas y medianas empresas que venden productos en canales físicos y digitales tienen un problema concreto con los precios: no saben si están cobrando más o menos que Amazon, y cuando lo descubren suele ser tarde. Amazon actualiza sus precios varias veces al día con algoritmos automáticos; una PYME que revisa precios una vez a la semana va siempre por detrás.

El proceso habitual es visitar los sitios de los competidores a mano, apuntar precios en una hoja de cálculo y reaccionar cuando un cliente ya se ha ido a comprar más barato. Funciona más o menos si tienes diez productos; con cincuenta ya es inviable.

## 1.2 Solución Propuesta

PriceWise es una aplicación formada por una API REST backend y un cliente móvil para Android que permite a las PYMEs:

- Gestionar su catálogo de productos con control de precios de coste y venta.
- Consultar el precio actual de cada producto en Amazon España de forma controlada y bajo demanda.
- Recibir recomendaciones automáticas sobre si el precio propio es competitivo respecto a la competencia.
- Consultar el historial de evolución de precios tanto propios como de la competencia.

Los precios de Amazon se obtienen a través de **Keepa API**, un servicio especializado con historial de 90 días por producto. Usé Keepa en lugar de scraping directo porque Amazon bloquea scrapers con bastante eficacia y además viola sus términos de uso, lo que descarta esa opción para cualquier proyecto serio.

## 1.3 Justificación del Alcance

El proyecto se centra en un **MVP** con tres funcionalidades principales:

1. Gestión completa del catálogo de productos propios.
2. Consulta de precios de una fuente de competencia concreta y bien definida: Amazon España vía Keepa API.
3. Generación de recomendaciones básicas de precio basadas en la comparativa.

Adicionalmente, el sistema incluye funcionalidades de automatización (monitorización periódica programada) y un panel de administración que, si bien exceden el MVP mínimo, han podido desarrollarse dentro del tiempo del proyecto y quedan documentadas como **mejoras implementadas**, no como requisitos del alcance base.

Las notificaciones push al dispositivo móvil y la integración con fuentes de datos adicionales se identifican explícitamente como **líneas de trabajo futuro** fuera del alcance de este TFC.

---

## 1.4 Motivación Personal

Elegí este proyecto porque durante las prácticas en empresa vi a un cliente perder ventas repetidamente porque Amazon bajó un producto un 15% y ellos no se enteraron hasta dos semanas después. El problema era real, recurrente y sin solución accesible para empresas pequeñas. Eso me pareció un buen punto de partida para un TFC: algo que resuelve un problema concreto, no un ejercicio académico.

---

# 2. Antecedentes

Antes de diseñar PriceWise busqué qué herramientas existen ya para monitorizar precios de competidores. El mercado tiene soluciones, pero ninguna encajaba bien con el perfil de una PYME pequeña que quiere algo sencillo, accesible desde el móvil y sin pagar cientos de euros al mes.

## 2.1 Soluciones Existentes

**Prisync** es probablemente la herramienta más conocida en el segmento medio. Funciona como SaaS: el usuario importa su catálogo, configura los competidores a vigilar y la plataforma rastrea precios periódicamente. Su plan básico cuesta alrededor de 59 €/mes para 100 productos y sube rápidamente. No tiene aplicación móvil nativa; el acceso es web. La monitorización se basa en scraping de páginas de producto, lo que la hace dependiente de que Amazon no cambie su estructura HTML.

**Minderest** es una empresa española con una propuesta más completa: análisis de posicionamiento, informes de mercado, integración con Google Shopping. El precio no es público pero está orientado a empresas medianas y grandes; los planes de entrada suelen superar los 300 €/mes. Tampoco tiene aplicación móvil y requiere un proceso de onboarding manual con su equipo comercial.

**Wiser** (absorbida por Commerce IQ en 2022) es una plataforma americana enfocada en marcas que venden en Amazon Vendor o Seller Central. Ofrece análisis de Buy Box, gestión de inventario y repricing automático. Es la opción más completa del mercado, pero su precio es enterprise y su configuración requiere soporte técnico dedicado. Fuera del alcance de cualquier PYME.

**Netrivals** es otra empresa española con un perfil similar a Minderest: scraping de webs y marketplaces, dashboards de análisis, integración con Prestashop y Magento. Los planes de entrada rondan los 200-350 €/mes. Sin app móvil. Orientada a e-commerce con catálogos de decenas o cientos de productos.

## 2.2 Análisis Comparativo

| Funcionalidad | Prisync | Minderest | Wiser | Netrivals | **PriceWise** |
|---|---|---|---|---|---|
| Precio de entrada | ~59 €/mes | >300 €/mes | Enterprise | ~200 €/mes | **Gratuito (autoalojado)** |
| Aplicación móvil nativa | No | No | No | No | **Sí (Android)** |
| Integración Amazon vía API oficial | No (scraping) | No (scraping) | Parcial | No (scraping) | **Sí (Keepa API)** |
| Historial de precios propios | No | Sí | Sí | Sí | **Sí** |
| Recomendaciones automáticas de precio | Básicas | Sí | Sí | Básicas | **Sí** |
| Multi-empresa con roles diferenciados | No | Sí | Sí | No | **Sí (ADMIN / COMPANY_ADMIN / EMPLOYEE)** |
| Monitorización periódica automática | Sí | Sí | Sí | Sí | **Sí (Quartz, cada 6h)** |
| Personalizable / código propio | No | No | No | No | **Sí (open source)** |
| Alertas configurables por producto | Limitadas | Sí | Sí | Limitadas | **Sí** |

## 2.3 Deficiencias que PriceWise Cubre

De este análisis se extraen tres brechas que PriceWise trata de cubrir:

**Accesibilidad económica.** Las soluciones del mercado son inaccesibles para PYMEs con catálogos pequeños. Un negocio con 30 productos paga lo mismo que uno con 500. PriceWise es autoalojado: el único coste es el servidor, que puede ser un VPS de 5 €/mes.

**Aplicación móvil.** Ninguna de las soluciones analizadas tiene app nativa. El gestor de una PYME consulta datos desde el móvil; forzarle a usar una web de escritorio no adaptada es una fricción que PriceWise elimina.

**Integración con Amazon vía API oficial.** El scraping directo de Amazon viola sus términos de servicio y es frágil: cualquier cambio en el HTML rompe el parser. PriceWise usa la Keepa API, que es un servicio oficial con datos históricos de 90 días y disponibilidad garantizada contractualmente.

---

# (Objetivos — contenido integrado en sección 1)

## 2.1 Objetivo General

Desarrollar un sistema funcional y desplegable que permita a una PYME gestionar su catálogo de productos y tomar decisiones de precio informadas basándose en datos reales de la competencia, accesible desde un dispositivo móvil Android.

## 2.2 Objetivos Específicos

**Backend (API REST):**
- OB1. Implementar un sistema de autenticación seguro basado en JWT con roles diferenciados (USER y ADMIN).
- OB2. Desarrollar un CRUD completo de productos con validación, paginación y registro automático del historial de precios.
- OB3. Integrar la API de Keepa para obtener el precio actual de cualquier producto de Amazon identificado por su ASIN, de forma controlada y con gestión de errores robusta.
- OB4. Implementar un motor de análisis que compare el precio propio con el de la competencia y genere recomendaciones priorizadas.
- OB5. Documentar todos los endpoints de la API con OpenAPI 3.0 / Swagger UI.

**Cliente Móvil (Android):**
- OB6. Desarrollar una aplicación Android que consuma la API REST y cubra los flujos principales del MVP: registro, gestión de productos y consulta de recomendaciones.

**Calidad:**
- OB7. Escribir tests unitarios que cubran la lógica de negocio crítica del motor de análisis de precios y la capa de servicio de productos.

## 2.3 Objetivos Fuera del Alcance

Lo siguiente queda fuera del alcance de este proyecto y se deja para versiones futuras:

- Notificaciones push al dispositivo móvil.
- Integración con fuentes de datos adicionales a Amazon/Keepa.
- Scraping web automatizado y no supervisado.
- Análisis predictivo o basado en machine learning.
- Panel de analíticas avanzadas con visualización de tendencias.

---

# 3. Análisis y Especificación de Requisitos

## 3.1 Requisitos Funcionales

### RF-01: Registro e Inicio de Sesión
El sistema debe permitir que un usuario se registre con email, nombre de usuario y contraseña, e inicie sesión recibiendo un token de autenticación válido durante 24 horas.

**Criterios de aceptación:**
- El email y el nombre de usuario deben ser únicos en el sistema.
- La contraseña debe tener al menos 6 caracteres.
- El token generado debe identificar unívocamente al usuario y su rol.

### RF-02: Gestión de Productos
El sistema debe permitir al usuario autenticado crear, consultar, modificar y eliminar los productos de su catálogo.

**Criterios de aceptación:**
- Cada producto pertenece exclusivamente a su usuario creador.
- Un usuario no puede acceder a los productos de otro usuario.
- El borrado es lógico (campo `active = false`), preservando el historial.
- El SKU es único dentro del catálogo de cada usuario.
- Al crear un producto se registra automáticamente su precio inicial en el historial.
- Al modificar el precio se registra automáticamente el tipo de cambio (subida/bajada).

### RF-03: Búsqueda y Filtrado de Productos
El sistema debe ofrecer búsqueda de productos por nombre, categoría y marca, con resultados paginados y ordenables.

### RF-04: Consulta de Precio en Amazon
El sistema debe permitir consultar el precio actual de un producto en Amazon España introduciendo su ASIN (Amazon Standard Identification Number), devolviendo el precio, disponibilidad e información del producto.

**Criterios de aceptación:**
- La consulta se realiza a través de Keepa API.
- Si Keepa no está disponible, el sistema devuelve un error descriptivo, no un crash.
- La consulta tiene un tiempo máximo de espera de 30 segundos.

### RF-05: Sincronización de Producto con Amazon
El sistema debe permitir asociar un producto del catálogo con su ASIN de Amazon y almacenar el precio obtenido como precio de competidor, para su posterior análisis.

### RF-06: Generación de Recomendaciones de Precio
El sistema debe analizar automáticamente la diferencia entre el precio propio y el precio de Amazon y generar una recomendación priorizada.

**Criterios de aceptación:**
- Si el precio propio supera en más de un 10% al de Amazon: recomendación PRICE_TOO_HIGH.
- Si el precio propio está más de un 10% por debajo del de Amazon: recomendación PRICE_TOO_LOW (oportunidad de margen).
- Si el precio de Amazon cambia más de un 15% respecto al último registrado: alerta de cambio brusco.
- Las recomendaciones tienen prioridad LOW / MEDIUM / HIGH / URGENT según el porcentaje de diferencia.
- Una recomendación no se duplica si ya existe una pendiente del mismo tipo para el mismo producto.

### RF-07: Historial de Precios
El sistema debe almacenar y permitir consultar el historial de cambios de precio de cada producto propio, indicando el precio anterior, el nuevo precio y el tipo de cambio.

### RF-08: Monitorización Periódica (Mejora Implementada)
El sistema incluye un job programado que, cada 6 horas, actualiza automáticamente el precio de Amazon para todos los productos del catálogo que tengan ASIN configurado y tengan activada la monitorización, y ejecuta el análisis de precios para los usuarios afectados.

### RF-09: Panel de Administración
El sistema debe incluir un área de gestión accesible solo para usuarios con rol ADMIN que permita listar usuarios, cambiar sus roles y estados, y consultar estadísticas generales del sistema.

## 3.2 Requisitos No Funcionales

### RNF-01: Seguridad
- Las contraseñas deben almacenarse hasheadas con BCrypt.
- Todos los endpoints salvo registro, login y health check requieren autenticación JWT.
- Tres roles diferenciados: `ADMIN` (superadministrador del sistema), `COMPANY_ADMIN` (gestor de empresa) y `EMPLOYEE` (empleado de empresa). El acceso a recursos se protege con `@PreAuthorize`.
- Un usuario no puede operar sobre recursos de otra empresa: todas las queries incluyen `companyId` obtenido del token JWT, verificado en capa de servicio.
- Las variables de entorno con secretos (JWT_SECRET, DB_PASSWORD) no deben estar en el repositorio de código. Las API keys de Keepa se gestionan por empresa desde Ajustes > Integración Keepa, cifradas con AES-256 y almacenadas en la tabla `company_api_keys`.

### RNF-02: Rendimiento
- Las consultas de listado de productos no deben generar el problema N+1 de Hibernate.
- Las categorías y marcas del catálogo se cachean en memoria para reducir consultas repetidas.
- La integración con Keepa es asíncrona para no bloquear el hilo HTTP del servidor.
- El pool de conexiones a la base de datos permite hasta 20 conexiones simultáneas.

### RNF-03: Mantenibilidad
- Arquitectura en capas (Controller → Service → Repository → Entity).
- Separación entre entidades JPA y DTOs de entrada/salida.
- Manejo centralizado de excepciones con GlobalExceptionHandler.
- Toda la API documentada con OpenAPI 3.0.

### RNF-04: Portabilidad
- La base de datos se levanta con Docker Compose para reproducibilidad del entorno.
- La configuración sensible se gestiona con variables de entorno.
- Perfiles diferenciados para desarrollo y producción.

## 3.3 Casos de Uso Principales

```
+-------------------+          +-------------------+
|      Usuario      |          |      Admin        |
+-------------------+          +-------------------+
        |                               |
        | Registrarse                   | Listar usuarios
        | Iniciar sesión                | Cambiar rol/estado
        | Gestionar productos           | Ver estadísticas
        | Buscar productos              | Pausar/reanudar scheduler
        | Consultar precio Amazon       |
        | Ver recomendaciones           |
        | Ver historial de precios      |
        | Sincronizar producto          |
```

---

# 5. Plan de Trabajo

## 5.1 Metodología de Desarrollo

El desarrollo lo organicé en fases, cada una centrada en una capa o funcionalidad del sistema. La idea era tener algo funcional al final de cada fase antes de pasar a la siguiente, en lugar de construir todo en paralelo y ensamblar al final. Esto funcionó bien en la práctica: cuando la fase de Keepa reveló que necesitaba refactorizar el modelo de datos para añadir `company_id`, ya tenía el backend de autenticación estable y no tuve que tocar dos capas a la vez.

No seguí ninguna metodología formal tipo Scrum; fue más un tablero personal en Notion con tareas por fase y revisión al final de cada una.

## 5.2 Planificación por Fases (Estimada vs. Real)

La planificación inicial era de 36 días laborables. La real fue de 44, con desviaciones en dos fases concretas.

| Fase | Contenido | Estimado | Real | Desviación | Motivo |
|------|-----------|----------|------|------------|--------|
| 1 | Configuración: Spring Boot, Docker, PostgreSQL | 1 día | 1 día | — | — |
| 2 | Entidades JPA, repositorios, esquema inicial | 3 días | 4 días | +1 | Rediseño del modelo al añadir `company_id` |
| 3 | Autenticación JWT, Spring Security, registro | 2 días | 2 días | — | — |
| 4 | CRUD de productos, historial de precios, caché | 4 días | 5 días | +1 | Índice parcial en PostgreSQL (MySQL no lo soporta) |
| 5 | Integración Keepa API, procesamiento asíncrono | 3 días | 5 días | +2 | Condición de carrera en `@PostConstruct` + semáforo |
| 6 | Motor de análisis, recomendaciones y alertas | 3 días | 3 días | — | — |
| 7 | Scheduler Quartz, monitorización periódica | 2 días | 3 días | +1 | Configuración del job store y gestión de errores por lotes |
| 8 | Panel de administración, gestión de usuarios | 2 días | 2 días | — | — |
| 9 | Excepciones, validaciones, OpenAPI | 2 días | 2 días | — | — |
| 10 | Tests unitarios e integración | 4 días | 4 días | — | — |
| 11 | Cliente móvil Android | 7 días | 9 días | +2 | Refactor de ViewModels (bug de estado compartido entre pantallas) |
| 12 | Ajustes finales, documentación | 3 días | 4 días | +1 | Google Sign-In añadido fuera de planificación inicial |
| **Total** | | **36 días** | **44 días** | **+8 días** | |

Las desviaciones más significativas fueron la fase 5 (Keepa) y la fase 11 (Android). En ambos casos el problema fue técnico, no de estimación: la librería de Keepa tiene un modelo de callbacks propio que no es evidente hasta que se trabaja con ella, y el bug de estado compartido en Android no se detectó hasta tener varias pantallas integradas.

## 5.3 Presupuesto

El presupuesto se calcula sobre la duración real de 44 días laborables, asumiendo una jornada de 5 horas de trabajo efectivo por día (descontando interrupciones y revisiones). Se aplica una tarifa de desarrollador junior con experiencia en Java y Android: 18 €/hora.

**Desglose por fase:**

| Fase | Horas reales | Coste (18 €/h) |
|------|-------------|----------------|
| 1 — Configuración del entorno | 5 h | 90 € |
| 2 — Modelo de datos y entidades | 20 h | 360 € |
| 3 — Autenticación JWT | 10 h | 180 € |
| 4 — CRUD de productos e historial | 25 h | 450 € |
| 5 — Integración Keepa API | 25 h | 450 € |
| 6 — Motor de análisis y alertas | 15 h | 270 € |
| 7 — Scheduler Quartz | 15 h | 270 € |
| 8 — Panel de administración | 10 h | 180 € |
| 9 — Excepciones, validaciones, OpenAPI | 10 h | 180 € |
| 10 — Tests unitarios e integración | 20 h | 360 € |
| 11 — Cliente móvil Android | 45 h | 810 € |
| 12 — Ajustes y documentación | 20 h | 360 € |
| **Total desarrollo** | **220 h** | **3.960 €** |

**Costes de infraestructura y herramientas:**

| Concepto | Coste |
|----------|-------|
| IntelliJ IDEA Ultimate (licencia estudiante) | 0 € |
| Android Studio | 0 € |
| PostgreSQL + Docker (local) | 0 € |
| Keepa API (plan Developer — entorno de pruebas) | 0 € |
| VPS para despliegue (estimado anual, opcional) | ~60 € |
| **Total infraestructura** | **~60 €** |

**Coste total del proyecto: ~4.020 €**

Este presupuesto refleja el coste real de desarrollo individual. En un contexto de empresa con varios desarrolladores y entorno de producción gestionado, el coste de infraestructura aumentaría (servidor dedicado, dominio, certificado SSL), pero el coste de desarrollo por hora sería similar o inferior al tratarse de tecnologías estándar con amplia oferta de mercado.

## 5.4 Herramientas Utilizadas

| Herramienta | Uso |
|-------------|-----|
| IntelliJ IDEA | Desarrollo backend Java |
| Android Studio | Desarrollo cliente móvil |
| Git + GitHub | Control de versiones |
| Docker Desktop | Entorno local de PostgreSQL |
| Postman | Prueba manual de endpoints |
| Swagger UI | Documentación y prueba interactiva de la API |
| pgAdmin | Inspección de la base de datos |

---

# 4. Propuesta de Solución

## 4.1 Visión General de la Arquitectura

El sistema se compone de dos módulos independientes:

```
+------------------+         HTTPS / JSON          +------------------+
|  Cliente Móvil   |  <------------------------->  | API REST Backend |
|  Android         |      Authorization: Bearer     | Spring Boot      |
+------------------+                               +--------+---------+
                                                            |
                                                            | JDBC
                                                            |
                                                   +--------+---------+
                                                   |   PostgreSQL     |
                                                   +------------------+
                                                            |
                                             (externo)      |
                                                   +--------+---------+
                                                   |   Keepa API      |
                                                   | (Amazon prices)  |
                                                   +------------------+
```

El backend expone una API REST completamente stateless. Toda la lógica de negocio reside en el servidor. El cliente móvil es un consumidor de la API sin lógica de negocio propia más allá de la presentación.

## 4.2 Arquitectura del Backend

El backend sigue una **arquitectura en capas** estándar de Spring Boot [1]:

```
HTTP Request
     |
     v
+--------------------+
|   Security Filter  |  JWT validation, SecurityContextHolder
+--------------------+
     |
     v
+--------------------+
|    Controllers     |  Reciben request, delegan al servicio, devuelven ApiResponse
+--------------------+
     |
     v
+--------------------+
|     Services       |  Lógica de negocio, @Transactional
+--------------------+
     |
     v
+--------------------+
|   Repositories     |  Spring Data JPA, queries automáticas y JPQL
+--------------------+
     |
     v
+--------------------+
|    PostgreSQL      |  Persistencia relacional
+--------------------+
```

**Principios de diseño aplicados:**
- **Separación de responsabilidades:** cada capa tiene una única función.
- **Bajo acoplamiento:** las capas se comunican mediante interfaces y DTOs, no exponen entidades internas.
- **Inyección de dependencias por constructor** (Lombok `@RequiredArgsConstructor`): facilita el testing y garantiza la inmutabilidad de las dependencias.

**Diagrama de Clases (simplificado — clases principales):**

```
┌──────────────────────────────────────────────────────────────────────┐
│                         CAPA CONTROLLER                              │
│                                                                      │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────────┐  │
│  │  AuthController  │  │ ProductController │  │  AdminController  │  │
│  │─────────────────│  │──────────────────│  │───────────────────│  │
│  │ +register()     │  │ +create()        │  │ +listUsers()      │  │
│  │ +login()        │  │ +list()          │  │ +changeRole()     │  │
│  │ +googleAuth()   │  │ +update()        │  │ +getStats()       │  │
│  └────────┬────────┘  │ +delete()        │  └─────────┬─────────┘  │
│           │           │ +syncAmazon()    │             │            │
│           │           └────────┬─────────┘             │            │
└───────────┼────────────────────┼─────────────────────── ┼────────────┘
            │                    │                         │
┌───────────┼────────────────────┼─────────────────────────┼────────────┐
│           ▼           CAPA SERVICE         ▼             ▼            │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────────────┐│
│  │   AuthService   │  │  ProductService   │  │  PriceAnalysisService ││
│  │─────────────────│  │──────────────────│  │───────────────────────││
│  │ +register()     │  │ +createProduct() │  │ +analyzeProduct()     ││
│  │ +login()        │  │ +updateProduct() │  │ +generateRecommend.() ││
│  │ +validateGoogle │  │ +deleteProduct() │  │ +createAlert()        ││
│  │  Token()        │  │ +recordHistory() │  └───────────┬───────────┘│
│  └────────┬────────┘  └────────┬─────────┘              │            │
│           │                    │            ┌────────────┴──────────┐ │
│           │           ┌────────┴──────────┐ │     KeepaService      │ │
│           │           │  AlertRuleService  │ │───────────────────────│ │
│           │           │──────────────────│ │ +fetchPriceByAsin()   │ │
│           │           │ +getRules()       │ │ +getApiStatus()       │ │
│           │           │ +createRule()     │ │ -rateLimiter:Semaphore│ │
│           │           └───────────────────┘ └───────────────────────┘ │
└───────────┼────────────────────┼──────────────────────────────────────┘
            │                    │
┌───────────┼────────────────────┼──────────────────────────────────────┐
│           ▼          CAPA REPOSITORY       ▼                          │
│  ┌─────────────────┐  ┌──────────────────────────────────────────┐   │
│  │  UserRepository  │  │  ProductRepository  PriceHistoryRepo     │   │
│  │  (JpaRepository) │  │  CompetitorPriceRepo AlertRepository     │   │
│  └─────────────────┘  │  PriceRecommendationRepo AlertRuleRepo    │   │
│                        └──────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        CAPA SECURITY                             │
│  ┌────────────────────┐  ┌───────────────────────────────────┐  │
│  │     JwtService     │  │    JwtAuthenticationFilter        │  │
│  │────────────────────│  │───────────────────────────────────│  │
│  │ +generateToken()   │  │ +doFilterInternal()               │  │
│  │ +validateToken()   │  │  (intercepta cada HTTP request)   │  │
│  │ +extractClaims()   │  └───────────────────────────────────┘  │
│  └────────────────────┘                                          │
│  ┌────────────────────┐  ┌───────────────────────────────────┐  │
│  │  UserPrincipal     │  │   UserDetailsServiceImpl          │  │
│  │────────────────────│  │───────────────────────────────────│  │
│  │ +getCompanyId()    │  │ +loadUserByUsername()             │  │
│  │ +getRole()         │  └───────────────────────────────────┘  │
│  └────────────────────┘                                          │
└─────────────────────────────────────────────────────────────────┘
```

## 4.3 Modelo de Datos

El esquema relacional se compone de 9 tablas gestionadas mediante migraciones Flyway (V1–V8). La entidad raíz es `companies`, no `users`: todos los datos de negocio (productos, alertas, reglas, claves API) están asociados a una empresa, no a un usuario individual. Esto permite que varios empleados de la misma empresa compartan catálogo sin duplicar datos.

```
companies
  id, name, company_code (único, 8 chars), business_type, tax_id,
  plan (FREE/PREMIUM), shared_stock_enabled, active,
  created_at, updated_at

users
  id, username, email, password, auth_provider (LOCAL|GOOGLE),
  company_id (FK companies), active,
  role (ADMIN|COMPANY_ADMIN|EMPLOYEE),
  created_at, updated_at

products
  id, name, description, sku, ean, asin,
  current_price, cost_price, min_margin,
  category, brand, image_url, active, monitoring_enabled,
  stock_quantity,
  company_id (FK companies), created_by (FK users, nullable),
  created_at, updated_at

price_history
  id, product_id (FK products, CASCADE DELETE),
  price, previous_price, change_type, change_reason, recorded_at

competitors
  id, name, code, base_url, logo_url, source_type,
  source_config, active, last_scraped_at, created_at, updated_at

competitor_prices
  id, product_id (FK products, CASCADE DELETE),
  competitor_id (FK competitors, CASCADE DELETE),
  price, original_price, currency, available, free_shipping,
  shipping_cost, product_url, competitor_product_title,
  scraped_at, source

alerts
  id, user_id (FK users, nullable desde V7), product_id (FK products),
  alert_type, title, message, previous_price, new_price,
  change_percent, is_read, severity, created_at, read_at

alert_rules
  id, company_id (FK companies, CASCADE DELETE),
  product_id (FK products, nullable — null = regla global de empresa),
  alert_type, name, threshold, target_price, enabled,
  created_at, updated_at

price_recommendations
  id, product_id (FK products, CASCADE DELETE),
  recommendation_type, current_price, competitor_price,
  suggested_price, price_difference_percent,
  potential_saving_or_profit, reason,
  status (PENDING|APPLIED|DISMISSED), priority (LOW|MEDIUM|HIGH|URGENT),
  created_at, applied_at, dismissed_at

company_api_keys
  id, company_id (FK companies, CASCADE DELETE),
  provider (KEEPA|…), encrypted_key (AES-256), enabled,
  created_at, updated_at
  UNIQUE (company_id, provider)
```

**Diagrama Entidad-Relación (E/R):**

```
┌─────────────────────────┐
│        companies         │
│─────────────────────────│
│ PK id                   │
│    name                 │
│    company_code (único) │
│    plan                 │
│    active               │
└────────────┬────────────┘
             │ 1
             │
      ┌──────┴──────┐
      │             │
      │ N           │ N
      ▼             ▼
┌──────────────┐   ┌──────────────────────────┐
│    users     │   │         products          │
│──────────────│   │──────────────────────────│
│ PK id        │   │ PK id                    │
│    username  │   │    name, sku, asin       │
│    email     │   │    current_price         │
│    password  │   │    cost_price            │
│    role      │   │    monitoring_enabled    │
│    auth_     │   │    active                │
│    provider  │   │ FK company_id → companies│
│ FK company_id│   │ FK created_by → users    │
└──────┬───────┘   └──────────────────────────┘
       │                        │ 1
       │                        │
       │         ┌──────────────┼──────────────┐
       │         │ N            │ N             │ N
       │         ▼             ▼              ▼
       │  ┌─────────────┐ ┌──────────────┐ ┌──────────────────────┐
       │  │price_history│ │competitor_   │ │ price_               │
       │  │─────────────│ │prices        │ │ recommendations      │
       │  │ PK id       │ │──────────────│ │──────────────────────│
       │  │    price    │ │ PK id        │ │ PK id                │
       │  │    prev_    │ │    price     │ │    recommendation_   │
       │  │    price    │ │    available │ │    type              │
       │  │    change_  │ │    scraped_at│ │    suggested_price   │
       │  │    type     │ │ FK product_id│ │    priority          │
       │  │ FK product_id│ │ FK competitor│ │    status            │
       │  └─────────────┘ └──────────────┘ │ FK product_id        │
       │                        │           └──────────────────────┘
       │                        │ N
       │                        ▼
       │               ┌─────────────────┐
       │               │   competitors   │
       │               │─────────────────│
       │               │ PK id           │
       │               │    name, code   │
       │               │    source_type  │
       │               │    active       │
       │               └─────────────────┘
       │
       │ (vía company_id)
┌──────┴──────────────────┐
│       alert_rules        │         ┌───────────────────┐
│─────────────────────────│         │      alerts        │
│ PK id                   │         │───────────────────│
│    alert_type           │         │ PK id             │
│    threshold            │         │    alert_type     │
│    target_price         │         │    severity       │
│    enabled              │         │    is_read        │
│ FK company_id → companies│         │ FK product_id     │
│ FK product_id (nullable) │         │ FK user_id (null) │
└─────────────────────────┘         └───────────────────┘

┌─────────────────────────────┐
│      company_api_keys        │
│─────────────────────────────│
│ PK id                       │
│    provider (KEEPA)         │
│    encrypted_key (AES-256)  │
│    enabled                  │
│ FK company_id → companies   │
│ UNIQUE (company_id, provider)│
└─────────────────────────────┘
```

**Relaciones principales:**
- Una `Company` tiene muchos `Users` (1:N) y muchos `Products` (1:N).
- Un `Product` pertenece a una `Company`, fue creado por un `User`.
- Un `Product` tiene muchos `PriceHistory` (1:N, cascade DELETE).
- Un `Product` tiene muchos `CompetitorPrice` (1:N).
- Un `Product` tiene muchas `PriceRecommendations` (1:N).
- Una `Company` tiene muchas `AlertRule` (1:N); una regla puede ser global (product_id=null) o específica de un producto.
- Las `Alert` se generan automáticamente por `PriceAnalysisService` y referencian opcionalmente a un usuario.

**Índices de base de datos definidos:**

| Tabla | Columna(s) indexadas | Justificación |
|-------|----------------------|---------------|
| users | company_id | Todas las queries de usuario filtran por empresa |
| products | company_id | Todas las queries de producto filtran por empresa |
| products | sku | Búsquedas por código interno |
| products | category | Filtros de búsqueda |
| products | (sku, company_id) WHERE active=true | **Índice parcial único**: unicidad de SKU por empresa solo entre productos activos. Permite reutilizar el SKU de un producto borrado lógicamente sin violar la restricción. |
| price_history | product_id | Historial por producto |
| price_history | recorded_at | Consultas por rango de fecha |
| competitor_prices | product_id | Precio más reciente por producto |
| competitor_prices | competitor_id | Consultas por competidor |
| competitor_prices | scraped_at | Ordenación temporal |
| alerts | user_id | Alertas del usuario |
| alerts | is_read | Filtro de no leídas |
| alerts | alert_type | Filtro por tipo |
| alert_rules | company_id | Reglas de la empresa |
| alert_rules | alert_type | Consultas por tipo |
| price_recommendations | product_id | Recomendaciones por producto |
| price_recommendations | status | Filtro de pendientes |
| company_api_keys | company_id | Claves por empresa |

## 4.4 Diseño de la API REST

La API sigue los principios REST: recursos identificados por URI, operaciones mediante verbos HTTP, respuestas en JSON, stateless.

**Formato de respuesta estándar:**

Éxito:
```json
{
  "success": true,
  "message": "Producto creado correctamente",
  "data": { ... },
  "timestamp": "2026-02-10T10:30:00"
}
```

Error:
```json
{
  "success": false,
  "message": "No existe producto con id: 99",
  "timestamp": "2026-02-10T10:30:00"
}
```

Paginación:
```json
{
  "success": true,
  "data": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 87,
  "totalPages": 5,
  "isFirst": true,
  "isLast": false
}
```

**Endpoints implementados:**

| Método | Ruta | Descripción | Acceso |
|--------|------|-------------|--------|
| POST | /api/auth/register | Registro de usuario | Público |
| POST | /api/auth/login | Login, devuelve JWT | Público |
| GET | /api/auth/profile | Perfil del usuario autenticado | USER |
| POST | /api/products | Crear producto | USER |
| GET | /api/products | Listar productos (paginado) | USER |
| GET | /api/products/{id} | Detalle de producto | USER |
| PUT | /api/products/{id} | Actualizar producto | USER |
| DELETE | /api/products/{id} | Borrado lógico | USER |
| GET | /api/products/search | Buscar con filtros | USER |
| GET | /api/products/categories | Categorías del catálogo | USER |
| GET | /api/products/brands | Marcas del catálogo | USER |
| GET | /api/products/count | Total de productos | USER |
| GET | /api/competitors/status | Estado de Keepa API | Público |
| GET | /api/competitors/amazon/price/{asin} | Consultar precio por ASIN | USER |
| POST | /api/competitors/amazon/sync/{productId} | Sincronizar producto con Amazon | USER |
| GET | /api/admin/users | Listar todos los usuarios | ADMIN |
| GET | /api/admin/stats | Estadísticas del sistema | ADMIN |
| GET | /api/admin/dashboard | Métricas del dashboard | ADMIN |
| PUT | /api/admin/users/{id} | Editar usuario | ADMIN |
| PUT | /api/admin/users/{id}/role | Cambiar rol | ADMIN |
| PUT | /api/admin/users/{id}/status | Activar/desactivar | ADMIN |
| DELETE | /api/admin/users/{id} | Eliminar usuario | ADMIN |
| GET | /api/health | Health check del servidor | Público |

## 4.5 Diseño de Seguridad

La seguridad del sistema se basa en JWT [4]. El flujo es el que se ve en el diagrama: el cliente manda credenciales, el servidor las verifica con BCrypt, genera un token HS256 válido 24 horas con el `companyId` y el rol del usuario, y lo devuelve. En peticiones posteriores el cliente lo manda en el header `Authorization: Bearer`, el servidor valida la firma y ejecuta el endpoint si todo está bien.

```
Cliente                                    Servidor
  |                                            |
  | POST /api/auth/login                       |
  | { email, password }                        |
  |  ----------------------------------------> |
  |                                            | 1. Verificar credenciales (BCrypt)
  |                                            | 2. Generar token JWT (HS256, 24h)
  |                                            | 3. Incluir: userId, username, role
  | <----------------------------------------  |
  | { token: "eyJ...", expiresIn: 86400 }      |
  |                                            |
  | GET /api/products                          |
  | Authorization: Bearer eyJ...              |
  |  ----------------------------------------> |
  |                                            | 4. Validar firma HMAC-SHA256
  |                                            | 5. Verificar expiración
  |                                            | 6. Cargar UserPrincipal en contexto
  |                                            | 7. Ejecutar endpoint
  | <----------------------------------------  |
  | { success: true, data: [...] }             |
```

Internamente la seguridad está repartida en cuatro clases. `JwtService` genera y valida tokens; al arrancar comprueba que `JWT_SECRET` tiene al menos 32 caracteres y no es el valor por defecto, porque en producción un secreto débil invalida toda la seguridad. `JwtAuthenticationFilter` intercepta cada petición antes de que llegue al controlador. `UserDetailsServiceImpl` hace una consulta a BD para verificar que el usuario sigue activo (un token válido de un usuario desactivado no debe funcionar). `SecurityConfig` es donde está la lista blanca de rutas públicas y la configuración stateless.

## 4.6 Justificaciones de Decisiones Técnicas

A lo largo del desarrollo tomé varias decisiones de diseño que no eran obvias a priori. Aquí recojo las más relevantes y el razonamiento detrás de cada una.

### Por qué JWT y no sesiones

El cliente es una app Android, y gestionar cookies de sesión en Android es incómodo: hay que configurar un `CookieJar` en OkHttp, preocuparse de la persistencia entre reinicios, etc. JWT encaja mucho mejor: el token se guarda en DataStore, se añade a cada petición con un interceptor, y el servidor no necesita almacenar nada. Como ventaja adicional, el propio token incluye el `companyId` y el rol del usuario, así que el servidor no hace una query extra a BD en cada request para saber quién es.

El punto débil de JWT es que no se puede revocar un token antes de que expire. Lo mitigué poniendo una validez de 24 horas y verificando en cada petición que el usuario sigue activo en base de datos.

### BCrypt para contraseñas

Al principio planteé usar SHA-256 simplemente porque lo había usado antes. Investigando un poco, descubrí que SHA-256 no es adecuado para contraseñas: está diseñado para ser rápido, lo que facilita los ataques de fuerza bruta. BCrypt está diseñado específicamente para ser lento y tiene un factor de coste ajustable [2]. Además genera automáticamente una sal aleatoria, así que aunque dos usuarios tengan la misma contraseña sus hashes son distintos. Spring Security lo integra de forma nativa, así que el cambio no supuso ningún coste adicional.

### Por qué Keepa y no scraping de Amazon

Empecé mirando si podía hacer scraping directamente. El problema es que Amazon tiene un sistema anti-bot bastante efectivo y cambia la estructura de sus páginas con frecuencia, lo que hace que cualquier scraper sea frágil. Además, hacerlo viola los términos de servicio de Amazon, lo que descarta la opción para un proyecto real.

Keepa es un servicio especializado que lleva años recopilando datos de precios de Amazon. Tiene una API oficial, documentada y estable, con historial de 90 días por producto [7]. La integración es más compleja que un scraping simple, pero mucho más fiable.

### Flyway para las migraciones

Al principio usé `ddl-auto=update` de Hibernate porque es cómodo en desarrollo. El problema es que en producción Hibernate puede añadir columnas o índices pero nunca los elimina, y nunca sabes exactamente en qué estado está el esquema. Cuando empecé a tener varios entornos (local, servidor de pruebas) y necesitaba añadir columnas sin perder datos, Flyway fue la solución obvia: cada cambio es un script SQL versionado que se aplica una sola vez y queda registrado. En producción uso `ddl-auto=validate`, que hace que la aplicación no arranque si el esquema no coincide con las entidades, lo cual prefiero a que modifique cosas silenciosamente.

### PostgreSQL en lugar de MySQL

Usé PostgreSQL principalmente por los **índices parciales**. Necesitaba que el SKU fuera único por empresa, pero solo entre productos activos (los borrados lógicamente pueden reutilizar el SKU). MySQL no tiene índices parciales de forma nativa; PostgreSQL sí, y la sintaxis es directa:

```sql
CREATE UNIQUE INDEX uix_product_sku_company_active
    ON products(sku, company_id) WHERE active = true;
```

Hibernate no puede generar esto con `@UniqueConstraint`, así que lo puse directamente en la migración Flyway.

### Multi-tenancy: BD compartida con `company_id`

Consideré tener una base de datos separada por empresa, pero descartarlo fue fácil: gestionar decenas de esquemas distintos, migraciones independientes y conexiones separadas es inviable para un MVP. La alternativa es una sola BD donde cada tabla tiene `company_id`, y todas las queries lo incluyen como filtro obligatorio. El `companyId` viene en el token JWT, así que no hace falta una consulta extra para obtenerlo. El riesgo es que un bug en la capa de servicio pueda cruzar datos entre empresas, pero con las validaciones en cada método de servicio es un riesgo controlado.

### ViewModels separados en Android

Inicialmente tenía un solo `ProductViewModel` para todas las pantallas de productos. El bug que me llevó a separarlo fue este: al buscar un producto por ASIN, el resultado temporal aparecía en el listado de tracking porque ambas pantallas compartían el mismo estado. La solución fue crear un ViewModel por responsabilidad: `SearchViewModel` para búsqueda, `ProductDetailViewModel` para el detalle y sincronización con Keepa, y `ProductFormViewModel` para crear y editar. Desde entonces cada pantalla tiene su propio estado aislado y no hay interferencias.

### Soft delete en lugar de borrado físico

Decidí no borrar productos de la base de datos porque quería conservar el historial de precios. Si un usuario elimina un producto y luego quiere ver cómo evolucionó su precio antes de borrarlo, con borrado físico eso es imposible. Con `active = false`, el producto desaparece de todos los listados pero sus datos y su historial siguen ahí. El coste es mínimo: añadir `WHERE active = true` en los repositorios, lo que hago siempre a través de métodos con nombre explícito (`findByCompanyIdAndActiveTrue`) para que no sea fácil olvidarlo.

---

# 6. Desarrollo de la Solución

## 6.1 Tecnologías y Versiones

| Tecnología | Versión | Rol |
|-----------|---------|-----|
| Java | 17 (LTS) | Lenguaje de programación |
| Spring Boot | 3.2.1 | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.x | Acceso a base de datos |
| Hibernate | 6.x | Implementación JPA/ORM |
| PostgreSQL | 14+ | Base de datos relacional |
| jjwt | 0.12.3 | Generación y validación de JWT |
| Quartz Scheduler | 2.3.x | Tareas programadas |
| Keepa API | latest | Precios de Amazon |
| HikariCP | incluido en Boot | Pool de conexiones |
| Lombok | 1.18.40 | Reducción de boilerplate |
| SpringDoc OpenAPI | 2.3.0 | Documentación automática de la API |
| Maven | 3.8+ | Gestión de dependencias |
| Docker + Compose | - | Entorno de base de datos |

## 6.2 Migraciones de Base de Datos con Flyway

El esquema de base de datos se gestiona con **Flyway**, una herramienta de control de versiones para bases de datos relacionales [1]. En lugar de que Hibernate genere o altere el esquema automáticamente (lo que sería impredecible en producción), cada cambio estructural se describe en un script SQL versionado que Flyway aplica exactamente una vez, en orden.

**Migraciones implementadas:**

| Versión | Descripción |
|---------|-------------|
| V1 | Esquema completo inicial: companies, users, products, price_history, competitors, competitor_prices, alerts, price_recommendations |
| V2 | Índices compuestos adicionales para optimización de consultas frecuentes |
| V3 | Tabla `audit_logs` para trazabilidad de operaciones sensibles |
| V4 | Columna `auth_provider` (LOCAL/GOOGLE) en users para soporte de Google Sign-In |
| V5 | Tabla `alert_rules`: reglas de alerta configurables por empresa |
| V6 | Columna `target_price` en `alert_rules` para alertas de precio objetivo |
| V7 | `user_id` nullable en `alerts` (las alertas pueden generarse sin usuario asociado) |
| V8 | Tabla `company_api_keys`: almacenamiento cifrado de claves API por empresa |

**Perfil de producción:** `spring.jpa.hibernate.ddl-auto=validate` — Hibernate comprueba que el esquema en BD coincide con las entidades pero no lo modifica. Si hay discrepancia, la aplicación falla al arrancar con un error claro en lugar de modificar datos en producción silenciosamente.

## 6.3 Estructura del Proyecto

```
src/main/java/com/alvaro/pricewise/
├── PriceWiseApplication.java
├── config/
│   ├── AsyncConfig.java          Pool de hilos para Keepa
│   ├── KeepaConfig.java          Propiedades de configuración
│   ├── OpenApiConfig.java        Swagger/OpenAPI
│   ├── SchedulerConfig.java      Quartz job y trigger
│   └── SecurityConfig.java       JWT, CORS, BCrypt
├── controller/
│   ├── AdminController.java
│   ├── AuthController.java
│   ├── CompetitorController.java
│   ├── HealthController.java
│   └── ProductController.java
├── dto/
│   ├── auth/                     LoginRequest, RegisterRequest, AuthResponse
│   ├── product/                  CreateProductRequest, UpdateProductRequest,
│   │                             ProductResponse, ProductListResponse
│   ├── admin/                    AdminStatsResponse, UserDetailResponse
│   └── common/                   ApiResponse<T>, PageResponse<T>
├── entity/
│   ├── Alert.java
│   ├── Competitor.java
│   ├── CompetitorPrice.java
│   ├── PriceHistory.java
│   ├── PriceRecommendation.java
│   ├── Product.java
│   └── User.java
├── exception/
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── AlertRepository.java
│   ├── CompetitorPriceRepository.java
│   ├── CompetitorRepository.java
│   ├── PriceHistoryRepository.java
│   ├── PriceRecommendationRepository.java
│   ├── ProductRepository.java
│   └── UserRepository.java
├── scheduler/
│   └── PriceMonitorJob.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── UserDetailsServiceImpl.java
│   └── UserPrincipal.java
└── service/
    ├── AuthService.java
    ├── KeepaService.java
    ├── PriceAnalysisService.java
    └── ProductService.java
```

## 6.4 Módulo de Autenticación

El sistema soporta dos flujos de autenticación diferenciados, identificados por el campo `auth_provider` del usuario:

**Flujo local (email + contraseña):**
El registro valida unicidad de email y nombre de usuario, codifica la contraseña con BCrypt y genera un token JWT en la misma respuesta, evitando un segundo roundtrip de login al cliente.

```java
// AuthService.java - fragmento de registro
@Transactional
public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail()))
        throw new BadRequestException("El email ya está registrado");
    if (userRepository.existsByUsername(request.getUsername()))
        throw new BadRequestException("El nombre de usuario ya está en uso");

    User user = User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(User.Role.COMPANY_ADMIN)
        .authProvider(AuthProvider.LOCAL)
        .active(true)
        .build();

    userRepository.save(user);
    String token = jwtService.generateToken(new UserPrincipal(user));
    return buildAuthResponse(user, token);
}
```

**Flujo OAuth2 con Google Sign-In:**
El cliente Android inicia el flujo con la librería oficial de Google Identity (`GoogleSignIn`), obtiene un `idToken` y lo envía al backend en `POST /api/auth/google`. El backend valida el token con los servidores de Google (`GoogleIdTokenVerifier`), extrae el email, y determina si el usuario ya existe (login) o debe completar el registro (crear empresa o unirse a una existente). Si el usuario es nuevo, se le redirige a un segundo paso (`POST /api/auth/google/complete-new-company` o `/complete-join`) donde elige nombre de empresa o código de empresa. El campo `auth_provider=GOOGLE` distingue estos usuarios: no tienen contraseña en el sistema y no pueden usar el flujo de login local.

## 6.5 Módulo de Productos

El servicio de productos implementa validación de pertenencia a empresa en cada operación de lectura y escritura, garantizando el aislamiento de datos entre empresas (multi-tenancy). El `companyId` se extrae del token JWT en cada request y se pasa a todas las queries como filtro obligatorio.

El registro de historial de precios es automático y transparente: al crear un producto se registra la entrada `INITIAL`; al actualizar el precio, si ha cambiado, se registra `INCREASE` o `DECREASE`. El servicio determina el tipo de cambio comparando el precio anterior con el nuevo.

La caché de categorías y marcas evita consultas `SELECT DISTINCT` repetidas en el listado de filtros de búsqueda. Se invalida automáticamente al crear, actualizar o borrar un producto.

**Fragmento de actualización con historial:**

```java
@Transactional
@CacheEvict(value = {"categories", "brands"}, key = "#companyId")
public ProductResponse updateProduct(Long userId, Long productId,
                                     UpdateProductRequest request) {
    Product product = findProductByUserAndId(userId, productId);
    BigDecimal oldPrice = product.getCurrentPrice();

    // Actualizar campos
    if (request.getName() != null) product.setName(request.getName());
    if (request.getCurrentPrice() != null) {
        product.setCurrentPrice(request.getCurrentPrice());
    }

    // Registrar historial solo si el precio cambió
    if (request.getCurrentPrice() != null
            && request.getCurrentPrice().compareTo(oldPrice) != 0) {
        PriceHistory.ChangeType type =
            request.getCurrentPrice().compareTo(oldPrice) > 0
                ? PriceHistory.ChangeType.INCREASE
                : PriceHistory.ChangeType.DECREASE;
        createPriceHistoryEntry(product, oldPrice, type, "Actualización manual");
    }

    return mapToResponse(productRepository.save(product));
}
```

## 6.6 Integración con Keepa API

La integración con Keepa fue la parte técnicamente más compleja del proyecto. La librería tiene su propio modelo de callbacks y las llamadas pueden tardar varios segundos, así que no podía simplemente hacer un `RestTemplate.getForObject()` y esperar.

Lo primero que resolví fue el bloqueo: si proceso la llamada a Keepa en el hilo HTTP de Tomcat, ese hilo queda bloqueado mientras espera la respuesta. Con 10 usuarios pidiendo precios a la vez, el servidor se quedaría sin hilos. La solución fue mover las llamadas a un executor dedicado (`keepaExecutor`) y devolver un `CompletableFuture` al controlador [14], que puede hacer otras cosas mientras tanto.

El segundo problema fue la concurrencia con el scheduler. Cuando el job de monitorización lanza 50 productos en paralelo, sin control podría lanzar 50 llamadas simultáneas a Keepa, lo que violaría los límites del plan. Añadí un `Semaphore(3)` que garantiza máximo 3 llamadas en vuelo a la vez, independientemente de cuántos hilos estén intentando llamar.

El tercero fue más sutil: las primeras versiones fallaban intermitentemente con errores de clave duplicada al arrancar. El problema era que `@PostConstruct` crea el registro de "Amazon ES" en BD si no existe, pero si dos peticiones llegaban casi a la vez antes de que terminase, ambas intentaban crearlo. Lo resolví con double-checked locking: primero compruebo sin lock, y solo entro en la sección sincronizada si el registro no existe.

Por último, las llamadas a Keepa fallan a veces por timeouts o errores transitorios. En lugar de propagarlo como error, el servicio reintenta hasta 3 veces con esperas crecientes (1s, 2s, 4s). Si agota los reintentos, devuelve `Optional.empty()` y el controlador gestiona el caso.

```java
// KeepaService.java - constantes y método de consulta (simplificado)
private static final int MAX_CONCURRENT_REQUESTS = 3;
private static final int MAX_RETRIES = 3;
private static final long INITIAL_BACKOFF_MS = 1000;

private final Semaphore rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);

@Async("keepaExecutor")
public CompletableFuture<Optional<CompetitorPrice>> fetchPriceByAsin(
        String asin, Product product) {
    return fetchPriceWithRetry(asin, product, 0);
}

private CompletableFuture<Optional<CompetitorPrice>> fetchPriceWithRetry(
        String asin, Product product, int attempt) {
    try {
        rateLimiter.acquire();
        // ... llamada a Keepa API y mapeo de respuesta
    } catch (Exception e) {
        if (attempt < MAX_RETRIES) {
            long backoff = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt);
            Thread.sleep(backoff);
            return fetchPriceWithRetry(asin, product, attempt + 1);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    } finally {
        rateLimiter.release();
    }
}
```

**Datos que Keepa proporciona por producto:**

- Precio actual en Amazon España (precio AMAZON y precio NEW).
- Disponibilidad (si hay stock).
- URL del producto en Amazon.
- Título del producto en Amazon.
- Historial de precios de los últimos 90 días.

## 6.7 Motor de Análisis de Precios

El motor de análisis es donde está la lógica de negocio real del proyecto. La idea es simple: para cada producto que tenga precio de Amazon registrado, comparar ese precio con el precio propio y decidir si hay algo que recomendar.

La parte que más tuve que pensar fue qué hacer con la comparativa. Al principio generaba una recomendación para cualquier diferencia, pero eso era demasiado ruidoso: si Amazon está 2€ más caro que yo en un producto de 200€, no es relevante. Definí un umbral del 10% como mínimo para generar recomendación, y a partir de ahí la prioridad sube según la diferencia.

**Lógica de análisis por producto:**

```
Para cada producto con precios de competidor registrados:

  1. Obtener el precio más reciente del competidor
  2. Calcular diferencia porcentual: (precioCompetidor - precioPropio) / precioPropio

  3. Si diferencia > +10% (nuestro precio es más caro):
       → Crear recomendación PRICE_TOO_HIGH
       → Precio sugerido: precioCompetidor - 2% (por debajo del competidor)
       → Severidad: WARNING o CRITICAL según porcentaje

  4. Si diferencia < -10% (nuestro precio es más barato):
       → Crear recomendación PRICE_TOO_LOW (oportunidad de subir margen)
       → Precio sugerido: precioCompetidor - 5% (competitivo con más margen)
       → Severidad: INFO

  5. Si el precio del competidor cambió más de un 15% respecto al anterior:
       → Crear alerta de cambio brusco (COMPETITOR_PRICE_DROP o RISE)
       → Severidad según magnitud del cambio

  6. No crear recomendación duplicada si ya existe una PENDING del mismo tipo
```

**Priorización de recomendaciones:**

| Diferencia de precio | Prioridad |
|----------------------|-----------|
| < 10% | LOW |
| 10% - 20% | MEDIUM |
| 20% - 35% | HIGH |
| > 35% | URGENT |

**Tipos de alerta generados:**

| AlertType | Descripción |
|-----------|-------------|
| COMPETITOR_PRICE_DROP | Bajada brusca del precio de Amazon |
| COMPETITOR_PRICE_RISE | Subida brusca del precio de Amazon |
| HIGH_MARGIN_OPPORTUNITY | Precio propio muy por debajo, oportunidad de subir |
| PRICE_MATCH_NEEDED | Precio propio muy por encima, riesgo de perder ventas |

## 6.8 Monitorización Periódica (Mejora Implementada)

Esta funcionalidad no estaba en el MVP original, pero una vez que tenía Keepa integrado tenía sentido añadirla: un job Quartz que cada 6 horas actualiza automáticamente los precios de Amazon para los productos con monitorización activa.

Solo entran en el ciclo los productos con `monitoringEnabled = true`, `active = true` y ASIN configurado. El procesamiento es por lotes de 50 para no lanzar cientos de llamadas a Keepa de golpe. Entre lote y lote espera un segundo, y dentro de cada lote las peticiones van en paralelo respetando el semáforo de 3 concurrentes. Si se acumulan 5 errores seguidos, el job para y lo registra en logs para revisión manual.
- Si se producen 5 errores consecutivos, el job aborta y registra en logs para revisión.

## 6.9 Gestión de Excepciones

Un `@RestControllerAdvice` centraliza el manejo de errores y garantiza que todas las respuestas de error siguen el mismo formato JSON [11].

| Excepción | Código HTTP | Uso |
|-----------|-------------|-----|
| `BadRequestException` | 400 | Validación de negocio (SKU duplicado, precio inválido) |
| `ResourceNotFoundException` | 404 | Entidad no encontrada |
| `MethodArgumentNotValidException` | 400 | Validación de campos del DTO |
| `AccessDeniedException` | 403 | Acceso a recurso sin permiso |
| `ExpiredJwtException` | 401 | Token caducado |
| `JwtException` | 401 | Token malformado o inválido |
| `Exception` (genérica) | 500 | Error no controlado |

## 6.10 Configuración y Entornos

La aplicación diferencia dos perfiles de Spring:

**Perfil `dev` (desarrollo local):**
- `show-sql: true` para ver las queries generadas por Hibernate.
- `ddl-auto: update` para que Hibernate actualice el esquema automáticamente.
- CORS permisivo (`allow-all: true`) para facilitar el trabajo con el cliente local.
- Nivel de log DEBUG para el paquete `com.alvaro.pricewise`.

**Perfil `prod` (producción):**
- `show-sql: false`.
- `ddl-auto: validate` para que la aplicación falle al arrancar si el esquema no coincide con las entidades.
- CORS restrictivo con lista blanca de dominios.
- Nivel de log INFO.

**Variables de entorno requeridas:**

| Variable | Descripción |
|----------|-------------|
| `JWT_SECRET` | Clave HMAC-SHA256 para firmar tokens (mínimo 32 caracteres) |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `SPRING_PROFILES_ACTIVE` | `dev` o `prod` |

> **Nota:** Las API keys de Keepa ya no se configuran via variable de entorno. Se gestionan por empresa desde Ajustes > Integracion Keepa (cifradas AES-256 en tabla `company_api_keys`).

---

## 6.11 Cliente Móvil Android

### 6.11.1 Plataforma y Tecnología

El cliente móvil se desarrolla como **aplicación Android nativa** utilizando:

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose [13]
- **Arquitectura:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit 2 + OkHttp
- **Autenticación:** Interceptor HTTP que añade el token JWT a cada petición
- **Almacenamiento local:** DataStore para persistir el token entre sesiones

Elegí Android nativo en lugar de Flutter o Ionic principalmente porque es lo que he trabajado durante el ciclo y quería aprovechar ese conocimiento. Además, necesitaba acceder a APIs concretas del sistema (ConnectivityManager para detectar si hay red, DataStore para persistir el token, Google Identity para el login con Google) que desde un framework multiplataforma habrían requerido plugins adicionales con menor garantía de mantenimiento.

### 6.11.2 Pantallas del MVP

El cliente cubre las funcionalidades del MVP definido:

**Autenticación:**
- `LoginScreen`: formulario de email y contraseña con validación local.
- `RegisterScreen`: formulario de registro con validación de campos.

**Gestión de productos:**
- `TrackingScreen`: listado paginado del catálogo propio con búsqueda por nombre. Cada item muestra nombre, precio, categoría y un indicador visual si tiene monitoreo activo.
- `SearchScreen`: búsqueda de productos por ASIN en Amazon (via Keepa). Permite crear un producto directamente desde los resultados de búsqueda.
- `ProductDetailScreen`: detalle completo del producto con sección de comparación de precio Amazon, botón "Buscar precio ahora en Amazon" que lanza la sincronización con Keepa y muestra el resultado.
- `CreateProductScreen`: formulario de alta de producto con campos requeridos (nombre, precio de venta) y opcionales (ASIN, precio de coste, categoría, marca).
- `EditProductScreen`: edición de los campos del producto.

**Alertas y reglas:**
- `AlertsScreen`: dos pestañas — "Mis alertas" (reglas configurables con CRUD inline) y "Historial" (alertas generadas automáticamente por el motor de análisis). La gestión de recomendaciones de precio (aplicar/descartar) queda pendiente para una versión futura (ver sección 9.3).

**Diagrama de Flujo de Navegación Android:**

```
                    ┌──────────────┐
                    │ SplashScreen │  (400ms — verifica token)
                    └──────┬───────┘
              token válido │         │ sin token
                           ▼         ▼
                    ┌──────────┐  ┌──────────────┐
                    │          │  │  LoginScreen  │
                    │          │  │──────────────│
                    │          │  │ email/pass   │
                    │          │  │ Google OAuth │
                    │          │  └──────┬───────┘
                    │          │         │ login ok
                    │          │◄────────┘
                    │  MainScreen (Bottom Navigation)                    │
                    │  ┌──────────┬────────────┬──────────┬──────────┐ │
                    │  │ Tracking │  Search    │  Alerts  │ Settings │ │
                    └──┴────┬─────┴──────┬─────┴────┬─────┴──────────┘
                            │            │           │
              ┌─────────────▼──┐   ┌─────▼──────┐   ┌────▼────────────┐
              │ TrackingScreen │   │SearchScreen│   │  AlertsScreen   │
              │ (productos con │   │────────────│   │─────────────────│
              │ monitoring=true│   │ búsqueda   │   │ Tab: Mis alertas│
              └───────┬────────┘   │ por ASIN   │   │ Tab: Historial  │
                      │            └─────┬──────┘   └─────────────────┘
           ┌──────────┤                  │ resultado encontrado
           │          │                  ▼
     tap producto     │         ┌──────────────────┐
           │          │         │ CreateProductScr │
           ▼          │         │ (desde búsqueda) │
  ┌─────────────────┐ │         └────────┬─────────┘
  │ProductDetailScr │ │                  │ guardado
  │─────────────────│ │                  ▼
  │ precio propio   │ │         ┌──────────────────┐
  │ precio amazon   │ │         │  TrackingScreen  │
  │ diferencia      │ │         │  (tab activo)    │
  │ [Buscar Amazon] │ │         └──────────────────┘
  │ [Editar]        │ │
  └────────┬────────┘ │
           │ [Editar] │ [Crear nuevo]
           ▼          ▼
  ┌──────────────────────────────┐
  │      EditProductScreen       │
  │  (ProductDetailViewModel +  │
  │   ProductFormViewModel)      │
  └──────────────────────────────┘

  Settings → SettingsScreen → [Cerrar sesión / Tema / API Keys]
  Settings → UsersScreen (solo COMPANY_ADMIN/ADMIN)
  Settings → AdminDashboardScreen (solo ADMIN)
```

### 6.11.3 Arquitectura del Cliente

```
View (Composables)
        |
        | observa StateFlow
        v
ViewModel
        |
        | llama
        v
Repository (local)
        |
        | delega
        v
ApiService (Retrofit)
        |
        | HTTPS + JWT
        v
PriceWise Backend API
```

El `AuthInterceptor` de OkHttp añade automáticamente el header `Authorization: Bearer <token>` a todas las peticiones una vez que el usuario está autenticado, sin necesidad de gestionarlo manualmente en cada llamada.

---

## 6.12 Pruebas

### 6.12.1 Estrategia de Testing

Se aplica una estrategia de pruebas en tres niveles:

| Nivel | Herramienta | Cobertura objetivo |
|-------|-------------|-------------------|
| Tests unitarios de servicio | JUnit 5 + Mockito | Lógica de negocio crítica |
| Tests de integración de controlador | MockMvc + H2 | Flujos HTTP completos |
| Tests de repositorio | @DataJpaTest + H2 | Queries personalizadas |

### 6.12.2 Tests Unitarios - PriceAnalysisService

Los tests del motor de análisis verifican que los umbrales de precio se aplican correctamente y que las recomendaciones se generan con el tipo y prioridad adecuados.

**Casos de prueba:**

| Test | Descripción | Resultado esperado |
|------|-------------|-------------------|
| `precioPropioCaro_generaRecomendacionTooHigh` | Precio propio 15% mayor que Amazon | Recomendación PRICE_TOO_HIGH, prioridad MEDIUM |
| `precioPropioMuyAlto_generaRecomendacionUrgente` | Precio propio 40% mayor que Amazon | Recomendación PRICE_TOO_HIGH, prioridad URGENT |
| `precioPropioBajoOportunidad_generaRecomendacionTooLow` | Precio propio 12% menor que Amazon | Recomendación PRICE_TOO_LOW |
| `sinDatosCompetencia_noGeneraRecomendacion` | Sin CompetitorPrice registrado | Sin recomendación generada |
| `recomendacionDuplicada_noSeCreaNueva` | Ya existe recomendación PENDING | No se crea duplicado |
| `cambioBruscoPrecio_generaAlerta` | Competidor baja precio un 20% | Alerta COMPETITOR_PRICE_DROP, severidad WARNING |

### 6.12.3 Tests Unitarios - ProductService

| Test | Descripción | Resultado esperado |
|------|-------------|-------------------|
| `crearProducto_registraHistorialInicial` | Crear producto con precio | PriceHistory con tipo INITIAL |
| `actualizarPrecioAlAlza_registraIncrease` | Subir precio de 10 a 15 euros | PriceHistory con tipo INCREASE |
| `actualizarPrecioALaBaja_registraDecrease` | Bajar precio de 15 a 10 euros | PriceHistory con tipo DECREASE |
| `actualizarSinCambiarPrecio_noRegistraHistorial` | Actualizar nombre sin tocar precio | Sin nueva entrada en PriceHistory |
| `skuDuplicadoMismoUsuario_lanzaExcepcion` | SKU ya existe para ese usuario | BadRequestException |
| `skuDuplicadoOtroUsuario_esCorrecto` | Mismo SKU pero diferente usuario | Producto creado correctamente |

### 6.12.4 Tests de Integración - ProductController

Tests con MockMvc que verifican el flujo completo desde la petición HTTP hasta la respuesta, usando base de datos H2 en memoria y perfiles de test.

| Test | Descripción | Código esperado |
|------|-------------|-----------------|
| `crearProducto_sinToken_devuelve401` | POST sin header Authorization | 401 |
| `crearProducto_conToken_devuelve201` | POST con token válido | 201 con producto |
| `listarProductos_soloDevuelveLosPropios` | Usuario B no ve productos de A | Lista vacía |
| `buscarProductos_inactivosNoAparecen` | Producto con active=false | No aparece en resultados |
| `getProducto_deOtroUsuario_devuelve400` | Acceder a producto ajeno | 400 |

### 6.12.5 Tests de Concurrencia - KeepaService

Tests heredados del desarrollo que verifican la thread-safety del servicio de Keepa bajo carga concurrente:

- 50 hilos simultáneos llamando a `getApiStatus()`: sin condiciones de carrera.
- 100 hilos simultáneos llamando a `isAvailable()`: respuesta consistente.
- Verificación de que el Semaphore limita correctamente las peticiones concurrentes.

---

# 7. Despliegue e Instalación

## 7.1 Requisitos del Sistema

**Backend (servidor):**

| Requisito | Mínimo | Recomendado |
|-----------|--------|-------------|
| Java | 17 LTS | 21 LTS |
| RAM | 512 MB | 1 GB |
| Disco | 500 MB | 2 GB |
| SO | Linux / macOS / Windows | Ubuntu 22.04 LTS |
| PostgreSQL | 14 | 15+ |
| Docker | 20.x (opcional) | — |

**Cliente Android:**

| Requisito | Valor |
|-----------|-------|
| Android mínimo | API 26 (Android 8.0) |
| Android recomendado | API 34+ |
| Arquitectura | arm64-v8a, x86_64 |
| RAM dispositivo | 2 GB mínimo |

## 7.2 Instalación del Backend en Local

**Paso 1 — Clonar el repositorio:**

```bash
git clone https://github.com/alvaromartinez/pricewise.git
cd pricewise/pricewise-backend
```

**Paso 2 — Levantar PostgreSQL con Docker:**

```bash
docker compose up -d
```

El archivo `docker-compose.yml` crea una instancia de PostgreSQL 15 en el puerto 5432 con usuario `postgres`, contraseña `postgres` y base de datos `pricewise_db`. Para entornos de producción se recomienda cambiar las credenciales y montar un volumen externo para la persistencia.

**Paso 3 — Configurar variables de entorno:**

Crear el archivo `.env` en la raíz del módulo backend (no se sube al repositorio):

```
JWT_SECRET=clave-secreta-de-al-menos-32-caracteres
DB_PASSWORD=postgres
SPRING_PROFILES_ACTIVE=dev
```

En producción, `SPRING_PROFILES_ACTIVE=prod` y `JWT_SECRET` debe ser una cadena aleatoria de al menos 64 caracteres.

**Paso 4 — Compilar y ejecutar:**

```bash
mvn clean package -DskipTests
java -jar target/pricewise-backend-0.0.1-SNAPSHOT.jar
```

O directamente desde Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway ejecuta automáticamente las migraciones V1–V8 al arrancar. El servidor queda disponible en `http://localhost:8080`.

**Paso 5 — Verificar que el servidor arrancó:**

```bash
curl http://localhost:8080/api/health
# Respuesta esperada: {"success":true,"message":"OK"}
```

La documentación interactiva de la API está disponible en `http://localhost:8080/swagger-ui.html`.

## 7.3 Despliegue en Producción (VPS)

Para un despliegue en servidor Linux (Ubuntu 22.04):

**Instalar Java 17:**

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless
java -version
```

**Instalar y configurar PostgreSQL:**

```bash
sudo apt install -y postgresql postgresql-contrib
sudo -u postgres psql -c "CREATE DATABASE pricewise_db;"
sudo -u postgres psql -c "CREATE USER pricewise WITH PASSWORD 'contraseña-segura';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE pricewise_db TO pricewise;"
```

**Ejecutar como servicio systemd:**

Crear `/etc/systemd/system/pricewise.service`:

```ini
[Unit]
Description=PriceWise Backend
After=network.target postgresql.service

[Service]
User=ubuntu
WorkingDirectory=/opt/pricewise
ExecStart=/usr/bin/java -jar /opt/pricewise/pricewise-backend.jar
EnvironmentFile=/opt/pricewise/.env
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable pricewise
sudo systemctl start pricewise
sudo systemctl status pricewise
```

## 7.4 Instalación de la Aplicación Android

**Opción A — Instalar el APK directamente (para pruebas):**

1. Activar "Orígenes desconocidos" en Ajustes > Seguridad del dispositivo.
2. Copiar `pricewise-debug.apk` al dispositivo (cable USB o descarga directa).
3. Abrir el archivo desde el gestor de archivos y confirmar la instalación.

**Compilar el APK desde el código fuente:**

```bash
cd pricewise-android
./gradlew assembleDebug
# APK generado en: app/build/outputs/apk/debug/app-debug.apk
```

**Configurar la URL del servidor** antes de compilar, en `app/src/main/java/com/alvaro/pricewise/data/api/ApiConfig.kt`:

```kotlin
const val BASE_URL = "http://TU_SERVIDOR:8080/"
```

Para desarrollo local con emulador Android usar `http://10.0.2.2:8080/` (la IP del host desde el emulador).

## 7.5 Primer Uso — Registro de Empresa

Al arrancar la aplicación por primera vez, el sistema no tiene ningún usuario. El primer registro crea automáticamente una empresa y asigna el rol `COMPANY_ADMIN` al usuario.

1. Abrir la app → pantalla de Login → pulsar "Registrarse".
2. Introducir email, nombre de usuario y contraseña (mínimo 6 caracteres).
3. Al confirmar, se crea la empresa y se accede directamente al dashboard.
4. Para añadir empleados: Ajustes > Usuarios > Crear usuario (rol EMPLOYEE).

El usuario `ADMIN` (superadministrador del sistema) debe crearse directamente en base de datos o mediante el endpoint de registro especificando el rol, ya que no está disponible en la pantalla de registro pública.

## 7.6 Planes de Contingencia

**El servidor no arranca:** verificar que PostgreSQL está activo (`systemctl status postgresql`), que las migraciones Flyway no fallaron (revisar logs: `journalctl -u pricewise -n 100`) y que `JWT_SECRET` tiene al menos 32 caracteres. Con `SPRING_PROFILES_ACTIVE=prod` el perfil usa `ddl-auto=validate`: si el esquema no coincide con las entidades, el arranque falla con un mensaje explícito indicando la tabla o columna problemática.

**La app no conecta al servidor:** verificar que `BASE_URL` en `ApiConfig.kt` apunta al servidor correcto y que el puerto 8080 está abierto en el firewall (`ufw allow 8080`). En dispositivo físico no usar `10.0.2.2` (esa IP solo funciona en el emulador).

**Keepa no devuelve datos:** el sistema continúa funcionando sin datos de Amazon. Los productos siguen siendo gestionables y el historial de precios propios se mantiene. La monitorización periódica omite los productos sin ASIN válido sin lanzar excepciones.

---

# 8. Evolución y Trabajo Futuro

## 8.1 Conclusiones

El resultado del proyecto es una aplicación funcional y desplegable que cubre el MVP planteado: una empresa puede gestionar su catálogo, consultar el precio de sus productos en Amazon y recibir recomendaciones automáticas. Eso era el objetivo central y está completo.

Lo que más me ha aportado técnicamente ha sido la integración con Keepa. Trabajar con una API externa que tiene sus propias limitaciones de concurrencia me obligó a entender de verdad cómo funcionan los `CompletableFuture`, los semáforos y el backoff exponencial, cosas que había leído pero no había aplicado en un contexto real hasta ahora. El scheduler de Quartz también fue nuevo para mí y resultó más complejo de configurar de lo que esperaba, especialmente la parte de gestión del job store.

En Android, la transición a Jetpack Compose ha sido un cambio de mentalidad importante respecto a las vistas XML. Al principio me costó entender cómo gestionar el estado correctamente sin que las recomposiciones rompieran el flujo, pero una vez que interioriza el modelo reactivo con StateFlow el resultado es bastante limpio.

Si tuviera que repetir el proyecto, probablemente habría planificado mejor la separación entre el modelo de usuario y el de empresa desde el principio. Empecé con usuarios individuales y añadí el concepto de empresa a mitad del desarrollo, lo que obligó a refactorizar bastantes queries y a introducir migraciones adicionales que se podrían haber evitado.

En general estoy satisfecho con el resultado, especialmente con la parte de backend, que quedó bien estructurada y testeada.

## 8.2 Dificultades Encontradas

**Integración con Keepa API:** la librería de Keepa usa callbacks en lugar del modelo request-response habitual. Tuve que envolver cada llamada en un `CompletableFuture` para poder componerlas y añadir reintentos con backoff. El problema más difícil fue una condición de carrera al arrancar: si dos peticiones llegaban antes de que el registro del competidor "Amazon ES" estuviese creado en BD, se intentaba crearlo dos veces y petaba con una excepción de clave duplicada. Lo resolví con double-checked locking en `@PostConstruct`.

**Problema N+1 de Hibernate:** al listar productos, Hibernate lanzaba una query adicional por cada producto para cargar la empresa asociada. Con 50 productos eran 51 queries. Lo detecté mirando los logs de SQL en desarrollo (`show-sql: true`) y lo corregí con `FetchType.LAZY` y `@EntityGraph` en la query de listado. Desde entonces tengo más cuidado con cómo defino las relaciones JPA.

**Diseño del motor de análisis:** los umbrales de precio (a partir de qué porcentaje una diferencia es significativa) los tuve que ajustar varias veces. Los primeros valores que usé eran demasiado sensibles y generaban recomendaciones para diferencias del 2%, lo que no tiene sentido práctico. Acabé con 10% como umbral base tras pensar en qué diferencia real notaría el cliente final de una PYME.

## 8.3 Trabajo Futuro

Hay varias cosas que quedaron fuera del MVP por tiempo y que me gustaría implementar en versiones futuras:

**Corto plazo:**
- Notificaciones push al dispositivo móvil cuando se genera una alerta CRITICAL, usando Firebase Cloud Messaging (FCM).
- Panel de analíticas con gráficos de evolución de precios propios y de la competencia.
- Gestión de recomendaciones de precio desde el móvil (aplicar precio sugerido, descartar). Los endpoints backend ya existen (`/api/analytics/recommendations/{id}/apply` y `/dismiss`), falta integrar la UI en `AlertsScreen`.

**Medio plazo:**
- Integración con una segunda fuente de datos de competencia (por ejemplo, PCComponentes para productos de electrónica), usando la infraestructura de `Competitor` y `CompetitorPrice` ya diseñada para soportar múltiples fuentes.
- Exportación del catálogo y del historial de precios a CSV/Excel para su análisis fuera de la aplicación.
- Migración de la caché en memoria a Redis para persistir entre reinicios del servidor y soportar despliegues con múltiples instancias.

**Largo plazo:**
- Modelo predictivo básico (regresión lineal sobre el historial de precios) para anticipar tendencias de precio.
- Soporte para otros marketplaces (eBay, PcComponentes) como fuentes de precios adicionales.

---

# 9. Bibliografía

[1] Pivotal Software. *Spring Boot Reference Documentation 3.2.x*. VMware, 2024. Disponible en: https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/ — Referencia principal para la configuración del framework, perfiles, autoconfiguración y gestión de dependencias.

[2] Pivotal Software. *Spring Security Reference Documentation*. VMware, 2024. Disponible en: https://docs.spring.io/spring-security/reference/ — Utilizado para la configuración de la cadena de filtros JWT, `SecurityConfig`, BCrypt y `@PreAuthorize`.

[3] Pivotal Software. *Spring Data JPA Reference Documentation*. VMware, 2024. Disponible en: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/ — Base para los repositorios JPA, consultas derivadas del nombre del método y `@EntityGraph`.

[4] Jones, M., Bradley, J., Sakimura, N. *JSON Web Token (JWT) — RFC 7519*. IETF, 2015. Disponible en: https://datatracker.ietf.org/doc/html/rfc7519 — Especificación del estándar JWT utilizado para la autenticación stateless del sistema.

[5] Niemöller, B. *jjwt Library Documentation 0.12.x*. Stormpath / Broadcom, 2024. Disponible en: https://github.com/jwtk/jjwt — Librería empleada para generar y validar tokens JWT en `JwtService`.

[6] Terracotta, Inc. *Quartz Scheduler Documentation 2.3.x*. 2023. Disponible en: https://www.quartz-scheduler.org/documentation/ — Referencia para la configuración del job de monitorización periódica (`PriceMonitorJob`).

[7] Keepa GmbH. *Keepa API Documentation*. 2024. Disponible en: https://keepa.com/#!discuss/t/keepa-api/99 — Documentación de la API usada para obtener precios de Amazon España por ASIN.

[8] Wooldridge, B. *HikariCP — High-Performance JDBC Connection Pool*. 2024. Disponible en: https://github.com/brettwooldridge/HikariCP — Pool de conexiones configurado en el perfil de producción con un máximo de 20 conexiones.

[9] Springdoc contributors. *SpringDoc OpenAPI Documentation*. 2024. Disponible en: https://springdoc.org/ — Generación automática de la documentación Swagger UI a partir de las anotaciones de los controladores.

[10] Walls, C. *Spring in Action*, 6ª edición. Manning Publications, 2022. — Referencia general para patrones de diseño con Spring Boot, arquitectura en capas y gestión de transacciones.

[11] Fowler, M. *Patterns of Enterprise Application Architecture*. Addison-Wesley, 2002. — Base conceptual para los patrones Repository, Service Layer y DTO utilizados en la arquitectura del backend.

[12] Wieruch, R. *The Road to React*. Self-published, 2022. Disponible en: https://www.roadtoreact.com/ — Referencia conceptual para el modelo de estado reactivo aplicado en Android con StateFlow y Jetpack Compose.

[13] Witalec, F. *Jetpack Compose internals*. Leanpub, 2022. Disponible en: https://leanpub.com/composeinternals — Consulta para entender el ciclo de recomposición y la gestión del estado en Compose.

[14] Baeldung. *Guide to @Async in Spring*. 2023. Disponible en: https://www.baeldung.com/spring-async — Consulta para la implementación de `CompletableFuture` y el executor dedicado de Keepa.

[15] OWASP Foundation. *REST Security Cheat Sheet*. 2024. Disponible en: https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html — Referencia de seguridad para la configuración de cabeceras HTTP, CORS y validación de tokens.

---

## Anexos

### Anexo A: Instrucciones de Instalación y Ejecución

**Requisitos:**
- Java 17 o superior
- Maven 3.8 o superior
- Docker y Docker Compose

**Pasos:**

```bash
# 1. Clonar el repositorio
git clone https://github.com/[usuario]/pricewise-backend.git
cd pricewise-backend

# 2. Arrancar PostgreSQL con Docker
docker-compose up -d

# 3. Configurar variables de entorno
cp .env.example .env
# Editar .env con JWT_SECRET y DB_PASSWORD

# 4. Ejecutar la aplicación
mvn spring-boot:run

# La API estará disponible en:
# http://localhost:9090/swagger-ui.html  (documentación interactiva)
# http://localhost:9090/api/health        (health check)
```

### Anexo B: Ejemplos de Peticiones a la API

**Registro de usuario:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "miempresa",
  "email": "contacto@miempresa.com",
  "password": "password123",
  "businessName": "Mi Empresa SL",
  "businessType": "Electrónica"
}
```

**Crear un producto:**
```http
POST /api/products
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "name": "Auriculares Sony WH-1000XM5",
  "sku": "B09XS7JWHH",
  "currentPrice": 349.99,
  "costPrice": 220.00,
  "category": "Electrónica",
  "brand": "Sony"
}
```

**Consultar precio en Amazon:**
```http
GET /api/competitors/amazon/price/B09XS7JWHH
Authorization: Bearer eyJ...
```

Respuesta:
```json
{
  "success": true,
  "data": {
    "asin": "B09XS7JWHH",
    "price": 319.99,
    "available": true,
    "currency": "EUR",
    "productTitle": "Sony WH-1000XM5 Auriculares Inalámbricos...",
    "productUrl": "https://www.amazon.es/dp/B09XS7JWHH",
    "scrapedAt": "2026-02-10T14:30:00"
  }
}
```

**Sincronizar producto con Amazon y obtener recomendación:**
```http
POST /api/competitors/amazon/sync/1
Authorization: Bearer eyJ...
```

Respuesta:
```json
{
  "success": true,
  "message": "Sincronización iniciada. El precio se actualizará en segundo plano."
}
```

### Anexo C: Diagrama de Clases Simplificado

```
User
 ├── id: Long
 ├── username: String
 ├── email: String
 ├── role: Role {USER, ADMIN}
 └── products: Set<Product>

Product
 ├── id: Long
 ├── name: String
 ├── sku: String
 ├── currentPrice: BigDecimal
 ├── costPrice: BigDecimal
 ├── monitoringEnabled: Boolean
 ├── user: User
 ├── priceHistory: List<PriceHistory>
 └── getMargin(): BigDecimal

PriceHistory
 ├── price: BigDecimal
 ├── previousPrice: BigDecimal
 ├── changeType: {INITIAL, INCREASE, DECREASE, NO_CHANGE}
 └── recordedAt: LocalDateTime

CompetitorPrice
 ├── price: BigDecimal
 ├── available: Boolean
 ├── competitor: Competitor
 ├── product: Product
 └── getPriceDifferencePercent(): BigDecimal

PriceRecommendation
 ├── recommendationType: {PRICE_TOO_HIGH, PRICE_TOO_LOW, COMPETITOR_DROP, ...}
 ├── suggestedPrice: BigDecimal
 ├── priceDifferencePercent: BigDecimal
 ├── status: {PENDING, APPLIED, DISMISSED, EXPIRED}
 └── priority: {LOW, MEDIUM, HIGH, URGENT}

Alert
 ├── alertType: {COMPETITOR_PRICE_DROP, COMPETITOR_PRICE_RISE, ...}
 ├── severity: {INFO, WARNING, CRITICAL}
 ├── isRead: Boolean
 └── changePercent: BigDecimal
```
