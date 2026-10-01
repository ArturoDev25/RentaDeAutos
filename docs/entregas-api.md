# S3-09 — Entrega de vehículo

## Alcance

Operación que entrega físicamente un vehículo al cliente a partir de una
reservación **CONFIRMADA** (S3-08) e inicia la renta. Usa la tabla `entregas` de
V1 (máximo una entrega por reservación); no requiere una migración nueva.
La devolución, los cargos finales y la pantalla de entrega quedan fuera de este alcance.

Flujo de estados:

| Entidad | Antes | Después |
|---|---|---|
| Reservación | `CONFIRMADA` | `EN_CURSO` |
| Vehículo | `RESERVADO` (o `DISPONIBLE`) | `RENTADO` |

## Contrato REST

Todas las rutas requieren `Authorization: Bearer <token>`.

| Método | Ruta | Roles | Resultado |
|---|---|---|---|
| POST | `/api/v1/entregas` | Administrador, Agente, Supervisor | 201: entrega registrada y renta en curso |
| GET | `/api/v1/entregas/reservacion/{reservacionId}` | Los cuatro roles | 200: entrega de la reservación; 404 si no tiene |

Los permisos siguen la matriz de roles: "Registrar entrega y devolución" no está
permitido al Auditor (403).

### Cuerpo de POST

```json
{
  "reservacionId": 10,
  "kilometrajeSalida": 15020.5,
  "combustibleSalida": 75.5,
  "condicionSalida": "Sin golpes; rayón leve en defensa trasera",
  "observaciones": "Se entregó con silla para bebé"
}
```

| Campo | Regla |
|---|---|
| `reservacionId` | Obligatorio, positivo |
| `kilometrajeSalida` | Obligatorio, ≥ 0, 1 decimal; no puede ser menor al kilometraje actual del vehículo |
| `combustibleSalida` | Obligatorio, porcentaje entre 0 y 100, hasta 2 decimales |
| `condicionSalida` | Obligatoria, no vacía, máximo 2000 caracteres |
| `observaciones` | Opcional, máximo 2000 caracteres |

### Respuesta 201

```json
{
  "success": true,
  "data": {
    "id": 99,
    "reservacionId": 10,
    "vehiculoId": 3,
    "entregadoPorId": 7,
    "fechaEntrega": "2026-10-01T10:15:00",
    "kilometrajeSalida": 15020.5,
    "combustibleSalida": 75.50,
    "condicionSalida": "Sin golpes; rayón leve en defensa trasera",
    "observaciones": "Se entregó con silla para bebé",
    "estadoReservacion": "EN_CURSO",
    "estadoVehiculo": "RENTADO"
  }
}
```

`entregadoPorId` es el usuario de la sesión y `fechaEntrega` la hora del servidor;
no se envían en el cuerpo.

## Reglas y errores

| Caso | HTTP |
|---|---|
| Campos faltantes o fuera de rango | 400 |
| Kilometraje de salida menor al del vehículo | 400 |
| Sin token | 401 |
| Rol Auditor intenta registrar | 403 |
| Reservación inexistente | 404 |
| Reservación que no está `CONFIRMADA` (pendiente, cancelada, en curso, finalizada) | 409 |
| La reservación ya tiene entrega | 409 |
| Reservación vencida (`fechaFin` ya pasó) | 409 |
| Antes del día de `fechaInicio` | 409 |
| Cliente inactivo o con licencia vencida | 409 |
| Vehículo en `RENTADO`, `MANTENIMIENTO` o `BAJA`, o con otra renta `EN_CURSO` | 409 |

## Transacción y concurrencia

`EntregaService.registrar` es `@Transactional`: la entrega, los dos cambios de
estado, la actualización del kilometraje del vehículo y la auditoría se guardan
juntos o no se guarda nada. Se bloquean con `SELECT ... FOR UPDATE` primero el
vehículo y luego la reservación (mismo orden que crear/editar/confirmar), así dos
entregas simultáneas no pueden rentar el mismo auto; además, `uq_entregas_reservacion`
responde 409 si dos peticiones llegan a la vez para la misma reservación.

## Auditoría

Cada entrega exitosa registra en `auditoria`:

- `accion`: `ENTREGAR_VEHICULO`
- `entidad`: `Entrega`, `entidad_id`: ID de la entrega
- `valores_anteriores`: estado de la reservación y del vehículo, kilometraje previo
- `valores_nuevos`: estados nuevos, kilometraje, combustible y condición de salida
- `direccion_ip` y usuario de la sesión

## Verificación

Pruebas: `EntregaServiceTest` (reglas, estados, auditoría y que nada se guarde al
fallar) y `EntregaControllerTest` (201, validaciones 400, permisos 401/403, 409).

Prueba manual (Postman o similar):

1. Iniciar sesión y crear una reservación con fecha de inicio hoy.
2. Confirmarla: `POST /api/reservaciones/{id}/confirmar` → vehículo `RESERVADO`.
3. `POST /api/v1/entregas` con el cuerpo de ejemplo → 201, `EN_CURSO` / `RENTADO`.
4. Repetir el paso 3 → 409 (ya tiene entrega).
5. Revisar el módulo de Auditoría: aparece `ENTREGAR_VEHICULO`.
