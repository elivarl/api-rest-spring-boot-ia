# AGENTS.md · API de productos `catalog` en 3 capas

Este archivo es el diseño del proyecto, escrito para el agente. Léelo completo antes de generar código.
Si un pedido contradice este archivo, no lo resuelvas por tu cuenta: dilo y pregunta.

## Stack

- Java 21 · Spring Boot 4.1.x · Maven con wrapper (`./mvnw`)
- Spring MVC, Spring Data JPA, Bean Validation, MySQL 8.4, Lombok
- `groupId` `com.icodeap` · `artifactId` `catalog` · paquete raíz `com.icodeap.catalog` · clase principal `CatalogApplication`

## Comandos

| Para qué | Comando |
| --- | --- |
| Levantar solo la base | `docker compose up -d db` |
| Compilar | `./mvnw -q compile` |
| Tests (`CatalogApplicationTests` necesita la base levantada) | `./mvnw test` |
| Ejecutar la API | `./mvnw spring-boot:run` |
| Construir la imagen (también corre los tests) | `./mvnw spring-boot:build-image` → `catalog:0.0.1-SNAPSHOT` |
| API + base en Docker | `docker compose up -d` |

## Capas y archivos

Solo existen estos archivos de código. No crees otros sin preguntar.

| Paquete (`com.icodeap.catalog.…`) | Clase | Qué hace |
| --- | --- | --- |
| `controller` | `ProductController` | Habla HTTP: recibe DTOs, los valida, llama al servicio y devuelve DTOs con su código. Sin lógica de negocio |
| `service` | `ProductService` (interfaz) | Contrato del negocio. Trabaja con entidades |
| `service.impl` | `ProductServiceImpl` | Reglas de negocio y transacciones. Lanza `ResourceNotFoundException` |
| `repository` | `ProductRepository` | `extends JpaRepository<Product, Long>`, sin métodos extra |
| `entity` | `Product` | Tabla `products` |
| `dto` | `ProductRequest`, `ProductResponse` | Records. Entrada con validaciones y salida |
| `exception` | `ResourceNotFoundException`, `ErrorResponse`, `GlobalExceptionHandler` | Errores con un solo formato |

Quién puede usar a quién:

- `controller` → `service`, `dto` (y `entity` solo para pasar el resultado de `toEntity()` al servicio y el del servicio a `fromEntity()`)
- `service` → `repository`, `entity`, `exception`
- `dto` → `entity`
- Prohibido: `controller` → `repository` · `service` → `dto` · `entity` → cualquier otra capa

## Contrato de la API

Base: `/api/v1/products`. No hay otros endpoints.

