# S5-04 — Seed de demostración (avances 1 a 3)

## Alcance actual

Crea ocho clientes ficticios activos, con correo `demo01@s504.example` hasta
`demo08@s504.example`, licencias `S504-DEMO-01` a `S504-DEMO-08`, contraseña
BCrypt y vencimiento de licencia tres años después de la carga inicial.
Los nombres, teléfonos y domicilios son datos sintéticos.

No actualiza registros existentes, no restablece contraseñas ni elimina datos.
Las coincidencias parciales de correo/licencia provocan error antes de insertar.
La carga es transaccional. No requiere migración nueva.

Este avance aún NO completa S5-04: faltan los clientes pendientes de aprobación
y la validación del guion en el servidor de demostración.
S5-09 aún no está integrado en el main d9c93a5 usado como base. No se simula una
aprobación usando `activo`: aprobación y activación son conceptos diferentes.

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

Hay 25 casos específicos: nueve de clientes con H2, cinco de activación del
contexto, cuatro del catálogo y siete de reservaciones/rentas.
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

S5-09 sigue en su rama: no se inventa su aprobación en el esquema de main.
Pendiente incorporar sus estados y completar dos clientes pendientes antes de
considerar S5-04 terminado.
