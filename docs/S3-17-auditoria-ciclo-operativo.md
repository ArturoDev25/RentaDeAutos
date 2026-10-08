# S3-17 · Extensión de Auditoría del Ciclo Operativo (Issue #85)
**Sistema de Renta de Autos (MVP)**  
**Facultad de Sistemas — Universidad Autónoma de Coahuila**  
**Asignatura:** Desarrollo de Proyectos de Software  
**Sprint:** Sprint 3 — Operación / Ciclo de renta completo  
**Parent:** Bloque F — Auditoría  
**Responsable Técnico:** Julio César Pérez Romero  
**Estado:** Implementado y Verificado (388/388 pruebas en verde)  

---

## 1. Ficha Técnica

| Atributo | Detalle |
| :--- | :--- |
| **Identificador de Tarea** | S3-17 |
| **Issue GitHub** | [#85 (S3-17 Extender auditoría del ciclo operativo)](https://github.com/ArturoDev25/RentaDeAutos/issues/85) |
| **Módulo** | Auditoría y Seguridad Transaccional (`modules.audit`) |
| **Servicios Impactados** | `AuditService`, `ReservacionService`, `EntregaService`, `DevolucionService` |
| **Criterios de Aceptación** | Confirmación, entrega y devolución generan auditoría; registro de actor, fecha/hora, operación; snapshots de estado anterior y posterior; consulta posterior vía API/UI. |

---

## 2. Arquitectura de Auditoría Operativa

### 2.1 Principios de Diseño
1. **Inmutabilidad y Trazabilidad (RN-09):** Cada transición de estado en el ciclo operativo queda registrada con marca temporal (`LocalDateTime.now()`), dirección IP y usuario ejecutor.
2. **Atomicidad Transaccional:** La auditoría de una operación exitosa se ejecuta en la misma transacción Hibernate/JPA que la modificación del negocio; si la persistencia de la entidad falla, la auditoría hace rollback, garantizando que la bitácora refleje fielmente el estado real de la base de datos.
3. **Catálogo Unificado (`AuditoriaOperativa`):** Acciones (`CONFIRMAR_RESERVACION`, `ENTREGAR_VEHICULO`, `DEVOLVER_VEHICULO`) y entidades (`RESERVACION`, `ENTREGA`, `DEVOLUCION`) centralizadas para consistencia semántica.
4. **Serialización Centralizada Jackson:** El método `registrarEvento(...)` en `AuditServiceImpl` recibe `Object` (Maps, DTOs o String) y los serializa a JSON limpio, formateando fechas ISO-8601, `BigDecimal` sin notación científica y enmascarando cualquier secreto o credencial sensible (`[REDACTADO]`).
5. **Resolución Automática del Actor:** Si `usuarioId` no es provisto explícitamente, se resuelve automáticamente a partir de la sesión autenticada en `SecurityContextHolder` (o "Sistema" si no hay sesión).

---

## 3. Eventos Instrumentados en el Ciclo Operativo

### 3.1 Evento 1: Confirmar Reservación
* **Servicio:** `ReservacionService.confirmar(Long id)`
* **Acción:** `CONFIRMAR_RESERVACION`
* **Entidad:** `RESERVACION`
* **Entidad ID:** ID de la reservación
* **Resultado:** `EXITOSO`
* **Reglas preservadas:** RN-07 (tarifa histórica preservada), validación de disponibilidad y bloqueo de vehículo.

#### Ejemplo de Snapshot Anterior (`valores_anteriores`)
```json
{
  "reservacionId": 8,
  "estado": "PENDIENTE",
  "clienteId": 2,
  "vehiculoId": 3,
  "estadoVehiculo": "DISPONIBLE",
  "fechaInicio": "2026-10-01T10:00:00",
  "fechaFin": "2026-10-03T10:00:00",
  "tarifaDia": 850.00,
  "totalEstimado": 1700.00
}
```

#### Ejemplo de Snapshot Posterior (`valores_nuevos`)
```json
{
  "reservacionId": 8,
  "estado": "CONFIRMADA",
  "clienteId": 2,
  "vehiculoId": 3,
  "estadoVehiculo": "RESERVADO",
  "fechaInicio": "2026-10-01T10:00:00",
  "fechaFin": "2026-10-03T10:00:00",
  "tarifaDia": 850.00,
  "totalEstimado": 1700.00
}
```

---

### 3.2 Evento 2: Entrega de Vehículo
* **Servicio:** `EntregaService.registrar(EntregaRequest datos, String direccionIp)`
* **Acción:** `ENTREGAR_VEHICULO`
* **Entidad:** `ENTREGA`
* **Entidad ID:** ID de la entrega generada
* **Resultado:** `EXITOSO`
* **Reglas preservadas:** Reservación `CONFIRMADA` → `EN_CURSO`, Vehículo `RESERVADO` → `RENTADO`, validación de odómetro y licencia de conducir vigente.

#### Ejemplo de Snapshot Anterior (`valores_anteriores`)
```json
{
  "reservacionId": 10,
  "estadoReservacion": "CONFIRMADA",
  "vehiculoId": 3,
  "estadoVehiculo": "RESERVADO",
  "kilometrajeVehiculo": 15000.0
}
```

#### Ejemplo de Snapshot Posterior (`valores_nuevos`)
```json
{
  "entregaId": 99,
  "reservacionId": 10,
  "estadoReservacion": "EN_CURSO",
  "vehiculoId": 3,
  "estadoVehiculo": "RENTADO",
  "kilometrajeSalida": 15020.5,
  "combustibleSalida": 75.50,
  "condicionSalida": "Sin golpes; rayón leve en defensa trasera",
  "fechaEntrega": "2026-10-06T18:40:05"
}
```

---

### 3.3 Evento 3: Devolución de Vehículo
* **Servicio:** `DevolucionService.registrar(DevolucionRequest datos, String direccionIp)`
* **Acción:** `DEVOLVER_VEHICULO`
* **Entidad:** `DEVOLUCION`
* **Entidad ID:** ID de la devolución generada
* **Resultado:** `EXITOSO`
* **Reglas preservadas:** RN-08 (consistencia odómetro entrada ≥ salida), orden anti-deadlock (Vehículo → Reservación), estado del vehículo determinado exclusivamente por `requiereMantenimiento` (DISPONIBLE / MANTENIMIENTO), cálculo de subtotal por días pactados y cargos independientes de atraso y daños.

#### Ejemplo de Snapshot Anterior (`valores_anteriores`)
```json
{
  "reservacionId": 10,
  "estadoReservacion": "EN_CURSO",
  "vehiculoId": 3,
  "estadoVehiculo": "RENTADO",
  "kilometrajeVehiculo": 15000.0,
  "entregaId": 50,
  "kilometrajeSalida": 15000.0
}
```

#### Ejemplo de Snapshot Posterior (`valores_nuevos`)
```json
{
  "devolucionId": 200,
  "entregaId": 50,
  "reservacionId": 10,
  "estadoReservacion": "FINALIZADA",
  "vehiculoId": 3,
  "estadoVehiculo": "DISPONIBLE",
  "kilometrajeEntrada": 15100.0,
  "combustibleEntrada": 80.00,
  "subtotal": 1000.00,
  "cargoAtraso": 0.00,
  "cargoDanos": 0.00,
  "totalFinal": 1000.00,
  "fechaDevolucion": "2026-10-06T18:40:05"
}
```

---

## 4. Consulta Posterior y Visualización UI

* **Endpoint de Listado Paginado:** `GET /api/v1/audit`
  - Filtros soportados: texto libre `q`, `usuarioId`, `modulo`/`entidad`, `accion`.
  - Autorización: `ADMINISTRADOR`, `SUPERVISOR`, `AUDITOR`.
* **Endpoint de Detalle con Snapshots:** `GET /api/v1/audit/{id}`
  - Devuelve DTO `AuditDetailDTO` conteniendo las cadenas JSON formateadas de `valoresAnteriores` y `valoresNuevos`.
* **Frontend Web (`auditoria.html` y `auditoria.js`):**
  - Muestra badges específicos para las nuevas acciones del ciclo operativo.
  - Al pulsar "Ver detalle", renderiza el modal comparativo con JSON parseado y coloreado (Before/After).

---

## 5. Matriz de Pruebas Automatizadas

| Clase de Prueba | Pruebas | Estado | Aspecto Clave Validado |
| :--- | :---: | :---: | :--- |
| `AuditServiceTest` | 7 | Exitoso (100%) | Serialización de snapshots a JSON limpio, resolución de actor desde SecurityContext, enmascaramiento de datos sensibles. |
| `ReservacionServiceTest` | 10 | Exitoso (100%) | Confirmación genera auditoría con snapshot anterior (`PENDIENTE`) y posterior (`CONFIRMADA`), preservación de tarifa histórica (RN-07), rechazos sin efectos colaterales. |
| `EntregaServiceTest` | 15 | Exitoso (100%) | Auditoría de entrega con transición a `EN_CURSO` y `RENTADO`, preservación de combustible y kilometraje de salida. |
| `DevolucionServiceTest` | 18 | Exitoso (100%) | Auditoría de devolución con transición a `FINALIZADA` y `DISPONIBLE`/`MANTENIMIENTO`, cálculo de cargos sin duplicidad, orden anti-deadlock. |

### Evidencia de Ejecución de Maven
```text
[INFO] Running com.rentadeautos.modules.audit.service.AuditServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.664 s -- in com.rentadeautos.modules.audit.service.AuditServiceTest
[INFO] Running com.rentadeautos.modules.devolucion.service.DevolucionServiceTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.312 s -- in com.rentadeautos.modules.devolucion.service.DevolucionServiceTest
[INFO] Running com.rentadeautos.modules.rental.service.EntregaServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.106 s -- in com.rentadeautos.modules.rental.service.EntregaServiceTest
[INFO] Running com.rentadeautos.modules.reservation.service.ReservacionServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.225 s -- in com.rentadeautos.modules.reservation.service.ReservacionServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Suite completa del proyecto: **388 tests ejecutados, 0 fallos, 0 errores, BUILD SUCCESS**.
