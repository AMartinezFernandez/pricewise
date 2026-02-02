================================================================================
                    DOCUMENTACION TECNICA - PriceWise Backend
                    Guia Completa de Arquitectura y Justificaciones
================================================================================

INDICE
------
0. GLOSARIO DE TERMINOS
1. INTRODUCCION Y VISION GENERAL
2. ESTRUCTURA DEL PROYECTO
3. PATRON DE ARQUITECTURA - CAPAS
4. JAVA Y SPRING BOOT
5. GESTION DE DEPENDENCIAS (INYECCION)
6. ENTIDADES JPA Y PERSISTENCIA
7. REPOSITORIOS Y SPRING DATA JPA
8. SERVICIOS Y LOGICA DE NEGOCIO
9. CONTROLADORES REST Y DTOs
10. SEGURIDAD Y AUTENTICACION JWT
11. INTEGRACION KEEPA API
12. PROGRAMACION ASINCRONA Y MULTIHILO
13. TAREAS PROGRAMADAS CON QUARTZ
14. CACHE Y RENDIMIENTO
15. MANEJO DE EXCEPCIONES
16. VALIDACIONES
17. CONFIGURACION Y PERFILES
18. POOL DE CONEXIONES
19. TESTING
20. REFERENCIAS Y DOCUMENTACION OFICIAL


================================================================================
0. GLOSARIO DE TERMINOS TECNICOS
================================================================================

Este glosario define los terminos tecnicos usados en este documento.
Consultalo cuando encuentres un termino que no conozcas.

TERMINOS DE PROGRAMACION GENERAL
---------------------------------

ABSTRACCION
    Ocultar complejidad mostrando solo lo esencial.
    Ejemplo: un repositorio ofrece "findById()" sin exponer SQL.

ACOPLAMIENTO
    Grado de dependencia entre componentes.
    - Alto acoplamiento (malo): cambiar uno afecta a muchos.
    - Bajo acoplamiento (bueno): componentes independientes.

API (Application Programming Interface)
    Contrato que permite que programas se comuniquen entre si.
    En PriceWise: endpoints REST que el frontend consume.

ASINCRONO
    Operacion que no bloquea el hilo principal mientras espera.
    La aplicacion sigue atendiendo peticiones mientras se espera la respuesta.

DTO (Data Transfer Object)
    Objeto exclusivamente para transportar datos entre capas.
    Evita exponer la entidad directamente al exterior.

ENDPOINT
    URL especifica de la API que realiza una accion.
    Ejemplo: POST /api/products crea un producto.

IDEMPOTENTE
    Operacion que produce el mismo resultado al ejecutarse N veces.
    PUT y DELETE deben ser idempotentes.

INMUTABILIDAD
    Objeto que no puede cambiar una vez creado.
    Preferido para seguridad en entornos multihilo.

ORM (Object-Relational Mapping)
    Tecnica para mapear objetos Java a tablas de base de datos.
    JPA + Hibernate es el ORM utilizado en este proyecto.

PAGINACION
    Dividir grandes conjuntos de datos en paginas para no devolver todo.
    Reduce uso de memoria y tiempo de respuesta.

SINGLETON
    Patron de diseno que garantiza una sola instancia de una clase.
    Spring gestiona beans como singletons por defecto.


TERMINOS DE JAVA Y SPRING
--------------------------

@Autowired / @RequiredArgsConstructor
    Inyeccion de dependencias. Spring proporciona el objeto automaticamente.

@Bean
    Metodo que produce un objeto gestionado por el contenedor Spring.

@Component / @Service / @Repository / @Controller
    Anotaciones que marcan clases para ser detectadas por Spring.
    @Service y @Repository son especializaciones de @Component.

@Entity
    Marca una clase Java como tabla en la base de datos (JPA).

@Transactional
    Garantiza que todas las operaciones de BD en un metodo
    se ejecutan atomicamente. Si una falla, todas se revierten.

BEAN
    Objeto cuyo ciclo de vida gestiona el contenedor Spring.

HIBERNATE
    Implementacion de JPA. Genera y ejecuta SQL automaticamente.

IOC (Inversion of Control)
    El framework gestiona la creacion de objetos, no el programador.
    Spring inyecta dependencias donde se necesitan.

JAKARTA VALIDATION
    Libreria de validacion de campos con anotaciones como @NotNull,
    @Size, @Email, @DecimalMin.

JPA (Java Persistence API)
    Especificacion estandar de Java para acceso a base de datos.
    Hibernate es la implementacion usada en este proyecto.

LAZY / EAGER LOADING
    LAZY: la relacion se carga solo cuando se accede a ella (eficiente).
    EAGER: la relacion se carga siempre junto al padre (puede ser costoso).

LOMBOK
    Libreria que genera boilerplate automaticamente via anotaciones:
    @Data genera getters/setters/equals/hashCode/toString.
    @Builder genera el patron builder.
    @RequiredArgsConstructor genera constructor de campos finales.


TERMINOS DE SEGURIDAD
----------------------

BCrypt
    Algoritmo de hash unidireccional para contrasenas.
    Incluye "salt" automaticamente para prevenir ataques de diccionario.

CORS (Cross-Origin Resource Sharing)
    Politica del navegador que controla desde que dominios se puede
    consumir la API. Configurable por entorno.

CSRF (Cross-Site Request Forgery)
    Ataque que engana al usuario para ejecutar acciones no deseadas.
    Desactivado en APIs REST sin estado (JWT no necesita CSRF).

JWT (JSON Web Token)
    Token firmado que contiene informacion del usuario.
    Formato: Header.Payload.Signature (codificado en Base64).

STATELESS
    El servidor no guarda sesiones. Toda la informacion va en el JWT.
    Permite escalar horizontalmente sin estado compartido.


TERMINOS DE INTEGRACIONES
--------------------------

ASIN
    Amazon Standard Identification Number. Identificador unico de
    producto en Amazon. Formato: B0XXXXXXXXX.

BACKOFF EXPONENCIAL
    Estrategia de reintento: cada intento fallido espera el doble
    que el anterior. Reduce presion sobre APIs externas.

CIRCUIT BREAKER
    Patron que detiene llamadas a un servicio externo si este falla
    repetidamente, para no saturar con reintentos infinitos.

