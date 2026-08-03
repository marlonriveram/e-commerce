-- ============================================================
-- V2: DATOS DE PRUEBA (SEED)
-- Se ejecuta UNA sola vez (después de V1). Aquí metes tus datos
-- de prueba: usuarios, claims en todos los estados e historial.
--
-- CONSEJO: cuando agregues una entidad nueva, crea V3__..., V4__...
-- y Flyway los correrá en orden sin volver a tocar V1 y V2.
--
-- USO DE IDS EXPLÍCITOS:
--   Ponemos ids fijos para poder referenciar claims → user y
--   claim_history → claim sin ambigüedad. Al final reseteamos las
--   secuencias con setval() para que la app siga generando ids
--   nuevos sin chocar con estos.
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- USUARIOS: 2 clientes, 1 soporte, 1 finanzas
-- (la API solo crea CUSTOMER, así que estos roles especiales
--  solo pueden existir por seed o insert manual)
-- ────────────────────────────────────────────────────────────
INSERT INTO users (id, name, email, role) VALUES
    (1, 'Juan Pérez',        'juan.perez@ejemplo.com',    'CUSTOMER'),
    (2, 'María García',      'maria.garcia@ejemplo.com',  'CUSTOMER'),
    (3, 'Agente Soporte',    'soporte@ejemplo.com',       'SUPPORT'),
    (4, 'Usuario Finanzas',  'finanzas@ejemplo.com',      'FINANCE');

-- ────────────────────────────────────────────────────────────
-- CLAIMS en TODOS los estados para poder probar cada endpoint:
--   1 PENDING    → se puede revisar (PATCH /review)
--   2 IN_REVIEW  → se puede aprobar o rechazar (PATCH /review)
--   3 APPROVED   → se puede reembolsar (PATCH /refund)
--   4 REJECTED   → estado terminal, no acepta transiciones
--   5 REFUNDED   → estado terminal, no acepta transiciones
-- ────────────────────────────────────────────────────────────
INSERT INTO claims (id, order_id, description, status, user_id, created_at) VALUES
    (1, 1001, 'Pedido incompleto: falta un artículo',              'PENDING',   1, NOW()),
    (2, 1002, 'El producto llegó dañado',                         'IN_REVIEW', 1, NOW()),
    (3, 1003, 'Cobro duplicado en mi tarjeta',                    'APPROVED',  2, NOW()),
    (4, 1004, 'Nunca recibí el reembolso del mes pasado',         'REJECTED',  1, NOW()),
    (5, 1005, 'Me llegó un producto distinto al solicitado',      'REFUNDED',  2, NOW());

-- ────────────────────────────────────────────────────────────
-- HISTORIAL de cambios (ClaimHistory) coherente con cada claim:
--   claim 2: PENDING → IN_REVIEW
--   claim 3: PENDING → IN_REVIEW → APPROVED
--   claim 4: PENDING → IN_REVIEW → REJECTED
--   claim 5: PENDING → IN_REVIEW → APPROVED → REFUNDED
--   claim 1: sin historial (se creó en PENDING y nadie lo tocó)
-- ────────────────────────────────────────────────────────────
INSERT INTO claim_history (id, claim_id, previous_status, new_status, changed_by_user, changed_at) VALUES
    (1, 2, 'PENDING',  'IN_REVIEW', 3, NOW()),
    (2, 3, 'PENDING',  'IN_REVIEW', 3, NOW()),
    (3, 3, 'IN_REVIEW', 'APPROVED', 3, NOW()),
    (4, 4, 'PENDING',  'IN_REVIEW', 3, NOW()),
    (5, 4, 'IN_REVIEW', 'REJECTED', 3, NOW()),
    (6, 5, 'PENDING',  'IN_REVIEW', 3, NOW()),
    (7, 5, 'IN_REVIEW', 'APPROVED', 3, NOW()),
    (8, 5, 'APPROVED',  'REFUNDED', 4, NOW());

-- ────────────────────────────────────────────────────────────
-- RESETEAR SECUENCIAS
-- Como insertamos ids explícitos (1, 2, 3...), el contador interno
-- de Postgres sigue en 1 y el próximo INSERT de la app fallaría por
-- "duplicate key". setval() adelanta la secuencia al máximo usado.
-- ────────────────────────────────────────────────────────────
SELECT setval('users_id_seq',          (SELECT MAX(id) FROM users));
SELECT setval('claims_id_seq',         (SELECT MAX(id) FROM claims));
SELECT setval('claim_history_id_seq',  (SELECT MAX(id) FROM claim_history));
