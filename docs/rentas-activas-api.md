# S3-11 — Consulta de rentas activas

## Alcance y reglas

Consulta las reservaciones cuyo estado es `EN_CURSO`, iniciado por la entrega de
vehículo (S3-09). La consulta no modifica datos ni calcula cargos.

- Incluye rentas vencidas mientras continúen `EN_CURSO`.
- Excluye `PENDIENTE`, `CONFIRMADA`, `CANCELADA` y `FINALIZADA`.
- Ordena por devolución prevista ascendente y, en empate, por ID de reservación.
- Conserva la visibilidad de una renta aunque el cliente esté inactivo.
- Devuelve la tarifa diaria y el total estimado guardados en la reservación (RN-07).
- `fechaInicio` es el inicio programado de la reservación; no es la hora efectiva
  de entrega. `fechaDevolucionPrevista` corresponde a `reservaciones.fecha_fin`.
- Las fechas se serializan en ISO 8601 sin zona horaria, como los demás campos
  `LocalDateTime` del proyecto.

## Contrato REST

`GET /api/v1/rentas/activas`

Requiere `Authorization: Bearer <token>`. En esta entrega el acceso es exclusivo
para **ADMINISTRADOR**, siguiendo el alcance del panel de reservaciones.
La ampliación a otros roles debe acordarse con el equipo y la matriz de permisos.
No recibe parámetros ni cuerpo; devuelve todas las rentas activas sin paginación.

### Respuesta 200

```json
{
  "success": true,
  "data": [
    {
      "reservacionId": 10,
      "clienteId": 2,
      "clienteNombre": "Ana Pérez",
      "clienteTelefono": "8441234567",
      "vehiculoId": 3,
      "marca": "Nissan",
      "modelo": "Kicks",
      "placa": "DEM1234",
      "fechaInicio": "2026-10-01T10:00:00",
      "fechaDevolucionPrevista": "2026-10-04T10:00:00",
      "estado": "EN_CURSO",
      "tarifaDia": 850.00,
      "totalEstimado": 2550.00
    }
  ]
}
```

Sin rentas activas devuelve HTTP 200 y `{"success":true,"data":[]}`.

| Caso | HTTP |
|---|---|
| Consulta de Administrador, con o sin resultados | 200 |
| Sin autenticación válida | 401 |
| Otro rol autenticado | 403 |
| POST, PUT, PATCH o DELETE, incluso de Administrador | 403 |

Los errores de seguridad usan `success: false` y un `message` descriptivo.

## Implementación

| Archivo | Responsabilidad |
|---|---|
| `RentaActivaResponse.java` | Datos que devuelve la API |
| `RentaActivaRepository.java` | Consulta SQL con clientes y vehículos; filtro por estado y orden |
| `RentaActivaService.java` | Ejecuta la consulta dentro de una transacción de solo lectura |
| `RentaActivaController.java` | Publica GET y envuelve la lista en `ApiResponse` |
| `SecurityConfig.java` | Restringe el acceso y bloquea métodos de escritura |

No requiere tablas ni migraciones nuevas. El frontend de rentas activas se integra
por separado consumiendo este contrato.

## Pruebas automatizadas

Desde la raíz del repositorio, en PowerShell:

```powershell
cd backend
.\mvnw.cmd test
```

Requiere Java 21. Las pruebas usan H2 en memoria y mocks; no necesitan MySQL
ni Docker en ejecución.

- `RentaActivaRepositoryTest`: 6 casos sobre consulta vacía, estados, datos
  operativos, orden, rentas vencidas con cliente inactivo y cierre de la renta.
- `RentaActivaControllerTest`: 12 casos contando parámetros; datos JSON,
  lista vacía, ausencia de autenticación, cinco roles rechazados y cuatro
  métodos de escritura bloqueados.

Validación reportada en el equipo del desarrollador: **344 pruebas, 0 fallos,
0 errores, 0 omitidas**, el 2 de octubre de 2026. El total corresponde a todo el
backend; este issue agregó 18 casos. No acredita una prueba manual contra MySQL.

## Guion de comprobación manual

Con el backend y su base de desarrollo disponibles:

1. Obtener un token de Administrador según [autenticacion.md](autenticacion.md).
2. Consultar GET y anotar los IDs actuales (puede haber datos previos).
3. Crear una reservación y confirmarla mediante el flujo de reservaciones.
   Esa reservación aún no debe aparecer en rentas activas.
4. Registrar su entrega siguiendo [entregas-api.md](entregas-api.md).
5. Volver a consultar GET: debe aparecer el ID con `EN_CURSO`, cliente,
   vehículo y fecha de devolución coincidentes con la reservación.
6. Comprobar que la tarifa y el total coincidan con los valores históricos,
   aunque se edite después el catálogo de tarifas.
7. Consultar sin token (401) y con otro rol autenticado (403).
8. Cuando esté integrada la devolución (S3-10), finalizar esa renta y comprobar
   que desaparece. El caso equivalente ya se verifica con H2 en el repositorio.

Ejemplo PowerShell, reemplazando el token con uno válido:

```powershell
$tokenAdministrador = "TOKEN_DEL_ADMINISTRADOR"
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/rentas/activas" `
  -Headers @{ Authorization = "Bearer $tokenAdministrador" }
```

Pendiente para la revisión: ejecutar el guion contra MySQL y adjuntar evidencia.
