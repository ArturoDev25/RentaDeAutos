# API de vehículos

- **Issue:** S2-06 — CRUD de vehículos
- **Ruta base:** `/api/v1/vehiculos` (la misma que reservó S2-10 en `SecurityConfig`)
- **Autenticación:** todas las rutas requieren `Authorization: Bearer <token>`.

## Endpoints

| Acción | Método y ruta | Roles | Éxito | Errores posibles |
|---|---|---|---|---|
| Registrar | `POST /api/v1/vehiculos` | ADMINISTRADOR, SUPERVISOR | 201 | 400, 401, 403, 409 |
| Listar | `GET /api/v1/vehiculos` | Todos | 200 | 400, 401 |
| Listar con filtros | `GET /api/v1/vehiculos?estado=DISPONIBLE&categoriaId=1` | Todos | 200 | 400, 401 |
| Consultar uno | `GET /api/v1/vehiculos/{id}` | Todos | 200 | 401, 404 |
| Editar | `PUT /api/v1/vehiculos/{id}` | ADMINISTRADOR, SUPERVISOR | 200 | 400, 401, 403, 404, 409 |
| Cambiar estado | `PATCH /api/v1/vehiculos/{id}/estado` | ADMINISTRADOR, SUPERVISOR | 200 | 400, 401, 403, 404 |

No existe endpoint de eliminación: un vehículo se retira cambiándolo a estado `BAJA`.

## Cuerpo para registrar y editar

```json
{
  "categoriaId": 1,
  "placa": "FAK-1234",
  "vin": "3N1AB7AP0FY123456",
  "marca": "Nissan",
  "modelo": "Versa",
  "anio": 2022,
  "color": "Blanco",
  "kilometraje": 15000.0
}
```

| Campo | Regla |
|---|---|
| `categoriaId` | Obligatorio. La categoría debe existir y estar activa (al editar solo se revisa si se cambia de categoría). |
| `placa` | Obligatoria, de 5 a 15 letras, números o guiones. **Única**. Se guarda en mayúsculas. |
| `vin` | Obligatorio, exactamente 17 letras o números, sin `I`, `O` ni `Q` (norma ISO 3779). **Único**. Se guarda en mayúsculas. |
| `marca` | Obligatoria, máximo 80 caracteres. |
| `modelo` | Obligatorio, máximo 80 caracteres. |
| `anio` | Obligatorio, entre 2000 y el año actual + 1. |
| `color` | Opcional, máximo 40 caracteres. Vacío se guarda como nulo. |
| `kilometraje` | Obligatorio, mayor o igual a 0, hasta 1 decimal. |

El cuerpo **no** incluye el estado: todo vehículo nuevo inicia en `DISPONIBLE`.

## Cuerpo para cambiar el estado

```json
{ "estado": "MANTENIMIENTO" }
```

Estados válidos: `DISPONIBLE`, `RESERVADO`, `RENTADO`, `MANTENIMIENTO`, `BAJA`.
Cualquier otro valor se rechaza con 400.

## Formato de respuesta

Éxito:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "categoriaId": 1,
    "categoriaNombre": "Compacto",
    "placa": "FAK-1234",
    "vin": "3N1AB7AP0FY123456",
    "marca": "Nissan",
    "modelo": "Versa",
    "anio": 2022,
    "color": "Blanco",
    "kilometraje": 15000.0,
    "estado": "DISPONIBLE"
  }
}
```

Error:

```json
{ "success": false, "message": "Ya existe un vehículo con esa placa" }
```

## Mensajes de error

| Situación | HTTP | Mensaje |
|---|---|---|
| Placa repetida | 409 | `Ya existe un vehículo con esa placa` |
| VIN repetido | 409 | `Ya existe un vehículo con ese VIN` |
| Vehículo inexistente | 404 | `Vehículo no encontrado` |
| Categoría inexistente | 400 | `La categoría indicada no existe` |
| Categoría inactiva | 400 | `La categoría indicada está inactiva` |
| Año fuera de rango | 400 | `El año debe estar entre 2000 y <año actual + 1>` |
| Campo inválido | 400 | `campo: mensaje` (uno por cada campo con error) |
| Sin token | 401 | `No autenticado` |
| Rol sin permiso | 403 | `No tiene permisos para esta operación` |

## Pruebas

Desde la carpeta `backend`, en Windows:

    mvnw.cmd -Dtest="Vehiculo*Test" test

- `VehiculoServiceTest`: reglas de negocio (unicidad, estado inicial, año, categoría).
- `VehiculoControllerTest`: endpoints, validaciones y permisos por rol.

## Pendientes para siguientes sprints

- Las transiciones a `RESERVADO` y `RENTADO` deberán hacerlas los módulos de
  reservaciones y entregas. Por ahora el cambio de estado es libre.
- Registrar en auditoría las altas, ediciones y cambios de estado.
