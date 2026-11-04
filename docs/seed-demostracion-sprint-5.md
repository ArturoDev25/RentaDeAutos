# S5-04 — Seed de demostración (avance 1)

## Alcance actual

Crea ocho clientes ficticios activos, con correo `demo01@s504.example` hasta
`demo08@s504.example`, licencias `S504-DEMO-01` a `S504-DEMO-08`, contraseña
BCrypt y vencimiento de licencia tres años después de la carga inicial.
Los nombres, teléfonos y domicilios son datos sintéticos.

No actualiza registros existentes, no restablece contraseñas ni elimina datos.
Las coincidencias parciales de correo/licencia provocan error antes de insertar.
La carga es transaccional. No requiere migración nueva.

Este avance NO completa S5-04: faltan los clientes pendientes de aprobación,
vehículos con fotos, categorías/tarifas y reservaciones/rentas del guion final.
S5-09 aún no está integrado en el main d9c93a5 usado como base. No se simula una
aprobación usando `activo`: aprobación y activación son conceptos diferentes.

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
.\mvnw.cmd "-Dtest=ClienteDemoInitializerTest,ClienteDemoActivacionTest" test
.\mvnw.cmd test
```

Hay 14 casos nuevos: nueve de carga con H2 y cinco de activación del contexto.
No requieren conexión al servidor. Pendiente ejecutar contra MySQL de demostración
cuando el SM confirme el acceso y la base destino. No eliminar el perfil ni los datos
para volver a cargar: ejecutar otra vez conserva los registros ya creados.
