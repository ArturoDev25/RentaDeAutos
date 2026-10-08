# Flujo de trabajo de una reservación y una renta

Este documento describe el flujo funcional implementado en esta rama para
administrar una reservación desde su creación hasta la devolución del vehículo.
También aclara la diferencia entre registrar una entrega y consultar una entrega
que ya existe.

## Resumen del flujo

```text
PENDIENTE
   ├── Confirmar ──> CONFIRMADA
   │                   ├── Entregar ──> EN_CURSO
   │                   │                 └── Devolver ──> FINALIZADA
   │                   └── Cancelar ──> CANCELADA
   └── Cancelar ──> CANCELADA
```

Los estados de la reservación representan el ciclo de vida de la renta:

| Estado | Significado |
|---|---|
| `PENDIENTE` | La reservación fue creada, pero todavía no ha sido confirmada. |
| `CONFIRMADA` | La reservación está autorizada para entregar el vehículo. |
| `EN_CURSO` | El vehículo ya fue entregado al cliente y la renta está activa. |
| `FINALIZADA` | El vehículo ya fue devuelto y la renta terminó. |
| `CANCELADA` | La reservación fue cancelada y no puede continuar. |

## 1. Crear la reservación

El flujo comienza en **Reservaciones**, creando una reservación con:

- Cliente.
- Vehículo.
- Fecha y hora de inicio.
- Fecha y hora de devolución.
- Tarifa diaria.
- Total estimado.
- Observaciones opcionales.

Al crearla, la reservación queda en estado `PENDIENTE`. En este estado todavía
no se puede entregar el vehículo.

Desde la tabla de reservaciones se pueden realizar las acciones disponibles para
una reservación pendiente:

- **Confirmar** la reservación.
- **Editar** sus datos.
- **Cancelar** la reservación.

## 2. Confirmar la reservación

Al seleccionar **Confirmar**, el sistema valida que la reservación siga
pendiente y que el vehículo pueda apartarse para las fechas indicadas.

Si la confirmación es exitosa:

| Entidad | Estado anterior | Estado nuevo |
|---|---|---|
| Reservación | `PENDIENTE` | `CONFIRMADA` |
| Vehículo | Disponible | `RESERVADO` |

Una reservación `CONFIRMADA` muestra la acción **Entregar**. También puede
cancelarse mientras todavía no se ha iniciado la renta.

Si el vehículo tiene un traslape con otra reservación o la reservación cambió
de estado mientras se confirmaba, la operación se rechaza y no se guardan
cambios.

## 3. Entregar el vehículo

La acción **Entregar** abre la pantalla de entrega para una reservación
confirmada. Antes de mostrar el formulario, el sistema consulta la reservación,
el cliente, el vehículo y si ya existe una entrega.

El formulario solicita los datos con los que se entrega el vehículo:

- Kilometraje de salida.
- Combustible de salida.
- Condición general.
- Descripción de la condición.
- Observaciones opcionales.
- Confirmación de que el vehículo fue revisado con el cliente.

Entre las validaciones principales están:

- La reservación debe estar `CONFIRMADA`.
- La entrega no puede registrarse antes del día de inicio.
- La reservación no debe estar vencida.
- El cliente debe estar activo y tener licencia vigente.
- El vehículo no debe estar rentado, en mantenimiento o dado de baja.
- El kilometraje de salida no puede ser menor al kilometraje actual del
  vehículo.
- Solo puede existir una entrega por reservación.

Al registrar correctamente la entrega:

| Entidad | Estado anterior | Estado nuevo |
|---|---|---|
| Reservación | `CONFIRMADA` | `EN_CURSO` |
| Vehículo | `RESERVADO` o `DISPONIBLE` | `RENTADO` |

También se actualiza el kilometraje del vehículo y se registra el evento de
auditoría. La entrega y los cambios de estado se guardan dentro de una misma
transacción.

La pantalla confirma el registro mostrando el folio, la fecha, kilometraje,
combustible, condición y estado resultante.

## 4. Consultar la entrega

Después de registrar la entrega, la reservación muestra dos acciones:

- **Ver entrega**.
- **Devolver**.

La acción **Ver entrega** está disponible tanto cuando la reservación está
`EN_CURSO` como cuando ya está `FINALIZADA`.

Esta acción es únicamente de consulta. No inicia una nueva entrega ni permite
editar los datos registrados. La pantalla muestra:

- Datos de la reservación.
- Cliente y vehículo.
- Periodo de la reservación.
- Estado de la reservación y del vehículo.
- Folio de entrega.
- Fecha de entrega.
- Kilometraje de salida.
- Combustible de salida.
- Condición y observaciones.

El formulario de registro permanece oculto y no editable.

### Mensaje de consulta

Una entrega existente no es un error. Significa que la entrega ya fue
registrada correctamente y que ahora se está consultando su información.

Por eso, al usar **Ver entrega**, no debe aparecer el mensaje:

> No se puede iniciar la entrega

