-- S4-06: distingue al cliente del portal del empleado que registra en mostrador.
-- Las filas existentes conservan creado_por_id y no requieren actualización.
--
-- MySQL 8 no permite un CHECK sobre una columna que participa en una llave foránea
-- con acción ON UPDATE/ON DELETE distinta de RESTRICT (error 3823). El CHECK de abajo
-- usa cliente_id, creado_por_id y creado_por_cliente_id, por eso las tres llaves
-- foráneas de esas columnas se declaran con RESTRICT.
-- fk_reservaciones_cliente y fk_reservaciones_usuario (V1) se recrean sin ON UPDATE CASCADE:
-- los ids de clientes y de usuarios no se modifican, así que el comportamiento no cambia.
ALTER TABLE reservaciones MODIFY COLUMN creado_por_id BIGINT UNSIGNED NULL;
ALTER TABLE reservaciones ADD COLUMN creado_por_cliente_id BIGINT UNSIGNED NULL;
ALTER TABLE reservaciones DROP FOREIGN KEY fk_reservaciones_cliente;
ALTER TABLE reservaciones ADD CONSTRAINT fk_reservaciones_cliente
    FOREIGN KEY (cliente_id) REFERENCES clientes (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE reservaciones DROP FOREIGN KEY fk_reservaciones_usuario;
ALTER TABLE reservaciones ADD CONSTRAINT fk_reservaciones_usuario
    FOREIGN KEY (creado_por_id) REFERENCES usuarios_app (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE reservaciones ADD CONSTRAINT fk_reservaciones_creador_cliente
    FOREIGN KEY (creado_por_cliente_id) REFERENCES clientes (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE reservaciones ADD CONSTRAINT chk_reservaciones_un_creador CHECK (
    (creado_por_id IS NOT NULL AND creado_por_cliente_id IS NULL)
    OR
    (creado_por_id IS NULL AND creado_por_cliente_id IS NOT NULL
     AND creado_por_cliente_id = cliente_id)
);
