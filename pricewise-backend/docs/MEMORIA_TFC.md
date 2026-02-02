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

1. Introducción y justificación
2. Objetivos del proyecto
3. Análisis de requisitos
4. Planificación y metodología
5. Diseño del sistema
6. Implementación del backend
7. Implementación del cliente móvil
8. Pruebas
9. Conclusiones y trabajo futuro
10. Bibliografía y referencias

---

---

# 1. Introducción y Justificación

## 1.1 Contexto del Problema

Las pequeñas y medianas empresas que venden productos en canales físicos y digitales se enfrentan a un reto cotidiano: fijar precios competitivos sin disponer de los recursos de análisis que tienen las grandes empresas. En el entorno actual, donde plataformas como Amazon actualizan sus precios varias veces al día mediante algoritmos automatizados, una PYME que establece sus tarifas de forma manual y esporádica queda en una posición de desventaja estructural.

El proceso habitual en estas empresas consiste en visitar manualmente los sitios de los competidores, anotar precios en hojas de cálculo y tomar decisiones de forma reactiva, normalmente cuando ya se ha perdido competitividad. Este proceso es ineficiente, propenso a errores y difícilmente escalable cuando el catálogo supera unas pocas decenas de productos.

## 1.2 Solución Propuesta

PriceWise es una aplicación formada por una API REST backend y un cliente móvil para Android que permite a las PYMEs:

- Gestionar su catálogo de productos con control de precios de coste y venta.
- Consultar el precio actual de cada producto en Amazon España de forma controlada y bajo demanda.
- Recibir recomendaciones automáticas sobre si el precio propio es competitivo respecto a la competencia.
- Consultar el historial de evolución de precios tanto propios como de la competencia.

La obtención de precios de Amazon se realiza a través de **Keepa API**, un servicio especializado en datos de precios de Amazon con cobertura histórica de 90 días. Esta decisión técnica garantiza fiabilidad, estabilidad y cumplimiento de los términos de servicio de Amazon, a diferencia del scraping directo que es frágil y puede vulnerar los términos de uso de la plataforma.

## 1.3 Justificación del Alcance

El presente trabajo aborda un **MVP (Producto Mínimo Evaluable)** centrado en tres funcionalidades nucleares:

1. Gestión completa del catálogo de productos propios.
2. Consulta de precios de una fuente de competencia concreta y bien definida: Amazon España vía Keepa API.
3. Generación de recomendaciones básicas de precio basadas en la comparativa.

Adicionalmente, el sistema incluye funcionalidades de automatización (monitorización periódica programada) y un panel de administración que, si bien exceden el MVP mínimo, han podido desarrollarse dentro del tiempo del proyecto y quedan documentadas como **mejoras implementadas**, no como requisitos del alcance base.

Las notificaciones push al dispositivo móvil y la integración con fuentes de datos adicionales se identifican explícitamente como **líneas de trabajo futuro** fuera del alcance de este TFC.

---

# 2. Objetivos del Proyecto

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

Los siguientes elementos quedan explícitamente excluidos del presente TFC y se identifican como trabajo futuro:

- Notificaciones push al dispositivo móvil.
- Integración con fuentes de datos adicionales a Amazon/Keepa.
- Scraping web automatizado y no supervisado.
- Análisis predictivo o basado en machine learning.
- Panel de analíticas avanzadas con visualización de tendencias.

---

# 3. Análisis de Requisitos

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
- Un usuario no puede operar sobre recursos de otro usuario (verificación en capa de servicio).
- Las variables de entorno con secretos (JWT_SECRET, DB_PASSWORD, KEEPA_API_KEY) no deben estar en el repositorio de código.

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

# 4. Planificación y Metodología

## 4.1 Metodología de Desarrollo

Se ha aplicado una metodología iterativa e incremental organizada en fases semanales, con entregables funcionales al final de cada una. Cada fase cubre una capa o funcionalidad del sistema, permitiendo probar lo construido antes de avanzar a la siguiente.

