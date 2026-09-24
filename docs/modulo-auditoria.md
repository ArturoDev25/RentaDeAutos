# Módulo de Auditoría — Bitácora y Pantalla de Consulta (S2-18)
**Sistema de Renta de Autos (MVP)**  
**Facultad de Sistemas — Universidad Autónoma de Coahuila**  
**Asignatura:** Desarrollo de Proyectos de Software  
**Docente:** Francisco Horacio Ramos González  
**Marco de Trabajo:** Scrumban (Sprints de 2 semanas)  
**Sprint:** Sprint 2 — «Catálogos» (Ventana efectiva: 16 al 25 de septiembre de 2026)  
**Responsable Técnico:** Julio César Pérez Romero  
**Estado:** Implementado, verificado con pruebas automatizadas (152/152 tests en verde) y documentado  

---

## 1. Alcance y Objetivos de la Tarea

La tarea **S2-18 («Auditoría: bitácora y pantalla de consulta»)** tiene como propósito registrar y consultar de manera centralizada e inmutable las acciones críticas y modificaciones efectuadas sobre el panel administrativo de la plataforma.

### 1.1 Objetivos Técnicos
* **Trazabilidad de operaciones:** Registro persistente del actor, fecha y hora, dirección IP, entidad afectada, acción realizada y snapshots de datos anteriores y nuevos.
* **Inmutabilidad estricta:** La bitácora actúa como registro forense de solo lectura (`HTTP GET`) hacia los clientes externos; cualquier operación de modificación (`POST`, `PUT`, `PATCH`, `DELETE`) queda denegada a nivel de seguridad.
* **Consulta multicriterio y analítica:** Provisión de endpoints paginados con filtros dinámicos (por texto libre, usuario, módulo y acción) y cálculo de indicadores clave (KPIs) en tiempo real.
* **Interfaz de usuario fidedigna:** Implementación de la vista `auditoria.html` con topbar institucional, barra lateral de navegación, 4 tarjetas métricas, tabla con badges semánticos por acción y modal comparativo de cambios (*Before/After* en JSON formateado).

---

## 2. Reglas de Negocio y Políticas de Seguridad

1. **Inmutabilidad del Registro:** La tabla de auditoría no admite alteraciones ni eliminaciones físicas. Los eventos representan hechos históricos inalterables.
2. **Principio de Menor Privilegio:** 
   * Roles autorizados para consulta: `ADMINISTRADOR`, `SUPERVISOR` y `AUDITOR`.
   * Peticiones sin token JWT autenticado responden `401 Unauthorized`.
   * Roles no autorizados (como `AGENTE`) reciben `403 Forbidden`.
   * Intentos de emisión de métodos `POST`, `PUT` o `DELETE` sobre `/api/v1/audit/**` son rechazados con `403 Forbidden` / `405 Method Not Allowed`.
3. **Mapeo Semántico de Resultados:** En la base de datos MySQL, el constraint de verificación define `CHECK (resultado IN ('EXITOSO', 'FALLIDO'))`. En la capa de transporte (DTOs) y presentación visual, los valores se normalizan al español femenino formal: `"Exitosa"` y `"Fallida"`.
4. **Eventos del Sistema y Nulabilidad:** Cuando una operación no proviene de una sesión autenticada (por ejemplo, fallos de autenticación en login o tareas automáticas del servidor), el campo `usuario_id` almacena `NULL` y la capa de servicio proyecta el autor como `"Sistema"`.
5. **Privacidad y Datos Sensibles:** La auditoría omite de forma explícita contraseñas en texto claro, credenciales o hashes criptográficos dentro de los snapshots JSON.

---

## 3. Modelo de Datos y Persistencia

El módulo consume y mapea directamente la tabla relacional `auditoria` definida en la migración `V1__init_schema.sql` de MySQL 8.0:

### 3.1 Estructura de la Tabla `auditoria`

