# Matriz de roles y permisos

**Issue:** S1-09 — Implementar roles y permisos base
**Sistema:** Renta de Autos

## Objetivo

Definir el acceso de los usuarios según su rol. La autorización se valida
en la API REST mediante Spring Security y no depende únicamente de ocultar
elementos en el frontend.

## Roles del MVP

- `ADMINISTRADOR`
- `AGENTE`
- `SUPERVISOR`
- `AUDITOR`

## Matriz de permisos

| Operación | Administrador | Agente | Supervisor | Auditor |
|---|:---:|:---:|:---:|:---:|
| Iniciar sesión | Sí | Sí | Sí | Sí |
| Consultar perfil propio | Sí | Sí | Sí | Sí |
| Consultar clientes | Sí | Sí | Sí | Sí |
| Registrar y modificar clientes | Sí | Sí | Sí | No |
| Consultar vehículos | Sí | Sí | Sí | Sí |
| Registrar y modificar vehículos | Sí | No | Sí | No |
| Consultar reservaciones | Sí | Sí | Sí | Sí |
| Crear y modificar reservaciones | Sí | Sí | Sí | No |
| Registrar entrega y devolución | Sí | Sí | Sí | No |
| Registrar incidencias | Sí | Sí | Sí | No |
| Administrar tarifas | Sí | No | Sí | No |
| Consultar reportes | Sí | No | Sí | Sí |
| Consultar auditoría | Sí | No | Sí | Sí |
| Administrar usuarios y roles | Sí | No | No | No |
| Activar o desactivar usuarios | Sí | No | No | No |

## Reglas de autorización

1. Las rutas públicas son el inicio de sesión y el health check.
2. Las demás rutas requieren un token JWT válido.
3. Las rutas bajo `/api/admin/**` requieren el rol `ADMINISTRADOR`.
4. Un usuario autenticado sin el rol requerido recibe HTTP `403 Forbidden`.
5. Una solicitud sin autenticación válida recibe HTTP `401 Unauthorized`.
6. El backend consulta que el usuario y su rol continúen activos.
7. Desactivar un usuario no elimina su registro ni su historial.
8. Los permisos de los módulos futuros deberán configurarse en la API conforme
   a esta matriz.

## Endpoint implementado

```http
PATCH /api/admin/usuarios/{id}/estado