Este enfoque es adecuado para un proyecto individual donde los requisitos son conocidos desde el inicio pero la implementación técnica puede revelar decisiones de diseño que afecten a fases posteriores.

## 4.2 Planificación por Fases

| Fase | Contenido | Duración |
|------|-----------|----------|
| 1 | Configuración del proyecto, Spring Boot, Docker, PostgreSQL | 1 día |
| 2 | Entidades JPA, repositorios, esquema de BD | 3 días |
| 3 | Autenticación JWT, Spring Security, registro y login | 2 días |
| 4 | CRUD de productos, historial de precios, caché | 4 días |
| 5 | Integración Keepa API, procesamiento asíncrono | 3 días |
| 6 | Motor de análisis de precios, recomendaciones y alertas | 3 días |
| 7 | Scheduler Quartz, monitorización periódica | 2 días |
| 8 | Panel de administración, gestión de usuarios | 2 días |
| 9 | Manejo de excepciones, validaciones, documentación OpenAPI | 2 días |
| 10 | Tests unitarios e integración | 4 días |
| 11 | Cliente móvil Android | 7 días |
| 12 | Ajustes finales, documentación técnica | 3 días |

**Duración total estimada:** 36 días laborables

## 4.3 Herramientas Utilizadas

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

# 5. Diseño del Sistema

## 5.1 Visión General de la Arquitectura

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

## 5.2 Arquitectura del Backend

El backend sigue una **arquitectura en capas** estándar de Spring Boot:

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

## 5.3 Modelo de Datos

El esquema relacional se compone de 7 tablas principales:

```
users
  id, username, email, password, business_name, business_type,
  active, role, created_at, updated_at

products
  id, name, description, sku, ean, current_price, cost_price,
  category, brand, image_url, active, monitoring_enabled,
  user_id (FK users), created_at, updated_at

price_history
  id, product_id (FK products), price, previous_price,
  change_type, change_reason, recorded_at

competitors
  id, name, code, base_url, logo_url, source_type,
  source_config, active, last_scraped_at, created_at, updated_at

competitor_prices
  id, product_id (FK products), competitor_id (FK competitors),
  price, original_price, currency, available, free_shipping,
  shipping_cost, product_url, competitor_product_title,
  scraped_at, source

alerts
  id, user_id (FK users), product_id (FK products),
  alert_type, title, message, previous_price, new_price,
  change_percent, is_read, severity, created_at, read_at

price_recommendations
  id, product_id (FK products), recommendation_type,
  current_price, competitor_price, suggested_price,
  price_difference_percent, potential_saving_or_profit,
  reason, status, priority, created_at, applied_at, dismissed_at
```

**Relaciones principales:**
- Un `User` tiene muchos `Products` (1:N).
- Un `Product` tiene muchos `PriceHistory` (1:N, cascade ALL).
- Un `Product` tiene muchos `CompetitorPrice` (1:N).
- Un `Product` tiene muchas `PriceRecommendations` (1:N).
- Un `Product` tiene muchas `Alerts` (1:N).
- Un `Competitor` tiene muchos `CompetitorPrice` (1:N).

**Índices de base de datos definidos:**

| Tabla | Columna(s) indexadas | Justificación |
|-------|----------------------|---------------|
| products | sku | Búsquedas por código interno |
| products | user_id | Todas las queries filtran por usuario |
| products | category | Filtros de búsqueda y consultas de categorías |
| price_history | product_id | Historial por producto |
| price_history | recorded_at | Consultas por rango de fecha |
| competitor_prices | product_id | Consultas de precio por producto |
| competitor_prices | competitor_id | Consultas por competidor |
| competitor_prices | scraped_at | Ordenación temporal |
| alerts | user_id | Alertas del usuario |
| alerts | is_read | Filtro de no leídas |
| price_recommendations | product_id | Recomendaciones por producto |
| price_recommendations | status | Filtro de pendientes |