| Columna | Tipo de Dato | Restricción / Índice | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | `PRIMARY KEY AUTO_INCREMENT` | Identificador autoincremental único. Proyectado en UI como `AUD-XXXX`. |
| `usuario_id` | `BIGINT UNSIGNED` | `NULL`, `FK -> usuarios_app(id)` | Identificador del usuario ejecutor (`NULL` para acciones automáticas/sistema). |
| `accion` | `VARCHAR(100)` | `NOT NULL` | Operación efectuada (`Crear`, `Editar`, `Eliminar`, `Inicio de sesión`, etc.). |
| `entidad` | `VARCHAR(80)` | `NOT NULL`, `INDEX` | Módulo o catálogo impactado (`Vehículos`, `Reservaciones`, `Clientes`, etc.). |
| `entidad_id` | `BIGINT UNSIGNED` | `NULL`, `INDEX` | ID de la entidad específica intervenida. |
| `resultado` | `VARCHAR(30)` | `CHECK IN ('EXITOSO', 'FALLIDO')` | Resultado final de la transacción. |
| `valores_anteriores` | `JSON` | `NULL` | Estado previo de la entidad antes de modificarse. |
| `valores_nuevos` | `JSON` | `NULL` | Estado resultante de la entidad tras la operación. |
| `direccion_ip` | `VARCHAR(45)` | `NULL` | Dirección IPv4/IPv6 de origen de la solicitud HTTP. |
| `fecha_hora` | `DATETIME` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Marca temporal del evento con precisión a segundos. |

---

## 4. Arquitectura Backend (Spring Boot 3.3.x / Java 21)

La implementación se encuentra modularizada dentro del paquete `com.rentadeautos.modules.audit`:
### 4.1 Capa de Dominio y Repositorio
* **`Auditoria.java`:** Entidad JPA inmutable con mapeo `@Entity` y `@Table(name = "auditoria")`. Contiene relación `@ManyToOne(fetch = FetchType.LAZY)` opcional contra `UsuarioApp`.
* **`AuditRepository.java`:** Interfaz que extiende `JpaRepository<Auditoria, Long>` y `JpaSpecificationExecutor<Auditoria>`. Incluye métodos de agregación para métricas:
  * `count()`: Total de registros en bitácora.
  * `countDistinctUsuarioIdByUsuarioIdIsNotNull()`: Usuarios activos con registros de actividad.
  * `countByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin)`: Acciones ocurridas en la fecha en curso.
  * `countByResultadoIgnoreCase(String resultado)`: Total de eventos con resultado `FALLIDO`.
  * Consultas de proyección para listas distintas de entidades y acciones.
* **`AuditSpecification.java`:** Factoría de criterios JPA Criteria que compone búsquedas dinámicas:
  * Coincidencia parcial insensible a mayúsculas sobre acción, entidad, dirección IP o nombre del usuario.
  * Filtros exactos por `usuarioId`, `modulo` (entidad) y `tipoAccion` (accion).

### 4.2 DTOs (Java Records Inmutables)
* **`AuditResponseDTO`:** Proyección tabular ligera (`id`, `idFormateado`, `fechaHora`, `usuarioNombre`, `modulo`, `accion`, `descripcion`, `direccionIp`, `resultado`).
* **`AuditDetailDTO`:** Extiende la respuesta base incorporando los snapshots `valoresAnteriores` y `valoresNuevos` formateados para su visualización.
* **`AuditMetricsDTO`:** Registro numérico que alimenta los 4 indicadores KPI superiores.
* **`AuditFilterOptionsDTO`:** Colección de catálogos únicos para poblar los selectores desplegables de filtro.

### 4.3 Servicio y Precarga de Datos
* **`AuditServiceImpl.java`:** Orquesta la paginación (`Pageable`), la conversión DTO con formato `AUD-%04d`, la traducción de estados de base de datos a UI y la generación de descripciones humanizadas.
* **`AuditDataInitializer.java`:** Componente de arranque (`CommandLineRunner`) activo en perfiles `dev` y `default`. Valida de forma idempotente (`count() == 0`) e inserta 10 eventos representativos coincidentes con el diseño oficial (AUD-1250 a AUD-1241) para garantizar que la pantalla sea demostrable localmente de inmediato.

---

## 5. Catálogo de Endpoints de la API REST

Ruta base: `/api/v1/audit`  
Seguridad: `@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR','SUPERVISOR')")`

