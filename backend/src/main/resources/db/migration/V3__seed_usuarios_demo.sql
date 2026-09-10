-- ============================================================================
-- V3__seed_usuarios_demo.sql
-- Usuarios ficticios de demostración para el Login (S1-08).
-- Contraseña de todos: Demo1234 (credencial ficticia, no usar en producción).
-- El usuario auditor se carga desactivado para probar el caso negativo.
-- ============================================================================

INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
SELECT r.id, 'Ana Demo Administradora', 'admin@demo.local',
       '$2a$10$qJKqLeBH.9s93j5vBcBL6eNgGkcOieqqIYEL5MiKi0wR9M1m1WBd.', TRUE
FROM roles r WHERE r.nombre = 'ADMINISTRADOR';

INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
SELECT r.id, 'Beto Demo Agente', 'agente@demo.local',
       '$2a$10$E0otQPvFSL73p1MgGS/C5eVM8/Pix.u/NZA18hKw2NEkL4UUaQGuO', TRUE
FROM roles r WHERE r.nombre = 'AGENTE';

INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
SELECT r.id, 'Carla Demo Supervisora', 'supervisor@demo.local',
       '$2a$10$GfX2UYL05Jc5IxZWxRnuveWqUr36WH5r7z5gfZQosnYmoISoaAOmi', TRUE
FROM roles r WHERE r.nombre = 'SUPERVISOR';

INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
SELECT r.id, 'Dario Demo Auditor', 'auditor@demo.local',
       '$2a$10$8Xij424Pu2AIT4G7aJ5OqeLyfn/So6WMW3OA.HvQ8ICX2MADckoZO', FALSE
FROM roles r WHERE r.nombre = 'AUDITOR';