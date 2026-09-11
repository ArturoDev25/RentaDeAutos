# Autenticación — S1-08

Sistema de inicio y cierre de sesión con Spring Security, BCrypt y JWT.

- **Issue:** S1-08 — Implementar autenticación
- **Responsable:** Rafael de Jesús Martínez Vélez
- **Estado:** Backend terminado y probado

## 1. Qué incluye

- Inicio de sesión con correo y contraseña contra la tabla `usuarios_app`.
- Contraseñas almacenadas con BCrypt; nunca en texto plano ni en respuestas.
- Rechazo de credenciales incorrectas y de usuarios desactivados.
- Token JWT firmado, con el rol incluido y expiración configurable.
- Consulta de la identidad autenticada y cierre de sesión.

## 2. Configuración requerida

Agrega estas variables a tu `.env` (usa `.env.example` como plantilla):

```bash
JWT_SECRET="genera-la-tuya-con-openssl-rand-base64-48"
JWT_EXPIRATION_MINUTES=30
```

Genera tu clave con:

```bash
openssl rand -base64 48
```

La aplicación **no arranca** sin `JWT_SECRET`. Es intencional: evita correr con una
clave vacía y una seguridad falsa.

Antes de iniciar el backend, carga las variables en tu terminal:

```bash
set -a; source .env; set +a
cd backend && ./mvnw spring-boot:run
```

## 3. Usuarios de demostración

Los carga la migración `V3__seed_usuarios_demo.sql`. Contraseña de todos: `Demo1234`.

| Correo | Rol | Activo |
|---|---|---|
| admin@demo.local | ADMINISTRADOR | Sí |
| agente@demo.local | AGENTE | Sí |
| supervisor@demo.local | SUPERVISOR | Sí |
| auditor@demo.local | AUDITOR | **No** |

El auditor está desactivado a propósito, para poder probar ese caso negativo.

## 4. Endpoints

### POST /api/auth/login

Público. Recibe:

```json
{ "correo": "admin@demo.local", "password": "Demo1234" }
```

Responde 200:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": { "id": 1, "nombre": "Ana Demo Administradora",
              "correo": "admin@demo.local", "rol": "ADMINISTRADOR" }
  }
}
```

| Código | Situación |
|---|---|
| 200 | Credenciales válidas |
| 400 | Correo con formato inválido o campos vacíos |
| 401 | Contraseña incorrecta, correo inexistente o usuario desactivado |

Los tres casos de 401 devuelven el **mismo mensaje genérico** (RN-AUTH-05), para no
revelar qué correos existen en el sistema.

### GET /api/auth/me

Requiere token. Devuelve la identidad del usuario autenticado, sin el hash.

### POST /api/auth/logout

Requiere token. Responde 204.

> Un JWT no puede invalidarse desde el servidor sin una lista negra, que quedó fuera
> del Sprint 1 por la decisión D-01. **El frontend debe descartar el token.**
> El endpoint existe para dar un punto formal de cierre y para alojar la auditoría
> de cierre de sesión cuando se implemente.

## 5. Cómo enviar el token

Authorization: Bearer <accessToken>


Ejemplo:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@demo.local","password":"Demo1234"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -i http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

## 6. Notas para quien construya encima

- El rol viaja dentro del token como `ROLE_<NOMBRE>`, así que `hasRole("ADMINISTRADOR")`
  funciona sin configuración extra (útil para US-AUTH-02).
- El filtro **no consulta la base de datos**: si desactivas a un usuario, su token
  sigue siendo válido hasta que expire. Con 30 minutos el riesgo es acotado.
- El contenido de un JWT es legible por cualquiera; solo la firma está protegida.
  Nunca metas datos sensibles dentro del token.
- CORS está abierto para `localhost` en cualquier puerto. Para staging hay que
  ajustar `SecurityConfig.corsConfigurationSource()`.

## 7. Pendientes fuera de este issue

| Pendiente | Responsable |
|---|---|
| Auditoría en la tabla `auditoria` (hoy solo se escribe en logs) | Luis — US-AUTH-04 |
| Conectar `renta-autos-app/js/auth.js` con la API real | Luis y Frida |
| Permisos por rol en endpoints protegidos | Miguel — US-AUTH-02 |
| Confirmar prefijo de rutas: `/api/auth` vs `/api/v1` | Equipo |

## 8. Pruebas

```bash
cd backend
./mvnw test
```

12 pruebas. `AuthServiceTest` cubre los seis criterios de aceptación, incluyendo
que los tres casos de fallo devuelvan mensajes idénticos.