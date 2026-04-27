================================================================================
                    DOCUMENTACIÓN TÉCNICA - PriceWise Backend
                    Guía Completa de Arquitectura y Justificaciones
================================================================================

ÍNDICE
------
- [0. Glosario de términos técnicos](#seccion-0)
- [1. Introducción y visión general](#seccion-1)
- [2. Estructura del proyecto](#seccion-2)
- [3. Patrón de arquitectura — capas](#seccion-3)
- [4. Spring Boot y sus anotaciones](#seccion-4)
- [5. Gestión de dependencias (inyección)](#seccion-5)
- [6. Entidades JPA y persistencia](#seccion-6)
- [7. Repositorios y Spring Data JPA](#seccion-7)
- [8. Servicios y lógica de negocio](#seccion-8)
- [9. Controladores REST](#seccion-9)
- [10. Seguridad y autenticación JWT](#seccion-10)
- [11. Programación asíncrona y multihilo](#seccion-11)
- [12. Manejo de excepciones](#seccion-12)
- [13. Validaciones](#seccion-13)
- [14. Caché y rendimiento](#seccion-14)
- [15. Tareas programadas con Quartz](#seccion-15)
- [16. Configuración y perfiles](#seccion-16)
- [17. Pool de conexiones](#seccion-17)
- [18. Testing](#seccion-18)
- [19. Referencias y documentación oficial](#seccion-19)
- [20. Documentación relacionada](#seccion-20)


<a id="seccion-0"></a>
================================================================================
0. GLOSARIO DE TÉRMINOS TÉCNICOS
================================================================================

Este glosario define los términos técnicos usados en este documento.
Consúltalo cuando encuentres un término que no conozcas.

TÉRMINOS DE PROGRAMACIÓN GENERAL
--------------------------------

ABSTRACCION
    Ocultar complejidad mostrando solo lo esencial.
    Ejemplo: Un coche tiene "acelerar" sin que sepas como funciona el motor.

ACOPLAMIENTO
    Grado de dependencia entre componentes.
    - Alto acoplamiento (malo): cambiar uno afecta a muchos otros
    - Bajo acoplamiento (bueno): componentes independientes

API (Application Programming Interface)
    Conjunto de reglas que permiten que programas se comuniquen.
    En este proyecto, nuestra API REST permite que apps frontend 
    se conecten al backend vía HTTP.

BOILERPLATE
    Código repetitivo que hay que escribir aunque no aporte valor.
    Ejemplo: getters, setters, constructores, equals, hashCode.
    Lombok elimina boilerplate generandolo automáticamente.

BUG
    Error en el código que causa comportamiento incorrecto.

CALLBACK
    Función que se pasa como argumento a otra función para 
    ser ejecutada después de que algo ocurra.

CLASSPATH
    Lista de ubicaciones donde Java busca clases y recursos.

COMPILAR
    Convertir código fuente (.java) en bytecode (.class) 
    que la JVM puede ejecutar.

CONSOLA / TERMINAL
    Interfaz de linea de comandos donde ejecutas programas
    y ves mensajes de log.

DEBUG / DEPURAR
    Proceso de encontrar y corregir errores en el código.

DEPLOY / DESPLEGAR
    Poner una aplicación en un servidor para que los usuarios
    puedan accederla.

ENDPOINT
    URL específica de una API que realiza una acción concreta.
    Ejemplo: POST /api/auth/login es el endpoint de login.

FRAMEWORK
    Estructura de software que proporciona funcionalidad base
    sobre la que construyes tu aplicación.
    Spring Boot es un framework.

INMUTABLE
    Objeto que no puede ser modificado después de crearse.
    Son seguros en entornos multihilo.
    Ejemplo: String en Java es inmutable.

INSTANCIA
    Objeto creado a partir de una clase.
    "new User()" crea una instancia de la clase User.

LAZY LOADING (Carga perezosa)
    Retrasar la carga de datos hasta que realmente se necesiten.
    Optimiza rendimiento evitando cargar datos innecesarios.

LIBRERIA / BIBLIOTECA
    Código ya escrito que puedes usar en tu proyecto.
    Ejemplo: JJWT es una libreria para manejar tokens JWT.

LOG / LOGGING
    Registro de eventos que ocurren en la aplicación.
    Util para debugging y monitoreo.
    Niveles: DEBUG < INFO < WARN < ERROR

PARAMETRO
    Valor que pasas a un método o función.

PERSISTENCIA
    Guardar datos de forma permanente (en BD, archivos, etc.)
    para que sobrevivan al reinicio de la aplicación.

REFACTORIZAR
    Mejorar la estructura del código sin cambiar su comportamiento.

RUNTIME
    Momento en que el programa esta ejecutándose
    (opuesto a compile-time).

SCOPE (Ambito)
    Contexto en el que una variable o método es accesible.

SINGLETON
    Patrón de diseño donde solo existe una instancia de una clase.
    Spring gestiona beans como singletons por defecto.

STACK TRACE
    Lista de llamadas a métodos que llevaron a un error.
    Util para encontrar donde ocurrio el problema.


TÉRMINOS DE SPRING Y JAVA EMPRESARIAL
-------------------------------------

ANOTACION (@)
    Metadatos que añades al código para darle comportamiento especial.
    Ejemplo: @Service le dice a Spring que gestione esa clase.

BEAN
    Objeto gestionado por el contenedor de Spring.
    Spring crea, configura e inyecta beans automáticamente.
    Todo @Component, @Service, @Repository, @Controller es un bean.

CONTENEDOR (Spring Container)
    El "motor" de Spring que gestiona los beans.
    Crea instancias, inyecta dependencias, gestiona ciclo de vida.

CONTEXTO (ApplicationContext)
    El contenedor de Spring donde viven todos los beans.
    Puedes obtener beans del contexto.

DTO (Data Transfer Object)
    Objeto simple para transferir datos entre capas.
    No tiene lógica, solo campos y getters/setters.
    Separa la representación externa de la interna.

ENTITY (Entidad)
    Clase Java que representa una tabla de base de datos.
    Marcada con @Entity. Cada instancia es una fila.

INYECCIÓN DE DEPENDENCIAS (DI)
    Patrón donde las dependencias se "inyectan" desde fuera
    en lugar de crearlas dentro de la clase.
    Spring lo hace automáticamente vía constructores/@Autowired.

IoC (Inversión of Control)
    Principio donde el framework controla el flujo del programa
    en lugar del programador. Spring decide cuando crear beans.

JPA (Java Persistence API)
    Especificación estándar de Java para ORM.
    Define como mapear objetos Java a tablas de BD.
    Hibernate es la implementación más usada.

ORM (Object-Relational Mapping)
    Técnica que mapea objetos de programación a tablas de BD.
    Escribes clases Java, el ORM genera SQL automáticamente.

POJO (Plain Old Java Object)
    Clase Java simple sin dependencias de frameworks.
    Solo tiene campos, constructor, getters y setters.

REPOSITORY (Repositorio)
    Componente que abstrae el acceso a datos.
    Intermediario entre la lógica de negocio y la BD.

SERVICE (Servicio)
    Componente que contiene la lógica de negocio.
    Coordina operaciones entre repositorios.

TRANSACCION
    Conjunto de operaciones que deben ejecutarse como unidad.
    Si una falla, todas se revierten (rollback).
    Garantiza integridad de datos.


TÉRMINOS DE BASE DE DATOS
-------------------------

ACID
    Propiedades de transacciones fiables:
    - Atomicity: todo o nada
    - Consistency: datos siempre validos
    - Isolation: transacciones no interfieren entre si
    - Durability: cambios persistidos sobreviven a caidas

COMMIT
    Confirmar una transacción, haciendo sus cambios permanentes.

CONSTRAINT (Restricción)
    Regla que limita los datos que pueden guardarse.
    Ejemplos: NOT NULL, UNIQUE, FOREIGN KEY.

DDL (Data Definition Language)
    Comandos SQL para definir estructura: CREATE, ALTER, DROP.

DML (Data Manipulation Language)
    Comandos SQL para manipular datos: SELECT, INSERT, UPDATE, DELETE.

FK (Foreign Key / Clave Foranea)
    Campo que referencia la clave primaria de otra tabla.
    Crea relaciones entre tablas.

ÍNDICE (Index)
    Estructura que acelera busquedas en una columna.
    Como el indice de un libro.

JOIN
    Operación que combina filas de varias tablas
    basándose en una relación entre ellas.

JPQL (Java Persistence Query Language)
    Lenguaje de consultas similar a SQL pero para entidades JPA.
    Usa nombres de clases y campos en lugar de tablas y columnas.

MIGRACION
    Cambio controlado en el esquema de la BD.
    Historico de cambios versionados.

PK (Primary Key / Clave Primaria)
    Campo único que identifica cada fila de una tabla.

POOL (de conexiones)
    Conjunto de conexiones a BD listas para usar.
    Evita crear/destruir conexiones constantemente (costoso).
    HikariCP es el pool que usamos.

QUERY (Consulta)
    Petición a la BD para obtener o modificar datos.

ROLLBACK
    Revertir una transacción, deshaciendo todos sus cambios.

SCHEMA (Esquema)
    Estructura de la BD: tablas, columnas, tipos, relaciones.


TÉRMINOS DE SEGURIDAD
---------------------

AUTENTICACIÓN
    Verificar la identidad del usuario ("quien eres").
    Ejemplo: login con email/password.

AUTORIZACIÓN
    Verificar permisos ("que puedes hacer").
    Ejemplo: solo ADMIN puede borrar usuarios.

BCRYPT
    Algoritmo de hash diseñado para contraseñas.
    Lento a propósito para dificultar ataques.
    Incluye salt automáticamente.

CLAIM
    Pieza de información contenida en un JWT.
    Ejemplo: "sub" (subject/usuario), "exp" (expiración).

CORS (Cross-Origin Resource Sharing)
    Mecanismo que permite peticiones desde otros dominios.
    Necesario cuando el frontend esta en otro servidor.

CSRF (Cross-Site Request Forgery)
    Ataque donde un sitio malicioso engaña al navegador
    para hacer peticiones no deseadas.
    JWT stateless no es vulnerable, por eso lo desactivamos.

ENCRIPTACION
    Transformar datos para que solo quien tiene la clave
    pueda leerlos. Proceso reversible.

HASH
    Transformación irreversible de datos.
    Mismo input = mismo output, pero no puedes revertirlo.
    Usado para contraseñas.

JWT (JSON Web Token)
    Token firmado que contiene información del usuario.
    Stateless: el servidor no guarda sesiones.

SALT
    Valor aleatorio añadido antes de hashear.
    Evita que contraseñas iguales tengan el mismo hash.

SESION
    Estado guardado en el servidor asociado a un usuario.
    JWT es stateless (sin sesion en servidor).

TOKEN
    Cadena de texto que representa una autenticación.
    El cliente lo envía en cada petición.


TÉRMINOS DE CONCURRENCIA Y MULTIHILO
------------------------------------

ASINCRONO (Async)
    Operación que no bloquea el hilo que la inicia.
    El hilo puede hacer otras cosas mientras espera.

BLOQUEO (Lock)
    Mecanismo para que solo un hilo acceda a un recurso.
    Previene condiciones de carrera.

BLOQUEAR (Block)
    Cuando un hilo se detiene esperando algo
    (respuesta de red, BD, otro hilo).

COMPLETABLEFUTURE
    Clase Java que representa el resultado futuro de una operación.
    Permite encadenar acciones cuando el resultado este listo.

CONDICION DE CARRERA (Race Condition)
    Bug que ocurre cuando dos hilos modifican el mismo dato
    simultaneamente con resultado impredecible.

CONCURRENCIA
    Múltiples tareas ejecutándose en periodos superpuestos.
    No necesariamente al mismo tiempo.

DEADLOCK
    Situación donde dos hilos se esperan mutuamente
    y ninguno puede continuar.

EXECUTOR
    Componente que gestiona la ejecución de tareas
    en un pool de hilos.

HILO (Thread)
    Unidad de ejecución independiente dentro de un proceso.
    Múltiples hilos pueden ejecutarse en paralelo.

PARALELISMO
    Múltiples tareas ejecutándose literalmente al mismo tiempo
    en diferentes nucleos del procesador.

POOL (de hilos)
    Conjunto de hilos reutilizables para ejecutar tareas.
    Evita crear/destruir hilos constantemente (costoso).
    ThreadPoolTaskExecutor es el pool que usamos.

SEMAFORO (Semaphore)
    Mecanismo que limita el numero de accesos concurrentes.
    Ejemplo: max 3 llamadas simultaneas a la API.

SINCRONIZADO (Synchronized)
    Mecanismo Java para que solo un hilo ejecute
    una sección de código a la vez.

THREAD-SAFE
    Código que funciona correctamente con múltiples hilos.
    No tiene condiciones de carrera.

VOLATILE
    Modificador Java que garantiza visibilidad inmediata
    de cambios en una variable entre hilos.


TÉRMINOS DE TESTING
-------------------

ASSERT / ASSERTION
    Verificación de que un valor es el esperado.
    Si falla, el test falla.
    Ejemplo: assertEquals(expected, actual)

COVERAGE (Cobertura)
    Porcentaje de código ejecutado por los tests.
    80%+ es un buen objetivo.

FIXTURE
    Datos de prueba preparados antes de ejecutar tests.

INTEGRACION (Test de)
    Test que prueba múltiples componentes juntos.
    Ejemplo: servicio con BD real.

MOCK
    Objeto falso que simula el comportamiento de uno real.
    Usados para aislar el código que se esta testeando.
    Mockito es la libreria que usamos.

STUB
    Mock simple que devuelve valores predefinidos.

TEST UNITARIO
    Test que prueba una unidad de código aislada
    (normalmente un método o clase).

TDD (Test-Driven Development)
    Metodologia donde escribes tests antes del código.


TÉRMINOS DE API REST
--------------------

BODY
    Contenido de una petición/respuesta HTTP.
    Normalmente JSON en APIs REST.

CRUD
    Create, Read, Update, Delete - operaciones básicas de datos.
    Mapeadas a POST, GET, PUT/PATCH, DELETE.

HEADER
    Metadatos de una petición/respuesta HTTP.
    Ejemplo: Authorization, Content-Type.

HTTP
    Protocolo de comunicación web.
    Define como cliente y servidor intercambian mensajes.

JSON (JavaScript Object Notation)
    Formato de texto para intercambiar datos.
    Ejemplo: {"nombre": "Juan", "edad": 30}

PAGINACION
    Dividir resultados grandes en páginas más pequeñas.
    Evita cargar miles de registros de golpe.

PATH VARIABLE
    Valor dinámico en la URL.
    GET /api/products/{id} -> id es path variable.

QUERY PARAMETER
    Parámetro en la URL después de "?".
    GET /api/products?page=1&size=20

REQUEST (Petición)
    Mensaje del cliente al servidor pidiendo algo.

RESPONSE (Respuesta)
    Mensaje del servidor al cliente con el resultado.

REST (Representational State Transfer)
    Estilo de arquitectura para APIs web.
    Recursos (URLs), verbos HTTP (GET/POST/PUT/DELETE), stateless.

STATELESS
    El servidor no guarda estado entre peticiones.
    Cada petición lleva toda la información necesaria.
    JWT permite autenticación stateless.

STATUS CODE
    Numero que indica el resultado de una petición HTTP.
    200=OK, 404=No encontrado, 500=Error servidor.


TÉRMINOS DE INFRAESTRUCTURA
---------------------------

CONTENEDOR (Docker)
    Entorno aislado y ligero para ejecutar aplicaciones.
    Incluye todo lo necesario: código, runtime, dependencias.

VARIABLE DE ENTORNO
    Valor de configuración definido fuera del código.
    El sistema operativo o contenedor lo proporciona.
    Ejemplo: DB_PASSWORD, JWT_SECRET.

YAML / YML
    Formato de texto para configuración, legible para humanos.
    Alternativa a JSON, usa indentación en lugar de llaves.
<a id="seccion-1"></a>
================================================================================
1. INTRODUCCIÓN Y VISIÓN GENERAL
================================================================================

PriceWise es una aplicación de monitorización de precios que permite a usuarios
rastrear sus productos y compararlos con la competencia (principalmente Amazon).

PROBLEMA QUE RESUELVE:
- Los comerciantes necesitan saber si sus precios son competitivos
- Amazon cambia precios constantemente
- Monitorear manualmente es tedioso y propenso a errores

SOLUCION TÉCNICA:
- API REST para gestión de productos
- Integración con Keepa API para precios de Amazon
- Sistema de alertas y recomendaciones automáticas
- Procesamiento asíncrono para no bloquear la aplicación

TECNOLOGIAS ELEGIDAS Y JUSTIFICACION:

| Tecnologia      | Para que                  | Por que esta y no otra                    |
|-----------------|---------------------------|-------------------------------------------|
| Spring Boot     | Framework base            | Productividad, ecosistema, documentación  |
| PostgreSQL      | Base de datos             | Robustez, ACID, escalabilidad             |
| JWT             | Autenticación             | Stateless, ideal para APIs                |
| Quartz          | Tareas programadas        | Mas flexible que @Scheduled               |
| HikariCP        | Pool de conexiones        | El más rápido, default de Spring Boot     |
| Lombok          | Reducir boilerplate       | Código limpio sin getters/setters         |

REFERENCIAS:
- Spring Boot: https://spring.io/projects/spring-boot
- Documentación oficial: https://docs.spring.io/spring-boot/docs/current/reference/html/


<a id="seccion-2"></a>
================================================================================
2. ESTRUCTURA DEL PROYECTO
================================================================================

ESTRUCTURA DE CARPETAS:

pricewise-backend/
├── src/main/java/com/alvaro/pricewise/
│   ├── PriceWiseApplication.java      # Punto de entrada
│   ├── config/                         # Configuraciones de Spring
│   ├── controller/                     # Controladores REST (entrada HTTP)
│   ├── dto/                            # Data Transfer Objects
│   ├── entity/                         # Entidades JPA (tablas)
│   ├── exception/                      # Excepciones personalizadas
│   ├── repository/                     # Acceso a datos
│   ├── scheduler/                      # Tareas programadas
│   ├── security/                       # JWT y filtros de seguridad
│   └── service/                        # Logica de negocio
├── src/main/resources/
│   └── application.yml                 # Configuración
├── src/test/java/                      # Tests unitarios e integración
└── pom.xml                             # Dependencias Maven

POR QUE ESTA ESTRUCTURA:

Esta estructura sigue el patrón "Package by Layer" (paquetes por capa):
- Separa responsabilidades claramente
- Facilita encontrar código relacionado
- Es la convención de Spring Boot

ALTERNATIVA: Package by Feature (paquetes por funcionalidad)
- Tendriamos: user/, product/, analytics/
- Cada uno con su controller, service, repository
- Mejor para proyectos muy grandes

DOCUMENTACIÓN:
- Guía de estructura: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.structuring-your-code


<a id="seccion-3"></a>
================================================================================
3. PATRÓN DE ARQUITECTURA - CAPAS
================================================================================

Usamos arquitectura de 3 capas (Three-Tier Architecture):

┌─────────────────────────────────────────────────────────────────┐
│                         CAPA DE PRESENTACION                     │
│                     (Controllers + DTOs)                         │
│                                                                  │
│  • Recibe peticiones HTTP                                        │
│  • Valida entrada con @Valid                                     │
│  • Convierte DTOs a entidades y viceversa                       │
│  • Devuelve respuestas JSON                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                         CAPA DE NEGOCIO                          │
│                          (Services)                              │
│                                                                  │
│  • Contiene reglas de negocio                                   │
│  • Coordina operaciones entre repositorios                       │
│  • Maneja transacciones con @Transactional                      │
│  • NO conoce HTTP ni JSON                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                         CAPA DE DATOS                            │
│                   (Repositories + Entities)                      │
│                                                                  │
│  • Mapea objetos Java a tablas SQL                              │
│  • Ejecuta queries (JPQL o nativos)                             │
│  • Abstrae el acceso a base de datos                            │
└─────────────────────────────────────────────────────────────────┘

POR QUE SEPARAR EN CAPAS:

1. MANTENIBILIDAD: Cambios en una capa no afectan a otras
   Ejemplo: Cambiar de PostgreSQL a MySQL solo afecta a la capa de datos

2. TESTABILIDAD: Podemos hacer mock de cada capa
   Ejemplo: Testar ProductService sin base de datos real

3. REUSABILIDAD: Servicios pueden ser usados por múltiples controllers
   Ejemplo: ProductService usado por ProductController y CompetitorController

4. SEPARACION DE RESPONSABILIDADES (SoC):
   - Controller: "Como llegue la petición"
   - Service: "Que hacer con los datos"
   - Repository: "Como guardar/leer de BD"

REFERENCIAS:
- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- Spring MVC: https://docs.spring.io/spring-framework/reference/web/webmvc.html


<a id="seccion-4"></a>
================================================================================
4. SPRING BOOT Y SUS ANOTACIONES
================================================================================

Spring Boot es un framework que simplifica la creación de aplicaciones Spring.

ANOTACION @SpringBootApplication
--------------------------------
Ubicación: PriceWiseApplication.java

@SpringBootApplication
public class PriceWiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriceWiseApplication.class, args);
    }
}

Esta anotación es equivalente a 3 anotaciones juntas:

1. @Configuration: Indica que la clase puede definir beans
2. @EnableAutoConfiguration: Configura automáticamente según dependencias
3. @ComponentScan: Escanea este paquete y subpaquetes buscando componentes

POR QUE @SpringBootApplication:
- Una sola anotación en lugar de tres
- Convención sobre configuración
- Menos código, menos errores

ANOTACIONES DE COMPONENTES
--------------------------

@Component    - Componente genérico gestionado por Spring
@Service      - Servicio de lógica de negocio (semanticamente igual que @Component)
@Repository   - Acceso a datos (añade traducción de excepciones de BD)
@Controller   - Controlador MVC que devuelve vistas
@RestController - Controlador REST que devuelve JSON (@Controller + @ResponseBody)
@Configuration  - Clase de configuración que define beans

EJEMPLO PRACTICO:

@Service                          // Spring lo detecta y crea una instancia
@RequiredArgsConstructor          // Lombok: genera constructor con campos final
public class ProductService {
    
    private final ProductRepository productRepository;  // Inyectado automáticamente
    
    @Transactional                // Spring gestiona la transacción
    public Product save(Product p) {
        return productRepository.save(p);
    }
}

CICLO DE VIDA DE LOS BEANS:

1. Spring escanea el classpath buscando @Component y similares
2. Crea instancias de cada componente (beans)
3. Inyecta dependencias en constructores/@Autowired
4. Ejecuta métodos @PostConstruct
5. Beans listos para usar
6. Al cerrar: ejecuta métodos @PreDestroy

DOCUMENTACIÓN OFICIAL:
- Spring Core: https://docs.spring.io/spring-framework/reference/core.html
- Anotaciones: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html


<a id="seccion-5"></a>
================================================================================
5. GESTIÓN DE DEPENDENCIAS (INYECCIÓN)
================================================================================

INYECCIÓN DE DEPENDENCIAS (DI) - Concepto fundamental de Spring

QUE ES:
En lugar de crear objetos manualmente, Spring los "inyecta" donde se necesitan.

SIN INYECCIÓN (MAL):
public class ProductService {
    private ProductRepository repo = new ProductRepository(); // Mal: acoplamiento
}

CON INYECCIÓN (BIEN):
public class ProductService {
    private final ProductRepository repo; // Spring lo inyecta
    
    public ProductService(ProductRepository repo) {  // Constructor injection
        this.repo = repo;
    }
}

FORMAS DE INYECCIÓN:

1. POR CONSTRUCTOR (RECOMENDADA):
   @Service
   @RequiredArgsConstructor  // Lombok genera el constructor
   public class ProductService {
       private final ProductRepository productRepository;
   }

   Por que es la mejor:
   - Los campos pueden ser final (inmutables)
   - Las dependencias son claras y obligatorias
   - Facilita el testing (puedes pasar mocks en el constructor)
   - No permite instancias incompletas

2. POR SETTER (NO RECOMENDADA):
   @Autowired
   public void setRepository(ProductRepository repo) {
       this.repo = repo;
   }

   Problemas:
   - Permite objetos sin todas las dependencias
   - Campos no pueden ser final

3. POR CAMPO (EVITAR):
   @Autowired
   private ProductRepository repo;

   Problemas:
   - No se puede hacer final
   - Dificil de testear sin reflection
   - Dependencias ocultas

@REQUIREDARGSCONSTRUCTOR DE LOMBOK:
Genera automáticamente un constructor con todos los campos "final":

@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;    // En constructor
    private final UserRepository userRepository;          // En constructor
    private String optionalField;                         // NO en constructor
}

// Lombok genera:
public ProductService(ProductRepository productRepository, UserRepository userRepository) {
    this.productRepository = productRepository;
    this.userRepository = userRepository;
}

DOCUMENTACIÓN:
- DI en Spring: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
- Lombok: https://projectlombok.org/features/constructor


<a id="seccion-6"></a>
================================================================================
6. ENTIDADES JPA Y PERSISTENCIA
================================================================================

JPA (Java Persistence API) es el estándar de Java para ORM (Object-Relational Mapping).
Hibernate es la implementación más usada de JPA.

ANOTACIONES JPA PRINCIPALES
---------------------------

@Entity
-------
Marca una clase como entidad persistible (tabla en BD).

@Entity
@Table(name = "products")  // Nombre de tabla (opcional si coincide con clase)
public class Product {
    ...
}

Por que @Entity:
- JPA necesita saber que clases mapear a tablas
- Hibernate genera el DDL automáticamente

@Id y @GeneratedValue
---------------------
@Id                                           // Este campo es la clave primaria
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
private Long id;

Estrategias de generación:
- IDENTITY: Auto-increment de la BD (el más común)
- SEQUENCE: Secuencias (mejor para batch inserts)
- TABLE: Tabla auxiliar (evitar, lento)
- AUTO: Hibernate decide

@Column
-------
Configura propiedades de la columna.

@Column(name = "current_price", nullable = false, precision = 10, scale = 2)
private BigDecimal currentPrice;

@Column(unique = true)       // Restricción UNIQUE
private String email;

@Column(length = 1000)       // VARCHAR(1000)
private String description;

RELACIONES ENTRE ENTIDADES
--------------------------

@ManyToOne - Muchos a uno (lo más común)
-----------------------------------------
Muchos productos pertenecen a un usuario.

@Entity
public class Product {
    @ManyToOne(fetch = FetchType.LAZY)  // No cargar usuario automáticamente
    @JoinColumn(name = "user_id", nullable = false)  // FK en esta tabla
    private User user;
}

Por que LAZY:
- Evita cargar datos innecesarios
- Mejora rendimiento
- El usuario se carga solo cuando se accede

@OneToMany - Uno a muchos
-------------------------
Un usuario tiene muchos productos.

@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();
}

mappedBy: indica que la FK esta en la otra tabla (Product.user)
cascade: propaga operaciones (si borras usuario, borra productos)

@ManyToMany - Muchos a muchos
-----------------------------
Requiere tabla intermedia. No lo usamos en PriceWise pero:

@ManyToMany
@JoinTable(
    name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags;

INDICES
-------
Mejoran velocidad de consultas.

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku", columnList = "sku"),
    @Index(name = "idx_product_user", columnList = "user_id"),
    @Index(name = "idx_product_category", columnList = "category")
})
public class Product { ... }

Por que estos indices:
- sku: Buscamos productos por SKU frecuentemente
- user_id: Filtramos por usuario en casi todas las queries
- category: Filtramos por categoría

AUDITORIA AUTOMATICA
--------------------
@EntityListeners(AuditingEntityListener.class)
public class Product {
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}

Requiere @EnableJpaAuditing en una clase @Configuration.

DOCUMENTACIÓN:
- JPA Specification: https://jakarta.ee/specifications/persistence/
- Hibernate: https://hibernate.org/orm/documentation/
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/reference/


<a id="seccion-7"></a>
================================================================================
7. REPOSITORIOS Y SPRING DATA JPA
================================================================================

Spring Data JPA genera implementaciones de repositorios automáticamente.

INTERFAZ BASE
-------------

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Spring genera automáticamente: save, findById, findAll, delete, etc.
}

JpaRepository hereda de:
- CrudRepository: operaciones CRUD básicas
- PagingAndSortingRepository: paginación y ordenación
- JpaRepository: flush, batch operations

QUERY METHODS
-------------
Spring genera queries a partir del nombre del método:

// SELECT * FROM products WHERE user_id = ?
List<Product> findByUserId(Long userId);

// SELECT * FROM products WHERE name LIKE ?
List<Product> findByNameContaining(String name);

// SELECT * FROM products WHERE active = true AND monitoring_enabled = true
List<Product> findByActiveTrueAndMonitoringEnabledTrue();

// SELECT * FROM products WHERE user_id = ? ORDER BY created_at DESC LIMIT 1
Optional<Product> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

PALABRAS CLAVE SOPORTADAS:
- findBy, readBy, getBy, queryBy, countBy, existsBy, deleteBy
- And, Or
- Is, Equals
- Between, LessThan, GreaterThan
- Like, Containing, StartingWith, EndingWith
- True, False
- OrderBy, Asc, Desc
- First, Top

@QUERY - JPQL PERSONALIZADO
---------------------------
Cuando la query method se vuelve muy larga:

@Query("SELECT p FROM Product p WHERE p.user.id = :userId " +
       "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
       "AND (:category IS NULL OR p.category = :category)")
Page<Product> searchProducts(@Param("userId") Long userId,
                             @Param("name") String name,
                             @Param("category") String category,
                             Pageable pageable);

Por que JPQL y no SQL nativo:
- Portable entre bases de datos
- Usa nombres de clases/campos Java
- Validado en tiempo de compilación

QUERY NATIVO
------------
Para queries muy complejas o específicas de PostgreSQL:

@Query(value = "SELECT * FROM products WHERE similarity(name, :term) > 0.3",
       nativeQuery = true)
List<Product> findBySimilarity(@Param("term") String term);

PAGINACION
----------
Spring soporta paginación transparente:

// En Repository
Page<Product> findByUserId(Long userId, Pageable pageable);

// En Service
Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
Page<Product> page = productRepository.findByUserId(userId, pageable);

// Page contiene:
page.getContent();       // Lista de productos
page.getTotalElements(); // Total en BD
page.getTotalPages();    // Total de páginas
page.getNumber();        // Pagina actual
page.hasNext();          // Hay más páginas?

DOCUMENTACIÓN:
- Spring Data JPA Reference: https://docs.spring.io/spring-data/jpa/reference/
- Query Methods: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html


<a id="seccion-8"></a>
================================================================================
8. SERVICIOS Y LOGICA DE NEGOCIO
================================================================================

Los servicios contienen la lógica de negocio de la aplicación.

ESTRUCTURA DE UN SERVICIO
-------------------------

@Slf4j                          // Logger de Lombok
@Service                        // Componente de Spring
@RequiredArgsConstructor        // Constructor con dependencias
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    
    @Transactional              // Metodo transaccional
    public ProductResponse createProduct(Long userId, CreateProductRequest request) {
        // 1. Validaciones de negocio
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // 2. Crear entidad
        Product product = Product.builder()
            .name(request.getName())
            .user(user)
            .build();
        
        // 3. Guardar
        product = productRepository.save(product);
        
        // 4. Operaciones adicionales
        createPriceHistory(product);
        
        // 5. Convertir a DTO y devolver
        return ProductResponse.fromEntity(product);
    }
}

@TRANSACTIONAL - GESTIÓN DE TRANSACCIONES
-----------------------------------------
Spring gestiona transacciones automáticamente.

@Transactional
public void transferMoney(Long from, Long to, BigDecimal amount) {
    accountRepository.withdraw(from, amount);    // Si falla aqui...
    accountRepository.deposit(to, amount);       // ...esto no se ejecuta
}

Comportamiento:
1. Spring abre transacción antes del método
2. Si el método termina bien: COMMIT
3. Si hay excepción RuntimeException: ROLLBACK
4. Si hay excepción checked: COMMIT (configurable)

Opciones importantes:

@Transactional(readOnly = true)  // Optimiza para solo lectura
public List<Product> getProducts() { ... }

@Transactional(propagation = Propagation.REQUIRES_NEW)  // Nueva transacción
public void logAction() { ... }

@Transactional(isolation = Isolation.SERIALIZABLE)  // Nivel de aislamiento
public void criticalOperation() { ... }

PROPAGATION (Propagación):
- REQUIRED (default): Usa transacción existente o crea nueva
- REQUIRES_NEW: Siempre crea nueva transacción
- SUPPORTS: Usa existente o ejecuta sin transacción
- MANDATORY: Debe existir transacción, si no, excepción

POR QUE @TRANSACTIONAL EN SERVICIOS:
- Un servicio puede llamar a múltiples repositorios
- Todas las operaciones deben ser atómicas
- Si algo falla, todo se revierte

PATRÓN DTO (Data Transfer Object)
---------------------------------
Separamos entidades JPA de objetos de respuesta API.

Por que DTOs:
1. No exponer estructura interna de BD
2. Ocultar campos sensibles (password)
3. Combinar datos de varias entidades
4. Evitar problemas de serialización con LAZY loading
5. Versionado de API independiente de entidades

Ejemplo:

// Entidad - refleja la tabla
@Entity
public class User {
    private Long id;
    private String email;
    private String password;  // No queremos exponerlo
    private LocalDateTime createdAt;
}

// DTO - lo que devuelve la API
public class UserProfileResponse {
    private Long id;
    private String email;
    // Sin password!
    private long totalProducts;  // Dato calculado
}

DOCUMENTACIÓN:
- Transacciones: https://docs.spring.io/spring-framework/reference/data-access/transaction.html


<a id="seccion-9"></a>
================================================================================
9. CONTROLADORES REST
================================================================================

Los controladores reciben peticiones HTTP y devuelven respuestas.

ANATOMIA DE UN CONTROLADOR
--------------------------

@RestController                           // Controlador REST
@RequestMapping("/api/products")          // Prefijo de rutas
@RequiredArgsConstructor
@Tag(name = "Productos", description = "CRUD de productos")  // OpenAPI
public class ProductController {

    private final ProductService productService;

    @PostMapping                          // POST /api/products
    @Operation(summary = "Crear producto")  // Documentación OpenAPI
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,  // Usuario actual
            @Valid @RequestBody CreateProductRequest request        // Body JSON validado
    ) {
        ProductResponse response = productService.createProduct(
            userPrincipal.getId(), 
            request
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)     // 201 Created
            .body(ApiResponse.success(response, "Producto creado"));
    }

    @GetMapping("/{id}")                   // GET /api/products/123
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id          // Variable de ruta
    ) {
        ProductResponse response = productService.getProduct(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping                            // GET /api/products?page=0&size=20
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,    // Query param
            @RequestParam(defaultValue = "20") int size
    ) {
        // ...
    }
}

ANOTACIONES HTTP
----------------

@GetMapping    - GET (obtener datos)
@PostMapping   - POST (crear recursos)
@PutMapping    - PUT (actualizar completo)
@PatchMapping  - PATCH (actualizar parcial)
@DeleteMapping - DELETE (eliminar)

ANOTACIONES DE PARAMETROS
-------------------------

@PathVariable - Variable en la URL
  GET /api/products/{id} -> @PathVariable Long id

@RequestParam - Parámetro de query string
  GET /api/products?name=foo -> @RequestParam String name

@RequestBody - Cuerpo de la petición (JSON)
  POST /api/products con JSON -> @RequestBody CreateProductRequest request

@RequestHeader - Header HTTP
  Authorization: Bearer xxx -> @RequestHeader("Authorization") String auth

@AuthenticationPrincipal - Usuario autenticado
  Inyecta el usuario del token JWT

RESPONSEENTITY
--------------
Permite controlar la respuesta completa (status, headers, body).

// 201 Created con body
return ResponseEntity.status(HttpStatus.CREATED).body(data);

// 200 OK con body
return ResponseEntity.ok(data);

// 204 No Content (sin body)
return ResponseEntity.noContent().build();

// 404 Not Found
return ResponseEntity.notFound().build();

// Con headers personalizados
return ResponseEntity.ok()
    .header("X-Custom-Header", "value")
    .body(data);

WRAPPER ApiResponse
-------------------
Estandarizamos todas las respuestas:

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private LocalDateTime timestamp;
}

Por que un wrapper:
1. Respuestas consistentes
2. Siempre sabemos donde estan los datos
3. Mensajes de error estandarizados
4. Facilita el frontend

DOCUMENTACIÓN:
- Spring MVC: https://docs.spring.io/spring-framework/reference/web/webmvc.html
- REST con Spring: https://spring.io/guides/tutorials/rest


<a id="seccion-10"></a>
================================================================================
10. SEGURIDAD Y AUTENTICACIÓN JWT
================================================================================

Usamos Spring Security con autenticación JWT (JSON Web Token).

POR QUE JWT:
- Stateless: no necesita sesiones en servidor
- Escalable: cualquier servidor puede validar el token
- Autocontenido: contiene info del usuario en el propio token
- Estandar: ampliamente soportado

ESTRUCTURA DE UN JWT:
--------------------
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsImlhdCI6MTcwMDAwMDAwMH0.firma

Tres partes separadas por punto:
1. Header: algoritmo de firma (HS256)
2. Payload: datos del usuario (claims)
3. Signature: firma para verificar integridad

FLUJO DE AUTENTICACIÓN:
----------------------

┌──────────┐                              ┌──────────┐
│  Cliente │                              │ Servidor │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │ POST /api/auth/login                    │
     │ {email, password}                       │
     │────────────────────────────────────────>│
     │                                         │
     │         Valida credenciales             │
     │         Genera JWT                      │
     │                                         │
     │ {token: "eyJ...", userId, email}        │
     │<────────────────────────────────────────│
     │                                         │
     │ GET /api/products                       │
     │ Authorization: Bearer eyJ...            │
     │────────────────────────────────────────>│
     │                                         │
     │         Valida token                    │
     │         Extrae usuario                  │
     │         Ejecuta petición                │
     │                                         │
     │ {products: [...]}                       │
     │<────────────────────────────────────────│

COMPONENTES DE SEGURIDAD:
------------------------

1. SecurityConfig.java - Configuración central
   - Define rutas públicas/protegidas
   - Configura CORS
   - Anade filtro JWT
   - Configura hash de contraseñas

2. JwtService.java - Operaciones con tokens
   - generateToken(): crea token firmado
   - extractUsername(): obtiene email del token
   - isTokenValid(): valida firma y expiración

3. JwtAuthenticationFilter.java - Intercepta cada request
   - Extrae token del header Authorization
   - Valida token
   - Establece autenticación en SecurityContext

4. UserPrincipal.java - Representa usuario autenticado
   - Implementa UserDetails de Spring Security
   - Envuelve entidad User

CONFIGURACIÓN DE SEGURIDAD:
--------------------------

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Desactivar CSRF (no necesario con JWT stateless)
        .csrf(AbstractHttpConfigurer::disable)
        
        // Configurar CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        
        // Headers de seguridad
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())      // Previene clickjacking
            .contentTypeOptions(content -> {})        // Previene MIME sniffing
        )
        
        // Rutas públicas y protegidas
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // Login/registro público
            .requestMatchers("/api/health").permitAll()   // Health check público
            .anyRequest().authenticated()                  // Todo lo demas requiere auth
        )
        
        // Sesiones stateless (no cookies)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // Nuestro filtro JWT antes del filtro de username/password
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

HASH DE CONTRASEÑAS:
-------------------
Usamos BCrypt, el estándar de la industria.

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

Por que BCrypt:
- Diseñado especificamente para contraseñas
- Salt automático (cada hash es diferente)
- "Cost factor" configurable (más lento = más seguro)
- Resistente a ataques de fuerza bruta

Ejemplo:
"password123" -> "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

VALIDACION DE CONTRASEÑAS:
-------------------------
Usamos regex para forzar contraseñas seguras:

@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
    message = "Debe contener mayuscula, minuscula y numero"
)
@Size(min = 8, max = 100)
private String password;

- (?=.*[a-z]) - Al menos una minuscula
- (?=.*[A-Z]) - Al menos una mayuscula
- (?=.*\\d)   - Al menos un digito
- .+$         - Uno o más caracteres

MULTI-TENANCY EN EL JWT:
-----------------------
El sistema aisla los datos por empresa (multi-tenancy) y para no consultar
la base de datos en cada petición almacenamos el companyId como claim
personalizado dentro del propio JWT, junto al userId y al rol:

JwtService.generateToken(userPrincipal):
  claims.put("userId", userPrincipal.getId());
  claims.put("companyId", userPrincipal.getCompanyId());
  claims.put("role", userPrincipal.getRole());

Al validar el token, JwtAuthenticationFilter reconstruye el UserPrincipal
con esos claims y lo deja en el SecurityContext. En cada servicio se obtiene
con userPrincipal.requireCompanyId() y se incluye como filtro en cualquier
query: findByCompanyIdAndActiveTrue(...). Sin ese filtro, una consulta
podría devolver datos de otra empresa.

Los usuarios con rol ADMIN no tienen empresa asociada (companyId=null);
requireCompanyId() lanza BadRequestException si se invoca en un contexto
que sí necesita empresa, lo que evita filtraciones accidentales.

DOCUMENTACIÓN:
- Spring Security: https://docs.spring.io/spring-security/reference/
- JWT: https://jwt.io/introduction
- BCrypt: https://en.wikipedia.org/wiki/Bcrypt
- JJWT Library: https://github.com/jwtk/jjwt


<a id="seccion-11"></a>
================================================================================
11. PROGRAMACIÓN ASÍNCRONA Y MULTIHILO
================================================================================

Usamos programación asíncrona para operaciones lentas (API externa).

POR QUE ASINCRONO:
- Las llamadas a Keepa API tardan ~500ms-2s
- Sin async, el hilo se bloquea esperando
- Con async, el hilo puede atender otras peticiones
- Mejor utilización de recursos

COMPONENTES PRINCIPALES:
-----------------------

1. @EnableAsync - Activa soporte asíncrono
2. @Async("executor") - Marca método como asíncrono
3. ThreadPoolTaskExecutor - Pool de hilos configurado
4. CompletableFuture<T> - Resultado futuro de operación asíncrona

CONFIGURACIÓN DE POOLS:
----------------------

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "keepaExecutor")
    public Executor keepaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Hilos mínimos siempre activos
        executor.setCorePoolSize(2);
        
        // Hilos máximos bajo carga
        executor.setMaxPoolSize(5);
        
        // Tareas en espera si todos los hilos ocupados
        executor.setQueueCapacity(50);
        
        // Prefijo para identificar en logs
        executor.setThreadNamePrefix("Keepa-");
        
        // Que hacer si la cola esta llena
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Esperar a tareas pendientes antes de cerrar
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}

EXPLICACION DE POLITICAS DE RECHAZO:
-----------------------------------

Cuando la cola esta llena y todos los hilos ocupados:

- CallerRunsPolicy (usado): El hilo que llama ejecuta la tarea
  Pro: No se pierde ninguna tarea
  Contra: Puede bloquear el hilo llamante

- AbortPolicy: Lanza RejectedExecutionException
- DiscardPolicy: Descarta la tarea silenciosamente
- DiscardOldestPolicy: Descarta la tarea más antigua de la cola

USO DE @ASYNC:
-------------

@Service
public class KeepaService {
    
    @Async("keepaExecutor")  // Ejecutar en pool keepaExecutor
    public CompletableFuture<Optional<CompetitorPrice>> fetchPriceByAsin(String asin, Product product) {
        // Este método se ejecuta en otro hilo
        Optional<CompetitorPrice> result = callKeepaApi(asin);
        return CompletableFuture.completedFuture(result);
    }
}

// Uso:
CompletableFuture<Optional<CompetitorPrice>> future = keepaService.fetchPriceByAsin("B0...", product);
// El hilo principal continua...
Optional<CompetitorPrice> result = future.get();  // Bloquea hasta tener resultado

COMPLETABLEFUTURE:
-----------------
Representa el resultado futuro de una operación asíncrona.

Metodos importantes:

// Crear
CompletableFuture.completedFuture(value)  // Ya completado
CompletableFuture.supplyAsync(() -> ...)  // Ejecutar async

// Transformar
.thenApply(x -> transform(x))      // Transformar resultado
.thenAccept(x -> consume(x))       // Consumir sin devolver
.thenCompose(x -> anotherFuture)   // Encadenar futuros

// Combinar
CompletableFuture.allOf(f1, f2, f3)    // Esperar todos
CompletableFuture.anyOf(f1, f2, f3)    // Esperar cualquiera

// Manejar errores
.exceptionally(ex -> defaultValue)
.handle((result, ex) -> ...)

SEMAPHORE - CONTROL DE CONCURRENCIA:
-----------------------------------
Limitamos llamadas concurrentes a la API.

private final Semaphore rateLimiter = new Semaphore(3);  // Max 3 concurrentes

public void callApi() {
    rateLimiter.acquire();  // Espera si hay 3 llamadas activas
    try {
        // Llamar API
    } finally {
        rateLimiter.release();  // Libera para el siguiente
    }
}

Por que Semaphore:
- Keepa tiene rate limits (máximo peticiones por minuto)
- Evitamos saturar la API y ser bloqueados
- Semaphore es thread-safe por diseño

SINCRONIZACION CON SYNCHRONIZED:
-------------------------------
Para proteger recursos compartidos.

Problema: amazonCompetitor es accedido por múltiples hilos.

Solución: Double-checked locking pattern

private volatile Competitor amazonCompetitor;  // volatile = visible a todos los hilos
private final Object lock = new Object();

private Competitor getAmazonCompetitor() {
    Competitor localRef = amazonCompetitor;  // Lectura local (rápida)
    if (localRef == null) {                  // Primer check sin lock
        synchronized (lock) {                 // Adquirir lock
            localRef = amazonCompetitor;     // Segundo check
            if (localRef == null) {
                initAmazonCompetitor();
                localRef = amazonCompetitor;
            }
        }
    }
    return localRef;
}

Por que volatile:
- Garantiza que todos los hilos ven el valor actualizado
- Sin volatile, cada hilo puede tener una "copia" desactualizada

Por que double-checked locking:
- Evita sincronización en cada acceso (costoso)
- Solo sincroniza durante inicialización
- Seguro a partir de Java 5 gracias a las garantías de `volatile` en la JMM (en Java 1.4 y anteriores el patrón era inseguro por reordenación de instrucciones)
- Alternativas más modernas: lazy holder con `static class Holder` o `AtomicReference`. Se eligió DCL por simplicidad y porque la inicialización ocurre una sola vez en arranque

DOCUMENTACIÓN:
- @Async: https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async
- CompletableFuture: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html
- Semaphore: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Semaphore.html
- Thread Safety: https://jenkov.com/tutorials/java-concurrency/thread-safety.html


<a id="seccion-12"></a>
================================================================================
12. MANEJO DE EXCEPCIONES
================================================================================

Spring permite manejar excepciones de forma centralizada con @ControllerAdvice.

EXCEPCIONES PERSONALIZADAS:
--------------------------

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

Por que RuntimeException:
- No requiere try-catch explicito
- @Transactional hace rollback automático
- Mas limpio el código

MANEJADOR GLOBAL:
----------------

@Slf4j
@RestControllerAdvice  // Aplica a todos los controladores
public class GlobalExceptionHandler {

    // Recurso no encontrado -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    // Error de validación -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Error de validación", errors));
    }

    // Violación de restricciones de BD -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Error de integridad de datos";
        String cause = Optional.ofNullable(ex.getMostSpecificCause())
            .map(Throwable::getMessage)
            .map(String::toLowerCase)
            .orElse("");
        if (cause.contains("duplicate")) {
            message = "Ya existe un registro con esos datos";
        }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(message));
    }

    // Credenciales invalidas -> 401
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Credenciales invalidas"));
    }

    // Acceso denegado -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("No tienes permisos para esta operación"));
    }

    // Cualquier otra excepción -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Error interno", ex);  // Log completo con stack trace
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Error interno del servidor"));  // Mensaje genérico
    }
}

