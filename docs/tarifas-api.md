# S3-05 — CRUD de tarifas por categoría

## Alcance y datos

Backend REST para crear, consultar, editar y desactivar tarifas. Utiliza la tabla
`tarifas` y la relación con `categorias` de V1; no requiere una migración nueva.
La pantalla pertenece a S3-06. No calcula devoluciones ni cargos finales.

Los importes usan `BigDecimal` y columnas `DECIMAL(10,2)`; las vigencias usan
`LocalDate`. `fechaFin = null` significa que el periodo no tiene término.

## Contrato REST

Todas las rutas requieren `Authorization: Bearer <token>`.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/v1/tarifas` | 200: todas las tarifas, incluidas inactivas, por fecha inicial e ID descendentes |
| GET | `/api/v1/tarifas/{id}` | 200: detalle; 404 si no existe |
| POST | `/api/v1/tarifas` | 201: nueva tarifa activa |
| PUT | `/api/v1/tarifas/{id}` | 200: modifica categoría, importes y vigencia; conserva el estado activo/inactivo |
| PATCH | `/api/v1/tarifas/{id}/desactivar` | 200: baja lógica, sin cuerpo; repetir la baja conserva el estado |

No existe eliminación física ni reactivación en este alcance.

### Cuerpo de POST y PUT

```json
{
  "categoriaId": 2,
  "precioDia": 850.00,
  "cargoAtrasoDia": 100.00,
  "fechaInicio": "2026-10-01",
  "fechaFin": "2026-10-31"
}
```

La categoría debe existir y estar activa. No se envían `id`, `activo` ni campos
de reservación. La API responde con el formato estándar:

```json
{
  "success": true,
  "data": {
    "id": 7,
    "categoriaId": 2,
    "categoriaNombre": "SUV",
    "precioDia": 850.00,
    "cargoAtrasoDia": 100.00,
    "fechaInicio": "2026-10-01",
    "fechaFin": "2026-10-31",
    "activo": true
  }
}
```

Por la configuración JSON existente, los campos nulos pueden omitirse.
Los errores utilizan `{"success":false,"message":"..."}`.

## Reglas y errores

- Categoría obligatoria y con ID positivo.
- Precio diario obligatorio, mayor que cero, hasta ocho enteros y dos decimales.
- Cargo de atraso obligatorio, no negativo, hasta ocho enteros y dos decimales.
- Fecha inicial obligatoria; fecha final opcional e igual o posterior a la inicial.
- Límites inclusivos: si una tarifa termina el día 10, la siguiente puede iniciar el 11.
- Solo las tarifas activas participan en la comprobación de traslapes por categoría.
- Al editar se excluye el propio ID; editar una tarifa inactiva no la reactiva.
- Las categorías se bloquean antes de consultar y guardar para serializar operaciones.
  Al cambiar de categoría se bloquean origen y destino en orden ascendente de ID.
- Las consultas de traslapes y del registro que se modifica usan bloqueo pesimista
  para obtener una lectura actual. Una categoría cambiada concurrentemente genera 409.
- La baja cambia `activo` a `false`; conserva ID, importes, fechas e historial.

| Código | Situación |
|---|---|
| 400 | Cuerpo o campos inválidos, fechas invertidas, categoría inexistente o inactiva |
| 401 | Sin autenticación válida |
| 403 | Rol sin permiso |
| 404 | Tarifa inexistente |
| 409 | Vigencia superpuesta o cambio concurrente de categoría detectado |

## Permisos existentes

Se reutiliza `SecurityConfig`; este cambio no modifica la matriz del proyecto.

| Acción | Administrador | Supervisor | Agente | Auditor |
|---|:---:|:---:|:---:|:---:|
| Consultar | Sí | Sí | Sí | Sí |
| Crear, editar, desactivar | Sí | Sí | No | No |

El rol CLIENTE no tiene acceso a estas rutas administrativas. El catálogo público
para clientes, si se implementa, requiere su propio contrato y permisos.

## Compatibilidad con reservaciones (RN-07)

`ReservacionService.crear()` obtiene el precio de la única tarifa activa cuya
vigencia incluye la fecha inicial de la renta y lo copia a `reservaciones.tarifa_dia`.
Editar o desactivar el catálogo no actualiza las reservaciones existentes.
Editar fechas conservando el vehículo mantiene la tarifa histórica; cambiar el
vehículo vuelve a seleccionar la tarifa de su categoría, según el módulo existente.

Si al desactivar una tarifa no queda otra vigente, nuevas reservaciones para ese
periodo se rechazan con 409. La baja no crea automáticamente un reemplazo.
Para sustituir una tarifa, el operador debe planificar las vigencias sin traslape.

## Pruebas y evidencia

Desde `backend`, con Java 21 y Maven Wrapper:

```powershell
.\mvnw.cmd test
```

También pueden ejecutarse los tests del módulo:

```powershell
.\mvnw.cmd "-Dtest=TarifaServiceTest,TarifaAltaServiceTest,TarifaEdicionServiceTest,TarifaBajaServiceTest,TarifaControllerTest,TarifaRepositoryTest" test
```

- Servicios: mapeo, existencia, reglas de fechas/categoría, rechazo de traslapes,
  cambio de categoría, conservación del estado y baja repetida.
- MockMvc: contrato HTTP, validación de importes y permisos.
- H2: consultas reales de vigencias inclusivas, periodos abiertos, categoría,
  exclusión del propio ID y conservación del registro tras la baja.
- Regresión existente: `editarMismoVehiculoConservaTarifaHistorica` y cálculo del
  estimado en `ReservacionServiceTest`.

Evidencia del desarrollador al 30 de septiembre de 2026: 294 casos ejecutados,
0 fallos, 0 errores y 0 omitidos; BUILD SUCCESS. El módulo agregó 64 casos.
Esto no sustituye la comprobación manual ni demuestra concurrencia real en MySQL.

## Comprobación manual pendiente en el entorno de prueba

Usar MySQL y el backend con JWT configurado. Docker no es obligatorio si ya hay
una instancia MySQL disponible. Ejecutar en datos de prueba y guardar evidencia
sin contraseñas ni tokens.

1. Iniciar sesión como Administrador y consultar `/api/categorias`; elegir una
   categoría activa y un periodo sin tarifas existentes.
2. Crear tarifa a $850 por día, cargo de atraso $100, con vigencia que cubra la
   fecha inicial de una reserva de prueba. Esperar 201 y anotar el ID.
3. Consultar listado y detalle; verificar importes, categoría y fechas.
4. Intentar alta con precio 0, cargo negativo y fechas invertidas: esperar 400.
5. Intentar otra tarifa de la misma categoría con fechas superpuestas: esperar 409.
   Comprobar además que compartir la fecha límite se rechaza y el día siguiente se permite.
6. Crear una reservación de tres días para un vehículo de esa categoría: comprobar
   tarifa histórica $850 y total estimado $2,550.
7. Editar la tarifa del catálogo a $900: esperar 200. Consultar la reservación
   anterior: debe conservar $850 y $2,550. Una nueva reserva válida debe usar $900.
8. Editar sin cambiar la vigencia: debe permitirse. Editar para invadir la vigencia
   de otra tarifa activa debe devolver 409.
9. Desactivar la tarifa: esperar 200 y `activo=false`; repetir la baja. El registro
   debe seguir en listado y detalle; la reservación anterior conserva su tarifa.
10. Sin otra tarifa vigente, una nueva reserva para ese periodo debe devolver 409.
    Crear una tarifa de reemplazo en el periodo liberado debe permitirse.
11. Comprobar 401 sin token y 403 al crear/editar/desactivar como Agente o Auditor.
12. En dos sesiones simultáneas, intentar crear tarifas para la misma categoría y
    periodo: solo una debe guardarse. Verificar en MySQL que no quedaron dos activas.

### Evidencia manual ejecutada (30 de septiembre de 2026)

Se ejecuto contra MySQL 8.0.46 en Docker y el backend con JWT:

- `POST /api/v1/tarifas`: 201; las validaciones de precio cero, cargo negativo
   y fechas invertidas devolvieron 400.
- Solapamiento en la fecha limite: 409; inicio al dia siguiente: 201.
- Reserva de tres dias: 201 con `tarifaDia=850.00` y `totalEstimado=2550.00`.
   Tras editar la tarifa a 900, la reserva conservo 850 y 2550.
- Baja logica y repeticion: 200 en ambos casos; el registro inactivo permanecio
   en MySQL. Sin tarifa vigente, una nueva reserva devolvio 409; el reemplazo
   devolvio 201.
- Permisos: sin token 401; Agente consulto con 200 y recibio 403 al crear.
- Concurrencia: dos altas simultaneas para la misma categoria y periodo
   devolvieron 201 y 409; MySQL conservo una sola tarifa activa.

La bateria focalizada de tarifas ejecuto 64 casos, con 0 fallos, 0 errores y 0
omitidos. Falta la revision de un compañero antes de cerrar la Definition of Done.
