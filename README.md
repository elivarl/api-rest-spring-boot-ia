# Crea una API REST con Spring Boot 4 e IA, sin perder el control de tu código

> **Este repositorio es el resultado final de la clase, para que compares con lo tuyo.**
> La guía es el video: síguelo paso a paso y arma tu propio proyecto desde cero.
> 👉 [Ver la clase]([ENLACE A LA CLASE])

Una API REST de productos en 3 capas, construida con un agente de IA siguiendo un método de tres pasos: **tú diseñas, la IA genera, tú revisas**.

---

## Qué hay aquí

| Archivo | Para qué sirve |
| --- | --- |
| `AGENTS.md` | Las reglas de diseño que lee el agente: capas, nombres, contrato de la API, validaciones y errores. Puedes adaptarlo a tu propio proyecto |
| `CLAUDE.md` | Hace que Claude Code lea `AGENTS.md`. Otros agentes, como Antigravity, leen `AGENTS.md` directamente |
| `PROMPTS.md` | Los prompts de la clase, en orden, para que los copies |
| `compose.yaml` | MySQL y la API en Docker |
| `src/` | El código que generó el agente en la clase, con las correcciones que se hicieron en el video |

## Stack

Java 21 · Spring Boot 4.2 · Spring MVC · Spring Data JPA · Bean Validation · MySQL 8.4 · Lombok · Docker

## Requisitos

| Herramienta | Para qué |
| --- | --- |
| JDK 21 | Compilar y correr la API. No necesitas instalar Maven: el proyecto trae `mvnw` |
| Docker Desktop | MySQL, construir la imagen y levantar todo. Ya incluye Docker Compose |
| Un agente de IA | En la clase uso Claude Code. Cualquier agente que lea `AGENTS.md` sirve |
| Postman o cURL | Probar los endpoints |

> La primera vez que construyas la imagen se descargan varios cientos de MB. Hazlo con buena conexión.

---

## Seguir la clase desde cero

1. **Genera el proyecto** en [Spring Initializr](https://start.spring.io):

   | Campo | Valor |
   | --- | --- |
   | Project / Language | Maven / Java |
   | Spring Boot | 4.2.x |
   | Group / Artifact | `com.icodeap` / `catalog` |
   | Configuration | YAML |
   | Java | 21 |
   | Dependencias | Spring Web, Spring Data JPA, Validation, MySQL Driver, Lombok, Spring Boot DevTools |

2. **Copia a la raíz** de tu proyecto, junto al `pom.xml`, estos 3 archivos de este repo: `AGENTS.md`, `CLAUDE.md` y `compose.yaml`.
3. **Abre la terminal en la carpeta de tu proyecto** (donde está el `pom.xml`) y levanta la base:
   ```bash
   docker compose up -d db
   ```
4. **Inicia tu agente desde esa misma carpeta** y sigue los prompts de [`PROMPTS.md`](PROMPTS.md), uno por uno.

---

## Ejecutar este repositorio

```bash
docker compose up -d db                 # la base
./mvnw test                             # tests (necesitan la base levantada)
./mvnw spring-boot:run                  # la API en http://localhost:8080
```

Todo en Docker, sin Dockerfile:

```bash
./mvnw spring-boot:build-image          # construye la imagen catalog:0.0.1-SNAPSHOT (corre los tests)
docker compose up -d                    # API + MySQL
```

> En Windows usa `.\mvnw.cmd` en lugar de `./mvnw`.

Para borrar todo, incluidos los datos: `docker compose down -v`.

---

## Endpoints

Base: `/api/v1/products`

| Método | Ruta | Éxito | Errores |
| --- | --- | --- | --- |
| POST | `/api/v1/products` | `201` con el producto creado | `400` |
| GET | `/api/v1/products` | `200` con la lista | — |
| GET | `/api/v1/products/{id}` | `200` con el producto | `400` (id no numérico), `404` |
| PUT | `/api/v1/products/{id}` | `200` con el producto actualizado | `400`, `404` |
| DELETE | `/api/v1/products/{id}` | `204` sin cuerpo | `400`, `404` |

Cuerpo para crear o actualizar:

```json
{
  "name": "Teclado Mecánico RGB",
  "description": "Teclado mecánico con switches red",
  "category": "Periféricos",
  "stock": 50,
  "active": true
}
```

Error de validación:

```json
{
  "status": 400,
  "message": "Datos de entrada inválidos",
  "timestamp": "2026-11-23T11:00:00",
  "errors": { "name": "El nombre es obligatorio" }
}
```

---

## Problemas frecuentes

**El agente no sigue las reglas de `AGENTS.md`.**
Seguramente lo iniciaste desde otra carpeta. Ciérralo, abre la terminal en la carpeta del proyecto (donde está el `pom.xml`) y vuelve a iniciarlo. En Claude Code, el comando `/memory` muestra qué archivos de reglas cargó.

**En Windows, `./mvnw` falla pero desde tu terminal compila.**
Pídele al agente que use el script de Windows: `.\mvnw.cmd` (en Claude Code: `cmd //c ".\\mvnw.cmd -q compile"`). El `CLAUDE.md` de este repo ya lo indica.

**`PKIX path building failed` al compilar o construir la imagen.**
Tu antivirus está inspeccionando el tráfico HTTPS y Java no confía en su certificado. Desactiva el escaneo HTTPS del antivirus mientras se descargan las dependencias, o agrega `repo.maven.apache.org` a sus excepciones. Las dependencias quedan en caché: solo pasa la primera vez.

**La API no conecta a MySQL dentro de Docker.**
Dentro de Docker el host de la base es `db`, no `localhost`. Eso ya lo define `compose.yaml` con `SPRING_DATASOURCE_URL`.

---

## ¿Y ahora?

La API funciona, pero corre solo en tu máquina.

- **¿Cómo llega a producción sin desplegarla a mano cada vez?** → [Curso de CI/CD con Spring Boot, GitHub Actions y AWS]([https://devs-icodeap.wisboo.com/product/curso-ci-cd-con-spring-boot-github-actions-y-aws?utm_campaign=lead-api-ia])
- **¿Qué pasa con tus 3 capas si mañana cambias la base de datos o agregas un modelo de IA?** → [Curso de Arquitectura Hexagonal]([https://devs-icodeap.wisboo.com/product/spring-boot-avanzado-ecommerce-con-arquitectura-hexagonal-y-buenas-practicas?utm_campaign=lead-api-ia])

---

Hecho por **Elivar Largo** · [iCodeap](https://icodeap.com) · Con IA, pero entendiendo.