## 5.4 Diseño de la API REST

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

## 5.5 Diseño de Seguridad

La autenticación se implementa con **JWT (JSON Web Token)** siguiendo el estándar RFC 7519.

**Flujo de autenticación:**

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

**Componentes de seguridad:**

- `JwtService`: genera y valida tokens. Valida al arrancar que `JWT_SECRET` tiene mínimo 32 caracteres y no es el valor por defecto en producción.
- `JwtAuthenticationFilter`: intercepta cada petición, extrae y valida el token, y carga el `UserPrincipal` en el `SecurityContextHolder`.
- `UserDetailsServiceImpl`: carga el usuario desde la base de datos para validar que sigue activo.
- `SecurityConfig`: define la cadena de filtros, las rutas públicas y el modo stateless.

---

# 6. Implementación del Backend

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

## 6.2 Estructura del Proyecto

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

## 6.3 Módulo de Autenticación

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
        .role(User.Role.USER)
        .active(true)
        .build();

    userRepository.save(user);
    String token = jwtService.generateToken(new UserPrincipal(user));
    return buildAuthResponse(user, token);
}
```

## 6.4 Módulo de Productos

El servicio de productos implementa validación de propiedad en cada operación de lectura y escritura, garantizando el aislamiento de datos entre usuarios.

El registro de historial de precios es automático y transparente: al crear un producto se registra la entrada `INITIAL`; al actualizar el precio, si ha cambiado, se registra `INCREASE` o `DECREASE`. El servicio determina el tipo de cambio comparando el precio anterior con el nuevo.

La caché de categorías y marcas evita consultas `SELECT DISTINCT` repetidas en el listado de filtros de búsqueda. Se invalida automáticamente al crear, actualizar o borrar un producto.

**Fragmento de actualización con historial:**

```java
@Transactional
@CacheEvict(value = {"categories", "brands"}, key = "#userId")
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

## 6.5 Integración con Keepa API

Keepa es un servicio externo que proporciona datos históricos y actuales de precios de Amazon a través de una API oficial. Su uso garantiza estabilidad (no depende de la estructura HTML de Amazon, que cambia con frecuencia), cobertura histórica de 90 días y cumplimiento de los términos de servicio de Amazon.

**Decisiones de diseño en KeepaService:**

**1. Procesamiento asíncrono con CompletableFuture**

Las llamadas a la API de Keepa pueden tardar varios segundos. Procesar estas llamadas de forma síncrona en el hilo HTTP del servidor bloquearía el thread pool de Tomcat. La solución es ejecutar las llamadas en un executor dedicado (`keepaExecutor`) y devolver un `CompletableFuture`, liberando el hilo del servidor inmediatamente.

**2. Control de concurrencia con Semaphore**

Keepa limita el número de peticiones concurrentes según el plan contratado. Se usa un `Semaphore(3)` para garantizar que no se lancen más de 3 llamadas simultáneas, independientemente de cuántos hilos del scheduler estén activos.

**3. Reintentos con backoff exponencial**

Las llamadas a APIs externas pueden fallar por timeouts o errores transitorios. Se implementan hasta 3 reintentos con esperas crecientes (1s, 2s, 4s) para dar tiempo a que el servicio externo se recupere sin saturarlo con reintentos inmediatos.

**4. Inicialización thread-safe del competidor Amazon**

Al arrancar la aplicación se crea (si no existe) el registro del competidor "Amazon ES" en la base de datos. Esta operación se ejecuta en `@PostConstruct` (hilo único) usando double-checked locking para prevenir condiciones de carrera en el caso de que múltiples peticiones lleguen antes de que la inicialización termine.

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

## 6.6 Motor de Análisis de Precios

`PriceAnalysisService` implementa la lógica central del valor diferencial de PriceWise: la comparativa entre precio propio y precio de la competencia con generación de recomendaciones priorizadas.

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

