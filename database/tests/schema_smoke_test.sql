-- Ejecutar después de que Spring Boot/Flyway aplique V1 y V2.
USE rentadeautos;

-- Deben aparecer 11 tablas funcionales y flyway_schema_history.
SHOW TABLES;

-- Deben aparecer V1 y V2 con success = 1.
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Debe devolver 4.
SELECT COUNT(*) AS roles_creados FROM roles;

-- Debe devolver 11.
SELECT COUNT(*) AS tablas_funcionales
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'roles', 'usuarios_app', 'clientes', 'categorias', 'tarifas',
      'vehiculos', 'reservaciones', 'entregas', 'devoluciones',
      'incidencias', 'auditoria'
  );

-- Permite revisar PK, FK, UNIQUE y CHECK creados por V1.
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
ORDER BY table_name, constraint_type, constraint_name;

-- Pruebas adicionales recomendadas:
-- 1. Insertar dos vehículos con la misma placa: el segundo debe fallar.
-- 2. Insertar dos vehículos con el mismo VIN: el segundo debe fallar.
-- 3. Insertar una reservación con fecha_fin <= fecha_inicio: debe fallar.
-- 4. Insertar combustible mayor que 100: debe fallar.
-- 5. Reiniciar Spring Boot: V1 y V2 no deben volver a ejecutarse.
