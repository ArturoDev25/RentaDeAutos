# S2-10 — Permisos por rol

Issue relacionado: #33.

## Objetivo

Preparar la autorización del backend para los catálogos del Sprint 2:
clientes, vehículos, categorías y tarifas.

Las restricciones se aplican mediante Spring Security en SecurityConfig.java.

## Rutas de integración propuestas

| Catálogo | Ruta base |
|---|---|
| Clientes | /api/v1/clientes |
| Vehículos | /api/v1/vehiculos |
| Categorías | /api/v1/categorias |
| Tarifas | /api/v1/tarifas |

Las reglas incluyen la ruta base y sus subrutas, como /api/v1/clientes/5.

Los controladores de estos catálogos todavía no existían en la copia revisada.
Estas rutas deben coordinarse con quienes implementan los módulos.

Si se utiliza otra ruta, se debe actualizar SecurityConfig y sus pruebas.

## Matriz implementada

| Operación | ADMINISTRADOR | AGENTE | SUPERVISOR | AUDITOR |
|---|---|---|---|---|
| Consultar los cuatro catálogos | Sí | Sí | Sí | Sí |
| Crear y modificar clientes | Sí | Sí | Sí | No |
| Crear y modificar vehículos | Sí | No | Sí | No |
| Crear y modificar categorías | Sí | No | Sí | No |
| Crear y modificar tarifas | Sí | No | Sí | No |
| Eliminar registros de los cuatro catálogos | Sí | No | No | No |

La consulta de tarifas, los permisos de categorías y la eliminación son
propuestas pendientes de validación por el equipo. Las demás reglas se basan
en docs/matriz-roles-permisos.md.

## Contrato de integración

- GET: consultar.
- POST: crear.
- PUT y PATCH: modificar.
- DELETE: eliminar.
- Los métodos no contemplados se bloquean en estas rutas.
- Las restricciones se aplican por método HTTP y ruta, no por el nombre
  del método Java.
- Una operación especial, como eliminar mediante POST, requiere revisar
  y agregar una regla específica antes de integrarla.

## Respuestas de seguridad

- Sin autenticación válida: HTTP 401.
- Usuario autenticado sin el rol requerido: HTTP 403.
- Con permiso: la solicitud continúa al controlador; la respuesta final
  depende de la operación y sus validaciones.

## Validación realizada

PermisosPorRolTest contiene 100 casos:
- 80 combinaciones de rol, catálogo y operación.
- 20 solicitudes sin autenticación.

Las pruebas utilizan la configuración real de seguridad, usuarios simulados
y un controlador ubicado únicamente en src/test.

No verifican JWT reales, persistencia ni los futuros controladores de negocio.
Cuando se integren los módulos, deben comprobarse también sus rutas reales.

Comando en Windows, desde backend:

    mvnw.cmd -Dtest=PermisosPorRolTest test

## Pendientes de integración

- Validar con el equipo las reglas propuestas y las rutas.
- Comprobar los permisos sobre los controladores reales al integrarlos.
- Revisar por separado el registro público, que actualmente crea usuarios
  activos con rol AGENTE. Este cambio no modifica ese comportamiento.