KEEPA
    Servicio que ofrece API para consultar precios historicos y
    actuales de productos Amazon en multiples localizaciones.

SEMAFORO
    Mecanismo de control de concurrencia que limita cuantas operaciones
    pueden ejecutarse simultaneamente.

WEBHOOK
    Notificacion HTTP que un servicio externo envia al tuyo cuando
    ocurre un evento.


================================================================================
1. INTRODUCCION Y VISION GENERAL
================================================================================

PriceWise es una API REST backend para monitorizacion y analisis de precios
orientada a PYMEs. Permite gestionar el catalogo propio de productos, consultar
precios de competidores (principalmente Amazon via Keepa) y generar alertas y
recomendaciones de precios con logica de negocio automatizada.

PROBLEMA QUE RESUELVE:
- Dificultad para saber si el precio propio es competitivo
- Sin visibilidad de como evolucionan los precios de la competencia
- Toma de decisiones de precio sin datos objetivos
- Stock de alertas manuales para seguimiento de precios

SOLUCION TECNICA:
- API REST documentada con OpenAPI / Swagger
- Base de datos relacional PostgreSQL con historial de precios
- Integracion asincrona con Keepa API para precios de Amazon
- Motor de analisis que genera recomendaciones automaticas
- Tarea programada cada 6 horas que actualiza precios de competencia
- Seguridad JWT stateless con roles USER y ADMIN

TECNOLOGIAS ELEGIDAS Y JUSTIFICACION:

| Tecnologia           | Para que                        | Por que esta y no otra                  |
|----------------------|---------------------------------|-----------------------------------------|
| Java 17              | Lenguaje de programacion        | LTS, ecosistema maduro, tipado fuerte   |
| Spring Boot 3.2      | Framework principal             | Estandar de la industria, productivo    |
| Spring Security 6    | Autenticacion / autorizacion    | Integrado, configurable, JWT nativo     |
| PostgreSQL 14+       | Base de datos relacional        | ACID, JSON, indices avanzados           |
| Spring Data JPA      | Acceso a base de datos          | Queries automaticas, menos SQL manual   |
| Hibernate            | ORM                             | Implementacion JPA mas madura           |
| JWT (jjwt 0.12.3)   | Tokens de sesion stateless      | Sin estado, escalable, estandar RFC7519 |
| Quartz Scheduler     | Tareas programadas              | Robusto, persistente, configurable      |
| Keepa API            | Precios Amazon                  | Unica API fiable para Amazon            |
| Lombok               | Reducir boilerplate             | Menos codigo repetitivo, mas legible    |
| SpringDoc OpenAPI    | Documentacion API               | Auto-generada desde el codigo           |
| HikariCP             | Pool de conexiones BD           | El mas rapido de la industria           |
| Docker / Docker Compose | Entorno de desarrollo        | Reproducible, sin instalar Postgres     |

REFERENCIAS:
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security


================================================================================
2. ESTRUCTURA DEL PROYECTO
================================================================================

ESTRUCTURA DE CARPETAS:

pricewise-backend/
├── src/
│   ├── main/
│   │   ├── java/com/alvaro/pricewise/
│   │   │   ├── PriceWiseApplication.java   # Entry point, activa scheduling y cache
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java     # JWT, CORS, BCrypt, cadena de filtros
│   │   │   │   ├── AsyncConfig.java        # Executor para llamadas Keepa
│   │   │   │   ├── KeepaConfig.java        # Propiedades de configuracion Keepa
│   │   │   │   ├── SchedulerConfig.java    # Configuracion del job Quartz
│   │   │   │   └── OpenApiConfig.java      # Info Swagger / OpenAPI
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java     # Login, registro, perfil
│   │   │   │   ├── ProductController.java  # CRUD productos
│   │   │   │   ├── CompetitorController.java  # Keepa status y sync
│   │   │   │   ├── AdminController.java    # Gestion de usuarios (ADMIN)
│   │   │   │   ├── AnalyticsController.java  # Metricas y analisis
│   │   │   │   └── HealthController.java   # Health check
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java        # Registro, login, tokens
│   │   │   │   ├── ProductService.java     # CRUD, historial, cache
│   │   │   │   ├── KeepaService.java       # Integracion Amazon/Keepa
│   │   │   │   └── PriceAnalysisService.java # Alertas y recomendaciones
│   │   │   ├── entity/
│   │   │   │   ├── User.java               # Usuario con rol
│   │   │   │   ├── Product.java            # Producto con SKU/EAN
│   │   │   │   ├── PriceHistory.java       # Historico de precios
│   │   │   │   ├── Competitor.java         # Tienda competidora
│   │   │   │   ├── CompetitorPrice.java    # Precio de competidor
│   │   │   │   ├── Alert.java              # Alertas de precio
│   │   │   │   └── PriceRecommendation.java # Recomendaciones IA
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── PriceHistoryRepository.java
│   │   │   │   ├── CompetitorRepository.java
│   │   │   │   ├── CompetitorPriceRepository.java
│   │   │   │   ├── AlertRepository.java
│   │   │   │   └── PriceRecommendationRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java         # Generacion y validacion JWT
│   │   │   │   ├── JwtAuthenticationFilter.java  # Filtro por peticion HTTP
│   │   │   │   ├── UserPrincipal.java      # Objeto autenticado en contexto
│   │   │   │   └── UserDetailsServiceImpl.java # Carga usuario desde BD
│   │   │   ├── scheduler/
│   │   │   │   └── PriceMonitorJob.java    # Tarea Quartz cada 6 horas
│   │   │   ├── dto/
│   │   │   │   ├── auth/                   # LoginRequest, RegisterRequest, AuthResponse
│   │   │   │   ├── product/                # ProductRequest, ProductResponse, SearchRequest
│   │   │   │   ├── admin/                  # UserUpdateRequest, AdminStatsResponse
│   │   │   │   ├── analytics/              # DashboardResponse, StatsResponse
│   │   │   │   └── common/                 # ApiResponse, PageResponse
│   │   │   └── exception/
│   │   │       ├── BadRequestException.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.yml             # Configuracion Spring Boot
│   └── test/
│       └── java/com/alvaro/pricewise/      # Tests unitarios e integracion
├── pom.xml                                 # Dependencias Maven
├── docker-compose.yml                      # PostgreSQL local
├── Dockerfile                              # Imagen de produccion
├── .env.example                            # Variables de entorno de ejemplo
├── PriceWise_API.postman_collection.json   # Coleccion Postman
└── README.md                              # Documentacion de inicio

