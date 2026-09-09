-- ============================================================================
-- V1__init_schema.sql
-- Migración inicial - Sistema de Renta de Autos
-- Issue: S1-06 — Crear migración inicial de la base de datos
-- Modelo base: docs/modelo-datos.md (S1-05)
-- Motor requerido: MySQL 8.0.16 o posterior
-- Ejecutor: Flyway desde Spring Boot
-- ============================================================================

-- La base rentadeautos debe existir antes de iniciar Spring Boot.
-- Flyway administra únicamente los objetos del esquema seleccionado.

-- 1. Roles autorizados en la aplicación.
CREATE TABLE roles (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(50)     NOT NULL,
    descripcion  VARCHAR(255)    NULL,
    activo       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre),
    CONSTRAINT chk_roles_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles autorizados en el sistema';

-- 2. Personal que inicia sesión y opera el sistema.
CREATE TABLE usuarios_app (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rol_id         BIGINT UNSIGNED NOT NULL,
    nombre         VARCHAR(120)    NOT NULL,
    correo         VARCHAR(150)    NOT NULL,
    password_hash  VARCHAR(255)    NOT NULL,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    ultimo_acceso  DATETIME        NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuarios_app PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_app_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_app_rol FOREIGN KEY (rol_id)
        REFERENCES roles (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_usuarios_app_nombre
        CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_usuarios_app_correo
        CHECK (CHAR_LENGTH(TRIM(correo)) > 0),
    CONSTRAINT chk_usuarios_app_password
        CHECK (CHAR_LENGTH(password_hash) >= 50),
    INDEX idx_usuarios_app_rol_activo (rol_id, activo)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Personal que inicia sesión y opera el sistema';

-- 3. Clientes que reservan vehículos; no son usuarios de la aplicación.
CREATE TABLE clientes (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre                VARCHAR(100)    NOT NULL,
    apellidos             VARCHAR(150)    NOT NULL,
    correo                VARCHAR(150)    NULL,
    telefono              VARCHAR(20)     NOT NULL,
    numero_licencia       VARCHAR(50)     NOT NULL,
    licencia_vencimiento  DATE            NOT NULL,
    direccion             VARCHAR(255)    NULL,
    activo                BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                              ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    CONSTRAINT uq_clientes_licencia UNIQUE (numero_licencia),
    CONSTRAINT chk_clientes_nombre
        CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_clientes_apellidos
        CHECK (CHAR_LENGTH(TRIM(apellidos)) > 0),
    CONSTRAINT chk_clientes_telefono
        CHECK (CHAR_LENGTH(TRIM(telefono)) > 0),
    CONSTRAINT chk_clientes_licencia
        CHECK (CHAR_LENGTH(TRIM(numero_licencia)) > 0),
    INDEX idx_clientes_nombre (apellidos, nombre),
    INDEX idx_clientes_activo (activo)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Clientes que reservan o rentan vehículos';

-- 4. Clasificación de vehículos.
CREATE TABLE categorias (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)     NOT NULL,
    descripcion    VARCHAR(255)    NULL,
    deposito_base  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre),
    CONSTRAINT chk_categorias_nombre
        CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_categorias_deposito
        CHECK (deposito_base >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Clasificación de vehículos y depósito base';

-- 5. Tarifas vigentes por categoría y periodo.
CREATE TABLE tarifas (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    categoria_id      BIGINT UNSIGNED NOT NULL,
    precio_dia        DECIMAL(10,2)   NOT NULL,
    cargo_atraso_dia  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    fecha_inicio      DATE            NOT NULL,
    fecha_fin         DATE            NULL,
    activo            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tarifas PRIMARY KEY (id),
    CONSTRAINT fk_tarifas_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_tarifas_importes
        CHECK (precio_dia > 0 AND cargo_atraso_dia >= 0),
    CONSTRAINT chk_tarifas_fechas
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    INDEX idx_tarifas_categoria_vigencia
        (categoria_id, activo, fecha_inicio, fecha_fin)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Precio aplicable a una categoría durante un periodo';

-- 6. Vehículos disponibles para reservación y renta.
CREATE TABLE vehiculos (
    id            BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    categoria_id  BIGINT UNSIGNED   NOT NULL,
    placa         VARCHAR(15)       NOT NULL,
    vin           VARCHAR(17)       NOT NULL,
    marca         VARCHAR(80)       NOT NULL,
    modelo        VARCHAR(80)       NOT NULL,
    anio          SMALLINT UNSIGNED NOT NULL,
    color         VARCHAR(40)       NULL,
    kilometraje   DECIMAL(10,1)     NOT NULL DEFAULT 0,
    estado        VARCHAR(30)       NOT NULL DEFAULT 'DISPONIBLE',
    created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_vehiculos PRIMARY KEY (id),
    CONSTRAINT uq_vehiculos_placa UNIQUE (placa),
    CONSTRAINT uq_vehiculos_vin UNIQUE (vin),
    CONSTRAINT fk_vehiculos_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_vehiculos_vin CHECK (CHAR_LENGTH(TRIM(vin)) = 17),
    CONSTRAINT chk_vehiculos_anio CHECK (anio BETWEEN 1900 AND 2100),
    CONSTRAINT chk_vehiculos_kilometraje CHECK (kilometraje >= 0),
    CONSTRAINT chk_vehiculos_estado CHECK (
        estado IN ('DISPONIBLE', 'RESERVADO', 'RENTADO', 'MANTENIMIENTO', 'BAJA')
    ),
    INDEX idx_vehiculos_categoria_estado (categoria_id, estado)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Vehículos administrados por el sistema';

-- 7. Reservaciones de vehículos por periodo.
CREATE TABLE reservaciones (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cliente_id      BIGINT UNSIGNED NOT NULL,
    vehiculo_id     BIGINT UNSIGNED NOT NULL,
    creado_por_id   BIGINT UNSIGNED NOT NULL,
    fecha_inicio    DATETIME        NOT NULL,
    fecha_fin       DATETIME        NOT NULL,
    estado          VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE',
    tarifa_dia      DECIMAL(10,2)   NOT NULL,
    total_estimado  DECIMAL(10,2)   NOT NULL,
    observaciones   TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_reservaciones PRIMARY KEY (id),
    CONSTRAINT fk_reservaciones_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_reservaciones_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_reservaciones_usuario FOREIGN KEY (creado_por_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_reservaciones_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_reservaciones_importes
        CHECK (tarifa_dia > 0 AND total_estimado >= 0),
    CONSTRAINT chk_reservaciones_estado CHECK (
        estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'EN_CURSO', 'FINALIZADA')
    ),
    INDEX idx_reservaciones_cliente (cliente_id),
    INDEX idx_reservaciones_disponibilidad
        (vehiculo_id, estado, fecha_inicio, fecha_fin)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Reservaciones de vehículos por periodo';

-- Los traslapes se rechazan en un servicio transaccional de Spring Boot.

-- 8. Entrega física del vehículo; máximo una por reservación.
CREATE TABLE entregas (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reservacion_id      BIGINT UNSIGNED NOT NULL,
    entregado_por_id    BIGINT UNSIGNED NOT NULL,
    fecha_entrega       DATETIME        NOT NULL,
    kilometraje_salida  DECIMAL(10,1)   NOT NULL,
    combustible_salida  DECIMAL(5,2)    NOT NULL,
    condicion_salida    TEXT            NOT NULL,
    observaciones       TEXT            NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_entregas PRIMARY KEY (id),
    CONSTRAINT uq_entregas_reservacion UNIQUE (reservacion_id),
    CONSTRAINT fk_entregas_reservacion FOREIGN KEY (reservacion_id)
        REFERENCES reservaciones (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_entregas_usuario FOREIGN KEY (entregado_por_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_entregas_valores CHECK (
        kilometraje_salida >= 0
        AND combustible_salida BETWEEN 0 AND 100
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Entrega física del vehículo al cliente';

-- 9. Devolución del vehículo; máximo una por entrega.
CREATE TABLE devoluciones (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entrega_id           BIGINT UNSIGNED NOT NULL,
    recibido_por_id      BIGINT UNSIGNED NOT NULL,
    fecha_devolucion     DATETIME        NOT NULL,
    kilometraje_entrada  DECIMAL(10,1)   NOT NULL,
    combustible_entrada  DECIMAL(5,2)    NOT NULL,
    condicion_entrada    TEXT            NOT NULL,
    cargo_atraso         DECIMAL(10,2)   NOT NULL DEFAULT 0,
    cargo_danos          DECIMAL(10,2)   NOT NULL DEFAULT 0,
    total_final          DECIMAL(10,2)   NOT NULL,
    observaciones        TEXT            NULL,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_devoluciones PRIMARY KEY (id),
    CONSTRAINT uq_devoluciones_entrega UNIQUE (entrega_id),
    CONSTRAINT fk_devoluciones_entrega FOREIGN KEY (entrega_id)
        REFERENCES entregas (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_devoluciones_usuario FOREIGN KEY (recibido_por_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_devoluciones_valores CHECK (
        kilometraje_entrada >= 0
        AND combustible_entrada BETWEEN 0 AND 100
        AND cargo_atraso >= 0
        AND cargo_danos >= 0
        AND total_final >= 0
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Devolución del vehículo y cálculo final';

-- La API compara kilometraje_entrada con kilometraje_salida y valida las fechas.

-- 10. Daños, fallas, multas u otros problemas del vehículo.
CREATE TABLE incidencias (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    vehiculo_id        BIGINT UNSIGNED NOT NULL,
    reservacion_id     BIGINT UNSIGNED NULL,
    registrado_por_id  BIGINT UNSIGNED NOT NULL,
    tipo               VARCHAR(50)     NOT NULL,
    descripcion        TEXT            NOT NULL,
    costo_estimado     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    estado             VARCHAR(30)     NOT NULL DEFAULT 'ABIERTA',
    fecha_reporte      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion   DATETIME        NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_incidencias PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_incidencias_reservacion FOREIGN KEY (reservacion_id)
        REFERENCES reservaciones (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_incidencias_usuario FOREIGN KEY (registrado_por_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_incidencias_tipo
        CHECK (CHAR_LENGTH(TRIM(tipo)) > 0),
    CONSTRAINT chk_incidencias_costo CHECK (costo_estimado >= 0),
    CONSTRAINT chk_incidencias_fechas CHECK (
        fecha_resolucion IS NULL OR fecha_resolucion >= fecha_reporte
    ),
    CONSTRAINT chk_incidencias_estado CHECK (
        estado IN ('ABIERTA', 'EN_REVISION', 'RESUELTA')
    ),
    INDEX idx_incidencias_vehiculo_estado (vehiculo_id, estado),
    INDEX idx_incidencias_reservacion (reservacion_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Incidencias relacionadas con vehículos y rentas';

-- 11. Historial de acciones relevantes del sistema.
CREATE TABLE auditoria (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT UNSIGNED NULL,
    accion              VARCHAR(100)    NOT NULL,
    entidad             VARCHAR(80)     NOT NULL,
    entidad_id          BIGINT UNSIGNED NULL,
    resultado           VARCHAR(30)     NOT NULL,
    valores_anteriores  JSON            NULL,
    valores_nuevos      JSON            NULL,
    direccion_ip        VARCHAR(45)     NULL,
    fecha_hora          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT chk_auditoria_accion
        CHECK (CHAR_LENGTH(TRIM(accion)) > 0),
    CONSTRAINT chk_auditoria_entidad
        CHECK (CHAR_LENGTH(TRIM(entidad)) > 0),
    CONSTRAINT chk_auditoria_resultado CHECK (
        resultado IN ('EXITOSO', 'FALLIDO')
    ),
    INDEX idx_auditoria_usuario_fecha (usuario_id, fecha_hora),
    INDEX idx_auditoria_entidad (entidad, entidad_id),
    INDEX idx_auditoria_fecha (fecha_hora)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de operaciones; no almacena contraseñas ni tokens';
