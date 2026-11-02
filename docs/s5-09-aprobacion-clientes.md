# S5-09 — Aprobación y rechazo de clientes del portal

Las cuentas que los clientes crean desde el portal (`registro.html`) ya no quedan
activas de inmediato: nacen **inactivas y pendientes de aprobación** hasta que un
usuario autorizado del personal las aprueba o las rechaza desde el panel interno.

## Estados de la cuenta

| `estado_aprobacion` | `activo` | ¿Puede iniciar sesión? | Cuándo |
|---|---|---|---|
| `PENDIENTE` | `FALSE` | No (403, "pendiente de aprobación") | Al registrarse en el portal |
| `APROBADO` | `TRUE` | Sí | El personal aprobó la cuenta |
| `RECHAZADO` | `FALSE` | No (403, "fue rechazada") | El personal rechazó la cuenta |
| `NULL` | sin cambios | Igual que antes | Altas de mostrador y clientes previos a S5-09 |

- Migración: `V7__aprobacion_clientes.sql` agrega `estado_aprobacion`, `fecha_revision`,
  un `CHECK` de valores permitidos y un índice. No hace `UPDATE` masivo: los clientes
  existentes conservan su acceso.
- Se conserva la validación existente: una cuenta inactiva nunca inicia sesión
  (también una aprobada que después se desactiva).
- El mensaje específico (403) solo se da si la **contraseña es correcta**; con contraseña
  incorrecta se responde el 401 genérico, así no se revela qué correos existen.
- Una cuenta `PENDIENTE` o `RECHAZADO` no se puede "Activar" desde la pantalla de Clientes
  (respondería 400): se revisa en **Clientes pendientes**.

## Endpoints

Todos requieren JWT de **ADMINISTRADOR, AGENTE o SUPERVISOR**. Auditor y Cliente → 403; sin token → 401.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/v1/clientes/pendientes` | Lista de cuentas `PENDIENTE` (más antigua primero) | 200 |
| PATCH | `/api/v1/clientes/{id}/aprobar` | `PENDIENTE` → `APROBADO`, `activo = true` | 200, 404, 409 |
| PATCH | `/api/v1/clientes/{id}/rechazar` | `PENDIENTE` → `RECHAZADO`, `activo = false`. Cuerpo opcional: `{"motivo": "..."}` (máx. 255) | 200, 400, 404, 409 |

`409` = la cuenta ya no está pendiente (otra persona ya la revisó o es un cliente de mostrador).
La respuesta de `ClienteResponse` ahora incluye `estadoAprobacion`, `fechaRegistro` y `fechaRevision`.

Ejemplo (Windows PowerShell, con el backend corriendo):

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' -Body '{"correo":"agente@demo.local","password":"Demo1234"}'
$h = @{ Authorization = "Bearer $($login.data.accessToken)" }
Invoke-RestMethod -Uri http://localhost:8080/api/v1/clientes/pendientes -Headers $h
Invoke-RestMethod -Method Patch -Uri http://localhost:8080/api/v1/clientes/5/aprobar -Headers $h
```

## Auditoría

| Acción | Entidad | Resultado | Cuándo |
|---|---|---|---|
| `APROBAR_CLIENTE` | `CLIENTE` | `EXITOSO` | Aprobación (misma transacción, RN-09) |
| `RECHAZAR_CLIENTE` | `CLIENTE` | `EXITOSO` | Rechazo; `valores_nuevos` incluye el motivo |
| `APROBAR_CLIENTE` / `RECHAZAR_CLIENTE` | `CLIENTE` | `FALLIDO` | Intento sobre una cuenta que ya no está pendiente (409) |

El actor es el usuario autenticado. Los snapshots nunca incluyen la contraseña ni su hash.

## Frontend

- `registro.html`: al registrarse se oculta el formulario y se muestra
  **"Tu cuenta quedó pendiente de aprobación"** (coordinado con S5-02). También se corrigió
  que el botón rediseñado (`.registro-boton`) no enviaba el formulario.
- `login.html`: un cliente pendiente o rechazado ve el motivo; el login en cascada (S5-03)
  no reintenta como personal ante un 403.
- `clientes-pendientes.html` (nuevo, en el menú del panel): lista con datos para revisión
  (contacto, licencia con alerta si está vencida o por vencer, dirección, fecha de solicitud),
  botones **Aprobar** y **Rechazar** (con motivo opcional). Tras cada acción la fila muestra el
  nuevo estado y se actualizan los contadores. El menú la oculta al Auditor.
- `clientes.html`: el estado muestra "Pendiente" o "Rechazado" en lugar de solo "Inactivo".

## Pruebas

- Backend: `ClienteAprobacionIntegrationTest` (flujo completo con JWT real, Flyway y H2:
  registro pendiente, aprobación por cada rol, rechazo, login posterior, 401/403, 404/409 y
  auditoría), `ClienteAprobacionServiceTest`, y ajustes en `ClienteLoginServiceTest`,
  `ClienteServiceTest`, `ClienteRegistro*Test` y `PortalClienteIntegrationTest`.
- Frontend: `renta-autos-app/test/aprobacion-clientes.test.js` (`node --test` dentro de `renta-autos-app`).

## Para S5-04 (datos de demostración)

Para sembrar clientes pendientes basta con insertar filas con `activo = FALSE`,
`estado_aprobacion = 'PENDIENTE'`, correo y `password_hash` BCrypt.
