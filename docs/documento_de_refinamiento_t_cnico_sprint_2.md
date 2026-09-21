# UNIVERSIDAD AUTÓNOMA DE COAHUILA
## Facultad de Sistemas — Unidad Saltillo
### Asignatura: Desarrollo de Proyectos de Software
* **Docente:** Francisco Horacio Ramos González  
* **Proyecto:** Sistema de Renta de Autos (MVP)  
* **Marco de Trabajo:** Scrumban (Sprints de 2 semanas)  
* **Actividad:** S2-02 — Refinar y estimar en Planning Poker; dejar historias en Ready  

---

## Control del Documento de Refinamiento Técnico

| Elemento | Información Detallada |
| :--- | :--- |
| **Sprint y Fase** | Sprint 2: «Catálogos» (Ventana efectiva: 16 al 25 de septiembre de 2026) |
| **Scrum Master** | Oscar Arturo Peña Valdez |
| **Product Owner (Proxy)** | Miguel Ángel Machorro García |
| **Equipo de Desarrollo** | Julio César Pérez Romero (Backend), Luis Mireles Barrera (Frontend), Ricardo Javier López Alonso (DevOps), Frida Sofía Lucio Méndez (UX/Doc), Rafael de Jesús Martínez Vélez (QA) |
| **Estado del Documento** | Aprobado para transición a columna Ready |
| **Velocity Objetivo** | 13 a 14 Story Points (SP) por Sprint |

---

## 1. Contexto Operativo y Criterios de Entrada (Definition of Ready)

### 1.1 Objetivo del Sprint 2 (Sprint Goal)
> *«El personal autorizado puede registrar, consultar y buscar clientes y vehículos (con placa y VIN únicos) aplicando permisos por rol; demostrable de punta a punta, dejando los catálogos listos para crear reservaciones en el Sprint 3.»*

### 1.2 Ajustes de Capacidad y Alcance
Debido al inicio efectivo el miércoles 16 de septiembre (arranque tardío de 2 días), el equipo acordó proteger el calendario inamovible (timeboxing al viernes 25 de septiembre). El portal público del cliente (registro, inicio de sesión y autoservicio) fue trasladado formalmente al Sprint 3. Durante este sprint, las altas de clientes son ejecutadas de manera interna por el personal de la empresa.

### 1.3 Criterios Normativos de Definition of Ready (DoR)
Para habilitar el paso de cualquier tarjeta a la columna **Ready**, se validaron los seis requerimientos normativos del proyecto:
1. **R-01:** Redacción estandarizada desde el punto de vista del usuario final (*Como... Quiero... Para...*).
2. **R-02:** Criterios de aceptación verificables en formato formal **Gherkin** (*Given / When / Then*).
3. **R-03:** Dependencias técnicas, esquemas de bases de datos y roles de acceso explícitamente documentados.
4. **R-04:** Estimación consensuada mediante Planning Poker bajo la secuencia de Fibonacci (1, 2, 3, 5, 8 SP).
5. **R-05:** Identificación previa de casos límite (valores frontera, duplicidad, nulos y códigos de error HTTP).
6. **R-06:** Asignación técnica preliminar y apego al límite de trabajo en proceso ($WIP = 2$ en *In Progress*).

---

## 2. Historias de Usuario Comprometidas (Bloque Must Have — 15 SP)

### US-CAT: Catálogo Base de Categorías de Vehículos
* **Código Backlog:** US-CAT (Asociada a tareas S2-10 y S2-19)
* **Prioridad:** Must Have (Bloqueante estructural para el inventario)
* **Estimación:** 2 Story Points (CRUD estándar sobre tabla existente)
* **Responsables sugeridos:** Miguel Ángel Machorro (Backend) / Frida Sofía Lucio (Frontend)

#### Narrativa
> **Como** Administrador o Supervisor,  
> **quiero** registrar, listar y actualizar categorías de automóviles (nombre, descripción y depósito base),  
> **para** clasificar adecuadamente las unidades de la flota y parametrizar los depósitos de garantía antes del ingreso de vehículos.

#### Especificación Técnica
* **Entidad Relacional:** `categorias` (`id`, `nombre`, `descripcion`, `deposito_base`, `activo`, `created_at`, `updated_at`).
* **Endpoints:**
  * `GET /api/v1/categories` (Lectura para catálogo operativo).
  * `POST /api/v1/categories` (Creación restringida a Admin/Supervisor).
  * `PUT /api/v1/categories/{id}` (Modificación de parámetros).
* **Dependencias:** Tabla `categorias` generada en la migración `V1__init_schema.sql` (Sprint 1). Bloquea la inserción de vehículos por llave foránea.

