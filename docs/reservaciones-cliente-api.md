# S4-06 — Reservaciones e historial del cliente

## Identidad y permisos

Las rutas requieren `Authorization: Bearer <token>` de un cliente activo,
obtenido en `POST /api/auth/cliente/login` (S4-03 y S4-04).
El filtro autentica el subject `cliente:<id>` con `ROLE_CLIENTE`.
El ID usado en las consultas y operaciones sale de esa identidad, nunca del cuerpo,
la URL de consulta o un `clienteId` proporcionado por el frontend.

El personal usa sus rutas de administración; Administrador, Agente, Supervisor y
Auditor reciben 403 al intentar estas rutas del portal. Sin sesión válida: 401.

| Método | Ruta | Respuesta exitosa |
|---|---|---|
| GET | `/api/v1/cliente/reservaciones` | 200, lista de reservaciones propias |
| GET | `/api/v1/cliente/reservaciones/{id}` | 200, detalle propio |
| POST | `/api/v1/cliente/reservaciones` | 201, reservación creada |
| PATCH | `/api/v1/cliente/reservaciones/{id}/cancelar` | 200, reservación cancelada |
| GET | `/api/v1/cliente/reservaciones/historial` | 200, rentas propias iniciadas |

Todas las respuestas usan `ApiResponse`: `success`, `data` o `message`.
Una lista sin resultados devuelve `{"success":true,"data":[]}`.
Una reservación ajena y una inexistente producen el mismo 404, sin revelar su dueño.
Los listados no tienen paginación en esta entrega.

## Crear

```json
{
  "vehiculoId": 3,
  "fechaInicio": "2027-01-10T10:00:00",
  "fechaFin": "2027-01-13T10:00:00",
  "observaciones": "Recoger en sucursal"
}
```

Usar fechas futuras al probar. `vehiculoId` es obligatorio y positivo; ambas fechas
son obligatorias, inicio debe estar en el futuro y fin debe ser posterior al inicio.
Observaciones es opcional y admite hasta 2000 caracteres.
Los campos adicionales ignorados por Jackson no determinan dueño, creador, estado,
tarifa ni total: el DTO de entrada no contiene esos atributos.

La creación reutiliza `prepararAlta` del servicio del personal: cliente activo,
bloqueo del vehículo, estado DISPONIBLE o RESERVADO, validación de traslapes,
selección de una única tarifa vigente por categoría en la fecha de inicio y límite
máximo de total. La operación completa es transaccional.

Se guarda `PENDIENTE`; los días se redondean hacia arriba, mínimo uno.
`totalEstimado = días × tarifaDia`, sin cargos de devolución. La tarifa queda
copiada como valor histórico (RN-07). No se acepta un precio enviado por el portal.
Un traslape se rechaza con 409, sin guardar una segunda reservación.

### Respuesta de reserva (GET, POST y cancelación)

```json
{
  "success": true,
  "data": {
    "id": 10,
    "vehiculoId": 3,
    "fechaInicio": "2027-01-10T10:00:00",
    "fechaFin": "2027-01-13T10:00:00",
    "estado": "PENDIENTE",
    "tarifaDia": 850.00,
    "totalEstimado": 2550.00
  }
}
```

No se exponen identificadores del personal ni observaciones internas. Las fechas
usan LocalDateTime sin zona horaria, según la convención actual del backend.

## Cancelación

Solo el dueño puede cancelar estados `PENDIENTE` o `CONFIRMADA`, según el flujo
existente documentado. No se añade plazo de anticipación ni penalización nueva.
`EN_CURSO`, `FINALIZADA` y `CANCELADA` se rechazan con 409; repetir la cancelación
no es una operación exitosa idempotente en este contrato.

La cancelación conserva el registro y los importes. Bloquea primero el vehículo
y después lee la reservación con FOR UPDATE, siguiendo el orden de entrega.
Revalida dueño, estado y que el vehículo no haya cambiado mientras esperaba.
Una confirmada libera el auto únicamente si no hay otras confirmadas o rentas en
curso y el vehículo sigue RESERVADO; no altera RENTADO, MANTENIMIENTO ni BAJA.
Las pruebas funcionales no acreditan una ejecución concurrente real en MySQL.