## 6.7 Monitorización Periódica (Mejora Implementada)

El sistema incluye un job Quartz (`PriceMonitorJob`) que se ejecuta automáticamente cada 6 horas para actualizar los precios de Amazon de todos los productos que tengan activada la monitorización. Esta funcionalidad excede los requisitos del MVP base y se documenta como mejora implementada.

**Criterios para que un producto entre en el ciclo de monitorización:**
- `monitoringEnabled = true`
- `active = true`
- SKU con formato ASIN de Amazon (comienza por "B0")

**Procesamiento por lotes para evitar saturar la API de Keepa:**
- Se procesa en páginas de 50 productos.
- Entre cada lote se espera 1 segundo.
- Las peticiones dentro del lote se lanzan en paralelo, respetando el semáforo de 3 concurrentes.
- Si se producen 5 errores consecutivos, el job aborta y registra en logs para revisión.

## 6.8 Gestión de Excepciones

Un `@RestControllerAdvice` centraliza el manejo de errores y garantiza que todas las respuestas de error siguen el mismo formato JSON.

| Excepción | Código HTTP | Uso |
|-----------|-------------|-----|
| `BadRequestException` | 400 | Validación de negocio (SKU duplicado, precio inválido) |
| `ResourceNotFoundException` | 404 | Entidad no encontrada |
| `MethodArgumentNotValidException` | 400 | Validación de campos del DTO |
| `AccessDeniedException` | 403 | Acceso a recurso sin permiso |
| `ExpiredJwtException` | 401 | Token caducado |
| `JwtException` | 401 | Token malformado o inválido |
| `Exception` (genérica) | 500 | Error no controlado |

## 6.9 Configuración y Entornos

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
| `KEEPA_API_KEY` | API key de Keepa (keepa.com) |
| `SPRING_PROFILES_ACTIVE` | `dev` o `prod` |

---

# 7. Implementación del Cliente Móvil

## 7.1 Plataforma y Tecnología

El cliente móvil se desarrolla como **aplicación Android nativa** utilizando:

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Arquitectura:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit 2 + OkHttp
- **Autenticación:** Interceptor HTTP que añade el token JWT a cada petición
- **Almacenamiento local:** DataStore para persistir el token entre sesiones

La elección de Android nativo frente a frameworks multiplataforma (Ionic, Flutter) se justifica por:
- Coherencia con la formación del ciclo (Desarrollo de Aplicaciones Multiplataforma con módulo Android).
- Mejor rendimiento y acceso completo a las APIs nativas de Android.
- Experiencia de usuario más fluida sin capas de abstracción intermedias.

## 7.2 Pantallas del MVP

El cliente cubre las funcionalidades del MVP definido:

**Autenticación:**
- `LoginScreen`: formulario de email y contraseña con validación local.
- `RegisterScreen`: formulario de registro con validación de campos.

**Gestión de productos:**
- `ProductListScreen`: listado paginado del catálogo con búsqueda por nombre. Cada item muestra nombre, precio, categoría y un indicador visual si tiene recomendación pendiente.
- `ProductDetailScreen`: detalle completo del producto con botón "Consultar en Amazon" que lanza la sincronización con Keepa y muestra el resultado.
- `CreateProductScreen`: formulario de alta de producto con campos requeridos (nombre, precio de venta) y opcionales (SKU/ASIN, precio de coste, categoría, marca).
- `EditProductScreen`: edición de los campos del producto.

**Recomendaciones:**
- `RecommendationsScreen`: lista de recomendaciones pendientes ordenadas por prioridad. Cada recomendación muestra el tipo, el precio sugerido, la diferencia porcentual y el motivo. Permite marcarlas como aplicadas o descartadas.

## 7.3 Arquitectura del Cliente

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

# 8. Pruebas

## 8.1 Estrategia de Testing

Se aplica una estrategia de pruebas en tres niveles:

