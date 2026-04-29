# Arquitectura del backend PriceWise

Guía de la arquitectura interna y las decisiones técnicas tomadas en el backend. Para reglas operativas y gotchas reales del día a día, ver `PATRONES.md`. Para el detalle de seguridad, ver `SEGURIDAD.md`. Para endpoints, ver `README.md`.

## 1. Visión general

PriceWise permite a las PYMEs gestionar su catálogo y monitorizar precios de la competencia en Amazon. El backend expone una API REST con autenticación JWT, multi-tenancy por empresa, integración con Keepa para precios de Amazon y un sistema de alertas configurable.

Tecnologías y motivo de elección:

1. Spring Boot 3.3: framework base, productividad y ecosistema.
2. PostgreSQL 14: ACID, consultas complejas, soporte de índices únicos parciales (clave para el soft-delete).
3. Flyway: control de versiones del esquema en producción.
4. JWT (jjwt 0.12.3): autenticación stateless, escala horizontalmente.
5. Quartz Scheduler: jobs programados con persistencia y soporte de clustering, más flexible que `@Scheduled`.
6. HikariCP: pool de conexiones por defecto de Spring Boot, rápido y bien afinado.
7. Lombok 1.18.40: reduce boilerplate.

## 2. Estructura del proyecto

Organización por capas (`package by layer`), convención de Spring Boot:

```
pricewise-backend/
├── src/main/java/com/alvaro/pricewise/
│   ├── PriceWiseApplication.java
│   ├── config/        Configuraciones (Security, Async, Quartz, Cors, Cache)
│   ├── controller/    Controladores REST
│   ├── dto/           DTOs de request y response
│   ├── entity/        Entidades JPA
│   ├── exception/     Excepciones de negocio y handler global
│   ├── repository/    Repositorios Spring Data
│   ├── scheduler/     Jobs Quartz
│   ├── security/      JwtService, filtro JWT, UserPrincipal
│   └── service/       Lógica de negocio
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/  Migraciones Flyway V1 a V8
├── src/test/java/
└── pom.xml
```

## 3. Arquitectura por capas

Tres capas con responsabilidades separadas:

1. Presentación: `controller/` y `dto/`. Recibe HTTP, valida con `@Valid`, convierte DTOs y entidades, devuelve JSON.
2. Negocio: `service/`. Reglas de negocio y coordinación entre repositorios. Gestión transaccional con `@Transactional`. No conoce HTTP ni JSON.
3. Datos: `repository/` y `entity/`. Mapeo objeto-relacional, queries JPQL o nativas.

Esta separación permite testar capas aisladas con mocks, cambiar la BD afectando solo a la capa de datos y reutilizar servicios entre controladores.

## 4. Anotaciones de Spring usadas

Componentes:

1. `@RestController`: controladores REST, devuelve JSON directamente.
2. `@Service`: lógica de negocio.
3. `@Repository`: acceso a datos, traducción automática de excepciones.
4. `@Configuration`: define beans.
5. `@Component`: componente genérico.

Inyección por constructor con `@RequiredArgsConstructor` de Lombok. Campos `final` para forzar inmutabilidad y dependencias obligatorias. Sin `@Autowired` en campos.

## 5. Entidades JPA

`@Entity` mapea la clase a tabla. `@Id` con `@GeneratedValue(strategy = GenerationType.IDENTITY)` para autoincremento de PostgreSQL.

Relaciones:

1. `@ManyToOne(fetch = FetchType.LAZY)` por defecto para evitar carga innecesaria.
2. `@OneToMany(mappedBy = "...", cascade = CascadeType.ALL)` cuando aplica cascada.
3. Carga ansiosa (`EAGER`) solo cuando se justifica (caso muy concreto, evitar por defecto).

Índices declarados en `@Table(indexes = {...})` para columnas de filtrado frecuente: `sku`, `company_id`, `category`, `monitoring_enabled`.

Auditoría con `@EntityListeners(AuditingEntityListener.class)` y `@CreatedDate` y `@LastModifiedDate` en las entidades que lo necesitan, junto con `@EnableJpaAuditing` en config.

Reglas específicas del proyecto:

1. La entidad `Product` mantiene a la vez `sku` y `asin`. ASIN es el identificador semántico, `sku` se sincroniza desde `@PrePersist` y `@PreUpdate` para soportar el índice único parcial.
2. Soft-delete con `active = false`. La unicidad de `(sku, company_id)` se garantiza solo entre productos activos mediante un índice único parcial creado por Flyway, no por `@UniqueConstraint`.

## 6. Repositorios

`JpaRepository<Entity, Long>` proporciona CRUD básico. Uso de query methods cuando la query es directa:

```java
List<Product> findByCompanyIdAndActiveTrue(Long companyId);
Optional<Product> findBySkuAndCompanyIdAndActiveTrue(String sku, Long companyId);
Optional<CompetitorPrice> findTopByProductIdOrderByScrapedAtDesc(Long productId);
```