POR QUE ESTA ESTRUCTURA:

La estructura sigue una arquitectura en capas clasica de Spring Boot:

1. CONTROLLER: Recibe peticiones HTTP, delega al servicio, devuelve respuesta.
   - Sin logica de negocio. Solo traduce HTTP a llamadas Java.

2. SERVICE: Toda la logica de negocio aqui.
   - Transacciones, reglas de precios, orquestacion de repositorios.

3. REPOSITORY: Acceso a base de datos mediante Spring Data JPA.
   - Queries automaticas y personalizadas sin SQL manual.

4. ENTITY: Modelos de datos mapeados a tablas PostgreSQL.

5. SECURITY: Filtro JWT separado para no contaminar la logica de negocio.

6. DTO: Contratos de entrada/salida sin exponer entidades internas.


================================================================================
3. PATRON DE ARQUITECTURA - CAPAS
================================================================================

PriceWise usa arquitectura en capas (Layered Architecture) siguiendo el
estandar de Spring Boot.

DIAGRAMA DE CAPAS:

+-------------------------------------------------------------------+
|                     CAPA PRESENTACION                             |
|                    (Controllers + DTOs)                           |
|                                                                   |
|  Recibe peticiones HTTP, valida entrada, formatea salida JSON     |
+-------------------------------------------------------------------+
                              v ^
+-------------------------------------------------------------------+
|                     CAPA NEGOCIO                                  |
|                       (Services)                                  |
|                                                                   |
|  Reglas de precio, alertas, recomendaciones, transacciones        |
+-------------------------------------------------------------------+
                              v ^
+-------------------------------------------------------------------+
|                     CAPA DATOS                                    |
|              (Repositories + Entities)                            |
|                                                                   |
|  Acceso a PostgreSQL via JPA. Queries, filtros, paginacion        |
+-------------------------------------------------------------------+
                              v ^
+-------------------------------------------------------------------+
|                       BASE DE DATOS                               |
|                       (PostgreSQL)                                |
|                                                                   |
|  Tablas: users, products, price_history, competitors,             |
|  competitor_prices, alerts, price_recommendations                 |
+-------------------------------------------------------------------+

FLUJO DE DATOS (Request -> Response):

       +-------------+
       |   Cliente   |  (frontend / Postman)
       +------+------+
              | HTTP Request + JWT
              v
       +-------------+
       |  JWT Filter |  Verifica token, extrae usuario
       +------+------+
              |
              v
       +-------------+
       | Controller  |  Recibe @RequestBody, llama servicio
       +------+------+
              |
              v
       +-------------+
       |   Service   |  Logica de negocio, @Transactional
       +------+------+
              |
              v
       +-------------+
       | Repository  |  Consulta / persiste en BD
       +------+------+
              |
              v
       +-------------+
       | PostgreSQL  |  Datos persistidos
       +-------------+

EJEMPLO CONCRETO - FLUJO DE "CREAR PRODUCTO":

1. POST /api/products con JSON y header Authorization: Bearer <token>
2. JwtAuthenticationFilter valida token y carga UserPrincipal en contexto
3. ProductController recibe @RequestBody ProductRequest validado por @Valid
4. ProductController llama productService.createProduct(request, userId)
5. ProductService verifica unicidad de SKU
6. ProductService guarda el producto en BD
7. ProductService registra entrada en PriceHistory como INITIAL
8. ProductService invalida cache de categorias y marcas del usuario
9. Controller devuelve ApiResponse con el ProductResponse creado


================================================================================
4. JAVA Y SPRING BOOT
================================================================================

JAVA 17 - CARACTERISTICAS USADAS
----------------------------------

RECORDS (DTOs inmutables):
En algunos DTOs se podrian usar records, aunque en este proyecto se usa
principalmente Lombok @Data por compatibilidad con frameworks.

SWITCH EXPRESSIONS:
Usados en logica de calculo de tipo de cambio de precio:

    ChangeType changeType = switch (comparison) {
        case 1  -> ChangeType.INCREASE;
        case -1 -> ChangeType.DECREASE;
        default -> ChangeType.NO_CHANGE;
    };

VAR (inferencia de tipos local):
    var products = productRepository.findByUserId(userId);

SPRING BOOT 3.2
----------------

AUTOCONFIGURACION:
Spring Boot configura automaticamente componentes segun las dependencias
presentes en el classpath. Por ejemplo, detecta que hay JPA + Postgres y
configura el datasource automaticamente.

ANOTACIONES CLAVE:

@SpringBootApplication
    Combina @Configuration + @EnableAutoConfiguration + @ComponentScan.
    Solo se usa en PriceWiseApplication.java.

@RestController
    Combina @Controller + @ResponseBody. Todos los metodos devuelven
    JSON automaticamente.

@Service
    Marca clase como servicio Spring. Habilita transacciones con
    @Transactional.

@Repository
    Marca interfaz como repositorio. Spring Data JPA genera la
    implementacion automaticamente.

@Transactional
    Garantiza atomicidad. Si saveProduct() falla a mitad, todo se
    revierte. Sin esta anotacion, podria guardarse el producto sin
    el historial de precios.

EJEMPLO DE CONTROLADOR TIPICO:

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProductResponse product = productService.createProduct(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Producto creado", product));
    }
}

REFERENCIAS:
- Spring Boot: https://spring.io/projects/spring-boot
- Spring MVC: https://docs.spring.io/spring-framework/reference/web/webmvc.html


================================================================================
5. GESTION DE DEPENDENCIAS (INYECCION)
================================================================================

Spring gestiona todos los objetos (beans) y los inyecta automaticamente.

POR QUE INYECCION DE DEPENDENCIAS:
- Las clases no crean sus propias dependencias (bajo acoplamiento)
- Facil de testear (se pueden inyectar mocks)
- Spring gestiona el ciclo de vida (singleton por defecto)

PATRON PREFERIDO EN PRICEWISE: Constructor Injection con Lombok
---------------------------------------------------------------

// Lombok genera el constructor con todos los campos final
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PriceAnalysisService priceAnalysisService;

    // Spring inyecta los tres beans automaticamente al crear el servicio
}

