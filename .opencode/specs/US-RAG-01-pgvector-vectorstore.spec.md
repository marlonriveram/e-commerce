# US-RAG-01 — Configuración de pgvector y VectorStore

## 1. Requisitos

**User story:** Como Desarrollador Backend quiero habilitar la extensión pgvector en PostgreSQL y configurar la tabla `vector_store`, para permitir el almacenamiento y búsqueda de embeddings numéricos en la misma base de datos del e-commerce.

**Contexto:** es la base de datos del pipeline RAG (US-RAG-02). La base de conocimientos de políticas necesita persistencia vectorial sin infraestructura externa. No hay lógica de negocio propia: es configuración de infraestructura (docker, dependencia, Flyway, YAML).

## 2. Cambios propuestos

- `docker-compose.yml`: cambiar la imagen del servicio `db` de `postgres:16` a `pgvector/pgvector:pg16`. La imagen oficial `postgres:16` **NO incluye la extensión `vector`**; `pgvector/pgvector:pg16` es el mismo PostgreSQL 16 con la extensión compilada. Mismos puertos, credenciales y volumen `postgres_data` (los datos se conservan).
- `pom.xml`: agregar `spring-ai-starter-pgvector-store` (versión gestionada por el BOM `spring-ai-bom` 2.0.0 ya presente en el proyecto).
- Migración Flyway **`V5__enable_pgvector.sql`**: `CREATE EXTENSION IF NOT EXISTS vector;` (idempotente). El único rol de Flyway es habilitar la extensión; la tabla la gestiona Spring AI (ver "Reglas de comportamiento").
- `application.yaml`: bloque `spring.ai.vectorstore.pgvector.*`.

## 3. Contrato

**Migración Flyway `V5__enable_pgvector.sql`:**

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

**Propiedades `application.yaml` (`spring.ai.vectorstore.pgvector`):**

| Propiedad | Valor | Nota |
|---|---|---|
| `initialize-schema` | `true` | Spring AI crea la tabla `vector_store` + índice HNSW al arrancar |
| `schema-name` | `public` | schema por defecto |
| `table-name` | `vector_store` | nombre por defecto del starter |
| `dimensions` | `<PENDIENTE>` | placeholder: se fija en US-RAG-02 al resolver el embedding model de Groq |
| `index-type` | `HNSW` | índice ANN recomendado por Spring AI para texto |
| `distance-type` | `COSINE_DISTANCE` | distancia coseno estándar para embeddings de texto |
| `batching-strategy` | `partitioned` | inserción por lotes del starter |

**Sin endpoints HTTP ni eventos RabbitMQ** — solo configuración de arranque.

## 4. Reglas de comportamiento

- **La extensión la crea Flyway (`IF NOT EXISTS`, idempotente)** y corre antes del arranque de Spring AI, garantizando que `vector` exista cuando el starter cree la tabla.
- **La tabla `vector_store` la crea Spring AI (`initialize-schema: true`), NO Flyway.** Es una tabla interna de `PgVectorStore` (`id`, `content`, `metadata`, `embedding VECTOR(n)`) cuyo formato exacto depende de la versión de la librería; dejar que el starter la cree evita drift entre el `schema.sql` de la librería y una migración manual.
- **Consecuencia:** `vector_store` no queda trackeada en `flyway_schema_history`. Si cambia `dimensions`, hay que recrear tabla/índice manualmente (aceptable: es data regenerable vía la ingesta de US-RAG-02).
- `dimensions` debe coincidir con el vector que devuelve el embedding model; se resuelve en US-RAG-02 (verificación con curl).
- **HNSW** (Hierarchical Navigable Small World): índice de vecinos cercanos aproximados (ANN) en varias capas; búsqueda de arriba hacia abajo, O(log n), buena precisión con datos que cambian. Alternativa IVFFlat descartada (requiere training y pierde recall).
- **Docker:** `docker compose up -d` recrea el contenedor `db` con la imagen nueva y reconecta el mismo volumen `postgres_data` → los datos existentes (usuarios, claims, historial, migraciones aplicadas) se conservan. La imagen vieja `postgres:16` queda en caché sin uso (se puede borrar con `docker image rm`). ⚠️ `docker compose down -v` SÍ borra los volúmenes.
- No se escriben secrets ni API keys en configuración de este spec.

## 5. Criterios de aceptación

- [ ] `CREATE EXTENSION IF NOT EXISTS vector;` presente en `V5__enable_pgvector.sql` y ejecutada una sola vez (registrada en `flyway_schema_history`).
- [ ] `spring-ai-starter-pgvector-store` agregado en `pom.xml` (sin versión, gestionado por BOM).
- [ ] Propiedades `spring.ai.vectorstore.pgvector` configuradas en `application.yaml` (con `dimensions` placeholder documentado).
- [ ] `docker compose up -d` levanta PostgreSQL 16 con la extensión disponible y **conserva los datos existentes** del volumen `postgres_data`.
- [ ] La aplicación arranca sin errores (`./mvnw spring-boot:run`).
- [ ] La tabla `vector_store` existe con índice HNSW — verificación psql:
  - `SELECT extversion FROM pg_extension WHERE extname = 'vector';`
  - `\d vector_store` (columna `embedding vector`, índice `vector_store_hnsw_index`).

## 6. Tests esperados

- Sin lógica de negocio propia: no aplica el estándar de tests unitarios Mockito (no hay clase propia que probar).
- La validación es de infraestructura y manual (psql + logs de arranque + docker compose), siguiendo la política del repo de no usar `@SpringBootTest`.
- La verificación de la creación real de la tabla en este entorno queda cubierta por los criterios de aceptación de psql.