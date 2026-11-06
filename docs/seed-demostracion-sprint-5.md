# S5-04 — Seed de demostración (avances 1 a 5)

## Alcance actual

Crea diez clientes ficticios: `demo01@s504.example` a `demo08@s504.example`
quedan APROBADO y activos; `demo09@s504.example` y `demo10@s504.example`
quedan PENDIENTE e inactivos, visibles en el panel de aprobación de S5-09.
Las licencias son S504-DEMO-01 a S504-DEMO-10, con vencimiento a tres años.
La contraseña del entorno se guarda con BCrypt; nombres y contactos son sintéticos.

No restablece contraseñas ni elimina datos. Las coincidencias parciales de
correo/licencia provocan error antes de insertar. Para compatibilidad con el
primer avance, asigna APROBADO a las ocho cuentas demo anteriores únicamente
si aún no tienen estado, están activas y tienen credenciales de portal.
No reactiva cuentas desactivadas ni cambia aprobaciones o rechazos del personal.
Los nuevos aprobados tienen fecha de revisión ficticia del momento de carga;
los pendientes no tienen fecha de revisión.

Se requiere S5-09 y la migración V7, ya incorporadas desde main 3b3f06a.
Aprobación y activación son conceptos distintos. No se agrega una migración propia.
La carga sigue siendo transaccional y opcional.

Pendiente validar la carga en MySQL y las pantallas de la app. Puede hacerse
localmente; no requiere un servidor remoto.

## Catálogo demo

Carga cinco vehículos disponibles: Tsuru, Audi Q7, Tesla Model 3, BMW Serie 3 y
Ferrari F8. Los identifica con placas `S504-DEMO-01` a `S504-DEMO-05` y VIN
ficticios. Conserva los vehículos ya registrados y rechaza colisiones parciales.

Crea las categorías Demo Económico, Demo SUV y Demo Lujo, con tarifas diarias
iniciales de $500, $850 y $1,800. Si existe una tarifa vigente, la conserva.
Si hay una tarifa futura, la vigencia creada termina el día anterior.

Maven empaqueta las cinco fotos existentes del frontend como recursos del backend,
sin duplicarlas en el repositorio. Compilar desde el repositorio completo, con
`renta-autos-app` junto a `backend`. El seed guarda las fotos en el formato
que consume el catálogo actual.

## Reservaciones y rentas demo

El tercer initializer corre después de clientes y catálogo, con la misma activación
opcional. Inserta cinco escenarios para `demo01@s504.example`:

| Estado | Vehículo | Fechas relativas a la primera carga |
|---|---|---|
| PENDIENTE | Tsuru | Inicio en 5 días, por 3 días |
| CONFIRMADA | Audi Q7 | Inicio en 2 días, por 3 días |
| EN_CURSO | Tesla Model 3 | Inicio ayer, devolución dentro de 2 días |
| FINALIZADA | BMW Serie 3 | Inicio hace 6 días, devolución hace 3 días |
| CANCELADA | Ferrari F8 | Inicio en 9 días, por 3 días |

Las fechas parten de las 09:00 del día local del servidor. Se guarda una entrega
para la renta en curso y otra para la finalizada. Esta última tiene devolución
registrada en plazo, sin daños ni atraso: total final igual a tres días de tarifa.
El creador es el cliente demo; quien entrega/recibe es el administrador ficticio
`admin@demo.local`, localizado por correo y rol, sin asumir su ID.

Audi queda RESERVADO y Tesla RENTADO. Tsuru, BMW y Ferrari siguen disponibles,
salvo que la presentación cambie posteriormente su estado. Las consultas actuales
del catálogo, Mis reservaciones, Mis rentas y rentas activas consumen estos datos.
Otros clientes no ven las reservaciones de demo01.

Se selecciona una tarifa vigente en el inicio de cada escenario y se conserva como
snapshot. Para fechas pasadas sin tarifa, se completa únicamente el hueco histórico
de la categoría demo con el precio vigente actual, sin editar vigencias existentes.
Es una tarifa ficticia de reconstrucción para la presentación.

Los escenarios se identifican por observaciones `[S504-DEMO-ESTADO]`. No editar
esas marcas. Repetir la carga conserva las fechas, importes y estados ya guardados;
no desplaza fechas ni restaura la presentación. Si pasan varios días, la renta en
curso puede aparecer retrasada, lo cual es coherente con que no se ha devuelto.
No se elimina ni reinicia la base automáticamente.

La carga construye un escenario sintético dentro de una transacción; no ejecuta
los endpoints ni genera evidencias de acciones reales. Comprueba identidad demo,
estado del vehículo y compromisos previos; usa bloqueos antes de insertar. No
reemplaza reservaciones existentes ni fuerza unidades de mantenimiento a disponibles.

## Activación opcional

El bean requiere perfil `demo`, ausencia de `prod` y `production`, y propiedad
`app.demo.seed.enabled=true`. En el perfil demo, `DEMO_SEED_ENABLED` está
apagado por defecto. `DEMO_SEED_PASSWORD` debe tener al menos 12 caracteres
y no exceder 72 bytes UTF-8; no se imprime ni se guarda como texto en la base.
La variable se necesita también al repetir una carga habilitada.

`application-demo.yml` apunta por defecto a una base separada `rentadeautos_demo`.
`DEMO_DB_URL` permite cambiar ese destino. Verificar la URL efectiva y usar
únicamente datos de demostración: el perfil no inspecciona ni certifica el contenido
de la base. Las credenciales MySQL y JWT siguen la configuración del proyecto.