CODIGOS HTTP Y CUANDO USARLOS:
-----------------------------

| Código | Nombre                | Cuando usar                                    |
|--------|----------------------|------------------------------------------------|
| 200    | OK                   | Exito general                                  |
| 201    | Created              | Recurso creado exitosamente                    |
| 204    | No Content           | Exito sin contenido (DELETE)                   |
| 400    | Bad Request          | Datos invalidos del cliente                    |
| 401    | Unauthorized         | No autenticado                                 |
| 403    | Forbidden            | Autenticado pero sin permisos                  |
| 404    | Not Found            | Recurso no existe                              |
| 409    | Conflict             | Conflicto (duplicado, estado inválido)         |
| 422    | Unprocessable Entity | Semanticamente incorrecto                      |
| 500    | Internal Server Error| Error del servidor                             |

POR QUE OCULTAR DETALLES EN 500:
- No exponer estructura interna
- No revelar vulnerabilidades
- Mensajes de error ayudan a atacantes
- El log tiene los detalles para debugging

DOCUMENTACIÓN:
- Exception Handling: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
- HTTP Status Codes: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status


<a id="seccion-13"></a>
================================================================================
13. VALIDACIONES
================================================================================

Usamos Bean Validation (JSR-380) para validar datos de entrada.

ANOTACIONES DE VALIDACION:
-------------------------

