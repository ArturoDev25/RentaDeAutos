-- ============================================================================
-- V2__seed_roles.sql
-- Catálogo mínimo requerido para autenticación y autorización.
-- No contiene usuarios ni contraseñas.
-- ============================================================================

INSERT INTO roles (nombre, descripcion)
VALUES
    ('ADMINISTRADOR', 'Administración total del sistema'),
    ('AGENTE',        'Operación de clientes, vehículos y rentas'),
    ('SUPERVISOR',    'Supervisión y autorización operativa'),
    ('AUDITOR',       'Consulta de historial y auditoría');
