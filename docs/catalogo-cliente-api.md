# S4-05 — Catálogo de vehículos del cliente

## Contrato

`GET /api/v1/catalogo/vehiculos`

Consulta pública, sin token obligatorio y de solo lectura. No crea ni modifica
vehículos, categorías, tarifas ni reservaciones. No requiere tablas o migraciones
nuevas. La integración de la pantalla del portal es una tarea separada.

| Parámetro opcional | Regla |
|---|---|
| `categoriaId` | Entero positivo dentro del rango de Long |
| `precioMin` | Entre 0 y 99999999.99, hasta dos decimales significativos |
| `precioMax` | Entre 0 y 99999999.99, hasta dos decimales significativos |

Los filtros se combinan con AND. Los límites de precio son inclusivos;
`precioMin` no puede ser mayor que `precioMax`. Una categoría inexistente o
un rango sin coincidencias devuelve una lista vacía, no un error.

Ejemplo:

```text
GET /api/v1/catalogo/vehiculos?categoriaId=1&precioMin=500&precioMax=1000
```

### Respuesta 200

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "marca": "Nissan",
      "modelo": "Kicks",
      "anio": 2024,
      "color": "Blanco",
      "categoriaId": 1,
      "categoria": "SUV",
      "tarifaDia": 850.00,
      "tieneImagen": true
    }
  ]
}
```

El color puede ser nulo. No se exponen placa, VIN, kilometraje, datos de clientes
ni datos de usuarios del personal. Sin resultados: `{"success":true,"data":[]}`.

`tieneImagen` indica si el vehículo tiene foto registrada desde el panel (columna
`imagen`, migración V4). La foto no viaja en la lista porque cada una puede pesar
hasta 2 MB; se pide por separado.

## Foto del vehículo

`GET /api/v1/catalogo/vehiculos/{id}/imagen`

Consulta pública de solo lectura. Devuelve la foto como archivo binario con su
`Content-Type` (`image/png`, `image/jpeg`, `image/webp` o `image/gif`) y
`Cache-Control: max-age=600, public`, para usarla directamente en `<img src>`.

| Situación | HTTP |
|---|---|
| Vehículo con foto válida y que no está en `BAJA` | 200 con la imagen |
| Sin foto, vehículo inexistente o en `BAJA` | 404 sin cuerpo |
| Contenido guardado que no es una data URL de imagen permitida (por ejemplo SVG o HTML) | 404 |
| `id` menor o igual a cero | 404 sin consultar la base |
| Escritura por esta ruta, incluso de Administrador | 403 |

Cualquier otra ruta bajo `/api/v1/catalogo/` sigue bloqueada.

| Situación | HTTP y comportamiento |
|---|---|
| Consulta válida, incluso vacía | 200 |
| Categoría inválida, precio inválido o rango invertido | 400, `success: false` y `message` |
| Error de consulta a base de datos | 500, mensaje genérico sin SQL interno |
| Escritura por esta ruta con sesión, incluso de Administrador | 403 |
| Escritura sin sesión | 401 según la política de Spring Security |

## Disponibilidad y precio

- Solo se muestran vehículos `DISPONIBLE` con categoría activa.
- Se excluyen `RESERVADO`, `RENTADO`, `MANTENIMIENTO` y `BAJA`.
- La tarifa se obtiene por categoría, activa y vigente en la fecha de hoy del
  servidor. Inicio y fin de vigencia son inclusivos; fin nulo significa sin límite.
- Se exige exactamente una tarifa vigente: sin tarifa o con traslapes, el vehículo
  no se ofrece. No se escoge un precio arbitrario ni se duplican vehículos.
- Una renta `EN_CURSO`, aunque esté vencida, impide ofrecer la unidad incluso
  si su estado se cambió incorrectamente a `DISPONIBLE`.
- Una reservación `PENDIENTE` o `CONFIRMADA` impide ofrecerla cuando
  `fecha_inicio <= ahora < fecha_fin`. Las canceladas y finalizadas no bloquean.
- La lista se ordena por marca, modelo e ID.

**Alcance temporal:** el endpoint consulta disponibilidad actual, sin recibir fechas
solicitadas de renta. Una reservación futura pendiente no oculta un auto actualmente
disponible. La creación de una reservación debe revalidar disponibilidad para todo
su periodo y seleccionar la tarifa aplicable a su fecha de inicio. El precio mostrado
hoy no garantiza la tarifa de un inicio futuro. La consulta no bloquea vehículos ni
reserva inventario; otra operación puede cambiar su disponibilidad después.

Los valores del usuario se envían como parámetros JDBC, sin concatenarlos al SQL.

## Archivos

| Archivo | Responsabilidad |
|---|---|
| `VehiculoCatalogoResponse.java` | Contrato público de datos, incluido `tieneImagen` |
| `CatalogoRepository.java` | Consulta JDBC, disponibilidad, tarifa, filtros y foto |
| `CatalogoController.java` | Rutas GET de la lista y de la foto, validación de filtros |
| `CatalogoExceptionHandler.java` | Errores 400 del catálogo |
| `SecurityConfig.java` | Lectura pública y bloqueo de escrituras |
| `CatalogoRepositoryTest.java` | Consulta real en H2: estados, vigencias, filtros y compromisos |
| `CatalogoControllerTest.java` | MockMvc: JSON, validaciones, errores y permisos |

## Validación automatizada

Requiere Java 21. Desde la raíz en PowerShell:

```powershell
cd backend
.\mvnw.cmd test
```

Solo el catálogo:

```powershell
.\mvnw.cmd "-Dtest=CatalogoRepositoryTest,CatalogoControllerTest" test
```

El desarrollador reportó 554 pruebas aprobadas para la consulta inicial y 574
tras agregar filtros. Este último avance agrega 11 casos; requiere ejecutar de nuevo
la suite. Esos totales son del backend completo, no porcentajes de cobertura.
H2 y MockMvc no sustituyen la comprobación manual contra MySQL y el portal.

## Guion manual para revisión

Con backend y MySQL de desarrollo en ejecución:

1. Identificar una categoría activa, un auto disponible y una única tarifa vigente.
2. Consultar sin token y comprobar categoría y precio contra la base.
3. Consultar por categoría y precios mínimo/máximo; probar el precio exacto.
4. Probar una categoría inexistente: 200 con lista vacía.
5. Probar `precioMin=1000&precioMax=500` y `precioMin=texto`: 400 controlado.
6. En datos de desarrollo, cambiar un vehículo a mantenimiento y verificar que
   desaparece; restaurar su estado al terminar la prueba.
7. Verificar que las rentas en curso y las reservaciones que ocupan el momento
   actual no se ofrecen; no se deben alterar datos reales para esta comprobación.
8. Con token de cliente, intentar POST o PATCH contra la ruta: 403 y ningún cambio.
9. Revisar que el JSON no contenga placa, VIN ni datos privados.
10. Subir una foto a un vehículo desde el panel y abrir
    `/api/v1/catalogo/vehiculos/{id}/imagen` sin sesión: se ve la imagen.
11. Un vehículo sin foto responde 404 en esa ruta y `tieneImagen: false` en la lista.

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/catalogo/vehiculos"
Invoke-RestMethod "http://localhost:8080/api/v1/catalogo/vehiculos?categoriaId=1&precioMin=500&precioMax=1000"
```

La pantalla del portal (S4-08) usa estas rutas en `index.html`, `catalogo.html`
y `vehiculo.html`. Pendiente hasta aportar evidencia: comprobación manual contra MySQL. Ajustar host y puerto a la configuración de desarrollo.