| Método | Endpoint | Parámetros Query | Descripción | Códigos HTTP |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/audit` | `page`, `size`, `sort`, `q`, `usuarioId`, `modulo`, `accion` | Listado paginado de eventos con filtrado dinámico. | `200 OK`, `401 Unauthorized`, `403 Forbidden` |
| `GET` | `/api/v1/audit/{id}` | `id` (path variable) | Detalle forense de un evento con datos JSON anteriores/nuevos. | `200 OK`, `404 Not Found` |
| `GET` | `/api/v1/audit/metrics` | Ninguno | Obtención de las 4 métricas numéricas del panel superior. | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/audit/filters` | Ninguno | Listas únicas de usuarios, módulos y acciones para filtros UI. | `200 OK`, `401 Unauthorized` |

---

## 6. Interfaz de Usuario Frontend (`renta-autos-app/`)

### 6.1 Estructura Visual (`auditoria.html`)
* **Topbar Institucional:** Buscador global, notificaciones con badge circular `3`, y ficha de usuario `LR · Luis Ramírez (Auditor)`.
* **Sidebar Fijo:** Logotipo de la empresa, menú de navegación con la opción **Auditoría** resaltada con fondo azul corporativo (`#1565C0`), y pie con isotipo y lema *"Transparencia en movimiento"*.
* **4 Tarjetas KPI Superiores:**
  1. *Total de registros:* Azul pastel (`#EBF3FC`).
  2. *Usuarios activos:* Verde menta (`#E8F8F0`).
  3. *Acciones hoy:* Ámbar cálido (`#FFF6E9`).
  4. *Intentos fallidos:* Rosa pastel (`#FDECEF`).
* **Barra de Herramientas:** Búsqueda textual con *debounce*, desplegables de Usuario, Módulo y Tipo de acción, y botón interactivo para restablecer filtros.
* **Tabla de Registros con Badges Semánticos:**
  * `Editar`: Azul suave (`#E3F2FD` / `#1976D2`).
  * `Crear`: Verde menta (`#E8F8F0` / `#2E7D32`).
  * `Eliminar` / `Error de login`: Rojo pastel (`#FFEBEE` / `#C62828`).
  * `Inicio de sesión`: Cian suave (`#E0F7FA` / `#00838F`).
  * `Cancelar`: Naranja claro (`#FFF3E0` / `#EF6C00`).
  * `Actualizar`: Púrpura suave (`#F3E5F5` / `#7B1FA2`).
  * `Resultado`: Píldoras en verde ("Exitosa") o rojo ("Fallida").
* **Modal de Inspección Forense:** Ventana emergente accesible mediante clic en la fecha/hora o ID del registro, que organiza la metadata del evento y compara en dos columnas con formato de código (*JSON prettified*) los valores antes y después de la modificación.

### 6.2 Lógica de Cliente (`auditoria.js`)
* Integración con almacenamiento local (`localStorage`) para adjuntar el encabezado `Authorization: Bearer <token>`.
* Consumo asíncrono y reactivo de `/api/v1/audit/metrics` y `/api/v1/audit/filters`.
* Temporizador debounce de 300 ms en la barra de búsqueda para minimizar el tráfico hacia el backend.
* Paginador numérico dinámico y control seguro de apertura y cierre de modal.

---

## 7. Pruebas Automatizadas y Calidad (DoD)

La suite de pruebas automatizadas garantiza la estabilidad de la lógica de negocio y las directivas de seguridad web:

1. **`AuditServiceTest.java` (JUnit 5 + Mockito):**
   * Validación del cálculo matemático de las 4 métricas analíticas.
   * Verificación de la construcción y combinación de `AuditSpecification`.
   * Conversión consistente de entidades a DTOs con prefijo `AUD-`.
   * Proyección adecuada del usuario `"Sistema"` ante identificadores nulos.
   * Manejo de error `ResourceNotFoundException` ante consultas por identificador inexistente.
2. **`AuditControllerTest.java` (Spring Boot Test + MockMvc):**
   * Verificación de respuesta `200 OK` para solicitudes GET con rol `AUDITOR` o `ADMINISTRADOR`.
   * Bloqueo con `401 Unauthorized` en llamadas no autenticadas.
   * Restricción con `403 Forbidden` ante roles no facultados (`AGENTE`).
   * Verificación de inmutabilidad: rechazo de solicitudes de escritura (POST) para certificar que la bitácora no puede ser manipulada externamente.

### 7.1 Resultado de Ejecución de la Suite
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.rentadeautos.modules.audit.service.AuditServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.824 s
[INFO] Running com.rentadeautos.modules.audit.controller.AuditControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.411 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 152, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] -------------------------------------------