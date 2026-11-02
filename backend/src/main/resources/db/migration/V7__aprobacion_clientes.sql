-- ============================================================================
-- V7__aprobacion_clientes.sql
-- Issue: S5-09 — Aprobación y rechazo de clientes registrados desde el portal.
--
-- estado_aprobacion distingue el flujo de revisión de las cuentas del portal:
--   PENDIENTE  → se registró en el portal y espera revisión (activo = FALSE).
--   APROBADO   → el personal aprobó la cuenta (activo = TRUE).
--   RECHAZADO  → el personal rechazó la cuenta (activo = FALSE).
--   NULL       → cliente dado de alta en mostrador o registrado antes de S5-09;
--                no participa en el flujo y conserva su comportamiento actual.
--
-- PRESERVACIÓN DE REGISTROS EXISTENTES:
-- Las columnas se agregan como NULL, sin UPDATE masivo: los clientes que ya
-- existían (incluidos los del portal) conservan su acceso tal como estaba.
--
-- COMPATIBILIDAD: MySQL 8.0.16+ y H2 (MODE=MySQL).
-- ============================================================================

ALTER TABLE clientes
    ADD COLUMN estado_aprobacion VARCHAR(20) NULL DEFAULT NULL
        COMMENT 'PENDIENTE, APROBADO o RECHAZADO; NULL = alta en mostrador o previa a S5-09'
        AFTER activo;

ALTER TABLE clientes
    ADD COLUMN fecha_revision DATETIME NULL DEFAULT NULL
        COMMENT 'Fecha y hora en que el personal aprobó o rechazó la cuenta'
        AFTER estado_aprobacion;

ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_estado_aprobacion
        CHECK (estado_aprobacion IS NULL
               OR estado_aprobacion IN ('PENDIENTE', 'APROBADO', 'RECHAZADO'));

-- La consulta de pendientes filtra por esta columna.
CREATE INDEX idx_clientes_estado_aprobacion ON clientes (estado_aprobacion);