// Obligatoriedad
@NotNull                    // No puede ser null
@NotEmpty                   // No puede ser null ni vacio
@NotBlank                   // No puede ser null, vacio ni solo espacios

// Texto
@Size(min=3, max=50)        // Longitud entre min y max
@Pattern(regexp="...")      // Debe coincidir con regex
@Email                      // Formato de email válido

// Numeros
@Min(0)                     // Valor mínimo
@Max(100)                   // Valor máximo
@Positive                   // Mayor que 0
@PositiveOrZero             // Mayor o igual a 0
@DecimalMin("0.01")         // Para BigDecimal

// Fechas
@Past                       // Fecha en el pasado
@Future                     // Fecha en el futuro
@PastOrPresent              // Pasado o ahora

EJEMPLO DE DTO VALIDADO:
-----------------------

public class RegisterRequest {
    
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "Username entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "Contraseña entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Debe contener mayuscula, minuscula y numero"
    )
    private String password;
}

ACTIVAR VALIDACION EN CONTROLLER:
--------------------------------

@PostMapping("/register")
public ResponseEntity<...> register(@Valid @RequestBody RegisterRequest request) {
    // Si validación falla, lanza MethodArgumentNotValidException
    // GlobalExceptionHandler lo captura y devuelve 400 con errores
}

