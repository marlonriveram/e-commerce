-- ============================================================
-- V4: AGREGAR METADATA IA A CLAIMS (US-AI-02)
-- Categorización automática y análisis de sentimiento de reclamos.
-- La IA (Groq) genera estos campos de forma ASÍNCRONA tras la
-- creación del claim, por eso son NULLABLE: al insertarse el claim
-- están vacíos y el consumidor de la cola claim.ai.queue los rellena.
-- ============================================================

ALTER TABLE claims
    ADD COLUMN category VARCHAR(50),
    ADD COLUMN urgency VARCHAR(20),
    ADD COLUMN summary  VARCHAR(500);
