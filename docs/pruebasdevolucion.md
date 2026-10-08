# Tarea S3-19: Pruebas Automatizadas de Devolución de Vehículos

## 1. Objetivo
Validar mediante pruebas automatizadas el flujo correcto de devolución y sus reglas de consistencia: la renta solo se cierra y el vehículo solo se libera en casos válidos, y ante un rechazo los datos permanecen intactos.

Archivo de pruebas: `backend/src/test/java/com/rentadeautos/modules/devolucion/service/DevolucionConsistenciaTest.java`

## 2. Casos de Prueba Verificados

### CP-07: Devolución Válida
**Escenario:** Se devuelve un vehículo de una renta `EN_CURSO`, a tiempo (sin atraso), con kilometraje de entrada mayor al de salida.

- **Acciones:**
    - Reservación en estado `EN_CURSO`, 2 días pactados, tarifa de 500.00 por día.
    - Vehículo en estado `RENTADO` con 15000.0 km.
    - Ejecución de `servicio.registrar()` con kilometraje de entrada 15500.0.
- **Verificaciones:**
    - [x] **Estado de Reservación:** `EN_CURSO` → `FINALIZADA`.
    - [x] **Estado de Vehículo:** `RENTADO` → `DISPONIBLE`.
    - [x] **Kilometraje:** el vehículo actualiza su kilometraje a 15500.0.
    - [x] **Costo final:** total de 1000.00 (2 días × 500.00) y cargo por atraso en 0.
    - [x] **Persistencia:** se guarda la devolución, la reservación y el vehículo.
    - [x] **Auditoría:** se registra `DEVOLVER_VEHICULO` con estado anterior (`EN_CURSO`) y posterior (`FINALIZADA`).

### CP-08: Kilometraje Inconsistente
**Escenario:** Se intenta devolver un vehículo con kilometraje de entrada menor al de salida (14999.0 contra 15000.0).

- **Acciones:**
    - Mismo escenario base que CP-07.
    - Ejecución de `servicio.registrar()` con kilometraje de entrada 14999.0.
- **Verificaciones:**
    - [x] **Rechazo de Operación:** se lanza `DevolucionException` con código `400 BAD REQUEST` y mensaje de la regla RN-08.
    - [x] **Integridad de Datos:**
        - La reservación **mantiene** su estado `EN_CURSO`.
        - El vehículo **mantiene** su estado `RENTADO` y su kilometraje de 15000.0.
        - No se guarda ninguna devolución.
        - No se genera ninguna entrada de auditoría.
    - [x] **Caso límite:** un kilometraje de entrada igual al de salida sí se acepta (la regla es mayor o igual).

## 3. Resultados

Comando: `./mvnw test -Dtest='Devolucion*Test,Entrega*Test,Reservacion*Test'`

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.322 s -- in com.rentadeautos.modules.reservation.service.ReservacionServiceTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.520 s -- in com.rentadeautos.modules.devolucion.controller.DevolucionControllerTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.155 s -- in com.rentadeautos.modules.devolucion.service.DevolucionConsistenciaTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.047 s -- in com.rentadeautos.modules.devolucion.service.DevolucionServiceTest
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.354 s -- in com.rentadeautos.modules.rental.controller.EntregaControllerTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.096 s -- in com.rentadeautos.modules.rental.service.EntregaServiceTest
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **Estado Final:** PASADO
- **Cobertura:** camino feliz (CP-07), rechazo por kilometraje con consistencia de datos (CP-08) y caso límite.