@Valid activa la validación del objeto.

VALIDACION PERSONALIZADA:
------------------------
Puedes crear anotaciones propias:

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SkuValidator.class)
public @interface ValidSku {
    String message() default "SKU inválido";
    Class<?>[] groups() default {};
    Class<?>[] payload() default {};
}

public class SkuValidator implements ConstraintValidator<ValidSku, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.matches("^[A-Z0-9]{10}$");
    }
}

DOCUMENTACIÓN:
- Bean Validation: https://beanvalidation.org/2.0/spec/
- Hibernate Validator: https://hibernate.org/validator/documentation/


<a id="seccion-14"></a>
================================================================================
14. CACHE Y RENDIMIENTO
================================================================================

Usamos Spring Cache para evitar consultas repetidas a la base de datos.

CONFIGURACIÓN:
-------------

spring:
  cache:
    type: simple  # Cache en memoria (HashMap)

@EnableCaching en una clase @Configuration activa el soporte.

ANOTACIONES DE CACHE:
--------------------

@Cacheable - Guarda resultado si no existe
------------------------------------------------
@Cacheable(value = "categories", key = "#userId")
public List<String> getCategories(Long userId) {
    return productRepository.findDistinctCategoriesByUserId(userId);
}

Primera llamada: ejecuta método, guarda resultado
Siguientes llamadas: devuelve de cache sin ejecutar

