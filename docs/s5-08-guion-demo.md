# S5-08 - Guion de presentación y ensayo

Acceso para la demostración: https://srv01.tailb2f071.ts.net/login.html

## Objetivo

Demostrar registro, moderación, acceso y ciclo de reservación/renta, con datos
ficticios y la misma versión aprobada para el despliegue. Duración orientativa:
12 a 15 minutos. Este guion no acredita que el ensayo ya se realizó.

## Preparación

- Confirmar URL, commit, configuración efectiva y pruebas en el acta S5-08.
- Dos sesiones separadas: personal en un navegador y cliente en otro perfil o
  navegador. Evitar compartir el almacenamiento de sesión entre ambos.
- Cuenta activa de administrador, agente o supervisor y cliente aprobado.
- Dos registros ficticios nuevos con correos y licencias distintos: uno para
  aprobar y otro para rechazar. Guardar contraseñas fuera del material proyectado.
- Vehículo y tarifa vigentes para la fecha elegida, licencia vigente y datos de
  salida/devolución coherentes.
- La reserva se debe crear con inicio futuro, pero la entrega no puede ocurrir
  antes del día de inicio. Para recorrer todo en una sesión, elegir hoy con una
  hora posterior a la actual y confirmar su viabilidad en el ensayo. Si la interfaz
  obliga a otro día o la regla desplegada lo impide, preparar una reserva válida
  para entregar hoy y explicitar el cambio de folio: no simular un ciclo único.
- No editar la base ni adelantar el reloj para saltar validaciones.

## Recorrido

| Tiempo | Acción | Qué explicar | Resultado a comprobar |
|---|---|---|---|
| 1 min | Abrir portal y catálogo | “El sistema integra al cliente y al personal en una sola operación.” | Sitio accesible desde el equipo de la presentación |
| 2 min | Registrar cliente A | “El registro crea una solicitud pendiente, no acceso inmediato.” | Aviso de pendiente; login aún denegado |
| 1 min | Personal: Clientes pendientes, aprobar A | “Administrador, agente y supervisor revisan solicitudes.” | Aprobado y activo; la solicitud sale de pendientes |
| 1 min | Registrar B y rechazarlo | “El rechazo conserva la cuenta inhabilitada y queda auditado.” | B no entra; no aparece como pendiente después de actualizar |
| 1 min | Cliente A: iniciar sesión, filtrar catálogo y ver detalle | “Se muestran categoría, tarifa y características.” | Acceso correcto y detalle del vehículo elegido |
| 2 min | Crear reserva | “El importe es estimado; no hay pago en línea.” | Folio y PENDIENTE en Mis reservaciones |
| 1 min | Personal: confirmar ese folio | “El servidor revalida estado y disponibilidad.” | CONFIRMADA después de actualizar |
| 2 min | Personal: entregar | “Registramos kilometraje, combustible, condición y revisión.” | EN_CURSO; aparece en Mis rentas del cliente |
| 2 min | Personal: devolver | “El sistema registra devolución e importe final.” | FINALIZADA; total final visible en el historial |
| 1 min | Consultar reportes/auditoría y cerrar sesión | “La operación deja trazabilidad y el cliente ve sus propios datos.” | Eventos correspondientes y salida de sesión |

Para entrega: kilometraje de salida al menos igual al actual; cliente activo y
licencia vigente. Para devolución: kilometraje de entrada no menor al de salida,
combustible entre 0 y 100, daños no negativos y revisión confirmada. Capturar
valores coherentes, leer el resultado y anotar los folios sin asumir IDs fijos.

## Casos negativos breves

1. Contraseña incorrecta: mensaje genérico, sin acceso.
2. Cuenta pendiente o rechazada: no permite entrar.
3. Fecha final anterior o igual al inicio: rechazo, sin reserva nueva.
4. Vehículo ocupado en el periodo: rechazo de traslape, sin duplicar reserva.
5. Cliente B aprobado distinto de A: no ve las operaciones de A. Si B se usó para
   rechazo, utilizar una tercera cuenta aprobada para esta comprobación.

Realizar los casos largos durante el ensayo; seleccionar solo los necesarios para
la exposición. No desactivar validaciones ni protección de permisos.

## Si algo falla

Detener el paso, conservar el mensaje y registrar la incidencia. Revisar conexión,
sesión, versión, disponibilidad, tarifa y reglas de fechas. No repetir operaciones
sin comprobar si ya se guardaron. Un escenario precargado puede ilustrar una
consulta, pero debe identificarse como tal y no como resultado del flujo fallido.

## Cierre verbal

“El sistema permite registrar y aprobar clientes, consultar vehículos, solicitar
reservaciones y administrar entrega y devolución. El personal opera según su rol;
el cliente consulta únicamente su información. Los datos se conservan y las
acciones relevantes quedan registradas en auditoría.”

Completar el [acta de ensayo](s5-08-verificacion-y-ensayo.md) con resultados reales,
fecha, responsables, folios e incidencias. Repetir los pasos corregidos antes del cierre.