POR QUE CONSTRUCTOR INJECTION Y NO @Autowired EN CAMPO:
- Los campos final garantizan inmutabilidad
- El objeto nunca esta en estado parcialmente inicializado
- Mas facil de testear sin Spring

SCOPES DE SPRING BEANS:

| Scope       | Descripcion                      | Usado en          |
|-------------|----------------------------------|-------------------|
| @Singleton  | Una instancia para toda la app   | Services, Repos   |
| @Prototype  | Nueva instancia cada vez         | No usado aqui     |
| @RequestScoped | Una instancia por request HTTP | No usado aqui     |

CONFIGURACION MANUAL (AppConfig / SecurityConfig):
Cuando Spring no puede crear el bean automaticamente, se usa @Bean:

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(...) { ... }
}

REFERENCIAS:
- Dependency Injection: https://spring.io/guides/gs/rest-service/


================================================================================
6. ENTIDADES JPA Y PERSISTENCIA
================================================================================

Las entidades son clases Java mapeadas a tablas de PostgreSQL via JPA + Hibernate.

VENTAJAS DE JPA SOBRE SQL PURO:
- Generacion automatica de SQL
- Verificacion en tiempo de compilacion de relaciones
- Migracion de esquema con Hibernate DDL auto
- Operaciones CRUD sin SQL manual

ENTIDADES EN PRICEWISE:
------------------------

1. USER - Usuarios del sistema

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "username")
})
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;     // Unico, 3-50 chars
    private String email;        // Unico, formato valido
    private String password;     // BCrypt hash
    private String businessName; // Nombre del negocio
    private String businessType; // Tipo de empresa
    @Enumerated(EnumType.STRING)
    private Role role;           // USER o ADMIN
    private Boolean active;      // Soft-enable/disable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

Por que estos campos:
- role como String en BD (no numero): legible en consultas directas
- active para desactivar sin borrar (historial preservado)
- businessName/businessType: contexto de negocio para personalizacion futura

2. PRODUCT - Productos del catalogo del usuario

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku",      columnList = "sku"),
    @Index(name = "idx_product_user",     columnList = "user_id"),
    @Index(name = "idx_product_category", columnList = "category")
})
public class Product {
    private Long id;
    private String name;              // max 200 chars
    private String description;       // max 1000 chars
    private String sku;               // Codigo interno, unico
    private String ean;               // Codigo de barras EAN-13
    private BigDecimal currentPrice;  // Precio actual (precision 12,2)
    private BigDecimal costPrice;     // Coste para calcular margen
    private String category;
    private String brand;
    private String imageUrl;
    private Boolean active;           // Soft delete
    private Boolean monitoringEnabled;// Si se monitorea la competencia
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<PriceHistory> priceHistory;
}

Metodo helper getMargin():
    Calcula margen = (currentPrice - costPrice) / costPrice * 100
    Devuelve null si no hay costPrice configurado.

Por que los indices:
- idx_product_sku: busquedas frecuentes por codigo interno
- idx_product_user: todas las queries filtran por usuario propietario
- idx_product_category: filtros de busqueda y queries de categorias

3. PRICEHISTORY - Historico de precios

@Entity
@Table(name = "price_history", indexes = {
    @Index(columnList = "product_id"),
    @Index(columnList = "recorded_at")
})
public class PriceHistory {
    private Long id;
    private BigDecimal price;
    private BigDecimal previousPrice;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;    // INCREASE, DECREASE, NO_CHANGE, INITIAL
    private String changeReason;
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
    private LocalDateTime recordedAt;
}

Por que registrar changeType:
- Permite analytics de tendencia sin recalcular siempre
- INITIAL marca la primera entrada para no confundirla con cambios

4. COMPETITOR - Tiendas competidoras

@Entity
@Table(name = "competitors")
public class Competitor {
    private Long id;
    private String name;              // Unico, ej: "Amazon ES"
    private String code;              // Clave corta: "amazon_es"
    private String baseUrl;
    private String logoUrl;
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;    // API, SCRAPING, MANUAL
    private String sourceConfig;      // JSON de configuracion (API keys, etc.)
    private Boolean active;
    private LocalDateTime lastScrapedAt;
}

Por que sourceType como enum:
- Permite implementar estrategias distintas segun origen de datos
- API: Keepa; SCRAPING: Jsoup; MANUAL: entrada manual del usuario

5. COMPETITORPRICE - Precios de competidores

@Entity
@Table(name = "competitor_prices")
public class CompetitorPrice {
    private Long id;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String currency;          // EUR por defecto
    private Boolean available;
    private Boolean freeShipping;
    private BigDecimal shippingCost;
    private String productUrl;
    private String competitorProductTitle;
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    private Competitor competitor;
    private LocalDateTime scrapedAt;
    private String source;
}

Metodo getPriceDifferencePercentage():
    (competitorPrice - ourPrice) / ourPrice * 100
    Positivo = competidor mas caro. Negativo = competidor mas barato.

6. ALERT - Alertas de precio

@Entity
@Table(name = "alerts")
public class Alert {
    private Long id;
    @Enumerated(EnumType.STRING)
    private AlertType alertType;      // COMPETITOR_PRICE_DROP, PRICE_BELOW_COST, etc.
    private String title;
    private String message;
    @Enumerated(EnumType.STRING)
    private Severity severity;        // INFO, WARNING, CRITICAL
    private Boolean isRead;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

7. PRICERECOMMENDATION - Recomendaciones de precio

@Entity
@Table(name = "price_recommendations")
public class PriceRecommendation {
    private Long id;
    @Enumerated(EnumType.STRING)
    private RecommendationType recommendationType;  // PRICE_TOO_HIGH, etc.
    private BigDecimal suggestedPrice;
    @Enumerated(EnumType.STRING)
    private Status status;       // PENDING, APPLIED, DISMISSED
    @Enumerated(EnumType.STRING)
    private Priority priority;   // LOW, MEDIUM, HIGH, URGENT
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
    private LocalDateTime appliedAt;
    private LocalDateTime dismissedAt;
}

DATABASE:
---------

@SpringBootApplication
Con DDL auto = update en desarrollo: Hibernate crea/actualiza tablas automaticamente.
Con DDL auto = validate en produccion: Solo verifica que el esquema es correcto.

ESTRATEGIA DE IDENTIFICADORES:
GenerationType.IDENTITY delega la generacion del ID a PostgreSQL (SERIAL/BIGSERIAL).
Es la estrategia mas eficiente con PostgreSQL.

REFERENCIAS:
- JPA: https://jakarta.ee/specifications/persistence/
- Hibernate: https://hibernate.org/orm/documentation/


================================================================================
7. REPOSITORIOS Y SPRING DATA JPA
================================================================================

Los repositorios son interfaces. Spring Data JPA genera la implementacion SQL
automaticamente en tiempo de compilacion.

JERARQUIA DE INTERFACES:
    Repository (base)
        -> CrudRepository (CRUD basico)
            -> PagingAndSortingRepository (paginacion)
                -> JpaRepository (+ flush, batch, etc.)

PRICEWISE EXTIENDE JpaRepository SIEMPRE.

TIPOS DE QUERIES EN PRICEWISE:
-------------------------------

1. DERIVED QUERIES (automaticas por nombre):

interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring genera: SELECT * FROM products WHERE user_id = ? AND active = true
    List<Product> findByUserIdAndActiveTrue(Long userId);

