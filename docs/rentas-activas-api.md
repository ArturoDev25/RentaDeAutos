# S3-11 — Consulta de rentas activas

## Alcance y reglas

Consulta las reservaciones cuyo estado es `EN_CURSO`, iniciado por la entrega de
vehículo (S3-09). La consulta no modifica datos ni calcula cargos.

- Incluye rentas vencidas mientras continúen `EN_CURSO`.
- `retrasada` es verdadero cuando la devolución prevista es anterior al momento
  de consulta. Si coincide exactamente o está en el futuro, es falso. Se toma
  un único momento de referencia por consulta, usando la zona local del servidor.
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

Requiere `Authorization: Bearer <token>`. La consulta está permitida a
**ADMINISTRADOR, AGENTE, SUPERVISOR y AUDITOR**, conforme a US-11.
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
      "totalEstimado": 2550.00,
      "retrasada": true
    }
  ]
}
```

Sin rentas activas devuelve HTTP 200 y `{"success":true,"data":[]}`.

| Caso | HTTP |
|---|---|
| Consulta de personal autorizado, con o sin resultados | 200 |
| Sin autenticación válida | 401 |
| Rol sin permisos (por ejemplo, Cliente o Rentero) | 403 |
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

- `RentaActivaRepositoryTest`: 9 casos sobre consulta vacía, estados, datos
  operativos, orden, rentas vencidas con cliente inactivo y cierre de la renta, más tres límites de retraso con reloj fijo.
- `RentaActivaControllerTest`: 12 casos contando parámetros; datos JSON,
  lista vacía, ausencia de autenticación, tres roles adicionales autorizados, dos roles rechazados y cuatro
  métodos de escritura bloqueados.

Validación reportada en el equipo del desarrollador: **344 pruebas, 0 fallos,
0 errores, 0 omitidas**, el 2 de octubre de 2026. El total corresponde a todo el
backend; la versión inicial del issue agregó 18 casos.
Las correcciones de permisos y retraso requieren una nueva ejecución; agregan
3 casos respecto a esa versión. No acredita una prueba manual contra MySQL.

## Guion de comprobación manual

Con el backend y su base de desarrollo disponibles:

1. Obtener un token de Administrador según [autenticacion.md](autenticacion.md).
2. Consultar GET y anotar los IDs actuales (puede haber datos previos).
3. Crear una reservación y confirmarla mediante el flujo de reservaciones.
   Esa reservación aún no debe aparecer en rentas activas.
4. Registrar su entrega siguiendo [entregas-api.md](entregas-api.md).
5. Volver a consultar GET: debe aparecer el ID con `EN_CURSO`, cliente,
   vehículo y fecha de devolución coincidentes con la reservación.
   Verificar además `retrasada`: true si ya pasó la devolución prevista,
   false si aún no pasó.
6. Comprobar que la tarifa y el total coincidan con los valores históricos,
   aunque se edite después el catálogo de tarifas.
7. Consultar sin token (401) y con Agente, Supervisor y Auditor (200), y con un rol sin permisos (403).
8. Cuando esté integrada la devolución (S3-10), finalizar esa renta y comprobar
   que desaparece. El caso equivalente ya se verifica con H2 en el repositorio.

Ejemplo PowerShell, reemplazando el token con uno válido:

```powershell
$tokenAdministrador = "TOKEN_DEL_ADMINISTRADOR"
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/rentas/activas" `
  -Headers @{ Authorization = "Bearer $tokenAdministrador" }
```

## Evidencia de comprobación manual contra MySQL

**Fecha:** 4 de octubre de 2026
**Rama:** `feature/S3-11-rentas-activas`
**Entorno:** MySQL 8.0 en Docker, backend Spring Boot en `localhost:8080`

### Conectividad y flujo principal

- MySQL: `Up (healthy)`.
- `GET /api/v1/health`: HTTP 200, `status: UP`, `database: UP`.
- Login de Administrador: correcto; rol `ADMINISTRADOR`.
- Consulta inicial de `GET /api/v1/rentas/activas`: HTTP 200, 0 rentas activas.
- Reservación de prueba creada con ID `4`: `PENDIENTE`.
- Reservación confirmada mediante `POST /api/reservaciones/4/confirmar`: `CONFIRMADA`.
- La reservación confirmada no apareció en rentas activas.
- Entrega registrada mediante `POST /api/v1/entregas`: HTTP 201,
  reservación `EN_CURSO` y vehículo `RENTADO`.
- `GET /api/v1/rentas/activas`: HTTP 200; apareció la reservación `4`.

### Comparación API contra MySQL

La consulta SQL equivalente devolvió:

| ID | Cliente | Vehículo | Estado | Fecha de devolución | Tarifa diaria | Total estimado |
|---:|---|---|---|---|---:|---:|
| 4 | s s | Audi Q7 / STU-9012 | EN_CURSO | 2026-10-06 21:11:06 | 1800.00 | 5400.00 |

La respuesta JSON devolvió los mismos valores:

```json
{
  "reservacionId": 4,
  "clienteId": 2,
  "clienteNombre": "s s",
  "clienteTelefono": "844355557799",
  "vehiculoId": 7,
  "marca": "Audi",
  "modelo": "Q7",
  "placa": "STU-9012",
  "estado": "EN_CURSO",
  "tarifaDia": 1800.00,
  "totalEstimado": 5400.00,
  "retrasada": false
}
```

- La reservación vencida se probó temporalmente conservando `fecha_fin` posterior
  a `fecha_inicio`; el endpoint mantuvo el registro y devolvió `retrasada: true`.
- La fecha de devolución original se restauró después de la prueba.
- La tarifa diaria y el total estimado coincidieron con los valores históricos
  almacenados en `reservaciones`.
- El orden de la consulta SQL y de la respuesta siguió `fecha_fin ASC, id ASC`.


### Resultado

**Comprobación manual contra MySQL ejecutada y aprobada.** El endpoint cumple el
flujo de consulta de rentas `EN_CURSO`, incluye rentas vencidas, conserva los
valores históricos, ordena correctamente y aplica los permisos documentados.
