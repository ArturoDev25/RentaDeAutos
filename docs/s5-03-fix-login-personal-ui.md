# S5-03: Corregir Inicio de Sesión del Personal en la Interfaz (BUG)

## 1. Resumen y Contexto
En el Sprint 4 se conectó `login.html` exclusivamente al endpoint `/api/auth/cliente/login`. Esto provocaba que cualquier usuario del personal (`ADMINISTRADOR`, `AGENTE`, `SUPERVISOR`, `AUDITOR`), cuyas credenciales se encuentran en `usuarios_app`, recibiera un error HTTP `401 Unauthorized`, quedando impedido de acceder al panel administrativo desde la interfaz gráfica.

## 2. Solución Implementada
Se implementó un flujo de autenticación en cascada en `renta-autos-app/js/auth.js`:
1. **Primer intento:** `POST /api/auth/cliente/login` con `{ correo, password }`.
2. **Fallback condicional:** Si y solo si el primer intento responde HTTP `401` (el usuario no existe en la tabla de clientes o las credenciales no pertenecen a un cliente), se reintenta automáticamente con `POST /api/auth/login` (personal).
3. **Manejo de errores:**
   - Si ambos fallan con HTTP `401`, se presenta el mensaje de error genérico: `"Correo o contraseña incorrectos"`.
   - Si ocurre un fallo distinto a `401` (p. ej. error de red o HTTP `500`), el error se propaga inmediatamente sin reintentar.
4. **Redirección según rol:**
   - Roles de personal (`ADMINISTRADOR`, `AGENTE`, `SUPERVISOR`, `AUDITOR`) son redirigidos a `dashboard.html`.
   - Rol `CLIENTE` es redirigido a `index.html` (o al parámetro seguro `?redirect=...`).

## 3. Criterios de Aceptación Verificados (Issue #151)
- [x] El inicio de sesión intenta primero utilizar `POST /api/auth/cliente/login`.
- [x] Si la autenticación del cliente responde HTTP 401, se intenta `POST /api/auth/login`.
- [x] Los usuarios con rol `ADMINISTRADOR` pueden ingresar al panel interno (`dashboard.html`).
- [x] Los usuarios con rol `AGENTE` pueden ingresar al panel interno (`dashboard.html`).
- [x] Los usuarios con rol `SUPERVISOR` pueden ingresar al panel interno (`dashboard.html`).
- [x] Los usuarios con rol `CLIENTE` son redirigidos al portal del cliente (`index.html`).
- [x] El personal autenticado es redirigido a `dashboard.html`.
- [x] Las credenciales incorrectas muestran un mensaje genérico (`"Correo o contraseña incorrectos"`).
- [x] No se realizaron modificaciones en los endpoints ni en la lógica del backend.
- [x] Se verifica que el flujo funcione con los diferentes roles del sistema.
- [x] Compatible con el nuevo diseño del login (S5-01 / PR #158) y el botón `#togglePassword`.

## 4. Ejecución de Pruebas Automatizadas
Se añadieron pruebas unitarias automatizadas en `renta-autos-app/test/auth-login.test.js` ejecutadas con el runner nativo de Node.js:

```bash
node --test renta-autos-app/test/auth-login.test.js
```

### Resultado:
```text
✔ S5-03: Caso 1 - Cliente válido autentica en /auth/cliente/login sin llamar a personal
✔ S5-03: Caso 2a - Personal ADMINISTRADOR recibe 401 en /auth/cliente/login y autentica en /auth/login
✔ S5-03: Caso 2b - Personal AGENTE y SUPERVISOR autentican correctamente via /auth/login tras 401
✔ S5-03: Caso 3 - Credenciales inválidas para cliente y personal lanzan error 401
✔ S5-03: Caso 4 - Error de red o servidor en cliente no intenta endpoint de personal
✔ S5-03: Caso 5 - Determinar destino correcto por rol
✔ S5-03: Caso 6 - Mensaje de error genérico para 401 y otros códigos
ℹ tests 7
ℹ pass 7
ℹ fail 0
```
