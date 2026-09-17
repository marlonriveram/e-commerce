-- ============================================================
-- V5: HABILITAR LA EXTENSIÓN PGVECTOR (US-RAG-01)
-- Flyway ejecuta este script UNA sola vez. Habilita la extensión
-- `vector` necesaria para el VectorStore pgvector de Spring AI.
--
-- La tabla `vector_store` NO se crea aquí: la crea el propio
-- Spring AI al arrancar (spring.ai.vectorstore.pgvector
-- initialize-schema=true) con el formato exacto que espera la
-- librería (columnas + índice HNSW vía distancia coseno).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;