@CacheEvict - Invalida cache
----------------------------
@CacheEvict(value = {"categories", "brands"}, key = "#userId")
public ProductResponse createProduct(Long userId, CreateProductRequest request) {
    // Al crear producto, inválida cache de categorías/marcas
}

@CachePut - Actualiza cache
---------------------------
@CachePut(value = "products", key = "#result.id")
public Product updateProduct(Product product) {
    return productRepository.save(product);
}

Siempre ejecuta el método y actualiza cache.

CLAVES DE CACHE (key):
---------------------
- #userId          -> valor del parámetro
- #result.id       -> campo del resultado
- #p0              -> primer parámetro
- #root.methodName -> nombre del método

LIMITACIONES DEL CACHE SIMPLE:
-----------------------------
- Se pierde al reiniciar
- No compartido entre instancias
- Sin control de tamaño

ALTERNATIVAS PARA PRODUCCION:
- Redis: Distribuido, persistente
- Caffeine: Local, muy rápido, con eviction
- EhCache: Local, configurable

DOCUMENTACIÓN:
- Spring Cache: https://docs.spring.io/spring-framework/reference/integration/cache.html


<a id="seccion-15"></a>
================================================================================
15. TAREAS PROGRAMADAS CON QUARTZ
================================================================================

Usamos Quartz Scheduler para ejecutar tareas periodicamente.

