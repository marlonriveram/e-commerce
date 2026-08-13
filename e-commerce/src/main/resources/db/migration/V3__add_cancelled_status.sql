-- ============================================================
-- V3: AGREGAR ESTADO CANCELLED (US-03)
-- El enum EnumStatus ahora incluye CANCELLED. Los CHECK constraints
-- de V1 NO lo permitían, así que hay que recrearlos agregando 'CANCELLED'.
--
-- Regla de Flyway: NUNCA editar V1/V2 que ya se ejecutaron;
-- los cambios van en una migración nueva (V3, V4, ...).
-- ============================================================

-- Tabla CLAIMS: permitir 'CANCELLED' en el estado
ALTER TABLE claims
    DROP CONSTRAINT claims_status_check;

ALTER TABLE claims
    ADD CONSTRAINT claims_status_check CHECK (status IN
        ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REFUNDED', 'CANCELLED'));

-- Tabla CLAIM_HISTORY: permitir 'CANCELLED' en new_status
ALTER TABLE claim_history
    DROP CONSTRAINT claim_history_new_status_check;

ALTER TABLE claim_history
    ADD CONSTRAINT claim_history_new_status_check CHECK (new_status IN
        ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REFUNDED', 'CANCELLED'));

-- Tabla CLAIM_HISTORY: permitir 'CANCELLED' en previous_status
ALTER TABLE claim_history
    DROP CONSTRAINT claim_history_previous_status_check;

ALTER TABLE claim_history
    ADD CONSTRAINT claim_history_previous_status_check CHECK (previous_status IN
        ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REFUNDED', 'CANCELLED'));
