-- ============================================================================
-- V4__add_imagen_vehiculo.sql
-- S4 — Imagen del vehículo.
-- Agrega la columna `imagen` para guardar la foto del vehículo como data URL
-- en base64 (MEDIUMTEXT admite hasta ~16 MB). Es opcional: los vehículos sin
-- foto muestran un ícono por defecto en la interfaz.
-- ============================================================================

ALTER TABLE vehiculos
    ADD COLUMN imagen MEDIUMTEXT NULL
    COMMENT 'Foto del vehículo como data URL en base64 (opcional)';