Ese mensaje solo corresponde a una validación que impide registrar una nueva
entrega. Cuando la entrega ya existe, la pantalla muestra directamente
**Entrega registrada** y sus datos en modo consulta.

La validación que impide duplicar una entrega se conserva en el backend. Si
alguien intenta registrar otra entrega para la misma reservación mediante una
petición directa, el servidor responde con conflicto (`409`).

## 5. Devolver el vehículo

Mientras la reservación está `EN_CURSO`, la tabla muestra la acción **Devolver**.
Esta acción abre el formulario de devolución y no modifica la entrega original.

El formulario solicita cómo regresó el cliente el vehículo:

- Kilometraje de entrada.
- Combustible de entrada.
- Condición general y descripción de la condición.
- Cargo por daños, si corresponde.
- Observaciones opcionales.
- Confirmación de revisión con el cliente.
- Indicador de si el vehículo requiere mantenimiento.

Las validaciones principales son:

- La reservación debe estar `EN_CURSO`.
- Debe existir una entrega previa.
- El kilometraje de entrada no puede ser menor al kilometraje de salida.
- El combustible debe estar entre 0 y 100 por ciento.
- El cargo por daños no puede ser negativo.
- Solo puede existir una devolución por entrega.

Al registrar la devolución, el sistema calcula el costo final:

```text
subtotal = tarifa diaria × días reales de renta
cargo por atraso = cargo configurado × días de atraso
total final = subtotal + cargo por atraso + cargo por daños
```

El cargo por daños es opcional. Si no se captura, se considera cero. El costo
final queda guardado en la devolución y se muestra en la pantalla de resultado.

## 6. Resultado de la devolución

Después de registrar la devolución, la pantalla muestra una confirmación con:

- Folio de devolución.
- Fecha de devolución.
- Vehículo y placa.
- Kilometraje de entrada.
- Combustible de entrada.
- Recorrido realizado.
- Condición de devolución.
- Subtotal de la renta.
- Cargo por atraso.
- Cargo por daños.
- Total final.
- Estado resultante de la reservación y del vehículo.

El flujo de estados queda así:

| Entidad | Estado anterior | Estado nuevo |
|---|---|---|
| Reservación | `EN_CURSO` | `FINALIZADA` |
| Vehículo | `RENTADO` | `DISPONIBLE` o `MANTENIMIENTO` |

El vehículo queda en `MANTENIMIENTO` cuando se marca que requiere
mantenimiento o cuando se registra un cargo por daños mayor que cero. En caso
contrario, queda `DISPONIBLE`.

Después se puede seleccionar **Volver a reservaciones**.

## 7. Consultar la entrega después de devolver el vehículo

Una vez finalizada la renta, la reservación sigue mostrando **Ver entrega**.
Esto permite consultar los datos con los que el vehículo fue entregado,
incluso después de que el cliente lo devolvió.

La consulta continúa siendo de solo lectura:

- No permite modificar la entrega.
- No vuelve a mostrar el formulario de entrega.
- No intenta registrar una segunda entrega.
- No cambia los estados de la reservación ni del vehículo.

La devolución es un registro separado de la entrega. Por ello, **Ver entrega**
muestra los datos de salida, mientras que la pantalla de resultado de
**Devolver** muestra los datos de entrada y el resumen económico final.

## Acciones disponibles por estado

| Estado | Acciones principales |
|---|---|
| `PENDIENTE` | Confirmar, editar o cancelar |
| `CONFIRMADA` | Entregar, editar o cancelar |
| `EN_CURSO` | Ver entrega o devolver |
| `FINALIZADA` | Ver entrega |
| `CANCELADA` | Sin acciones operativas |

## Referencias de implementación

| Archivo | Responsabilidad |
|---|---|
| [`renta-autos-app/js/reservaciones.js`](../renta-autos-app/js/reservaciones.js) | Acciones disponibles en la tabla de reservaciones |
| [`renta-autos-app/js/entrega.js`](../renta-autos-app/js/entrega.js) | Registro y consulta de entregas |
| [`renta-autos-app/js/devolucion.js`](../renta-autos-app/js/devolucion.js) | Registro y resultado de devoluciones |
| [`renta-autos-app/entrega.html`](../renta-autos-app/entrega.html) | Interfaz de registro y consulta de entrega |
| [`renta-autos-app/devolucion.html`](../renta-autos-app/devolucion.html) | Interfaz de devolución |
| [`docs/entregas-api.md`](./entregas-api.md) | Reglas y contrato de la API de entregas |
| [`backend/src/main/java/com/rentadeautos/modules/rental/service/EntregaService.java`](../backend/src/main/java/com/rentadeautos/modules/rental/service/EntregaService.java) | Validaciones y registro de entregas |
| [`backend/src/main/java/com/rentadeautos/modules/devolucion/service/DevolucionService.java`](../backend/src/main/java/com/rentadeautos/modules/devolucion/service/DevolucionService.java) | Validaciones, costos y registro de devoluciones |
