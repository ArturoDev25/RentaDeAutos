# S4-01 — Migración V5: Campos de Acceso en Clientes (Issue #120 / #118)

## 1. Resumen de la Tarea
- **Sprint:** Sprint 4 — Portal del Cliente / Rentador
- **Issue:** #120 / #118
- **Objetivo:** Incorporar a la base de datos y a la capa JPA los campos necesarios para que los clientes puedan registrarse e iniciar sesión en el portal de autoservicio (Opción A de diseño de arquitectura).
- **Archivo de Migración:** [`V5__campos_acceso_clientes.sql`](file:///c:/Users/julio/RentaDeAutos/backend/src/main/resources/db/migration/V5__campos_acceso_clientes.sql)

---

## 2. Decisiones Técnicas y Arquitectura

### 2.1 Modelo de Identidad del Cliente: Opción A
En el refinamiento técnico y plan del Sprint 4 se evaluaron dos alternativas para el manejo de credenciales de los clientes:
- **Opción A (Adoptada):** Mantener separados al personal interno (`usuarios_app`) y a los clientes (`clientes`). Las credenciales de acceso del cliente (`correo` como identificador de login y `password_hash` en formato BCrypt) residen directamente en la tabla `clientes`.
- **Opción B:** Reutilizar `usuarios_app` mezclando clientes y empleados en la misma tabla. (Descartada para evitar acoplamientos, permisos accidentales y colisión de dominios).

### 2.2 Estado Previo de `correo` y `UNIQUE`
Durante la inspección exhaustiva de `V1__init_schema.sql` se constató que:
- La columna `correo VARCHAR(150) NULL` ya fue creada en `V1__init_schema.sql` (línea 65).
- La restricción de unicidad `CONSTRAINT uq_clientes_correo UNIQUE (correo)` ya fue definida en `V1__init_schema.sql` (línea 75).

Por esta razón, la migración `V5` **no redefine ni recrea** la columna `correo` ni el constraint `uq_clientes_correo`, previniendo errores de columna o índice duplicado (`Duplicate column` / `Duplicate key name`) al ejecutar Flyway tanto en MySQL 8.0 como en H2.

---

## 3. DDL de la Migración V5

```sql
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
```

---

## 4. Consideraciones para los Registros Existentes

### 4.1 Coexistencia de Clientes de Mostrador vs. Clientes del Portal
1. **Clientes de Mostrador (Preexistentes o Nuevos):**
   - Son registrados por agentes en ventanilla física mediante el back-office (`ClienteService.crear()`).
   - Tienen `password_hash = NULL`.
   - **Regla estricta:** NO se asignan contraseñas ficticias ni valores dummy (e.g. "temporal", "123456"). La presencia de `password_hash = NULL` indica inequívocamente que el cliente no cuenta con credenciales del portal.
   - Si no tienen correo electrónico (`correo = NULL`), el constraint UNIQUE permite múltiples filas con `NULL` en MySQL y H2 sin conflicto.
2. **Clientes del Portal:**
   - Tienen `password_hash != NULL` (BCrypt de 60 caracteres) y obligatoriamente `correo != NULL`.
   - Cuentan con el helper de dominio `cliente.tieneCuentaPortal() == true`.

### 4.2 Invariantes de Negocio y Seguridad
- **Protección contra fugas en API y Logs:**
  - La propiedad `passwordHash` en `Cliente.java` cuenta con la anotación `@JsonIgnore`.
  - El DTO de respuesta `ClienteResponse` (utilizado por todos los controladores REST actuales) no expone ni mapea ningún campo de contraseña.
  - El método `toString()` de `Cliente` fue sobreescrito omitiendo de manera intencional el hash para prevenir filtraciones en logs de aplicación.
- **Inmutabilidad en Edición Operativa:**
  - Cuando un agente o supervisor edita un cliente existente desde el panel administrativo (`ClienteService.editar()`), el método no altera el campo `passwordHash`, preservando intacta la contraseña del rentador.

### 4.3 Requisitos y Reglas de Negocio para Tareas Siguientes
- **Para S4-02 (Registro de Cliente):**
  - La tabla `clientes` exige restricciones `NOT NULL` en `nombre`, `apellidos`, `telefono`, `numero_licencia` y `licencia_vencimiento`. El endpoint de registro del cliente deberá solicitar estos campos obligatorios para no violar la integridad relacional requerida por el ciclo de reservación y entrega (`EntregaService`).
  - Si un usuario intenta registrarse con un correo que ya existe en la base de datos (por ejemplo, registrado previamente en mostrador), el endpoint debe retornar `409 Conflict` sin revelar información sensible.
- **Para S4-03 (Login de Cliente):**
  - La autenticación solo debe permitir el acceso si `cliente.getActivo() == true` y `cliente.getPasswordHash() != null`. Si `password_hash` es `NULL`, las credenciales son inválidas (mensaje genérico `401 Unauthorized`).
- **Orden de Integración con `V4` (PR #117 en `feat/s4-mejoras`):**
  - La migración `V4__add_imagen_vehiculo.sql` se encuentra en la rama `feat/s4-mejoras` (PR #117).
  - Al momento de integrar ambas ramas a `main`, `V4` debe incorporarse en el historial de Flyway antes o a la par de `V5` para evitar errores de migración fuera de orden en entornos con `outOfOrder=false`.

---

## 5. Procedimiento de Verificación Manual en MySQL 8.0

Para reproducir la verificación de esquema y migración en una base de datos MySQL 8.0 real:

```powershell
# 1. Asegurar que MySQL 8.0 esté corriendo y conectarse
mysql -u root -p

# 2. Verificar que V5 se haya aplicado en la base rentadeautos
USE rentadeautos;
SELECT version, description, type, script, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;

# 3. Comprobar la estructura de la columna password_hash
DESCRIBE clientes;
-- Debe mostrar:
-- correo        varchar(150)  YES  UNI  NULL
-- password_hash varchar(255)  YES       NULL

# 4. Probar inserción con hash NULL (mostrador)
INSERT INTO clientes (nombre, apellidos, correo, telefono, numero_licencia, licencia_vencimiento, activo)
VALUES ('Demo', 'Mostrador', 'demo.mostrador@test.com', '8441112233', 'LIC-VERIF-01', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), TRUE);

# 5. Probar inserción con hash BCrypt válido (portal)
INSERT INTO clientes (nombre, apellidos, correo, password_hash, telefono, numero_licencia, licencia_vencimiento, activo)
VALUES ('Demo', 'Portal', 'demo.portal@test.com', '$2a$10$w8qI.O7a4LpS77t68x9wfehC3z6Nn4ZcM1s2rL/4Q7w90/u8P6pfe', '8442223344', 'LIC-VERIF-02', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), TRUE);

# 6. Probar violación de CHECK (hash corto) -> Debe fallar con error 3819 (Check constraint 'chk_clientes_password_hash' is violated)
INSERT INTO clientes (nombre, apellidos, correo, password_hash, telefono, numero_licencia, licencia_vencimiento, activo)
VALUES ('Demo', 'Fail', 'demo.fail@test.com', '12345', '8443334455', 'LIC-VERIF-03', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), TRUE);

# 7. Probar violación de CHECK (hash sin correo) -> Debe fallar con error 3819 ('chk_clientes_acceso_requiere_correo')
INSERT INTO clientes (nombre, apellidos, correo, password_hash, telefono, numero_licencia, licencia_vencimiento, activo)
VALUES ('Demo', 'NoMail', NULL, '$2a$10$w8qI.O7a4LpS77t68x9wfehC3z6Nn4ZcM1s2rL/4Q7w90/u8P6pfe', '8443334455', 'LIC-VERIF-04', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), TRUE);
```

---

## 6. Cobertura de Pruebas Automatizadas

Se crearon 4 clases de pruebas automatizadas dedicadas y se actualizaron las pruebas unitarias existentes:
1. [`MigracionV5PreservacionTest`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/migration/MigracionV5PreservacionTest.java): Aplica Flyway hasta V3, inserta clientes simulados de mostrador (con y sin correo), ejecuta V5 y valida que el 100% de los datos se preserve intacto con `password_hash = NULL`.
2. [`EsquemaClientesV5Test`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/migration/EsquemaClientesV5Test.java): Inspecciona metadatos JDBC de `INFORMATION_SCHEMA` verificando `VARCHAR(255)`, nulabilidad, índice `UNIQUE` y las restricciones `CHECK`.
3. [`FlywayHibernateValidateTest`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/migration/FlywayHibernateValidateTest.java): Arranca el contexto Spring Boot completo con Flyway V1..V5 y `spring.jpa.hibernate.ddl-auto: validate`.
4. [`ClienteRepositoryIntegrationTest`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/repository/ClienteRepositoryIntegrationTest.java): Pruebas de integración sobre base migrada validando persistencia con BCrypt, búsquedas insensibles a mayúsculas (`findByCorreoIgnoreCase`), violación de unicidad (`DataIntegrityViolationException`), múltiples registros con `correo = NULL`, y pruebas negativas de los dos `CHECK` constraints.
5. [`ClienteSerializacionTest`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/model/ClienteSerializacionTest.java): Valida que Jackson y `toString()` omitan el hash y que `ClienteResponse` no exponga atributos de contraseña.
6. [`ClienteServiceTest`](file:///c:/Users/julio/RentaDeAutos/backend/src/test/java/com/rentadeautos/modules/client/service/ClienteServiceTest.java): Comprueba que `editar()` conserve el `passwordHash` existente y que `crear()` desde mostrador asigne `NULL`.

**Resultado de la suite completa de regresión:**
- Tests ejecutados: **551**
- Fallos: **0**
- Errores: **0**
- Skipped: **0**
- Estado: **BUILD SUCCESS**