#### Criterios de Aceptación (Gherkin)
* **Escenario 1: Alta exitosa de categoría**
  * **Given** que un Administrador autenticado ingresa nombre `"Sedán Intermedio"`, descripción `"4 puertas económico"` y depósito base de `$3,000.00`,
  * **When** envía la solicitud `POST /api/v1/categories`,
  * **Then** la API responde HTTP `201 Created`, persiste el registro con `activo = true` y retorna el recurso generado.
* **Escenario 2: Rechazo por nombre duplicado**
  * **Given** que ya existe una categoría registrada con el nombre `"SUV"`,
  * **When** se intenta crear otra categoría con la denominación `"suv"` o `"SUV"`,
  * **Then** la API rechaza la transacción con HTTP `409 Conflict` y el mensaje `"La categoría ya se encuentra registrada"`.

#### Casos Límite y Validaciones
* **Importe de depósito:** Validar $deposito\_base \ge 0.00$. Valores negativos deben responder HTTP `400 Bad Request` (RN-09).
* **Normalización de texto:** Aplicación de funciones `TRIM()` e insensibilidad a mayúsculas/minúsculas para evitar duplicidades por formato.
* **Integridad referencial:** No se permite la baja lógica de categorías que contengan vehículos activos asociados.

---

### US-03: Gestión Integral de Clientes y Verificación de Licencia
* **Código Backlog:** US-03 (Asociada a tareas S2-06, S2-16 y S2-23)
* **Prioridad:** Must Have
* **Estimación:** 5 Story Points (Complejidad media: validaciones compuestas, expresiones regulares y baja lógica)
* **Responsables sugeridos:** Julio César Pérez (Backend) / Luis Mireles (Frontend) / Rafael de Jesús Martínez (QA)

#### Narrativa
> **Como** Agente de renta, Supervisor o Administrador,  
> **quiero** dar de alta, consultar, editar y desactivar clientes validando la vigencia de su licencia y la unicidad de sus datos de contacto,  
> **para** mantener un padrón auditado de personas aptas para celebrar contratos de renta en el Sprint 3.

#### Especificación Técnica
* **Entidad Relacional:** `clientes` (`id`, `nombre`, `apellidos`, `correo`, `telefono`, `numero_licencia`, `licencia_vencimiento`, `direccion`, `activo`, `timestamps`).
* **Endpoints:**
  * `POST /api/v1/clients` (Alta de cliente).
  * `GET /api/v1/clients` (Consulta y listado paginado).
  * `GET /api/v1/clients/{id}` (Ficha detallada).
  * `PUT /api/v1/clients/{id}` (Actualización de datos).
  * `DELETE /api/v1/clients/{id}` (Baja lógica: `activo = false`).
* **Dependencias:** Integración base de autenticación con token JWT e interceptor HTTP en frontend (S2-16).

#### Criterios de Aceptación (Gherkin)
* **Escenario 1: Registro formal de un cliente**
  * **Given** que un Agente o Supervisor ingresa nombre, apellidos, teléfono de 10 dígitos, correo válido y licencia con fecha de expiración futura,
  * **When** envía la solicitud `POST /api/v1/clients`,
  * **Then** la API devuelve HTTP `201 Created` y persiste al cliente en estado `activo = true`.
* **Escenario 2: Conflicto por duplicidad de licencia**
  * **Given** que existe un cliente registrado con la licencia `"COAH-78901"`,
  * **When** se envía un nuevo registro con el mismo número de licencia,
  * **Then** la API cancela la operación respondiendo HTTP `409 Conflict` con el mensaje `"El número de licencia ya se encuentra registrado"`.
* **Escenario 3: Baja lógica sin eliminación física (RN-10)**
  * **Given** un cliente registrado previamente en el sistema,
  * **When** el Administrador ejecuta `DELETE /api/v1/clients/{id}`,
  * **Then** el campo `activo` cambia a `false`, no se destruye la fila en MySQL y la respuesta es HTTP `200 OK`.

#### Casos Límite y Validaciones
* **Licencia expirada al capturar:** Si $licencia\_vencimiento \le CURRENT\_DATE()$, el backend debe rechazar el registro o marcar al cliente como no apto para asignación de autos (RN-06).
* **Soporte de correos nulos:** El campo `correo` admite valores `NULL` sin romper la restricción `UNIQUE` en motores MySQL.

---

### US-04: Gestión de Vehículos con Placa y VIN Únicos (Regla Estrella)
* **Código Backlog:** US-04 (Asociada a tareas S2-08, S2-17 y S2-22)
* **Prioridad:** Must Have (Regla central del Sprint 2)
* **Estimación:** 5 Story Points (Complejidad alta: validación dual de unicidad, normalización y estados iniciales)
* **Responsables sugeridos:** Julio César Pérez (Backend) / Ricardo Javier López (Frontend) / Rafael de Jesús Martínez (QA)