| Nivel | Herramienta | Cobertura objetivo |
|-------|-------------|-------------------|
| Tests unitarios de servicio | JUnit 5 + Mockito | Lógica de negocio crítica |
| Tests de integración de controlador | MockMvc + H2 | Flujos HTTP completos |
| Tests de repositorio | @DataJpaTest + H2 | Queries personalizadas |

## 8.2 Tests Unitarios - PriceAnalysisService

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

## 8.3 Tests Unitarios - ProductService

| Test | Descripción | Resultado esperado |
|------|-------------|-------------------|
| `crearProducto_registraHistorialInicial` | Crear producto con precio | PriceHistory con tipo INITIAL |
| `actualizarPrecioAlAlza_registraIncrease` | Subir precio de 10 a 15 euros | PriceHistory con tipo INCREASE |
| `actualizarPrecioALaBaja_registraDecrease` | Bajar precio de 15 a 10 euros | PriceHistory con tipo DECREASE |
| `actualizarSinCambiarPrecio_noRegistraHistorial` | Actualizar nombre sin tocar precio | Sin nueva entrada en PriceHistory |
| `skuDuplicadoMismoUsuario_lanzaExcepcion` | SKU ya existe para ese usuario | BadRequestException |
| `skuDuplicadoOtroUsuario_esCorrecto` | Mismo SKU pero diferente usuario | Producto creado correctamente |

## 8.4 Tests de Integración - ProductController

Tests con MockMvc que verifican el flujo completo desde la petición HTTP hasta la respuesta, usando base de datos H2 en memoria y perfiles de test.

| Test | Descripción | Código esperado |
|------|-------------|-----------------|
| `crearProducto_sinToken_devuelve401` | POST sin header Authorization | 401 |
| `crearProducto_conToken_devuelve201` | POST con token válido | 201 con producto |
| `listarProductos_soloDevuelveLosPropios` | Usuario B no ve productos de A | Lista vacía |
| `buscarProductos_inactivosNoAparecen` | Producto con active=false | No aparece en resultados |
| `getProducto_deOtroUsuario_devuelve400` | Acceder a producto ajeno | 400 |

## 8.5 Tests de Concurrencia - KeepaService

Tests heredados del desarrollo que verifican la thread-safety del servicio de Keepa bajo carga concurrente:

- 50 hilos simultáneos llamando a `getApiStatus()`: sin condiciones de carrera.
- 100 hilos simultáneos llamando a `isAvailable()`: respuesta consistente.
- Verificación de que el Semaphore limita correctamente las peticiones concurrentes.

---

# 9. Conclusiones y Trabajo Futuro

## 9.1 Conclusiones

El proyecto ha alcanzado todos los objetivos definidos en el apartado 2:

- Se ha construido una API REST completa, documentada y segura que resuelve un problema real de las PYMEs: la falta de visibilidad sobre los precios de la competencia.
- La integración con Keepa API demuestra la capacidad de trabajar con servicios externos mediante patrones de concurrencia (semáforo, backoff exponencial, CompletableFuture) apropiados para entornos de producción.
- El motor de análisis de precios implementa lógica de negocio con umbrales configurables y priorización automática de recomendaciones.
- La arquitectura en capas facilita el mantenimiento y la extensión futura del sistema.
- El cliente Android cubre el flujo completo del MVP y puede operar de forma autónoma consumiendo la API.

Desde el punto de vista técnico, el proyecto ha servido para consolidar el uso de Spring Boot con seguridad JWT, JPA con relaciones complejas, programación asíncrona y diseño de APIs REST siguiendo buenas prácticas.

## 9.2 Dificultades Encontradas

**Integración con Keepa API:** la librería de Keepa usa un modelo de callbacks distinto al habitual. Fue necesario adaptar el flujo a `CompletableFuture` y gestionar la thread-safety de la inicialización del recurso compartido (el competidor Amazon) para evitar condiciones de carrera en arranques con carga concurrente.