Para queries complejas, JPQL con `@Query`:

```java
@Query("SELECT p FROM Product p WHERE p.company.id = :companyId " +
       "AND p.active = true " +
       "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))")
Page<Product> searchProducts(@Param("companyId") Long companyId,
                             @Param("name") String name,
                             Pageable pageable);
```

JPQL antes que SQL nativo siempre que se pueda: portable y validado contra el modelo de entidades.

Convención multi-tenancy: cualquier repositorio que acceda a entidades con `company_id` debe ofrecer variantes que filtren por `companyId` y por `active = true`. Los métodos globales (`findAll`, `findBySku`) no se usan como acceso primario.

## 7. Servicios

Lógica de negocio en `@Service`. Patrón general:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CompetitorPriceRepository competitorPriceRepository;

    @Transactional
    public ProductResponse createProduct(Long companyId, CreateProductRequest request) {
        // 1. Validaciones de negocio
        // 2. Construcción de la entidad
        // 3. Persistencia
        // 4. Operaciones derivadas (PriceHistory, etc.)
        // 5. Mapeo a DTO de respuesta
    }
}
```

`@Transactional` se aplica en la capa de servicio: agrupa varias operaciones de repositorio en una unidad atómica. Si lanza `RuntimeException`, Spring hace rollback automático. Para consultas se usa `@Transactional(readOnly = true)` cuando aplica.

Patrón DTO obligatorio. Las entidades no salen a la API: oculta campos sensibles (password con `@JsonIgnore`), evita problemas de serialización con LAZY loading y desacopla la versión del API de la del esquema.

## 8. Controladores REST

Patrón:

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(
            userPrincipal.requireCompanyId(), request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Producto creado"));
    }
}
```

Anotaciones HTTP: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`. Parámetros: `@PathVariable`, `@RequestParam`, `@RequestBody`, `@AuthenticationPrincipal`.

Respuestas envueltas en `ApiResponse<T>`:

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private LocalDateTime timestamp;
}
```

Permite respuestas consistentes en cliente, mensajes de error estandarizados y un único contrato de error.

## 9. Seguridad y JWT

`SecurityConfig` configura:

1. CSRF deshabilitado, apropiado para API REST stateless con JWT.
2. CORS por perfil: permisivo en dev, restrictivo en prod por `CORS_ORIGINS`.
3. `SessionCreationPolicy.STATELESS`: el servidor no guarda sesiones.
4. `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
5. `AuthenticationEntryPoint(HttpStatusEntryPoint(UNAUTHORIZED))` para devolver 401 ante peticiones sin token, no el 403 por defecto. Esta diferencia es la que permite al cliente Android distinguir token expirado de falta de permisos.

Componentes:

1. `JwtService`: genera, parsea y valida tokens.
2. `JwtAuthenticationFilter`: extrae el token del header `Authorization`, valida y deja `UserPrincipal` en el `SecurityContext`.
3. `UserPrincipal`: implementa `UserDetails`. Lleva `userId`, `companyId`, rol y `displayUsername`, lo justo para no consultar la BD en cada petición.

Multi-tenancy en el JWT. El `companyId` se mete como claim del token al hacer login y se lee en cada petición desde `UserPrincipal.requireCompanyId()`. Toda query de negocio filtra por ese `companyId`. Los usuarios `ADMIN` no tienen empresa, `requireCompanyId()` lanza `BadRequestException` en contextos que requieren empresa.

Contraseñas con BCrypt vía `BCryptPasswordEncoder`. Validación regex en `RegisterRequest` exige mayúscula, minúscula y dígito, mínimo 8 caracteres.

## 10. Programación asíncrona

Operaciones lentas de Keepa (500 ms a 2 s) van por `@Async` con un `ThreadPoolTaskExecutor` dedicado:

```java
@Bean(name = "keepaExecutor")
public Executor keepaExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("Keepa-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
}
```

`CallerRunsPolicy`: si la cola se llena, el hilo que llama ejecuta la tarea. Garantiza que ninguna tarea se pierde a costa de bloquear puntualmente al llamante.

`Semaphore(3)` en `KeepaService` limita las llamadas concurrentes a la API y respeta el rate limit de Keepa.

`amazonCompetitor` se inicializa una sola vez con `volatile` y double-checked locking. Es seguro en Java 5 o superior por las garantías de la JMM. Alternativa moderna sería `static class Holder` o `AtomicReference`, pero DCL aquí es claro y la inicialización es única.

## 11. Manejo de excepciones

`@RestControllerAdvice` global en `GlobalExceptionHandler`:

1. `ResourceNotFoundException`: 404.
2. `BadRequestException`: 400.
3. `MethodArgumentNotValidException`: 400 con detalle por campo.
4. `DataIntegrityViolationException`: 409, mensaje genérico ("Ya existe un registro con esos datos") sin filtrar detalles internos.
5. `BadCredentialsException`: 401.
6. `AccessDeniedException`: 403.
7. `Exception`: 500 con mensaje genérico al cliente y stack trace en log.

Las excepciones de negocio extienden `RuntimeException` para evitar try/catch obligatorios y para que `@Transactional` haga rollback automático.

Códigos HTTP en uso: 200, 201, 204, 400, 401, 403, 404, 409, 422, 500.

## 12. Validaciones

Bean Validation (Jakarta) con `@Valid` en los controladores. Las anotaciones más usadas:

1. `@NotBlank`, `@NotEmpty`, `@NotNull`.
2. `@Size(min, max)`, `@Email`, `@Pattern(regexp)`.
3. `@Min`, `@Max`, `@Positive`, `@PositiveOrZero`, `@DecimalMin`.

Ejemplo en `RegisterRequest`:

```java
@NotBlank
@Size(min = 3, max = 50)
private String username;