## Historial de rentas

Incluye solo reservaciones propias con entrega registrada y estado `EN_CURSO` o
`FINALIZADA`. Excluye pendientes, confirmadas y canceladas. Orden: entrega más
reciente primero, con ID de reservación descendente como desempate.

Campos: `reservacionId`, `vehiculoId`, `marca`, `modelo`, `estado`, `fechaEntrega`,
`fechaDevolucionPrevista`, `fechaDevolucionReal`, `tarifaDia`, `totalEstimado`,
`totalFinal`. La devolución real y el total final son nulos si no hay devolución.
Los importes se leen de los registros históricos, no se recalculan con el catálogo.
El historial no permite POST, PUT, PATCH ni DELETE (403 con sesión de cliente).

## Migración V6

`V6__creador_cliente_reservaciones.sql` permite `creado_por_id` nulo y agrega
`creado_por_cliente_id` con clave foránea a clientes. Un CHECK exige exactamente un
creador: empleado o cliente; si es cliente, debe coincidir con `cliente_id`.
Las filas anteriores conservan su creador empleado, sin actualización masiva.

Flyway debe aplicar V6 antes de ejecutar esta versión sobre MySQL. No modificar
V1 ni volver a aplicar manualmente la migración. Verificar con el equipo que no
exista otra migración V6 en ramas pendientes antes de integrar el PR.

## Pruebas

Desde la raíz, con Java 21:

```powershell
cd backend
.\mvnw.cmd test
```

Última ejecución reportada por el desarrollador: **769 pruebas del backend,
0 fallos, 0 errores y 0 omitidas**, el 21 de octubre de 2026. Es una ejecución
exitosa de casos, no un porcentaje de cobertura del código.

| Archivo de prueba | Comprueba |
|---|---|
| `ReservacionClienteRepositoryTest` | Aislamiento de consultas e historial, orden, datos de devolución y listas vacías |
| `ReservacionClienteControllerTest` | Contratos HTTP, identidad, permisos, datos privados y operaciones rechazadas |
| `EsquemaReservacionesV6Test` | Migración, preservación del creador anterior y restricciones |
| `ReservacionPortalAltaTest` | Creación y cancelación con servicio real y H2, tarifas, estados y rechazo sin guardar |

## Comprobación manual pendiente

Con MySQL de desarrollo, Flyway y backend disponibles:

1. Iniciar sesión con dos clientes diferentes (A y B) y guardar sus tokens.
2. Crear una reservación futura como A con un vehículo y tarifa válidos.
3. Verificar cliente y creador en base, estado PENDIENTE e importes calculados.
4. Consultar como A; como B, comprobar que no aparece y su detalle devuelve 404.
5. Intentar cancelar esa reserva como B: 404 y estado intacto.
6. Intentar otra reserva traslapada: 409 y ninguna fila adicional.
7. Cancelar como A: 200 y estado CANCELADA; repetir: 409.
8. Crear otra reserva y, con personal autorizado, confirmar y registrar entrega.
9. Verificar historial de A y ausencia en B; cancelar la renta EN_CURSO: 409.
10. Registrar devolución con personal; revisar fecha real y total final en historial.
11. Probar sin token (401), con token de personal (403) y escritura del historial (403).
12. Probar entrega y cancelación simultáneas contra MySQL: solo una debe completar
    la transición válida; no debe quedar entrega con reservación CANCELADA.

Ejemplo PowerShell (ajustar host, puerto y datos):

```powershell
$tokenCliente = "TOKEN_CLIENTE"
$headersCliente = @{ Authorization = "Bearer $tokenCliente" }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/cliente/reservaciones" -Headers $headersCliente
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/cliente/reservaciones/historial" -Headers $headersCliente
```

Pendiente adjuntar evidencia MySQL, incluida migración y concurrencia. La pantalla
del portal se integra por separado consumiendo estos endpoints.
