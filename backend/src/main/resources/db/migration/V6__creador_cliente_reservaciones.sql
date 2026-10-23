-- S4-06: distingue al cliente del portal del empleado que registra en mostrador.
-- Las filas existentes conservan creado_por_id y no requieren actualización.
ALTER TABLE reservaciones MODIFY COLUMN creado_por_id BIGINT UNSIGNED NULL;
ALTER TABLE reservaciones ADD COLUMN creado_por_cliente_id BIGINT UNSIGNED NULL;
ALTER TABLE reservaciones ADD CONSTRAINT fk_reservaciones_creador_cliente
    FOREIGN KEY (creado_por_cliente_id) REFERENCES clientes (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE reservaciones ADD CONSTRAINT chk_reservaciones_un_creador CHECK (
    (creado_por_id IS NOT NULL AND creado_por_cliente_id IS NULL)
    OR
    (creado_por_id IS NULL AND creado_por_cliente_id IS NOT NULL
     AND creado_por_cliente_id = cliente_id)
);