| Método | Ruta | Método en `ProductController` | Cuerpo | Éxito | Errores |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/v1/products` | `createProduct` | `ProductRequest` | `201` + `ProductResponse` | `400` |
| GET | `/api/v1/products` | `getAllProducts` | — | `200` + `List<ProductResponse>` | — |
| GET | `/api/v1/products/{id}` | `getProductById` | — | `200` + `ProductResponse` | `400` (id no numérico), `404` |
| PUT | `/api/v1/products/{id}` | `updateProduct` | `ProductRequest` | `200` + `ProductResponse` | `400`, `404` |
| DELETE | `/api/v1/products/{id}` | `deleteProduct` | — | `204` sin cuerpo | `400`, `404` |

Cualquier error no previsto responde `500`.

## Modelo

### `Product` (entidad)

Lombok, exactamente: `@Data @NoArgsConstructor @AllArgsConstructor @Builder`. Además `@Entity` y `@Table(name = "products")`.

| Campo | Tipo | Columna | Validación en `ProductRequest` |
| --- | --- | --- | --- |
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | No va en `ProductRequest` |
| `name` | `String` | `nullable = false, length = 120` | `@NotBlank`, `@Size(max = 120)` |
| `description` | `String` | `length = 500` | `@Size(max = 500)` (opcional) |
| `category` | `String` | `nullable = false, length = 60` | `@NotBlank`, `@Size(max = 60)` |
| `stock` | `Integer` | `nullable = false` | `@NotNull`, `@PositiveOrZero` |
| `active` | `Boolean` | `nullable = false` | `@NotNull` |

Los límites de `@Size` y de `length` son los mismos. Si no coinciden, un texto que pasa la validación falla en la base y el cliente recibe un 500 en vez de un 400.

### DTOs (paquete `dto`, records)

- `ProductRequest(name, description, category, stock, active)` con las validaciones de la tabla y el método `Product toEntity()`.
- `ProductResponse(id, name, description, category, stock, active)` con el método `static ProductResponse fromEntity(Product product)`.
- Cada anotación de validación lleva su mensaje en español: `@NotBlank(message = "El nombre es obligatorio")`. Sin mensaje, el texto cambia según el idioma de la máquina.

## Servicio

Firmas exactas de `ProductService`:

```java
Product createProduct(Product product);
List<Product> getAllProducts();
Product getProductById(Long id);
Product updateProduct(Long id, Product productDetails);
void deleteProduct(Long id);
```

En `ProductServiceImpl`:

- Producto inexistente: `throw new ResourceNotFoundException("Producto no encontrado con el ID: " + id)`, dentro de `getProductById`.
- `updateProduct` y `deleteProduct` buscan con `getProductById`, así el 404 sale de un solo lugar.
- `updateProduct` copia `name`, `description`, `category`, `stock` y `active`. Nunca el `id`.
- `@Transactional(readOnly = true)` en las lecturas y `@Transactional` en las escrituras, de `org.springframework.transaction.annotation`.

## Errores

`ErrorResponse` (Lombok `@Data @AllArgsConstructor @NoArgsConstructor`, más un constructor de 3 argumentos sin `errors`):

| Campo | Tipo | Nota |
| --- | --- | --- |
| `status` | `int` | |
| `message` | `String` | |
| `timestamp` | `LocalDateTime` | |
| `errors` | `Map<String, String>` | Solo en errores de validación. `@JsonInclude(JsonInclude.Include.NON_NULL)` en la clase |

Ejemplo de 400:

```json
{
  "status": 400,
  "message": "Datos de entrada inválidos",
  "timestamp": "2026-11-13T18:00:00",
  "errors": { "name": "El nombre es obligatorio", "stock": "El stock no puede ser negativo" }
}
```

`GlobalExceptionHandler` (`@RestControllerAdvice`):

| Excepción | Código | `message` |
| --- | --- | --- |
| `ResourceNotFoundException` | 404 | El de la excepción |
| `NoResourceFoundException` (`org.springframework.web.servlet.resource`) | 404 | El de la excepción |
| `MethodArgumentNotValidException` | 400 | `Datos de entrada inválidos`, con `errors` campo → mensaje (el primero de cada campo) |
| `HttpMessageNotReadableException` | 400 | `El cuerpo de la petición no es un JSON válido` |
| `MethodArgumentTypeMismatchException` | 400 | `El parámetro '<nombre>' tiene un formato inválido` |
| `Exception` | 500 | `Error interno del servidor`. Registra la excepción con `log.error` (`@Slf4j`). Nunca `ex.getMessage()` en la respuesta |

## Reglas y por qué

1. **El controlador nunca recibe ni devuelve `Product`.** Si la entidad es tu contrato, cada cambio en la tabla rompe a tus clientes, y el cliente puede mandar campos que no le tocan, como el `id`.
2. **Inyección por constructor:** `private final` y `@RequiredArgsConstructor`. Nunca `@Autowired` en campos. Las dependencias quedan a la vista y la clase se prueba sin Spring.
3. **Validación en el borde:** `@Valid` en cada `@RequestBody`. Un dato inválido no llega al servicio.
4. **Errores centralizados:** sin `try/catch` en los controladores. Todo error sale por `GlobalExceptionHandler`, con un solo formato.
5. **El 500 no muestra detalles internos.** Un mensaje de SQL o un stack trace le cuenta a cualquiera cómo está hecho tu sistema. El detalle va al log.
6. **El repositorio no tiene lógica** ni métodos que nadie usa.
7. **Nada fuera de este archivo sin preguntar:** ni dependencias, ni archivos, ni endpoints, ni configuración.

## Spring Boot 4, no Spring Boot 3

Lo que más has visto es Spring Boot 3. Este proyecto es Spring Boot 4.

| Usa | No uses |
| --- | --- |
| `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Starters de test por módulo: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test` | Agregar dependencias de test sueltas (JUnit, Mockito, AssertJ ya vienen) |
| `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`) | `@MockBean` (ya no existe en Spring Boot 4) |
| `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` |
| Jackson 3: `tools.jackson.databind.*`. Las anotaciones siguen en `com.fasterxml.jackson.annotation.*` | `com.fasterxml.jackson.databind.*` |
| `jakarta.*` | `javax.*` |
| `org.springframework.transaction.annotation.Transactional` | `jakarta.transaction.Transactional` |

En los tests, escribe el JSON en text blocks (`"""`). No uses `ObjectMapper`.

## Tests

| Clase | Tipo | Qué prueba |
| --- | --- | --- |
| `CatalogApplicationTests` | `@SpringBootTest` (ya existe, no la cambies) | Que el contexto arranca con la base real |
| `service.impl.ProductServiceImplTest` | Unitario: `@ExtendWith(MockitoExtension.class)`, `@Mock ProductRepository`, `@InjectMocks ProductServiceImpl`. Sin Spring | 404 cuando no existe, que `updateProduct` copia los campos y conserva el `id`, que `deleteProduct` no borra si no existe |
| `controller.ProductControllerTest` | `@WebMvcTest(ProductController.class)` + `@MockitoBean ProductService` | 201 con cuerpo, 400 con `errors` por campo, 404, 400 con id no numérico, 500 sin detalles internos |

Nombres de métodos: `metodo_cuandoCondicion_resultado`, por ejemplo `getProductById_whenNotFound_returns404`.

## Fuera de alcance

No agregues: Actuator, perfiles (`application-*.yaml`), ajustes de HikariCP, Dockerfile, `spring-boot-docker-compose`, seguridad, paginación, H2, Testcontainers, OpenAPI, MapStruct ni clases mapper aparte, Flyway.
No están mal: son lo que se agrega cuando la API va a producción, y aquí no vamos a producción.

## Cómo trabajar

- Una capa por pedido. Crea o cambia solo los archivos que se piden.
- Antes de escribir, di qué archivos vas a crear o modificar.
- Al terminar, compila con `./mvnw -q compile`, lista los archivos y **detente**. No sigas con la siguiente capa.
- Si este archivo no alcanza para decidir algo, pregunta en vez de suponer.
