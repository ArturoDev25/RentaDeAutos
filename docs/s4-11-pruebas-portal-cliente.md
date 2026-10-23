# S4-11 — Pruebas automatizadas del portal del cliente

## Objetivo y dependencias

Verificar registro, autenticación, permisos y aislamiento de operaciones
del portal mediante HTTP simulado por MockMvc, filtros de seguridad,
JWT firmados, servicios y base de datos reales de prueba.

Base de implementación: commit `415f690abe88eab3f6e8e4a9929d7a9c51209467`
de S4-06, rama `feature/S4-06-reservaciones-cliente`.
S4-04 ya estaba integrada en main al iniciar esta tarea.
S4-06 se encontraba en revisión en el PR #140; su integración es una
dependencia del PR de esta tarea. La rama también incluye S4-02 y S4-03.

## Suite nueva

Archivo: `backend/src/test/java/com/rentadeautos/modules/portal/PortalClienteIntegrationTest.java`.

La suite no usa servicios simulados, @WithMockUser ni mocks de JWT:
registra clientes por la API, inicia sesión y envía sus tokens reales.
Esto complementa las pruebas unitarias y de controladores existentes.

| Criterio | Comprobación |
|---|---|
| Registro exitoso | HTTP 201, cuenta activa, hash BCrypt verificable, respuesta sin credenciales |
| Correo duplicado | HTTP 409 con mensaje genérico; conserva la cuenta y cantidad de filas |
| Login correcto e incorrecto | Token de cliente válido; correo inexistente y contraseña incorrecta devuelven 401 sin token |
| Rol CLIENTE | Valida firma y claim, subject cliente:id y acceso por el filtro real |
| Protección de administración | Siete consultas devuelven 403; rechaza alta de personal y cambio de estado sin alterar usuarios |
| Personal autorizado | Login real de administrador; mantiene acceso administrativo y no utiliza rutas exclusivas del cliente |
| Aislamiento de reservas | Dos clientes con registros propios; listados ignoran clienteId externo; detalle y cancelación ajenos devuelven 404 |
| Aislamiento de rentas | Cada cliente ve únicamente su historial, incluso enviando el ID de otro cliente |
| Reserva válida | HTTP 201; identidad, creador, estado y precio determinados por el servidor |
| Fechas inválidas | Seis casos con HTTP 400 y sin nuevas filas: pasado, fin anterior/igual, campos ausentes y formato inválido |
| Traslapes | Cinco intervalos conflictivos entre clientes devuelven 409 sin modificar la reserva original ni insertar otra |
| Límites de intervalos | Reservas contiguas válidas; cancelar una propia permite volver a reservar el intervalo |
| Historial de solo lectura | POST, PUT, PATCH y DELETE devuelven 403 y conservan las filas |
| Renta en curso | Cancelación rechazada con 409, vehículo y reserva intactos |
| Sesiones inválidas | Sin token, token expirado, payload alterado y cliente desactivado: 401 |
| CI | Maven ejecuta la suite y reporta fallos; se conservan reportes Surefire |

## Datos y alcance

- H2 en memoria exclusiva: `portal_s411_db`, modo MySQL.
- Flyway aplica las migraciones reales, incluida V6.
- Hibernate usa validate; no se reemplaza el esquema por tablas simplificadas.
- Fechas relativas al día de ejecución para evitar vencimientos de los casos.
- Catálogo y tarifas propios de la prueba.
- @Transactional revierte los datos de cada caso.
- Las rentas del historial se preparan como fixtures persistidas con entrega:
  prueban consulta, aislamiento y bloqueo de escritura, no el flujo de entrega.
- MockMvc comprueba el backend; no automatiza clics ni estilos del navegador.
- Los casos secuenciales de traslape no acreditan concurrencia real en MySQL.

## Ejecución

Requisitos: Java 21 y Maven Wrapper del proyecto. No se requiere Docker,
servidor web ni archivo .env; se utiliza el perfil test.

Desde backend, Windows CMD:

```bat
mvnw.cmd -Dtest=PortalClienteIntegrationTest test
mvnw.cmd test
```

Linux/macOS, desde backend:

```sh
./mvnw -B -ntp -Dtest=PortalClienteIntegrationTest test
./mvnw -B -ntp test
```

Maven descubre automáticamente la clase por terminar en Test.
Los resultados quedan en backend/target/surefire-reports.
La suite completa conserva las pruebas existentes de registro, login,
permisos, repositorios, servicio de reservas y migraciones.

## Integración continua

Se amplía el workflow existente .github/workflows/ci.yml, que utiliza
Java 21 y ejecuta Maven en cada push y pull_request.

- Un fallo o error de pruebas produce un código de salida no cero y
  marca el paso y el job como fallidos.
- No se usa continue-on-error ni se ignoran fallos de Maven.
- El paso de publicación usa always() para ejecutarse también ante fallos.
- El artefacto reportes-pruebas-backend contiene los XML y TXT de Surefire
  y se conserva siete días.
- Si la compilación falla antes de generar reportes, se mantiene el fallo
  de Maven y se consulta el log del job; la ausencia de reportes no lo oculta.

Para comprobar la ejecución remota: abrir el PR, entrar a Checks > CI >
test y revisar Ejecutar pruebas. En la página de la ejecución, descargar
reportes-pruebas-backend en Artifacts para los detalles de los casos.

## Evidencia

Validación local del parche, 22 de octubre de 2026, con Temurin 21.0.13
y Maven 3.9.9:

| Ejecución | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| PortalClienteIntegrationTest | 40 | 0 | 0 | 0 |
| Suite completa | 809 | 0 | 0 | 0 |

Ambas ejecuciones terminaron con BUILD SUCCESS. En el entorno aislado
de preparación fue necesario precargar el agente Byte Buddy mediante
argLine, porque no permite el autoacoplamiento de Mockito a la JVM.
Ese ajuste fue exclusivo de la ejecución local y no modifica el proyecto.

Validación en el equipo del desarrollador — 22 de octubre de 2026:
809 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas.
BUILD SUCCESS. Tiempo de ejecución: 1 minuto y 29 segundos.

S4-06 quedó integrada mediante el PR #140. Se incorporaron los cambios
de origin/main a esta rama sin conflictos.

Validación posterior a la integración — 22 de octubre de 2026:
809 pruebas, 0 fallos, 0 errores y 0 omitidas.
BUILD SUCCESS. Duración: 1 minuto y 17 segundos.

CI aprobó el commit 8fe79f4 y generó reportes-pruebas-backend:
https://github.com/ArturoDev25/RentaDeAutos/actions/runs/39797165116

El siguiente push ejecutará nuevamente CI sobre la rama actualizada.
