# S4-03 — Inicio de sesión del cliente (portal)

## Endpoint
`POST /api/auth/cliente/login` — público (no requiere token).

Petición:

```json
{ "correo": "ana@demo.local", "password": "..." }
```

## Respuestas
| Caso | HTTP | Cuerpo |
|---|---|---|
| Credenciales válidas de un cliente activo | 200 | `{"success":true,"data":{"accessToken":"...","tokenType":"Bearer","expiresIn":1800,"user":{...,"rol":"CLIENTE"}}}` |
| Correo inexistente, contraseña incorrecta, cuenta inactiva o cliente sin cuenta de portal | 401 | `{"success":false,"message":"Correo o contraseña incorrectos"}` (siempre el mismo) |
| Correo vacío o con formato inválido, o contraseña vacía | 400 | error de validación |

El mensaje del 401 es idéntico en todos los casos para no revelar si un correo está registrado.
Además la contraseña se compara siempre (con un hash señuelo si el correo no existe), para que
tampoco lo delate el tiempo de respuesta.

## Contrato del token CLIENTE
- Claim `rol`: `"CLIENTE"`.
- `subject`: `"cliente:<id>"`, donde `<id>` es el id de la tabla `clientes`.
- El `subject` NO es el correo a propósito: `JwtAuthenticationFilter` busca el subject como correo en
  `usuarios_app` (personal) e ignora el claim `rol`. Con un correo, un cliente registrado con el
  correo de un usuario del personal sería tomado por ese usuario. Con `cliente:<id>` nunca coincide.
- `user.id` de la respuesta es el id del cliente (no el de un usuario del personal).

## Notas para S4-04 y S4-06
- Hoy el filtro no autentica tokens CLIENTE (no encuentra el subject en `usuarios_app`): las rutas
  protegidas responden 401, nunca dan acceso. S4-04 debe agregar al filtro la rama CLIENTE: leer el
  claim `rol`, extraer el id de `cliente:<id>`, verificar que el cliente exista y esté activo, y
  autenticar con la autoridad `ROLE_CLIENTE`.
- Los clientes sin `password_hash` (registrados en mostrador) no pueden iniciar sesión.