Para la carga manual, una vez confirmada la base demo y disponibles sus credenciales,
activar en la terminal el perfil `demo`, `DEMO_SEED_ENABLED=true` y
`DEMO_SEED_PASSWORD` con una contraseña elegida localmente. Iniciar el backend
con Maven y comprobar los ocho correos. Para excluir la carga, dejar la propiedad
apagada o no activar `demo`. Nunca incluir contraseñas del servidor en commits.
Usar solo `demo` para este entorno: los initializers antiguos dev/default se
mantienen sin cambios en este avance y no se activan con demo por sí solo.

## Verificación

Desde `backend`, Java 21:

```powershell
.\mvnw.cmd "-Dtest=ClienteDemoInitializerTest,ClienteDemoActivacionTest,CatalogoDemoInitializerTest,ReservacionesDemoInitializerTest" test
.\mvnw.cmd test
```

Hay 29 casos específicos: trece de clientes con H2, cinco de activación del
contexto, cuatro del catálogo y siete de reservaciones/rentas. Se comprueba
la visibilidad de pendientes y que no puedan iniciar sesión con contraseña correcta.
No requieren conexión al servidor. Pendiente ejecutar contra MySQL de demostración
cuando el SM confirme el acceso y la base destino. No eliminar el perfil ni los datos
para volver a cargar: ejecutar otra vez conserva los registros ya creados.

## Coordinación con Docker y S5-09

El main 3b3f06a incorpora Docker. Antes de integrar y desplegar esta rama hay que
coordinar el empaquetado de fotos: el Dockerfile actual compila solo backend,
mientras el POM del seed toma las imágenes de renta-autos-app. El build debe
disponer de ambas carpetas para incluirlas. No se ha validado ese despliegue aún.
El compose actual activa prod; el seed se bloquea deliberadamente en ese perfil.
Usar un entorno demo dedicado y acordar su configuración con el SM.

S5-09 ya está incorporado. Los dos pendientes completan los diez clientes del
conjunto inicial. Para ensayar aprobar y rechazar se usa la pantalla existente;
repetir el seed conserva las decisiones y no vuelve a ponerlos pendientes.
Si ya fueron revisados durante la demo, que no aparezcan como pendientes es el
resultado esperado. No se borra ni reinicia la base automáticamente.

## Avance 5 Ampliación de datos ficticios

Se agregan 50 clientes, 150 vehículos sin fotos y 54 escenarios operativos al
conjunto inicial, por solicitud del SM. Son datos sintéticos; se usan nombres
verosímiles y correos bajo el dominio reservado s504-ampliado.example.
Los teléfonos son ficticios y las licencias llevan el prefijo S504-CLI-.

- ClientelaDemoInitializer, orden 52: 40 activos (portal aprobado o mostrador),
  6 pendientes INACTIVOS y 4 rechazados INACTIVOS. No restablece contraseñas
  ni decisiones del personal sobre clientes ya creados.
- FlotaDemoInitializer, orden 55: 10 categorías, 150 vehículos con placas
  S504-FLT-001 a S504-FLT-150 y VIN propios. 16 inician en mantenimiento,
  8 en baja y 126 disponibles. Las categorías existentes se conservan; se
  completan huecos de tarifa con vigencias sin traslape desde seis meses atrás.
  No se modifica una tarifa existente ni se reactiva una categoría inactiva.
- OperacionDemoInitializer, orden 70: 18 EN_CURSO, 14 FINALIZADAS,
  12 CONFIRMADAS, 6 PENDIENTES y 4 CANCELADAS; 32 entregas y 14 devoluciones.
  Solo selecciona identidades de esta ampliación, no registros manuales.
  Valida compromisos previos y copia la tarifa vigente para la fecha de inicio.
  Requiere operadores ficticios activos autorizados: no usa cualquier usuario
  como reemplazo si faltan.

Cada escenario usa un vehículo distinto y se identifica como [S504-OP:01] a
[S504-OP:54] en observaciones. No editar estas marcas. Una carga posterior
conserva estados y fechas y completa los escenarios faltantes si todavía pueden
crearse válidamente; no fuerza vehículos comprometidos. Las devoluciones
coinciden con el kilometraje actual del auto y sus importes pactados.
Las cargas tienen transacciones separadas por initializer: si falla operación,
clientela y flota ya cargadas pueden conservarse. No es una única transacción
para todos los initializers. No se ha validado concurrencia entre dos arranques
de la aplicación: ejecutar la carga en una sola instancia demo.

En una base inicialmente vacía, los totales combinados son 60 clientes,
155 vehículos, 13 categorías y 59 reservaciones; los datos preexistentes
pueden cambiar esos totales. Los 150 vehículos nuevos no tienen foto: los
cinco del seed original sí. Mantiene el perfil demo y la activación explícita.

Las pruebas de la ampliación son 22: 7 de clientela, 6 de flota y 9 de operación.
Combinadas con las 29 anteriores suman 51 casos específicos. Su ejecución en el
equipo del desarrollador sigue pendiente hasta obtener el resumen de Maven.

Desde backend, Java 21:

```powershell
.\mvnw.cmd "-Dtest=ClienteDemoInitializerTest,ClienteDemoActivacionTest,CatalogoDemoInitializerTest,ReservacionesDemoInitializerTest,ClientelaDemoInitializerTest,FlotaDemoInitializerTest,OperacionDemoInitializerTest" test
```
