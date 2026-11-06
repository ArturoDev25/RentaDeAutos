# S5-07 - Regresión contra MySQL e integración de MySQL al CI

## Qué se hizo
- Se ejecutó la suite completa (856 pruebas) contra MySQL 8.0 real, además de H2.
- Las migraciones Flyway V1..V7 se aplican sin errores sobre una base MySQL vacía y Hibernate (`ddl-auto: validate`) arranca sin diferencias de esquema.
- Se agregó el job `test-mysql` en `.github/workflows/ci.yml`. El job `test` (H2) se conserva sin cambios.

## Cómo correr las pruebas contra MySQL
Las pruebas de integración toman la base de datos de variables de entorno con valor por defecto H2:
`TEST_DB_URL_PREFIX`, `TEST_DB_URL_SUFFIX`, `TEST_DB_DRIVER`, `TEST_DB_USER`, `TEST_DB_PASSWORD`.
Sin variables corren en H2 como siempre.

Se ejecutan en dos pasos con bases distintas, porque las pruebas `@DataJpaTest` (Hibernate crea las tablas)
y las que usan Flyway no pueden compartir la misma base:
1. 8 clases de repositorio (`@DataJpaTest`) con `SPRING_TEST_DATABASE_REPLACE=none` (93 pruebas).
2. El resto de las pruebas (763), excluyendo esas 8 clases.

## Hallazgos
1. **FlywayHibernateValidateTest** consultaba `"flyway_schema_history"` con comillas dobles (solo válido en H2). Ahora usa la API de Flyway.
2. **ClienteRepositoryIntegrationTest**: ante un CHECK violado, MySQL lanza `JpaSystemException` y H2 `DataIntegrityViolationException`. El CHECK funciona igual; la prueba ahora espera `DataAccessException`.
3. **Zona horaria (riesgo real)**: con la JVM en una zona distinta de UTC (por ejemplo CST), la conexión declara `serverTimezone=UTC` y dos reservaciones contiguas se detectan como empalmadas (409 falso).
   La prueba `intervalosContiguosNoSeConfundenConTraslape` pasa con `TZ=UTC` o con `serverTimezone` igual a la zona de la máquina.
   El CI corre en UTC, por lo que no lo detecta por sí solo.

## Recomendación para S5-05 y S5-06 (Docker y despliegue)
Ejecutar el backend con `TZ=UTC` (variable de entorno en el contenedor y/o `-Duser.timezone=UTC`).
La causa exacta de la conversión no se investigó a fondo; queda como mejora fijar la zona de forma explícita en la configuración.

## Prueba manual contra MySQL (2026-11-04)
Entorno: backend con `TZ=UTC` sobre MySQL 8.0 (Docker), base `rentadeautos_test` recién creada
(Flyway aplicó V1..V7), frontend servido en `http://localhost:5500`.

| # | Flujo | Resultado |
|---|-------|-----------|
| 1 | Login de personal (admin) redirige al dashboard (S5-03) | OK |
| 2 | Registro de cliente muestra "cuenta pendiente de aprobación" | OK |
| 3 | Registro duplicado muestra error claro (409) | OK |
| 4 | Cliente pendiente no puede iniciar sesión | OK |
| 5 | Aprobación y rechazo desde "Clientes pendientes" | OK |
| 6 | Login de cliente aprobado entra al portal; el rechazado no | OK |
| 7 | Alta de categoría, tarifa y vehículo desde el panel | OK |
| 8 | Reserva, reserva contigua aceptada y reserva empalmada rechazada | OK |
| 9 | Mis reservaciones y Mis rentas | OK |
| 10 | Reportes de cliente y de administrador | OK |

Nota: el seed de demo (S5-04) no estaba en `main` en ese momento, por lo que los vehículos se dieron de alta a mano.
