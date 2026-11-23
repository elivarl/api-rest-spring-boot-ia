# Prompts de la clase

Estos son los prompts que uso en el video, en el mismo orden. Cópialos uno por uno.

**Cómo usarlos:**
- Un prompt por capa. Si le pides todo de una vez, generará 15 archivos y no vas a revisar ninguno.
- Después de cada prompt, **revisa** lo que generó con la lista «Revisa» y hazle la **pregunta** al agente. Si no entiendes su respuesta, todavía no entiendes esa capa.
- Antes de empezar: los archivos `AGENTS.md`, `CLAUDE.md` y `compose.yaml` en la raíz del proyecto, la base levantada (`docker compose up -d db`) y el agente iniciado desde la carpeta del proyecto.

> En Windows, compila con `.\mvnw.cmd -q compile` en lugar de `./mvnw -q compile`.

---

## Prompt 0 · Configuración

```text
Lee AGENTS.md completo.

Configura src/main/resources/application.yaml para conectarse a la base de compose.yaml:
- spring.application.name: catalog
- datasource con valores por defecto para localhost:3306/catalog_db, usuario app y contraseña local-app-pass,
  que se puedan cambiar con SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME y SPRING_DATASOURCE_PASSWORD
- JPA con ddl-auto update y show-sql

Sin perfiles, sin Actuator y sin ajustes de HikariCP. No toques otros archivos. Muéstrame el archivo y detente.
```

**Revisa:**
- La URL usa `${SPRING_DATASOURCE_URL:...}`. Si está escrita fija, la API no podrá conectarse a la base dentro de Docker.
- No hay `profiles`, `management` ni `hikari`.

---

## Prompt 1 · Entidad y repositorio

```text
Crea solo estos dos archivos, según las secciones «Capas y archivos» y «Modelo» de AGENTS.md:
- com.icodeap.catalog.entity.Product
- com.icodeap.catalog.repository.ProductRepository

Todavía no crees DTOs, servicio, controlador ni excepciones.
Antes de escribir, dime qué archivos vas a crear. Al terminar, compila con ./mvnw -q compile y detente.
```

**Revisa:**
- `@Table(name = "products")` y `GenerationType.IDENTITY`.
- Cada `@Column` con el `length` y el `nullable` de la tabla «Modelo».
- Imports de `jakarta.persistence`, no de `javax`.
- `ProductRepository` extiende `JpaRepository<Product, Long>` y no tiene métodos extra.

**Pregunta al agente:** ¿Por qué `ProductRepository` es una interfaz sin implementación? ¿Quién la implementa y cuándo?

---

## Prompt 2 · Servicio

```text
Crea, según la sección «Servicio» de AGENTS.md:
- com.icodeap.catalog.exception.ResourceNotFoundException (solo esta clase del paquete exception)
- com.icodeap.catalog.service.ProductService con las firmas exactas
- com.icodeap.catalog.service.impl.ProductServiceImpl

El servicio trabaja con Product, no con DTOs. No crees nada más.
Antes de escribir, dime qué archivos vas a crear. Al terminar, compila y detente.
```

**Revisa:**
- Las 5 firmas son idénticas a las de `AGENTS.md`.
- `updateProduct` y `deleteProduct` buscan con `getProductById`, así el 404 sale de un solo lugar.
- `updateProduct` copia los 5 campos y nunca el `id`.
- `@Transactional` es de `org.springframework.transaction.annotation`, no de `jakarta.transaction`.
- Inyección por constructor: `private final` y `@RequiredArgsConstructor`.

**Pregunta al agente:** ¿Por qué pusiste `@Transactional` en el servicio y no en el controlador ni en el repositorio? ¿Qué pasa si llamo a `deleteProduct` con un id que no existe?

---

## Prompt 3 · DTOs y controlador

```text
Crea, según las secciones «Contrato de la API» y «Modelo» de AGENTS.md:
- com.icodeap.catalog.dto.ProductRequest (record con validaciones y toEntity())
- com.icodeap.catalog.dto.ProductResponse (record con fromEntity(Product))
- com.icodeap.catalog.controller.ProductController

El controlador nunca recibe ni devuelve Product. No toques el servicio y no crees el manejo de excepciones todavía.
Antes de escribir, dime qué archivos vas a crear. Al terminar, compila y detente.
```

**Revisa:**
- Ningún método del controlador recibe ni devuelve `Product`.
- `@Valid` en los dos `@RequestBody`.
- 201 en POST y 204 en DELETE.
- Cada `@Size(max = …)` es igual al `length` de su columna, y cada validación tiene su `message`.
- `ProductRequest` no tiene `id`.

**Pregunta al agente:** ¿Por qué el `id` no está en `ProductRequest`? ¿Qué pasaría si el controlador recibiera `Product` directamente?

**Antes del siguiente prompt, pruébalo:** levanta la API (`./mvnw spring-boot:run`) y pide un producto que no existe:

```bash
curl -i http://localhost:8080/api/v1/products/99999
```

Responde **500**, no 404: todavía nadie convierte `ResourceNotFoundException` en un 404. Por eso existe el siguiente prompt.

---

## Prompt 4 · Manejo de errores

```text
Completa el paquete com.icodeap.catalog.exception según la sección «Errores» de AGENTS.md:
- ErrorResponse
- GlobalExceptionHandler

ResourceNotFoundException ya existe: no la cambies. No toques el controlador ni el servicio.
Antes de escribir, dime qué archivos vas a crear. Al terminar, compila y detente.
```

**Revisa:**
- El handler de `Exception` responde `Error interno del servidor` y registra el error con `log.error`. **Nunca** devuelve `ex.getMessage()`.
- El 400 de validación arma `errors` con un `Map` y `putIfAbsent`, no con `Collectors.toMap`.
- No hay `try/catch` en el controlador.

**Pregunta al agente:** ¿Por qué el 500 no devuelve el mensaje de la excepción? Si un campo tiene dos errores, ¿cuál queda en `errors`?

Repite la prueba de antes: ahora el mismo `curl` responde **404** con el mensaje del producto no encontrado.

---

## Prompt 5 · Tests

```text
Crea los tests de la sección «Tests» de AGENTS.md:
- com.icodeap.catalog.service.impl.ProductServiceImplTest
- com.icodeap.catalog.controller.ProductControllerTest

Crea solo los casos de la columna «Qué prueba», sin agregar otros:
3 tests en ProductServiceImplTest y 5 en ProductControllerTest.

Usa la tabla «Spring Boot 4, no Spring Boot 3». No cambies código de producción: si un test falla, explícame por qué y detente.
CatalogApplicationTests ya existe y no se toca.
Al terminar, ejecuta ./mvnw test (la base ya está levantada) y muéstrame el resultado.
```

**Revisa:**
- Maven muestra `Tests run: 9` (3 + 5 + el `CatalogApplicationTests` que ya existía). Si sale otro número, el agente no siguió la instrucción.
- `@MockitoBean`, no `@MockBean` (no existe en Spring Boot 4).
- `@WebMvcTest` importado de `org.springframework.boot.webmvc.test.autoconfigure`.
- El test del 400 verifica que el servicio **no** se llamó.
- El agente no agregó dependencias al `pom.xml`.

**Pregunta al agente:** ¿Por qué `ProductServiceImplTest` no necesita Spring y `ProductControllerTest` sí?

---

## Al final: Docker

Esto no lo hace el agente, lo haces tú:

```bash
./mvnw spring-boot:build-image          # construye la imagen sin Dockerfile (corre los tests)
docker compose up -d                    # API + MySQL
```

Y prueba los endpoints con Postman o cURL, incluido un 400 y un 404.