POR QUE QUARTZ Y NO @Scheduled:
- Persistencia de jobs (sobrevive reinicios)
- Clustering (un solo nodo ejecuta)
- Configuración dinámica
- Mejor para tareas complejas

COMPONENTES:
-----------

1. Job - La tarea a ejecutar
@Component
public class PriceMonitorJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        // Logica de actualización de precios
    }
}

2. JobDetail - Configuración del job
@Bean
public JobDetail priceMonitorJobDetail() {
    return JobBuilder.newJob(PriceMonitorJob.class)
        .withIdentity("priceMonitorJob")
        .storeDurably()
        .build();
}

3. Trigger - Cuando ejecutar
@Bean
public Trigger priceMonitorTrigger(JobDetail jobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withIdentity("priceMonitorTrigger")
        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInHours(6)
            .repeatForever())
        .startAt(Date.from(Instant.now().plusSeconds(60)))  // 1 min después
        .build();
}

TIPOS DE TRIGGER:
----------------
- SimpleTrigger: Intervalos fijos (cada 6 horas)
- CronTrigger: Expresiones cron (0 0 */6 * * ?)
- CalendarIntervalTrigger: Intervalos de calendario

EXPRESIONES CRON:
----------------
┌───────────── segundo (0-59)
│ ┌───────────── minuto (0-59)
│ │ ┌───────────── hora (0-23)
│ │ │ ┌───────────── dia del mes (1-31)
│ │ │ │ ┌───────────── mes (1-12)
│ │ │ │ │ ┌───────────── dia de semana (0-7)
│ │ │ │ │ │
* * * * * *

