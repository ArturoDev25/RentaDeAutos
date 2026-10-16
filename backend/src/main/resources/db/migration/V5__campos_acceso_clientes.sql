-- ============================================================================
-- V5__campos_acceso_clientes.sql
-- Issue: S4-01 (#120 / #118) — Campos de acceso al portal en `clientes` (Opción A)
--
-- NOTA ARQUITECTÓNICA SOBRE `correo` Y `uq_clientes_correo`:
-- La columna `correo VARCHAR(150) NULL` y la restricción `uq_clientes_correo UNIQUE (correo)`
-- ya fueron creadas originalmente en `V1__init_schema.sql` (líneas 65 y 75).
-- Por ende, esta migración NO recrea dicha columna ni índice para evitar errores
-- de columna o llave duplicada en MySQL 8.0 y H2.
--
-- PRESERVACIÓN DE REGISTROS EXISTENTES:
-- 1. No se ejecutan instrucciones destructivas (DROP TABLE, TRUNCATE, UPDATE masivo, DELETE).
-- 2. La columna `password_hash` se agrega como `NULL DEFAULT NULL`, por lo que todos
--    los registros preexistentes conservan `password_hash = NULL` de forma íntegra.
-- 3. NO se asignan contraseñas ficticias ni valores dummy a clientes existentes.
--
-- COMPATIBILIDAD: MySQL 8.0.16+ y H2 (MODE=MySQL).
-- ============================================================================

-- 1. Columna para almacenar el hash de contraseña (BCrypt) para acceso al portal.
--    Permanece NULL para clientes que únicamente han sido registrados en sucursal/mostrador.
ALTER TABLE clientes
    ADD COLUMN password_hash VARCHAR(255) NULL DEFAULT NULL
        COMMENT 'Hash BCrypt de acceso al portal; NULL = cliente sin cuenta de portal'
        AFTER correo;

-- 2. Validación de formato de hash: Si se proporciona un hash, no debe ser una cadena vacía
--    o ficticia corta (BCrypt estándar utiliza 60 caracteres; validamos >= 50 chars).
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_password_hash
        CHECK (password_hash IS NULL OR CHAR_LENGTH(password_hash) >= 50);

-- 3. Coherencia de acceso: Todo cliente con credenciales activas del portal (password_hash no nulo)
--    debe contar obligatoriamente con un correo electrónico para autenticarse.
ALTER TABLE clientes
    ADD CONSTRAINT chk_clientes_acceso_requiere_correo
        CHECK (password_hash IS NULL OR correo IS NOT NULL);