**Problema N+1 de Hibernate:** el listado de productos lanzaba una query adicional por cada producto para cargar el usuario asociado. La solución requirió cambiar a `FetchType.LAZY` y usar `@EntityGraph` en la query de listado, lo que implicó entender en profundidad el comportamiento del ORM.

**Diseño del motor de análisis:** definir reglas de negocio claras (¿a partir de qué porcentaje es "caro"? ¿cómo priorizar?) requirió varias iteraciones hasta llegar a umbrales que tienen sentido en el contexto real de una PYME.

## 9.3 Trabajo Futuro

Las siguientes funcionalidades quedan identificadas como líneas de desarrollo para versiones posteriores:

**Corto plazo:**
- Notificaciones push al dispositivo móvil cuando se genera una alerta CRITICAL, usando Firebase Cloud Messaging (FCM).
- Panel de analíticas con gráficos de evolución de precios propios y de la competencia.
- Endpoints para que el usuario gestione sus propias alertas y recomendaciones desde el móvil (marcar como leídas, aplicar precio sugerido directamente).

**Medio plazo:**
- Integración con una segunda fuente de datos de competencia (por ejemplo, PCComponentes para productos de electrónica), usando la infraestructura de `Competitor` y `CompetitorPrice` ya diseñada para soportar múltiples fuentes.
- Exportación del catálogo y del historial de precios a CSV/Excel para su análisis fuera de la aplicación.
- Migración de la caché en memoria a Redis para persistir entre reinicios del servidor y soportar despliegues con múltiples instancias.

**Largo plazo:**
- Modelo predictivo básico (regresión lineal sobre el historial de precios) para anticipar tendencias de precio.
- Soporte para otros marketplaces (eBay, PcComponentes) como fuentes de precios adicionales.

---

# 10. Bibliografía y Referencias

## Documentación Oficial

- Spring Boot Reference Documentation (3.2.x).
  https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/

- Spring Security Reference Documentation.
  https://docs.spring.io/spring-security/reference/

- Spring Data JPA Reference Documentation.
  https://docs.spring.io/spring-data/jpa/docs/current/reference/html/

- Jakarta Persistence 3.1 Specification.
  https://jakarta.ee/specifications/persistence/3.1/

- JSON Web Token (JWT) — RFC 7519.
  https://datatracker.ietf.org/doc/html/rfc7519

- jjwt Library Documentation (0.12.x).
  https://github.com/jwtk/jjwt

- Quartz Scheduler Documentation.
  https://www.quartz-scheduler.org/documentation/

- Keepa API Documentation.
  https://keepa.com/#!discuss/t/keepa-api/99

- HikariCP — High-Performance JDBC Connection Pool.
  https://github.com/brettwooldridge/HikariCP

- SpringDoc OpenAPI Documentation.
  https://springdoc.org/

## Libros y Artículos

- Craig Walls. *Spring in Action* (6ª edición). Manning Publications, 2022.

- Josh Long, Kenny Bastani. *Cloud Native Java*. O'Reilly Media, 2017.

- Martin Fowler. *Patterns of Enterprise Application Architecture*. Addison-Wesley, 2002.

- Sam Newman. *Building Microservices* (2ª edición). O'Reilly Media, 2021.

## Recursos Técnicos

- Baeldung — Spring Security JWT Tutorial.
  https://www.baeldung.com/spring-security-oauth-jwt

- Baeldung — Spring Boot Pagination and Sorting.
  https://www.baeldung.com/spring-data-jpa-pagination-sorting

- Baeldung — Guide to @Async in Spring.
  https://www.baeldung.com/spring-async

- OWASP — REST Security Cheat Sheet.
  https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html

- Keepa.com — Información del servicio de seguimiento de precios Amazon.
  https://keepa.com/

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
# Editar .env con JWT_SECRET, DB_PASSWORD y KEEPA_API_KEY

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
