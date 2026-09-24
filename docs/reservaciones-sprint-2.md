# S2-16 — Reservaciones del panel de Administrador

## Alcance

El Administrador puede crear, listar, consultar, editar y cancelar reservaciones desde
`renta-autos-app/reservaciones.html`. La página usa la sesión del login y la API real.
El Dashboard del Administrador contiene el enlace a esta pantalla.
Los demás roles reciben 403 en `/api/reservaciones/**` durante este sprint.

## Contrato REST

Todas las rutas requieren `Authorization: Bearer <token>`. La respuesta usa
`{ "success": true, "data": ... }` o `{ "success": false, "message": "..." }`.

| Método | Ruta | Acción | Código |
|---|---|---|---|
| GET | `/api/reservaciones/opciones` | Clientes activos y vehículos disponibles o reservados para el formulario | 200 |
| GET | `/api/reservaciones` | Listado por inicio descendente | 200 |
| GET | `/api/reservaciones/{id}` | Detalle | 200, 404 |
| POST | `/api/reservaciones` | Alta en estado `PENDIENTE` | 201, 400, 409 |
| PUT | `/api/reservaciones/{id}` | Edición de pendiente o confirmada | 200, 400, 404, 409 |
| PATCH | `/api/reservaciones/{id}/cancelar` | Cambio a `CANCELADA` | 200, 404, 409 |

Ejemplo de cuerpo POST/PUT:

```json
{
  "clienteId": 2,
  "vehiculoId": 3,
  "fechaInicio": "2026-10-01T10:00:00",
  "fechaFin": "2026-10-03T10:00:00",
  "observaciones": "Entrega en oficina"
}
```

`creadoPorId` se obtiene del token autenticado. El usuario no envía `tarifaDia`.
Al crear, el servidor obtiene `precio_dia` de la única tarifa activa de la
categoría del vehículo cuya vigencia incluye la fecha de inicio, y copia el
valor en `reservaciones.tarifa_dia` (RN-07). Si faltan tarifas o más de una
coincide, devuelve 409 para evitar seleccionar un precio arbitrario.
Al editar fechas del mismo vehículo se conserva la tarifa aceptada; al cambiar
de vehículo se toma la vigente de la nueva categoría. El total estimado es
`tarifa_dia × ceil(duración en horas / 24)`, mínimo un día. El redondeo de días
parciales es la propuesta P-01 del Scrum Master; confirmar con el Product Owner.
No incluye atrasos, combustible ni daños. El esquema V1 exige `fecha_fin >
fecha_inicio` y montos válidos.

Se rechazan fechas invertidas (RN-02), clientes inactivos, vehículos en
`MANTENIMIENTO`, `BAJA` o `RENTADO` y períodos que se superponen en el mismo
vehículo con otra reservación `PENDIENTE`, `CONFIRMADA` o `EN_CURSO`. Dos períodos
adyacentes son válidos. La comprobación de disponibilidad bloquea la fila del
vehículo dentro de la transacción para serializar altas y ediciones concurrentes.
Cancelar no elimina el registro y libera el periodo para nuevas reservas.

## Integración y verificación

La lista del formulario lee directamente las tablas `clientes` y `vehiculos` de
la migración V1; por eso funciona antes de que estén listas las APIs S2-04 y S2-06.
No modifica los datos de esos catálogos. Los registros de demostración deben ser
creados por sus responsables y existir antes de probar el flujo de alta. Cada
categoría utilizada necesita también una tarifa activa vigente en `tarifas`.
El CRUD de tarifas por categoría es opcional (S2-09); si no se implementa,
la demostración necesita datos de tarifa cargados de forma coordinada con el equipo.

Desde `backend/`, ejecutar `./mvnw test` con Java 21 y acceso a Maven Central.
Para la demo, iniciar MySQL, ejecutar el backend con `JWT_SECRET` configurado,
iniciar sesión como Administrador, abrir `reservaciones.html` desde el Dashboard y crear,
editar y cancelar una reservación; probar fechas invertidas y un traslape.
La bitácora de cambios completa corresponde a S2-18 y se integra por ese apartado.