    // Spring genera: SELECT COUNT(*) FROM products WHERE user_id = ?
    long countByUserId(Long userId);

    // Spring genera: SELECT DISTINCT category FROM products WHERE user_id = ?
    // y active = true
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.user.id = :userId AND p.active = true")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);
}

2. CUSTOM JPQL QUERIES:

@Query("SELECT p FROM Product p WHERE p.user.id = :userId " +
       "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
       "AND (:category IS NULL OR p.category = :category) " +
       "AND p.active = true")
Page<Product> searchProducts(@Param("userId") Long userId,
                              @Param("name") String name,
                              @Param("category") String category,
                              Pageable pageable);

3. PAGINACION:

// En el controller:
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

// En el repositorio:
Page<Product> findByUserIdAndActiveTrue(Long userId, Pageable pageable);

// La respuesta incluye: content, totalElements, totalPages, number, size

VENTAJAS SOBRE SQL MANUAL:
- Sin riesgo de SQL Injection (queries parametrizadas siempre)
- Refactoring seguro (el compilador detecta errores)
- Menos codigo para operaciones estandar

REFERENCIAS:
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Query Methods: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods


================================================================================
8. SERVICIOS Y LOGICA DE NEGOCIO
================================================================================

Los servicios contienen toda la logica de negocio. Son @Transactional por defecto
para garantizar consistencia de datos.

AUTHSERVICE:
-----------

Responsabilidades:
- Registro de usuario con validacion de unicidad
- Login con generacion de JWT
- Recuperacion de perfil autenticado

Flujo de registro:
1. Verificar que el email no existe: userRepository.existsByEmail()
2. Verificar que el username no existe: userRepository.existsByUsername()
3. Codificar contrasena: passwordEncoder.encode(rawPassword)
4. Guardar usuario con rol USER por defecto
5. Generar JWT y devolver AuthResponse

PRODUCTSERVICE:
--------------

Responsabilidades:
- CRUD completo con validacion de propiedad del usuario
- Registro automatico de historial de precios al crear/actualizar
- Busqueda con filtros y paginacion
- Invalidacion de cache al modificar productos

Flujo de creacion de producto:
1. Verificar unicidad de SKU si se proporciona
2. Obtener entidad User del repositorio
3. Construir Product con builder de Lombok
4. Guardar en BD: productRepository.save(product)
5. Registrar PriceHistory con tipo INITIAL
6. Invalidar cache de categorias y marcas del usuario

Flujo de actualizacion:
1. Cargar producto y verificar que pertenece al usuario
2. Detectar si el precio ha cambiado
3. Si cambia: calcular ChangeType (INCREASE o DECREASE)
4. Registrar nueva entrada en PriceHistory
5. Guardar producto actualizado
6. Devolver ProductResponse mapeado

KEEPASERVICE:
------------

Responsabilidades:
- Consultar precios de Amazon via API Keepa
- Gestionar concurrencia y reintentos
- Crear entrada en Competitor si no existe

Caracteristicas clave:
- Thread-safe: uso de Semaphore(3) para maximo 3 peticiones concurrentes
- Backoff exponencial: espera 1s, 2s, 4s entre reintentos (max 3)
- Async: devuelve CompletableFuture para no bloquear el hilo
- Double-checked locking para inicializacion del competidor Amazon
- Timeout: 30 segundos por peticion
- Locales soportados: ES, US, DE, FR, UK, IT, CA, JP

PRICEANALYSISSERVICE:
--------------------

Responsabilidades:
- Analizar precios propios vs competencia
- Generar recomendaciones de precio automaticas
- Crear alertas para cambios significativos

Umbrales configurados:
- HIGH_PRICE_THRESHOLD = 10%: si el precio propio es >10% mas caro que la media
  de competidores, generar recomendacion PRICE_TOO_HIGH
- LOW_PRICE_THRESHOLD = 10%: si el precio propio es >10% mas barato,
  generar recomendacion PRICE_TOO_LOW (oportunidad de subir margen)
- SUDDEN_CHANGE_THRESHOLD = 15%: cambio mayor al 15% en competidor
  genera alerta inmediata

REFERENCIAS:
- Spring Transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction.html


================================================================================
9. CONTROLADORES REST Y DTOs
================================================================================

CONTROLADORES:
--------------

Los controladores son finos (thin controllers): solo reciben la peticion,
delegan al servicio y devuelven la respuesta. Sin logica de negocio.

ENDPOINTS PUBLICOS (sin autenticacion):
    POST   /api/auth/register          - Registro nuevo usuario
    POST   /api/auth/login             - Login, devuelve JWT
    GET    /api/health                 - Health check
    GET    /api/competitors/status     - Estado disponibilidad Keepa

