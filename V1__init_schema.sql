-- =====================================================================
-- V1__init_schema.sql
-- Migracion inicial de la base de datos - Sistema de Renta de Autos
-- =====================================================================

CREATE DATABASE IF NOT EXISTS rentadeautos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE rentadeautos;

-- 1. roles
CREATE TABLE roles (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(50)     NOT NULL,
    descripcion  VARCHAR(255)    NULL,
    activo       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catalogo de roles autorizados en el sistema';

-- 2. usuarios_app
CREATE TABLE usuarios_app (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rol_id         BIGINT UNSIGNED NOT NULL,
    nombre         VARCHAR(120)    NOT NULL,
    correo         VARCHAR(150)    NOT NULL,
    password_hash  VARCHAR(255)    NOT NULL,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    ultimo_acceso  DATETIME        NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id)
        REFERENCES roles (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_usuarios_rol_activo (rol_id, activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Usuarios (personal) que inician sesion y operan el sistema';

-- 3. clientes
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
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_clientes_correo   UNIQUE (correo),
    CONSTRAINT uq_clientes_licencia UNIQUE (numero_licencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Clientes que reservan o rentan vehiculos';

-- 4. categorias
CREATE TABLE categorias (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)     NOT NULL,
    descripcion    VARCHAR(255)    NULL,
    deposito_base  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Clasificacion de vehiculos y base para sus tarifas';

-- 5. tarifas
CREATE TABLE tarifas (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    categoria_id      BIGINT UNSIGNED NOT NULL,
    precio_dia        DECIMAL(10,2)   NOT NULL,
    cargo_atraso_dia  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    fecha_inicio      DATE            NOT NULL,
    fecha_fin         DATE            NULL,
    activo            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tarifas_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_tarifas_cat_activo_fechas (categoria_id, activo, fecha_inicio, fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Precio aplicable a una categoria durante un periodo';

-- 6. vehiculos
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
    estado        VARCHAR(30)       NOT NULL,
    created_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_vehiculos_placa UNIQUE (placa),
    CONSTRAINT uq_vehiculos_vin   UNIQUE (vin),
    CONSTRAINT fk_vehiculos_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_vehiculos_cat_estado (categoria_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Vehiculos disponibles para reservacion y renta';

-- 7. reservaciones
CREATE TABLE reservaciones (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cliente_id      BIGINT UNSIGNED NOT NULL,
    vehiculo_id     BIGINT UNSIGNED NOT NULL,
    creado_por_id   BIGINT UNSIGNED NOT NULL,
    fecha_inicio    DATETIME        NOT NULL,
    fecha_fin       DATETIME        NOT NULL,
    estado          VARCHAR(30)     NOT NULL,
    tarifa_dia      DECIMAL(10,2)   NOT NULL,
    total_estimado  DECIMAL(10,2)   NOT NULL,
    observaciones   TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservaciones_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_reservaciones_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculos (id)    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_reservaciones_usuario FOREIGN KEY (creado_por_id)
        REFERENCES usuarios_app (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_reservaciones_cliente (cliente_id),
    INDEX idx_reservaciones_veh_estado_fechas (vehiculo_id, estado, fecha_inicio, fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Solicitud de un cliente para usar un vehiculo en un periodo';

-- 8. entregas
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
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_entregas_reservacion UNIQUE (reservacion_id),
    CONSTRAINT fk_entregas_reservacion FOREIGN KEY (reservacion_id)
        REFERENCES reservaciones (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_entregas_usuario FOREIGN KEY (entregado_por_id)
        REFERENCES usuarios_app (id)  ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de la entrega fisica del vehiculo al cliente';

-- 9. devoluciones
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
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_devoluciones_entrega UNIQUE (entrega_id),
    CONSTRAINT fk_devoluciones_entrega FOREIGN KEY (entrega_id)
        REFERENCES entregas (id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_devoluciones_usuario FOREIGN KEY (recibido_por_id)
        REFERENCES usuarios_app (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de la devolucion del vehiculo';

-- 10. incidencias
CREATE TABLE incidencias (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    vehiculo_id        BIGINT UNSIGNED NOT NULL,
    reservacion_id     BIGINT UNSIGNED NULL,
    registrado_por_id  BIGINT UNSIGNED NOT NULL,
    tipo               VARCHAR(50)     NOT NULL,
    descripcion        TEXT            NOT NULL,
    costo_estimado     DECIMAL(10,2)   NOT NULL DEFAULT 0,
    estado             VARCHAR(30)     NOT NULL,
    fecha_reporte      DATETIME        NOT NULL,
    fecha_resolucion   DATETIME        NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculos (id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_incidencias_reservacion FOREIGN KEY (reservacion_id)
        REFERENCES reservaciones (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_incidencias_usuario FOREIGN KEY (registrado_por_id)
        REFERENCES usuarios_app (id)  ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_incidencias_veh_estado (vehiculo_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Danos, fallas, multas u otros problemas de un vehiculo';

-- 11. auditoria
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
    PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios_app (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_auditoria_usuario_fecha (usuario_id, fecha_hora),
    INDEX idx_auditoria_entidad (entidad, entidad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de acciones relevantes del sistema. Nunca guardar contrasenas ni tokens.';
