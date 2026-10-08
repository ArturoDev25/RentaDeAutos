\# S3-13 — Acción Confirmar en reservaciones



\## Objetivo

Confirmar reservaciones desde reservaciones.html, respetando

los permisos y las validaciones del backend.



\## Implementación

\- Acción Confirmar disponible para reservaciones PENDIENTES.

\- Diálogo previo con número de reserva, vehículo y fechas.

\- Consumo de POST /api/reservaciones/{id}/confirmar.

\- Revalidación de estado, vehículo y traslapes mediante el servicio existente.

\- Mensajes de éxito y de error.

\- Actualización del listado después de la operación.

\- Bloqueo de controles durante la solicitud para evitar envíos repetidos.

\- Verificación de identidad mediante /api/auth/me.

\- Operaciones disponibles para ADMINISTRADOR, AGENTE y SUPERVISOR.

\- AUDITOR dispone de consulta, sin acciones de modificación.



\## Pruebas automatizadas ejecutadas

Fecha: 2026-10-03.



\- ReservacionControllerTest y ReservacionServiceTest:

&#x20; 21 pruebas, 0 fallos, 0 errores, 0 omitidas.

\- Suite completa del backend:

&#x20; 335 pruebas, 0 fallos, 0 errores, 0 omitidas.



Las pruebas del backend verifican, entre otros casos:

\- Confirmación permitida para administrador, agente y supervisor.

\- Rechazo de confirmación para auditor (403) y sin sesión (401).

\- Rechazo de una reservación que ya no está pendiente.

\- Rechazo de traslape al confirmar, sin cambiar el estado,

&#x20; guardar la reservación ni marcar el vehículo como reservado.



\## Pruebas manuales aprobadas

\- Creación de una reservación pendiente y visualización de sus acciones.

\- Cancelación del diálogo sin modificar la reservación.

\- Confirmación con administrador, agente y supervisor.

\- Visualización del mensaje de confirmación exitosa.

\- Cambio a CONFIRMADA y desaparición de la acción Confirmar.

\- Persistencia del estado después de recargar.

\- Revalidación usando dos pestañas: tras confirmar en una,

&#x20; la otra rechaza repetir la operación y actualiza el listado.



\## Prueba de interfaz con respuesta simulada

Se interceptó temporalmente en el navegador el intento de

confirmar la reservación #5 para simular un error 409.



Resultado:

\- Se mostró: "El vehículo ya tiene una reservación para esas fechas".

\- La reservación permaneció PENDIENTE, incluso después de recargar.

\- El intento interceptado no se envió al backend.

\- La simulación se eliminó al recargar.



Esta comprobación valida la presentación del error en la interfaz.

El rechazo del servicio ante un traslape se verificó por separado

mediante pruebas automatizadas.



\## Alcance de permisos

La cuenta demo de auditor se mantuvo desactivada.

Su rechazo al confirmar se verificó mediante pruebas automatizadas;

no se realizó una prueba manual con esa cuenta.

