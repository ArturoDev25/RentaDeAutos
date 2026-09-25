# API de vehículos

- **Issues:** S2-06 — CRUD de vehículos · S2-07 — Búsqueda y filtros de vehículos
- **Ruta base:** `/api/v1/vehiculos` (la misma que reservó S2-10 en `SecurityConfig`)
- **Autenticación:** todas las rutas requieren `Authorization: Bearer <token>`.

## Endpoints

| Acción | Método y ruta | Roles | Éxito | Errores posibles |
|---|---|---|---|---|
| Registrar | `POST /api/v1/vehiculos` | ADMINISTRADOR, SUPERVISOR | 201 | 400, 401, 403, 409 |
| Listar | `GET /api/v1/vehiculos` | Todos | 200 | 400, 401 |
| Buscar y filtrar (S2-07) | `GET /api/v1/vehiculos?q=nissan&estado=DISPONIBLE&categoriaId=1&anioDesde=2020&anioHasta=2024` | Todos | 200 | 400, 401 |
| Consultar uno | `GET /api/v1/vehiculos/{id}` | Todos | 200 | 401, 404 |
| Editar | `PUT /api/v1/vehiculos/{id}` | ADMINISTRADOR, SUPERVISOR | 200 | 400, 401, 403, 404, 409 |
| Cambiar estado | `PATCH /api/v1/vehiculos/{id}/estado` | ADMINISTRADOR, SUPERVISOR | 200 | 400, 401, 403, 404 |

No existe endpoint de eliminación: un vehículo se retira cambiándolo a estado `BAJA`.

## Búsqueda y filtros (S2-07)

Todos los parámetros de `GET /api/v1/vehiculos` son opcionales y se combinan entre sí (se deben cumplir todos).

| Parámetro | Qué hace | Ejemplo |
|---|---|---|
| `q` | Busca el texto dentro de la placa, el VIN, la marca o el modelo. No distingue mayúsculas y encuentra coincidencias parciales. Máximo 50 caracteres; en blanco se ignora. | `q=versa`, `q=FAK`, `q=3n1ab` |
| `estado` | Estado exacto. | `estado=MANTENIMIENTO` |
| `categoriaId` | Id de la categoría. | `categoriaId=1` |
| `anioDesde` | Año mínimo, inclusive. | `anioDesde=2020` |
| `anioHasta` | Año máximo, inclusive. | `anioHasta=2024` |

Los caracteres `%` y `_` que escriba el usuario se buscan como texto literal, no como comodines.

Errores (400):

- `q` con más de 50 caracteres.
- `anioDesde` mayor que `anioHasta`.
- `estado` que no existe, o `categoriaId`, `anioDesde` o `anioHasta` que no son números.

Sin coincidencias, la respuesta es 200 con una lista vacía (`"data": []`).

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
| Búsqueda demasiado larga | 400 | `El texto de búsqueda no puede exceder 50 caracteres` |
| Rango de años invertido | 400 | `El año inicial no puede ser mayor que el año final` |
| Parámetro con formato inválido | 400 | `Valor inválido para el parámetro <nombre>` |
| Campo inválido | 400 | `campo: mensaje` (uno por cada campo con error) |
| Sin token | 401 | `No autenticado` |
| Rol sin permiso | 403 | `No tiene permisos para esta operación` |

## Pruebas

Desde la carpeta `backend`, en Windows:

    mvnw.cmd -Dtest="Vehiculo*Test" test

- `VehiculoServiceTest`: reglas de negocio (unicidad, estado inicial, año, categoría).
- `VehiculoControllerTest`: endpoints, validaciones y permisos por rol.
- `VehiculoRepositoryTest`: ejecuta la consulta de búsqueda contra H2 (S2-07).

## Pendientes para siguientes sprints

- S2-13: barra de búsqueda y filtros en la pantalla de vehículos.
- Las transiciones a `RESERVADO` y `RENTADO` deberán hacerlas los módulos de
  reservaciones y entregas. Por ahora el cambio de estado es libre.
- Registrar en auditoría las altas, ediciones y cambios de estado.
