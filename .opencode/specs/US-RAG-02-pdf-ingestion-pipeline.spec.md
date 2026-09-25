# US-RAG-02 — Pipeline de ingesta automática de PDFs desde Resources

## 1. Requisitos

**User story:** Como Desarrollador Backend quiero un componente ejecutable al inicio de la app que lea los archivos PDF de la carpeta `resources/docs/` y los indexe en pgvector, para cargar la base de conocimientos de políticas del e-commerce sin intervención manual.

**Contexto:** se apoya en US-RAG-01 (`VectorStore` pgvector + tabla `vector_store`). La ingesta es **síncrona y al arranque** (`CommandLineRunner`); migrar a eventos asíncronos queda como follow-up.

## 2. Cambios propuestos

- `src/main/resources/docs/politicas.pdf` — política(s) del e-commerce (base de conocimientos).
- `pom.xml`: agregar **`spring-ai-document-reader-pdf`**. ⚠️ En Spring AI 2.0 el artefacto `spring-ai-pdf-document-reader` (1.x) **no existe**; el nombre correcto para esta línea es `spring-ai-document-reader-pdf` (versión gestionada por BOM).
- Sub-paquete **`ai/rag/`** (nuevo dentro del módulo `ai/`, auto-contenido, espejo de `ai/claimclassifier`):
  - `application/service/PdfIngestionService.java` — orquesta: leer → split → verificación anti-duplicados → `vectorStore.add()`.
  - `infrastructure/PdfIngestionRunner.java` — `@Component implements CommandLineRunner` que llama al servicio al arranque.
  - `infrastructure/` helper de verificación de documento ya indexado (JdbcTemplate contra `vector_store`) si se opta por la verificación por metadata (opción 1 de "Reglas de comportamiento").
- Resolver embedding model (nueva tarea): verificar vía curl qué modelo de embeddings soporta la cuenta Groq (igual que se hizo con `gpt-oss-120b` para el chat) y configurar:
  - `spring.ai.openai.embedding.options.model`
  - `spring.ai.vectorstore.pgvector.dimensions` con la dimensión real devuelta (desbloquea el placeholder de US-RAG-01).

## 3. Contrato

- **Sin endpoints HTTP y sin eventos RabbitMQ** — ejecución local en el arranque de la aplicación.
- **Beans utilizados (Spring AI 2.0):**
  - `PagePdfDocumentReader` — lector de PDF página por página (provisto por `spring-ai-document-reader-pdf`).
  - `TokenCountTextSplitter` — división en chunks. ⚠️ `TokenTextSplitter` está deprecado/eliminado en 2.0; el nombre correcto es `TokenCountTextSplitter`.
  - `VectorStore` — el bean pgvector de US-RAG-01.
  - `EmbeddingModel` — auto-configurado por `spring-ai-starter-model-openai` apuntando a Groq.
- **Persistencia:** `vectorStore.add(List<Document>)` inserta los chunks con metadata trazable: `{source: "politicas.pdf", chunkIndex, sha256}`.
- **Ejecución:** `CommandLineRunner.run()` tras el arranque del contexto Spring.

## 4. Reglas de comportamiento

- **No duplicados en cada arranque.** Antes de indexar se verifica si los documentos ya fueron ingeridos. Opciones:
  1. **(Recomendada)** Probar contra `vector_store` si existe metadata `source = "politicas.pdf"` (JdbcTemplate/helper de infraestructura). Si hay filas → `log.info("Documento ya indexado")` y se omite la ingesta.
  2. (Alternativa simple) Si el store no está vacío → se omite la ingesta.
- **Fallos no fatales.** Si falta el PDF, falla la lectura o la división, se registra `log.warn`/`log.error` y la aplicación **sigue arrancando** (la excepción no debe salir de `run()`). Espejo de la filosofía del módulo IA del repo (cola limpia / no tumbar el arranque).
- **Si el embedding model de Groq no está disponible:** `log.error` con instrucción de configurar el modelo y se omite la ingesta (no romper el arranque).
- **Split:** `TokenCountTextSplitter` con chunk ~512 tokens y solapamiento ~50 tokens (contexto entre chunks, tamaño base razonable para RAG).
- Los PDFs se leen desde `resources/docs/` (classpath).
- No se escriben secrets ni API keys.

## 5. Criterios de aceptación

- [ ] `src/main/resources/docs/politicas.pdf` presente.
- [ ] `spring-ai-document-reader-pdf` en `pom.xml` (gestionado por BOM; notar que NO es `spring-ai-pdf-document-reader`).
- [ ] `PdfIngestionRunner` es un `@Component` que implementa `CommandLineRunner` y ejecuta la ingesta al arrancar.
- [ ] El PDF se procesa con `PagePdfDocumentReader` y se divide con `TokenCountTextSplitter`.
- [ ] Los chunks se insertan en `vector_store` con `vectorStore.add(documents)` — verificable: `SELECT count(*) FROM vector_store;` > 0 y metadata `source = 'politicas.pdf'`.
- [ ] **Segundo arranque:** NO se insertan duplicados (log de "ya indexado"; count se mantiene).
- [ ] Si falta el PDF o falla la ingesta, la aplicación arranca igual (no falla el arranque).
- [ ] Verificado con curl el modelo de embeddings de Groq y configurados `spring.ai.openai.embedding.options.model` y `spring.ai.vectorstore.pgvector.dimensions` (desbloquea US-RAG-01).
- [ ] Metadata de cada chunk traza el origen (`source`).

## 6. Tests esperados

Estándar del proyecto (JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, sin `@SpringBootTest`). Clase: `PdfIngestionServiceTest`.

- `shouldIndexDocuments_WhenPdfExistsAndStoreEmpty` — dado un PDF y store vacío, se lee, se divide y se llama `vectorStore.add(...)` una vez.
- `shouldSkipIngestion_WhenDocumentsAlreadyIndexed` — dado que el documento ya está indexado (metadata `source` presente), NO se llama a `vectorStore.add(...)`.
- `shouldNotFailStartup_WhenIngestionFails` — dado un fallo de lectura/división/embedding, la excepción se captura y se loguea (no se propaga desde el runner).