ENDPOINTS AUTENTICADOS (USER o ADMIN):
    GET    /api/auth/profile           - Perfil del usuario autenticado
    POST   /api/products               - Crear producto
    GET    /api/products               - Listar productos (paginado)
    GET    /api/products/{id}          - Detalle producto
    PUT    /api/products/{id}          - Actualizar producto
    DELETE /api/products/{id}          - Borrado logico
    GET    /api/products/search        - Buscar con filtros
    GET    /api/products/categories    - Categorias del usuario (cacheado)
    GET    /api/products/brands        - Marcas del usuario (cacheado)
    GET    /api/products/count         - Total de productos
    GET    /api/competitors/amazon/price/{asin}     - Precio Amazon por ASIN
    POST   /api/competitors/amazon/sync/{productId} - Sincronizar con Amazon

ENDPOINTS ADMIN (solo ADMIN):
    GET    /api/admin/stats                     - Estadisticas del sistema
    GET    /api/admin/dashboard                 - Metricas con desglose
    GET    /api/admin/users                     - Listar usuarios
    GET    /api/admin/users/{id}                - Detalle usuario
    PUT    /api/admin/users/{id}                - Actualizar usuario
    PUT    /api/admin/users/{id}/password       - Cambiar contrasena
    PUT    /api/admin/users/{id}/role           - Cambiar rol
    PUT    /api/admin/users/{id}/status         - Activar/desactivar
    DELETE /api/admin/users/{id}                - Eliminar permanentemente
    GET    /api/admin/scheduler/status          - Estado del scheduler
    POST   /api/admin/scheduler/trigger-now    - Ejecutar job ahora
    POST   /api/admin/scheduler/pause          - Pausar scheduler
    POST   /api/admin/scheduler/resume         - Reanudar scheduler

DOCUMENTACION:
    GET    /swagger-ui.html   - Interfaz Swagger interactiva
    GET    /api-docs          - Especificacion OpenAPI en JSON
    GET    /actuator/**       - Metricas y estado Spring Actuator

FORMATO DE RESPUESTA:
---------------------

Respuesta exitosa:
{
  "success": true,
  "message": "Producto creado correctamente",
  "data": { ... },
  "timestamp": "2026-02-10T10:30:00"
}

Respuesta paginada:
{
  "success": true,
  "data": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "isFirst": true,
  "isLast": false
}

Respuesta de error:
{
  "success": false,
  "message": "Producto no encontrado con id: 99",
  "timestamp": "2026-02-10T10:30:00"
}

DTOs:
-----

Los DTOs separan el contrato de la API de la entidad interna.
Ventajas:
- Se pueden cambiar las entidades sin romper la API
- Se puede incluir/excluir campos segun lo que necesita el cliente
- La validacion (@Valid) se hace en el DTO, no en la entidad

Ejemplo:

// Request (entrada)
public class ProductRequest {
    @NotBlank
    @Size(max = 200)
    private String name;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
    // ...
}

// Response (salida)
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal margin;        // Calculado, no existe en BD
    private LocalDateTime createdAt;
    // Sin campo password, sin campos internos
}

REFERENCIAS:
- Spring MVC: https://docs.spring.io/spring-framework/reference/web/webmvc.html


================================================================================
10. SEGURIDAD Y AUTENTICACION JWT
================================================================================

Ver SEGURIDAD.md para el analisis completo de seguridad.

RESUMEN DE LA IMPLEMENTACION:

1. FLUJO DE AUTENTICACION:

   Cliente envia:  POST /api/auth/login { email, password }
   Servidor responde: { token: "eyJ...", expiresIn: 86400 }
   Cliente guarda el token.
   En cada peticion: Authorization: Bearer eyJ...

2. VALIDACION DE TOKEN (por peticion):

   JwtAuthenticationFilter.doFilterInternal():
   a. Extraer header Authorization
   b. Validar formato "Bearer <token>"
   c. Extraer username del token
   d. Verificar firma HMAC-SHA256 con JWT_SECRET
   e. Verificar que no ha expirado
   f. Cargar usuario desde BD (UserDetailsServiceImpl)
   g. Guardar UserPrincipal en SecurityContextHolder

3. CONTROL DE ACCESO:

   @PreAuthorize("hasRole('ADMIN')")    - Solo admins
   @PreAuthorize("hasAnyRole('USER', 'ADMIN')")  - Autenticados

   NOTA: La seguridad de propiedad se verifica en el servicio:
   if (!product.getUser().getId().equals(userId)) {
       throw new BadRequestException("No tienes permiso para este producto");
   }

CONFIGURACION DE CORS:
    dev:  allowedOrigins = "*"  (permisivo para desarrollo local)
    prod: allowedOrigins configurado por variable de entorno

REFERENCIAS:
- Spring Security: https://spring.io/projects/spring-security
- JWT: https://jwt.io/introduction


================================================================================
11. INTEGRACION KEEPA API
================================================================================

Keepa proporciona acceso programatico a precios historicos y actuales de
productos de Amazon.

CONFIGURACION:

@ConfigurationProperties(prefix = "keepa")
public class KeepaConfig {
    private String apiKey;           // Desde variable de entorno KEEPA_API_KEY
    private String defaultLocale;    // "ES" por defecto
    private int priceHistoryDays;    // 90 dias por defecto
}

LLAMADA A KEEPA:

public CompletableFuture<Optional<CompetitorPrice>> getAmazonPrice(
        String asin, Long productId) {

    return CompletableFuture.supplyAsync(() -> {
        // 1. Adquirir semaforo (max 3 concurrentes)
        semaphore.acquire();
        try {
            // 2. Ejecutar con reintentos
            return executeWithRetry(asin, productId);
        } finally {
            // 3. Liberar semaforo siempre
            semaphore.release();
        }
    }, keepaExecutor);
}

private Optional<CompetitorPrice> executeWithRetry(String asin, Long productId) {
    int attempts = 0;
    while (attempts < MAX_RETRIES) {
        try {
            // Llamada a Keepa API
            KeepaAPI api = new KeepaAPI(config.getApiKey());
            Request request = Request.getProductRequest(
                parseLocale(config.getDefaultLocale()),
                0,   // system offers
                config.getPriceHistoryDays(),
                asin
            );
            Response response = api.sendRequestWithOptionsRetry(request);
            // Mapear respuesta a CompetitorPrice
            return mapToCompetitorPrice(response, productId);
        } catch (Exception e) {
            attempts++;
            if (attempts < MAX_RETRIES) {
                // Backoff exponencial: 1s, 2s, 4s
                Thread.sleep(1000L * (long) Math.pow(2, attempts - 1));
            }
        }
    }
    return Optional.empty();
}

POR QUE SEMAFORO Y NO SOLO ASYNC:
- Keepa tiene limite de peticiones por segundo segun el plan
- Sin semaforo se podrian lanzar 50+ peticiones simultaneas desde el scheduler
- El Semaphore(3) garantiza maximo 3 peticiones concurrentes a Keepa

POR QUE CompletableFuture:
- El hilo principal HTTP no se bloquea esperando respuesta de Keepa
- La peticion de sincronizacion devuelve respuesta inmediata al cliente
- La actualizacion de precio ocurre en segundo plano

LOCALES DE AMAZON SOPORTADOS:
    ES (España)   -> amazon.es
    US (EEUU)     -> amazon.com
    DE (Alemania) -> amazon.de
    FR (Francia)  -> amazon.fr
    UK (Reino Unido) -> amazon.co.uk
    IT (Italia)   -> amazon.it
    CA (Canada)   -> amazon.ca
    JP (Japon)    -> amazon.co.jp

REFERENCIAS:
- Keepa API: https://keepa.com/#!discuss/t/keepa-api/99


================================================================================
12. PROGRAMACION ASINCRONA Y MULTIHILO
================================================================================

PriceWise usa programacion asincrona para no bloquear el servidor durante
las llamadas a la API de Keepa.

EXECUTOR PERSONALIZADO:

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("keepaExecutor")
    public Executor keepaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("keepa-");
        executor.initialize();
        return executor;
    }
}

Por que no el executor por defecto de Spring:
- El executor por defecto es compartido con otras tareas async
- Un executor dedicado para Keepa aísla su impacto en el resto de la app
- El prefijo "keepa-" en el nombre del hilo facilita el debug de logs

COMPLETABLEFUTURE:

// Lanzar peticion async
CompletableFuture<Optional<CompetitorPrice>> future =
    keepaService.getAmazonPrice(asin, productId);

// El hilo principal puede seguir trabajando...

// Esperar resultado (con timeout)
Optional<CompetitorPrice> result = future.get(30, TimeUnit.SECONDS);

// Combinar multiples futures en paralelo (PriceMonitorJob)
List<CompletableFuture<Void>> futures = products.stream()
    .map(p -> keepaService.getAmazonPrice(p.getSku(), p.getId())
        .thenAccept(price -> price.ifPresent(this::savePrice)))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

REFERENCIAS:
- CompletableFuture: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
- Spring Async: https://spring.io/guides/gs/async-method/


================================================================================
13. TAREAS PROGRAMADAS CON QUARTZ
================================================================================

Quartz Scheduler ejecuta PriceMonitorJob cada 6 horas automaticamente.

CONFIGURACION:

@Configuration
public class SchedulerConfig {

    @Bean
    public JobDetail priceMonitorJobDetail() {
        return JobBuilder.newJob(PriceMonitorJob.class)
            .withIdentity("priceMonitorJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger priceMonitorTrigger(JobDetail priceMonitorJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(priceMonitorJobDetail)
            .withIdentity("priceMonitorTrigger")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 */6 * * ?")  // Cada 6 horas
            )
            .build();
    }
}