"0 0 6 * * ?"     - Cada dia a las 6:00
"0 */30 * * * ?"  - Cada 30 minutos
"0 0 */4 * * ?"   - Cada 4 horas

DOCUMENTACIÓN:
- Quartz: https://www.quartz-scheduler.org/documentation/
- Spring Quartz: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.quartz


<a id="seccion-16"></a>
================================================================================
16. CONFIGURACIÓN Y PERFILES
================================================================================

ESTRUCTURA DE application.yml:
-----------------------------

El archivo se divide por perfiles usando ---

spring:
  application:
    name: pricewise
  datasource:
    url: jdbc:postgresql://localhost:5432/pricewise_db
    
---
# Perfil DESARROLLO
spring:
  config:
    activate:
      on-profile: dev
  jpa:
    show-sql: true

---
# Perfil PRODUCCION
spring:
  config:
    activate:
      on-profile: prod
  jpa:
    show-sql: false

VARIABLES DE ENTORNO:
--------------------
Valores sensibles no van en el código:

jwt:
  secret: ${JWT_SECRET:valorPorDefecto}
  
datasource:
  password: ${DB_PASSWORD:postgres}

Sintaxis ${VARIABLE:default}:
- Busca variable de entorno VARIABLE
- Si no existe, usa el valor por defecto