#### Narrativa
> **Como** Administrador o Supervisor,  
> **quiero** registrar y administrar unidades vehiculares garantizando la unicidad de placa y VIN (número de serie),  
> **para** mantener el inventario exacto y evitar colisiones operativas en los contratos de renta (RN-01).

#### Especificación Técnica
* **Entidad Relacional:** `vehiculos` (`id`, `categoria_id`, `placa`, `vin`, `marca`, `modelo`, `anio`, `color`, `kilometraje`, `estado`, `timestamps`).
* **Estados Permitidos:** `DISPONIBLE`, `RESERVADO`, `RENTADO`, `MANTENIMIENTO`, `BAJA`.
* **Endpoints:**
  * `POST /api/v1/vehicles` (Alta de vehículo con estado `DISPONIBLE`).
  * `GET /api/v1/vehicles` (Consulta general y catálogo de unidades).
  * `GET /api/v1/vehicles/{id}` (Ficha técnica).
  * `PUT /api/v1/vehicles/{id}` (Edición de características físicas).
  * `PATCH /api/v1/vehicles/{id}/status` (Transición de estados operativos).
* **Dependencias:** Requiere categorías válidas en la base de datos (`US-CAT`). Casos de prueba vinculados: CP-01 y CP-02.

#### Criterios de Aceptación (Gherkin)
* **Escenario 1: Alta válida de unidad (CP-01)**
  * **Given** placa `"FAK-1234"`, VIN estandarizado de 17 posiciones alfanuméricas, año válido, kilometraje $\ge 0$ y categoría activa,
  * **When** se solicita la creación vía `POST /api/v1/vehicles`,
  * **Then** la API responde HTTP `201 Created`, guarda los datos en mayúsculas y asigna por defecto el estado inicial `DISPONIBLE`.
* **Escenario 2: Rechazo por placa o VIN duplicado (CP-02 / RN-01)**
  * **Given** que ya existe un vehículo con placa `"FAK-1234"` o VIN `"3N1AB7AP0FY123456"`,
  * **When** se intenta dar de alta otra unidad coincidente en cualquiera de los dos identificadores,
  * **Then** la API responde HTTP `409 Conflict` especificando el campo colisionado.
* **Escenario 3: Acceso no autorizado por rol operativo**
  * **Given** un usuario autenticado únicamente con rol `AGENTE`,
  * **When** envía una petición POST o PUT hacia `/api/v1/vehicles`,
  * **Then** el backend cancela la operación respondiendo HTTP `403 Forbidden`.

#### Casos Límite y Validaciones
* **Restricción ISO del VIN:** Excluir caracteres prohibidos en numeración automotriz estándar (letras I, O, Q) y obligar a 17 posiciones fijas.
* **Consistencia del año y odómetro:** Año acotado entre 2000 y el año actual $+ 1$; kilometraje no negativo ($kilometraje \ge 0$, RN-08).

---

### US-SEC: Control de Acceso y Permisos por Rol en Endpoints
* **Código Backlog:** US-SEC (Asociada a tarea S2-12)
* **Prioridad:** Must Have
* **Estimación:** 3 Story Points (Configuración `@PreAuthorize`, validación de tokens y pruebas de roles)
* **Responsables sugeridos:** Miguel Ángel Machorro (Backend) / Rafael de Jesús Martínez (QA)

#### Narrativa
> **Como** Auditor y Administrador del sistema,  
> **quiero** que la API evalúe el rol asignado en el token JWT en cada una de las peticiones,  
> **para** garantizar el principio de mínimo privilegio sobre los catálogos y evitar alteraciones no autorizadas.

#### Criterios de Aceptación (Gherkin)
* **Escenario 1: Intento de acceso no autenticado**
  * **Given** una solicitud a un endpoint de catálogos sin cabecera `Authorization: Bearer <token>`,
  * **When** la petición es interceptada por el filtro de seguridad de Spring,
  * **Then** el sistema detiene la ejecución respondiendo HTTP `401 Unauthorized`.
* **Escenario 2: Restricción de escritura al rol Auditor (CP-09)**
  * **Given** un usuario autenticado con rol `AUDITOR`,
  * **When** ejecuta operaciones de escritura (POST, PUT, DELETE) sobre clientes o vehículos,
  * **Then** el servidor devuelve HTTP `403 Forbidden` y la base de datos se mantiene inmutable.

#### Casos Límite y Validaciones
* **Tokens alterados o vencidos:** Expiración de payload o firma JWT inválida responde inmediatamente HTTP `401 Unauthorized`.

---

## 3. Historias de Usuario de Reserva (Bloque Should Have — 5 SP)
*Estas historias se encuentran en el backlog del sprint y solo se activarán si el equipo concluye el bloque comprometido antes del día 5.*

