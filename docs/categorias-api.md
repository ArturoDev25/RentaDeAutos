# API de categorías de vehículos

- **Issue:** S2-08 — Categorías de vehículos (#31)
- **Ruta base:** `/api/categorias`
- **Autenticación:** todas las rutas requieren `Authorization: Bearer <token>`.

## Endpoints

| Acción | Método y ruta | Éxito | Errores posibles |
|---|---|---|---|
| Crear | `POST /api/categorias` | 201 | 400, 401, 409 |
| Listar | `GET /api/categorias` | 200 | 401 |
| Listar por estado | `GET /api/categorias?activo=true` (o `false`) | 200 | 401 |
| Consultar una | `GET /api/categorias/{id}` | 200 | 401, 404 |
| Editar | `PUT /api/categorias/{id}` | 200 | 400, 401, 404, 409 |
| Activar o desactivar | `PATCH /api/categorias/{id}/estado` | 200 | 400, 401, 404 |

No existe endpoint de eliminación: las categorías solo se desactivan.

## Cuerpo para crear y editar

```json
{
  "nombre": "SUV",
  "descripcion": "Camionetas familiares",
  "depositoBase": 3000.00
}
```

| Campo | Regla |
|---|---|
| `nombre` | Obligatorio, máximo 80 caracteres. Único sin distinguir mayúsculas ni acentos ("Camión" y "camion" se consideran iguales). |
| `descripcion` | Opcional, máximo 255 caracteres. Vacía o en blanco se guarda como nula. |
| `depositoBase` | Obligatorio, mayor o igual a 0, hasta 2 decimales. |

## Cuerpo para cambiar el estado

```json
{ "activo": false }
```

## Formato de respuesta

Éxito:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nombre": "SUV",
    "descripcion": "Camionetas familiares",
    "depositoBase": 3000.00,
    "activo": true
  }
}
```

Error:

```json
{ "success": false, "message": "Ya existe una categoría con ese nombre" }
```

## Reglas de negocio

- Una categoría nueva inicia activa.
- Una categoría con vehículos o tarifas asociados no puede eliminarse: la llave foránea usa `ON DELETE RESTRICT`.
- Desactivar una categoría no modifica los vehículos que ya la usan.

## Pendientes fuera de este issue

- **Acceso actualizado en S2-14:** `/api/categorias` y sus subrutas
  requieren el rol `ADMINISTRADOR`, conforme al Plan Sprint 2 v1.1.
  Sin autenticación válida se responde 401; con otro rol, 403.
- **Vehículos (S2-06):** decidir si se puede asignar una categoría inactiva a un vehículo nuevo. Esta API no lo valida todavía.

## Pruebas

- `CategoriaControllerTest` (14 pruebas) y `CategoriaServiceTest` (11 pruebas).
- Verificación manual con MySQL real: crear, duplicados (incluido "Camion" contra "Camión"), consultar, editar, desactivar, filtrar y rechazo de borrado con vehículo asociado.
