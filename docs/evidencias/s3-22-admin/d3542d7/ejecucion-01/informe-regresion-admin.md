# S3-22 — Informe de pruebas aprobadas

## Datos de la ejecución

- Proyecto: Sistema de Renta de Autos.
- Tarea: S3-22, issue #91.
- Responsable: Gustavo Moreno.
- Fechas: 30 de septiembre y 1 de octubre de 2026 (America/Mexico_City).
- Versión evaluada: `d3542d7`. SHA completo en [commit.txt](commit.txt).
- Rama: `test/gustavo-s3-22-regresion-admin`.

Este documento presenta los resultados aprobados de la suite automatizada y de los casos manuales del administrador ejecutados y reportados por Gustavo. Su alcance se limita a las comprobaciones descritas; no constituye una certificación de todas las funcionalidades del sistema.

## Entorno

- Windows y Java 21.0.12.1.
- Backend Spring Boot 3.3.4.
- Pruebas automatizadas con H2 en memoria.
- Pruebas manuales con MySQL 8.0 en Docker, puerto 3307.
- Backend en localhost:8080 y frontend servido mediante Live Server.
- Navegador en ventana privada para las pruebas de acceso.

La preparación del entorno confirmó el contenedor MySQL en estado `healthy` y el endpoint `/api/v1/health` con `success: true`, `status: UP` y `database: UP`.

## Pruebas automatizadas aprobadas

Se ejecutó la suite existente completa del backend mediante Maven Wrapper, utilizando `clean test`. Esta suite abarca distintos módulos del sistema; no corresponde exclusivamente a pruebas de administrador.

| Indicador | Resultado |
|---|---:|
| Pruebas ejecutadas y aprobadas | 298 |
| Fallos | 0 |
| Errores | 0 |
| Código de salida | 0 |
| Duración | 42.146 segundos |

Resultado: **BUILD SUCCESS**. La ejecución terminó el 30/09/2026 a las 23:18:43, UTC-06:00.

## Casos manuales aprobados

Los siguientes **37 casos** fueron ejecutados por Gustavo y reportados como satisfactorios. Se conservan sus identificadores originales para mantener la trazabilidad.

| Caso | Comprobación y resultado observado | Estado |
|---|---|---|
| ADM-01 | Acceso directo al Dashboard sin sesión: redirigió al login. | Aprobada |
| ADM-02 | Contraseña incorrecta: rechazó el acceso con mensaje de credenciales inválidas. | Aprobada |
| ADM-03 | Credenciales de administrador correctas: ingresó directamente al Dashboard. | Aprobada |
| ADM-04 | Los siete módulos abrieron sin errores y el menú resaltó la sección correspondiente; no aparecieron módulos adicionales. | Aprobada |
| ADM-05 | Recarga de Categorías: conservó la sesión y cargó los datos. | Aprobada |
| ADM-06 | Categorías: impidió guardar el formulario vacío. | Aprobada |
| ADM-07 | Creó la categoría de prueba activa con los datos indicados. | Aprobada |
| ADM-08 | Rechazó una categoría con el mismo nombre e indicó que ya existía. | Aprobada |
| ADM-09 | Conservó la descripción editada y el depósito de 2000 después de recargar. | Aprobada |
| ADM-10 | Cancelar la desactivación de la categoría conservó el estado activo. | Aprobada |
| ADM-11 | La desactivación persistió al recargar y conservó los datos de la categoría. | Aprobada |
| ADM-12 | La categoría apareció o se ocultó correctamente en Activas, Inactivas y Todas. | Aprobada |
| ADM-13 | La reactivación persistió y la categoría apareció en Activas. | Aprobada |
| ADM-14 | Clientes: impidió guardar el formulario vacío. | Aprobada |
| ADM-15 | Registró el cliente de prueba como activo. | Aprobada |
| ADM-16 | Encontró al cliente por búsqueda y mostró correctamente una búsqueda sin coincidencias. | Aprobada |
| ADM-17 | Conservó el teléfono editado después de recargar. | Aprobada |
| ADM-18 | El detalle del cliente mostró sus datos correctamente y permitió cerrarlo. | Aprobada |
| ADM-19 | Cancelar la desactivación conservó al cliente activo. | Aprobada |
| ADM-20 | La desactivación del cliente persistió y funcionaron los filtros Activos, Inactivos y Todos. | Aprobada |
| ADM-21 | Reactivó al cliente y conservó sus datos después de recargar. | Aprobada |
| ADM-22 | Vehículos: impidió guardar el formulario vacío. | Aprobada |
| ADM-23 | Registró el vehículo de prueba con estado Disponible. | Aprobada |
| ADM-24 | Funcionaron la búsqueda por placa y la búsqueda sin coincidencias. | Aprobada |
| ADM-25 | Conservó el color Gris después de editar y recargar. | Aprobada |
| ADM-26 | El detalle del vehículo mostró los datos esperados. | Aprobada |
| ADM-27 | El filtro de categoría mostró los resultados correspondientes. | Aprobada |
| ADM-28 | Con la categoría seleccionada, el vehículo apareció en Disponible y no apareció en Mantenimiento. | Aprobada |
| ADM-29 | Limpiar filtros restableció los controles y actualizó la lista. | Aprobada |
| ADM-31 | Las secciones de Reportes cargaron sin errores. | Aprobada |
| ADM-32 | Los botones Actualizar de Reportes funcionaron. | Aprobada |
| ADM-34 | Auditoría cargó sin errores y mostró registros. | Aprobada |
| ADM-35 | Abrió el detalle correcto de auditoría y permitió cerrarlo. | Aprobada |
| ADM-36 | Filtró por el módulo Auditoría; las filas correspondieron al módulo y volver a Todos actualizó la lista. | Aprobada |
| ADM-37 | Cerrar sesión desde Auditoría redirigió al login. | Aprobada |
| ADM-38 | Después de cerrar sesión, acceder directamente al Dashboard regresó al login. | Aprobada |
| ADM-39 | Permitió iniciar sesión nuevamente y entrar al Dashboard. | Aprobada |

## Evidencias de ejecución

- [commit.txt](commit.txt): versión del código evaluada.
- [codigo-salida.txt](codigo-salida.txt): código de salida de Maven.
- [surefire-reports/](surefire-reports/): 24 reportes de texto de las clases de prueba presentes en el inventario de evidencia.
- Este informe: registro de los resultados manuales comunicados por el ejecutor.

## Resultado de los casos incluidos

Las **298 pruebas automatizadas** y los **37 casos manuales aquí documentados** finalizaron satisfactoriamente. Las verificaciones manuales cubrieron acceso del administrador, navegación, operaciones de Categorías, Clientes y Vehículos, carga y actualización de Reportes, consulta y filtrado de Auditoría y cierre de sesión.