### US-05: Búsqueda y Filtros Multicriterio de Clientes y Vehículos
* **Código Backlog:** US-05 (Asociada a tareas S2-07, S2-09 y S2-18)
* **Prioridad:** Should Have | **Estimación:** 3 Story Points
* **Criterios Clave:**
  * Búsqueda dinámica de clientes por coincidencia parcial (`LIKE %query%`) en nombre, apellidos o licencia.
  * Filtrado de vehículos por categoría y estado mediante especificaciones JPA.
  * Tiempo de respuesta inferior a 2.0 segundos sobre el conjunto de datos de prueba (RNF-02).

### US-TAR: Parametrización de Tarifas por Categoría
* **Código Backlog:** US-TAR (Asociada a tarea S2-11)
* **Prioridad:** Should Have | **Estimación:** 2 Story Points
* **Criterios Clave:**
  * Mapeo de la tabla `tarifas` vinculada a `categoria_id`.
  * Validación de importes: $precio\_dia > 0$ y $cargo\_atraso\_dia \ge 0$.
  * Validación cronológica de vigencia: $fecha\_inicio \le fecha\_fin$.

---

## 4. Matriz Consolidada de Estimación en Planning Poker

| Historia | Prioridad | Story Points | Justificación Técnica del Consenso | Dependencias Directas |
| :--- | :---: | :---: | :--- | :--- |
| **US-CAT** | Must | **2 SP** | CRUD sobre tabla `categorias` ya migrada en el Sprint 1. | Ninguna (Bloquea a Vehículos). |
| **US-03** | Must | **5 SP** | Expresiones regulares en licencia, verificación de correo y baja lógica. | Interceptor frontend S2-16. |
| **US-04** | Must | **5 SP** | Regla crítica de placa/VIN únicos, asignación de estado y pruebas CP-01/02. | Requiere US-CAT (`categoria_id`). |
| **US-SEC** | Must | **3 SP** | Configuración `@PreAuthorize`, tokens JWT y cobertura de errores 401/403. | Endpoints de catálogos creados. |
| **US-05** | Should | **3 SP** | Consultas dinámicas con `JpaSpecification` y controles UI de búsqueda. | Depende de US-03 y US-04. |
| **US-TAR** | Should | **2 SP** | Mapeo de la entidad `tarifas` y control de vigencias temporales. | Requiere US-CAT. |
| **Total Must** | — | **15 SP** | **Alineado a la capacidad nominal del equipo (13 a 14 SP).** | — |
| **Total Should** | — | **5 SP** | Condicionado al progreso del burn-down al día 5. | — |

---

## 5. Matriz de Trazabilidad y Permisos por Rol en Endpoints

| Ruta del Endpoint | Operación | Administrador | Agente de renta | Supervisor | Auditor |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `GET /api/v1/categories` | Consulta de categorías | Sí (200) | Sí (200) | Sí (200) | Sí (200) |
| `POST, PUT /api/v1/categories/**` | Gestión de categorías | Sí (201/200) | No (403) | Sí (201/200) | No (403) |
| `GET /api/v1/clients/**` | Búsqueda y consulta | Sí (200) | Sí (200) | Sí (200) | Sí (200) |
| `POST, PUT /api/v1/clients/**` | Registro y actualización | Sí (201/200) | Sí (201/200) | Sí (201/200) | No (403) |
| `DELETE /api/v1/clients/**` | Desactivación lógica | Sí (200) | Pendiente PO | Sí (200) | No (403) |
| `GET /api/v1/vehicles/**` | Consulta de inventario | Sí (200) | Sí (200) | Sí (200) | Sí (200) |
| `POST, PUT /api/v1/vehicles/**` | Alta y edición de unidades | Sí (201/200) | No (403) | Sí (201/200) | No (403) |

---

## 6. Dictamen Formal de Transición a «Ready»

Habiendo revisado los entregables técnicos y documentales correspondientes a la actividad **S2-02**:
* Las historias de usuario cuentan con estructura formal y cubren el alcance pactado del Sprint 2.
* Cada historia cuenta con criterios observables en formato Gherkin y contempla sus casos límite.
* Las dependencias de datos relacionales y autenticación se encuentran claramente trazadas.
* La estimación en Story Points fue acordada por consenso respetando la capacidad nominal del equipo.

> **Dictamen Oficial:** Se emite el dictamen de **Aprobado** para que las historias comprometidas (**US-CAT**, **US-03**, **US-04** y **US-SEC**) transiten de la columna *Backlog* a la columna **Ready** en el tablero oficial de GitHub Projects. El desarrollo se iniciará respetando la precedencia de `US-CAT` y `US-03` en paralelo, seguidas de `US-04` y `US-SEC`, bajo el límite operativo de trabajo en curso $WIP = 2$.