FLUJO DEL JOB (PriceMonitorJob):

1. Obtener productos con monitoringEnabled = true y SKU que empieza por "B0" (ASINs)
2. Procesar en lotes de 50 con 1 segundo de pausa entre lotes
3. Para cada producto, llamar keepaService.getAmazonPrice(sku, productId)
4. Si hay precio nuevo, guardarlo en CompetitorPrice
5. Ejecutar priceAnalysisService.analyzeUserProducts(userId)
6. Registrar estadisticas de ejecucion en logs

POR QUE LOTES DE 50:
- Limita el numero de peticiones concurrentes a Keepa
- Evita agotar la quota de la API en una sola ejecucion
- El delay de 1 segundo entre lotes da tiempo a procesar respuestas

POR QUE QUARTZ Y NO @Scheduled:
- Quartz persiste el estado del job en BD (si el servidor cae, no pierde el trigger)
- Permite pausar/reanudar via la API de admin (/api/admin/scheduler/*)
- Mejor para produccion que @Scheduled (puramente en memoria)

REFERENCIAS:
- Quartz: http://www.quartz-scheduler.org/documentation/


================================================================================
14. CACHE Y RENDIMIENTO
================================================================================

CACHE EN MEMORIA:

Se usa Spring Cache con implementacion Simple (ConcurrentHashMap en memoria).
Solo se cachean queries costosas y relativamente estaticas.

QUERIES CACHEADAS:

@Cacheable(value = "categories", key = "#userId")
public List<String> getProductCategories(Long userId) {
    return productRepository.findDistinctCategoriesByUserId(userId);
}

@Cacheable(value = "brands", key = "#userId")
public List<String> getProductBrands(Long userId) {
    return productRepository.findDistinctBrandsByUserId(userId);
}

INVALIDACION DE CACHE:

@CacheEvict(value = {"categories", "brands"}, key = "#userId")
public ProductResponse createProduct(ProductRequest request, Long userId) { ... }

@CacheEvict(value = {"categories", "brands"}, key = "#userId")
public ProductResponse updateProduct(...) { ... }

POR QUE CACHEAR CATEGORIAS Y MARCAS:
- Se usan en filtros de busqueda, se piden frecuentemente
- Cambian raramente (solo al crear/modificar productos)
- Sin cache: query DISTINCT en tabla products en cada request

INDICES DE POSTGRESQL:
Los indices definidos en las entidades (ver seccion 6) son la primera
linea de defensa para el rendimiento. La cache es un complemento.

POOL DE CONEXIONES (HIKARICP):
- corePoolSize:  5 conexiones siempre abiertas
- maximumPoolSize: 20 conexiones maximas
- connectionTimeout: 30 segundos de espera para obtener conexion
- idleTimeout: 600 segundos antes de cerrar conexion inactiva

REFERENCIAS:
- Spring Cache: https://docs.spring.io/spring-framework/reference/integration/cache.html
- HikariCP: https://github.com/brettwooldridge/HikariCP


================================================================================
15. MANEJO DE EXCEPCIONES
================================================================================

EXCEPCIONES PERSONALIZADAS:

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " no encontrado con id: " + id);
    }
}

HANDLER GLOBAL:

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        return ApiResponse.error("Error de validacion", errors);
    }
}

POR QUE @RestControllerAdvice:
- Centraliza el manejo de errores en un solo lugar
- Evita try-catch en cada controlador
- Garantiza formato de error consistente en toda la API

REFERENCIAS:
- Spring Exception Handling: https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc


================================================================================
16. VALIDACIONES
================================================================================

VALIDACION DE ENTRADA (Jakarta Validation):

// En el DTO
public class ProductRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @Email(message = "Formato de email invalido")
    private String contactEmail;
}

// En el controlador
public ResponseEntity<?> create(@Valid @RequestBody ProductRequest request) {
    // @Valid dispara la validacion. Si falla, lanza MethodArgumentNotValidException
    // que GlobalExceptionHandler captura y formatea
}

VALIDACIONES DE NEGOCIO EN SERVICIO:
// Unicidad
if (productRepository.existsBySkuAndUserId(request.getSku(), userId)) {
    throw new BadRequestException("Ya existe un producto con el SKU: " + request.getSku());
}

// Propiedad
if (!product.getUser().getId().equals(userId)) {
    throw new BadRequestException("No tienes permiso para modificar este producto");
}

REFERENCIAS:
- Jakarta Validation: https://jakarta.ee/specifications/bean-validation/


================================================================================
17. CONFIGURACION Y PERFILES
================================================================================

PERFILES DE SPRING:

dev (local):
- show-sql: true  (muestra SQL generado en consola)
- ddl-auto: update  (crea/actualiza tablas automaticamente)
- CORS: allow-all: true  (acepta peticiones de cualquier origen)
- Logging: DEBUG en com.alvaro.pricewise

prod:
- show-sql: false
- ddl-auto: validate  (solo verifica, no modifica esquema)
- CORS: allow-all: false, dominios configurados explicitamente
- Logging: WARN

VARIABLES DE ENTORNO REQUERIDAS:

| Variable                  | Descripcion                          | Ejemplo                         |
|---------------------------|--------------------------------------|---------------------------------|
| JWT_SECRET                | Clave HMAC-SHA256 (min 32 chars)     | un-secreto-de-32-o-mas-chars    |
| DB_PASSWORD               | Contrasena PostgreSQL                | mi_password_segura              |
| SPRING_PROFILES_ACTIVE    | Perfil activo                        | dev o prod                      |
| KEEPA_API_KEY             | API key de keepa.com                 | xxxxxxxxxxxxxxxxxxx             |

CONFIGURACION BASICA (application.yml):

server:
  port: 9090

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pricewise
    username: postgres
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 horas

keepa:
  api-key: ${KEEPA_API_KEY}
  default-locale: ES
  price-history-days: 90

REFERENCIAS:
- Spring Profiles: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles


================================================================================
18. POOL DE CONEXIONES
================================================================================

HikariCP gestiona las conexiones a PostgreSQL.

SIN POOL: cada request abre una conexion (lento, costoso)
CON POOL: las conexiones se reutilizan (rapido, eficiente)

CONFIGURACION OPTIMA PARA PRICEWISE:

spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # Maximo 20 conexiones simultaneas
      minimum-idle: 5              # 5 siempre abiertas en reposo
      idle-timeout: 600000         # Cerrar inactivas tras 10 min
      connection-timeout: 30000    # Esperar 30s antes de error
      max-lifetime: 1800000        # Renovar conexion cada 30 min

FORMULA PARA maximum-pool-size:
    Para I/O bound (acceso a BD): numCores * 2 a numCores * 4
    Un servidor con 4 cores: 8 a 16 conexiones es razonable.
    20 da margen para picos.

REFERENCIAS:
- HikariCP: https://github.com/brettwooldridge/HikariCP


================================================================================
19. TESTING
================================================================================

TESTS UNITARIOS (Service Layer):

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_withValidRequest_shouldSaveAndReturnResponse() {
        // Given
        ProductRequest request = ProductRequest.builder()
            .name("Test Product")
            .currentPrice(new BigDecimal("29.99"))
            .build();

        User user = User.builder().id(1L).username("testuser").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ProductResponse result = productService.createProduct(request, 1L);

        // Then
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }
}

TESTS DE INTEGRACION (Controller Layer):

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createProduct_withValidToken_shouldReturn201() throws Exception {
        String token = generateTestToken();

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Test", "currentPrice": 9.99 }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }
}

TESTS DE REPOSITORIO (con H2 en memoria):

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByUserIdAndActiveTrue_shouldReturnOnlyActiveProducts() {
        // Setup: insertar productos activos e inactivos
        // Then: verificar que solo se devuelven los activos
    }
}

DOCUMENTACION:
- JUnit 5: https://junit.org/junit5/
- Mockito: https://site.mockito.org/
- Spring Test: https://docs.spring.io/spring-framework/reference/testing.html


================================================================================
20. REFERENCIAS Y DOCUMENTACION OFICIAL
================================================================================

SPRING:
-------
- Spring Boot:     https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Cache:    https://docs.spring.io/spring-framework/reference/integration/cache.html
- Spring Async:    https://spring.io/guides/gs/async-method/

JAVA:
-----
- Java 17 LTS:         https://openjdk.org/projects/jdk/17/
- CompletableFuture:   https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
- Jakarta Validation:  https://jakarta.ee/specifications/bean-validation/

BASE DE DATOS:
--------------
- PostgreSQL:    https://www.postgresql.org/docs/
- Hibernate:     https://hibernate.org/orm/documentation/
- HikariCP:      https://github.com/brettwooldridge/HikariCP

LIBRERIAS:
----------
- Lombok:       https://projectlombok.org/
- jjwt:         https://github.com/jwtk/jjwt
- Quartz:       http://www.quartz-scheduler.org/documentation/
- SpringDoc:    https://springdoc.org/
- Jsoup:        https://jsoup.org/

APIS EXTERNAS:
--------------
- Keepa API:    https://keepa.com/#!discuss/t/keepa-api/99
- JWT:          https://jwt.io/introduction

ARQUITECTURA:
-------------
- Layered Architecture: https://www.oreilly.com/library/view/software-architecture-patterns/9781491971437/
- REST Best Practices:  https://restfulapi.net/

================================================================================
                              FIN DEL DOCUMENTO
================================================================================
