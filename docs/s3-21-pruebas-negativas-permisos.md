\# S3-21 — Pruebas negativas de permisos



Issue: #90

Fecha de ejecución: 2026-10-08

Commit base: 93c8ed8



\## Objetivo

Verificar que los usuarios sin autorización no puedan ejecutar

los endpoints del Sprint 3 y que los roles autorizados conserven acceso.



\## Implementación

Se agregó PermisosSprint3Test.java con 144 casos sobre 16 operaciones

de tarifas, reservaciones, entregas, devoluciones y rentas activas.



Las pruebas utilizan los controladores reales, SecurityConfig,

JwtAuthenticationFilter y servicios de negocio simulados.



\## Cobertura

\- 52 casos autorizados: comprueban el código HTTP, success=true

&#x20; y la invocación del método esperado con sus argumentos.

\- 44 casos de roles no autorizados: reciben 403.

\- 16 solicitudes sin autenticación: reciben 401.

\- 16 usuarios autenticados sin autoridades: reciben 403.

\- 16 solicitudes cuyo token es rechazado por el validador: reciben 401.



Cada rechazo comprueba success=false, el mensaje correspondiente

y ausencia de invocaciones a los cinco servicios de negocio.



\## Alcance de la verificación de datos

Se verifica que el rechazo ocurra antes de alcanzar los servicios

que ejecutan las operaciones de negocio.



No se conecta con MySQL ni se compara una base de datos antes y después.

La validación del token se simula; no se prueba su criptografía.

Los casos autorizados comprueban acceso y delegación al servicio,

no la ejecución completa de las reglas de negocio.



\## Ajuste de una prueba existente

La primera ejecución completa detectó tres fallos en

DevolucionConsistenciaTest, cuya preparación estaba desactualizada

respecto al servicio en el commit base.



Se corrigió:

\- La respuesta simulada de findVehiculoIdById.

\- La entidad de auditoría: DEVOLUCION.

\- La comprobación de los mapas de estados anteriores y posteriores.



Se conservaron las verificaciones de estados, persistencia y auditoría.

No se modificó código de producción ni se eliminaron pruebas.



\## Resultados

Todos los siguientes resultados tuvieron cero fallos, errores y omisiones:



\- Pruebas iniciales de cinco controladores: 77 aprobadas.

\- Nueva clase PermisosSprint3Test: 144 aprobadas.

\- DevolucionConsistenciaTest después del ajuste: 3 aprobadas.

\- Suite completa final: 535 aprobadas.



\## Ejecución

Desde la carpeta backend:



Solo permisos del Sprint 3:

&#x20;   mvnw.cmd -Dtest=PermisosSprint3Test test



Suite completa:

&#x20;   mvnw.cmd test



La nueva clase se descubre automáticamente con mvnw.cmd test.

