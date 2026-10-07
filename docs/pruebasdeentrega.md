# Tarea S3-18: Pruebas Automatizadas de Entrega de Vehículos

## 1. Objetivo
Validar mediante pruebas automatizadas que el flujo de entrega de vehículos funcione correctamente, asegurando que los estados de la reservación y el vehículo se actualicen solo en casos válidos y se rechacen las operaciones cuando el vehículo no sea apto.

## 2. Casos de Prueba Verificados

### CP-05: Entrega Válida
**Escenario:** Se procesa la entrega de un vehículo vinculado a una reservación confirmada y vigente, con un cliente activo y licencia vigente.

- **Acciones:**
    - Configuración de `Reservacion` en estado `CONFIRMADA`.
    - Configuración de `Vehiculo` en estado `RESERVADO`.
    - Ejecución del método `servicio.registrar()` con kilometraje válido.
- **Verificaciones:**
    - [x] **Creación de Entrega:** Se confirmó que se persistió el registro de entrega con los datos de salida (km, combustible, condición).
    - [x] **Estado de Reservación:** El estado cambió de `CONFIRMADA` $\rightarrow$ `EN_CURSO`.
    - [x] **Estado de Vehículo:** El estado cambió de `RESERVADO` $\rightarrow$ `RENTADO`.
    - [x] **Actualización de Kilometraje:** El vehículo actualizó su kilometraje al valor indicado en la entrega.
    - [x] **Auditoría:** Se verificó que se registró el evento `ENTREGAR_VEHICULO` con los estados antes y después.

### CP-06: Vehículo No Apto
**Escenario:** Se intenta procesar la entrega de un vehículo que se encuentra en un estado no permitido (ej. `MANTENIMIENTO` o `RENTADO`).

- **Acciones:**
    - Configuración de `Vehiculo` en estado `MANTENIMIENTO`.
    - Ejecución del método `servicio.registrar()`.
- **Verificaciones:**
    - [x] **Rechazo de Operación:** El sistema lanzó una `EntregaException` con código `409 CONFLICT`.
    - [x] **Integridad de Datos:** Se verificó mediante `assertSinCambios()` que:
        - No se creó ninguna entrega en la base de datos.
        - La reservación **mantuvo** su estado `CONFIRMADA`.
        - El vehículo **mantuvo** su estado `MANTENIMIENTO`.
        - No se generó ninguna entrada en la auditoría.

## 3. Resultados
- **Estado Final:** $\text{PASADO}$ $\checkmark$
- **Cobertura:** Se validaron tanto el camino feliz (Happy Path) como los flujos de error críticos según los criterios de aceptación del Sprint 3.