CREAR ARCHIVO .env:
------------------
JWT_SECRET=miClaveSecreta123456
DB_PASSWORD=miPasswordDeProduccion
# Las API keys de Keepa se gestionan por empresa desde la app (Ajustes)

ACTIVAR PERFIL:
--------------
Varias formas:

1. En application.yml:
spring:
  profiles:
    active: dev

2. Variable de entorno:
SPRING_PROFILES_ACTIVE=prod

3. Argumento de linea de comandos:
java -jar app.jar --spring.profiles.active=prod

DOCUMENTACIÓN:
- External Configuration: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config


<a id="seccion-17"></a>
================================================================================
17. POOL DE CONEXIONES
================================================================================

Usamos HikariCP, el pool de conexiones por defecto de Spring Boot.

POR QUE UN POOL:
- Crear conexiones es costoso (~100ms)
- Reutilizar conexiones es rápido (~1ms)
- Control del numero máximo de conexiones

CONFIGURACIÓN:
-------------

spring:
  datasource:
    hikari:
      # Conexiones siempre listas
      minimum-idle: 5
      
      # Máximo de conexiones
      maximum-pool-size: 20
      
      # Tiempo esperando conexión libre
      connection-timeout: 30000  # 30s
      
      # Tiempo máximo en idle antes de cerrar
      idle-timeout: 600000       # 10 min
      
      # Vida máxima de una conexión
      max-lifetime: 1800000      # 30 min
      
      # Detectar conexiones no devueltas
      leak-detection-threshold: 60000  # 1 min

EXPLICACION DE PARAMETROS:
-------------------------

minimum-idle: Conexiones "calientes" esperando
- Muy bajo: retrasos al necesitar conexiones
- Muy alto: desperdicio de recursos

maximum-pool-size: Limite absoluto
- Muy bajo: cuellos de botella
- Muy alto: sobrecarga la BD
- Regla: ~ conexiones = cores * 2 + discos

max-lifetime: Refresca conexiones periodicamente
- Evita problemas con firewalls que cierran conexiones idle
- Debe ser menor que timeout del servidor BD

leak-detection-threshold: Alerta si una conexión no se devuelve
- Ayuda a detectar bugs (olvidar cerrar conexión)
- Solo warning en log, no cierra conexión

DOCUMENTACIÓN:
- HikariCP: https://github.com/brettwooldridge/HikariCP


<a id="seccion-18"></a>
================================================================================
18. TESTING
================================================================================

TIPOS DE TESTS:
--------------

1. Tests Unitarios - Prueban una clase aislada
   - Mock de dependencias
   - Rapidos (~ms)
   - Muchos (80% de tests)

2. Tests de Integración - Prueban varias capas
   - Base de datos real o H2
   - Mas lentos (~s)
   - Menos

3. Tests End-to-End - Prueban todo el flujo
   - Peticiones HTTP reales
   - Los más lentos
   - Pocos

FRAMEWORKS USADOS:
-----------------

- JUnit 5: Framework base de testing
- Mockito: Crear mocks de dependencias
- Spring Boot Test: @SpringBootTest, TestRestTemplate
- AssertJ: Assertions fluidas

EJEMPLO TEST UNITARIO:
---------------------

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Debe lanzar excepción si producto no existe")
    void getProduct_shouldThrow_whenNotFound() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> productService.getProduct(1L, 1L));
    }
}

EJEMPLO TEST DE INTEGRACION:
---------------------------

@SpringBootTest
@ActiveProfiles("dev")
class AsyncConfigTest {

    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Test
    void taskExecutor_shouldHaveCorrectConfig() {
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(5);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(10);
    }
}

ANOTACIONES DE TEST:
-------------------

@Test                    - Marca método como test
@DisplayName("...")      - Nombre legible del test
@BeforeEach              - Ejecutar antes de cada test
@AfterEach               - Ejecutar después de cada test
@BeforeAll               - Ejecutar una vez antes de todos
@Disabled                - Desactivar test temporalmente
@ParameterizedTest       - Test con múltiples datos

DOCUMENTACIÓN:
- JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Mockito: https://site.mockito.org/
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing


<a id="seccion-19"></a>
================================================================================
19. REFERENCIAS Y DOCUMENTACIÓN OFICIAL
================================================================================

DOCUMENTACIÓN OFICIAL:
---------------------

Spring Framework:
- Pagina principal: https://spring.io/
- Documentación: https://docs.spring.io/spring-framework/reference/
- Guides: https://spring.io/guides

Spring Boot:
- Reference: https://docs.spring.io/spring-boot/docs/current/reference/html/
- API Docs: https://docs.spring.io/spring-boot/docs/current/api/

Spring Security:
- Reference: https://docs.spring.io/spring-security/reference/
- Oauth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html

Spring Data JPA:
- Reference: https://docs.spring.io/spring-data/jpa/reference/

Java:
- API Javadoc: https://docs.oracle.com/en/java/javase/17/docs/api/
- Tutorials: https://dev.java/learn/
- Concurrency: https://docs.oracle.com/javase/tutorial/essential/concurrency/

Hibernate:
- User Guide: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html
- Javadoc: https://docs.jboss.org/hibernate/orm/current/javadocs/

LIBRERIAS EXTERNAS:
------------------

Lombok:
- Features: https://projectlombok.org/features/all
- Annotations: https://projectlombok.org/api/

JWT (jjwt):
- GitHub: https://github.com/jwtk/jjwt
- README: https://github.com/jwtk/jjwt#readme

Quartz:
- Documentation: https://www.quartz-scheduler.org/documentation/
- Tutorial: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/

HikariCP:
- GitHub: https://github.com/brettwooldridge/HikariCP
- Configuration: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby

Keepa API:
- Documentation: https://keepa.com/#!discuss/t/using-the-keepa-api/47

PATRONES Y ARQUITECTURA:
-----------------------

Clean Architecture:
- Blog Uncle Bob: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

SOLID Principles:
- https://www.digitalocean.com/community/conceptual-articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design

Design Patterns:
- Refactoring Guru: https://refactoring.guru/design-patterns

REST Best Practices:
- https://restfulapi.net/
- https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api

TUTORIALES RECOMENDADOS:
-----------------------

Baeldung (Spring/Java):
- https://www.baeldung.com/

Java Concurrency:
- https://jenkov.com/tutorials/java-concurrency/index.html

Spring Security JWT:
- https://www.bezkoder.com/spring-boot-jwt-authentication/

<a id="seccion-20"></a>
================================================================================
20. DOCUMENTACIÓN RELACIONADA
================================================================================

- [README.md](README.md) — Endpoints REST, instalación y configuración
- [SEGURIDAD.md](SEGURIDAD.md) — Informe de seguridad (JWT, OAuth2, cifrado AES-256)
- [FLYWAY.md](FLYWAY.md) — Migraciones de base de datos V1-V8
- [CRONOGRAMA.md](CRONOGRAMA.md) — Timeline de desarrollo por fases
- [BUGS_Y_SOLUCIONES.md](BUGS_Y_SOLUCIONES.md) — Registro de incidencias resueltas
- [MEJORAS_FUTURAS.md](MEJORAS_FUTURAS.md) — Servicios retirados del MVP y plan de reintegración

================================================================================
                              FIN DEL DOCUMENTO
================================================================================