@NotBlank
@Email
private String email;

@NotBlank
@Size(min = 8, max = 100)
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
    message = "Debe contener mayúscula, minúscula y número"
)
private String password;
```

Si la validación falla, `MethodArgumentNotValidException` la captura `GlobalExceptionHandler` y devuelve 400 con la lista de errores por campo.

## 13. Quartz Scheduler

Jobs programados con persistencia. `PriceMonitorJob` se ejecuta cada 6 horas:

```java
@Bean
public JobDetail priceMonitorJobDetail() {
    return JobBuilder.newJob(PriceMonitorJob.class)
        .withIdentity("priceMonitorJob")
        .storeDurably()
        .build();
}

@Bean
public Trigger priceMonitorTrigger(JobDetail jobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(jobDetail)
        .withIdentity("priceMonitorTrigger")
        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInHours(6)
            .repeatForever())
        .startAt(Date.from(Instant.now().plusSeconds(60)))
        .build();
}
```

Quartz se eligió en lugar de `@Scheduled` por persistencia de jobs (sobreviven reinicios), soporte de clustering y configuración dinámica.

## 14. Configuración y perfiles

`application.yml` por perfiles:

1. `dev`: CORS permisivo, `show-sql: true`, `ddl-auto: update`, logs detallados.
2. `prod`: CORS restrictivo, `show-sql: false`, `ddl-auto: validate` (Flyway gestiona el esquema), logs mínimos.
3. `test`: H2 en memoria, Flyway deshabilitado, `ddl-auto: create-drop`.

Variables de entorno con `${VARIABLE:default}`:

```yaml
jwt:
  secret: ${JWT_SECRET}

datasource:
  url: ${DB_URL:jdbc:postgresql://localhost:5432/pricewise_db}
  username: ${DB_USERNAME:postgres}
  password: ${DB_PASSWORD}
```

En perfil `prod` el `JWT_SECRET` no tiene valor por defecto: la aplicación falla al arrancar si no está configurado.

Activación del perfil con `SPRING_PROFILES_ACTIVE=prod` o `--spring.profiles.active=prod`.

## 15. Pool de conexiones (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

`max-lifetime` por debajo del timeout del servidor de BD para evitar conexiones cerradas por firewall o por el propio PostgreSQL. `leak-detection-threshold` ayuda a detectar conexiones no devueltas a tiempo.

## 16. Caché

Spring Cache simple en memoria, activado con `@EnableCaching`. Anotaciones usadas:

1. `@Cacheable` en consultas que se repiten (`getCategories`, `getBrands`).
2. `@CacheEvict` al crear o actualizar productos.

Limitaciones del caché simple: se pierde al reiniciar y no se comparte entre instancias. La integración con Redis se retiró del MVP. Plan en `MEJORAS_FUTURAS.md`.

## 17. Testing

Tres niveles:

1. Unitarios con JUnit 5, Mockito y AssertJ. Mock de repositorios e injección con `@InjectMocks`.
2. Integración con `@SpringBootTest` y `@ActiveProfiles("test")`. H2 en memoria.
3. End-to-end con `TestRestTemplate` o `MockMvc` para los flujos completos de auth y productos.

Anotaciones más usadas: `@Test`, `@DisplayName`, `@BeforeEach`, `@ParameterizedTest`, `@Mock`, `@InjectMocks`, `@MockBean`.

## Documentación relacionada

1. `README.md`: endpoints REST, instalación y configuración.
2. `SEGURIDAD.md`: informe de seguridad detallado.
3. `PATRONES.md`: reglas internas y gotchas resueltos.
4. `FLYWAY.md`: migraciones V1 a V8.
5. `MEJORAS_FUTURAS.md`: servicios retirados del MVP y plan de reintegración.
