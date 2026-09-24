# S2-14 — Interfaz de categorías

Responsable: Gustavo Moreno.
Issue relacionado: #37.
Referencia: Plan Sprint 2 v1.1, del 23 de septiembre de 2026.

## Alcance

Pantalla del Panel de Administrador conectada a la API existente
de categorías, implementada previamente en S2-08.

Permite:
- Consultar categorías.
- Filtrar por estado activo o inactivo.
- Registrar y editar nombre, descripción y depósito base.
- Activar y desactivar sin eliminar físicamente.
- Mostrar errores y conservar el formulario cuando falla el guardado.

## Archivos principales

- renta-autos-app/categorias.html
- renta-autos-app/css/categorias.css
- renta-autos-app/js/categorias.js

## Integración

Se reutilizan client.js y la sesión usuarioSesion del login existente.
Las solicitudes incluyen el token en Authorization: Bearer.

La identidad se verifica mediante GET /api/auth/me.
El apartado está restringido al ADMINISTRADOR, según el alcance
del Sprint 2 v1.1.

SecurityConfig protege /api/categorias y sus subrutas.
La restricción se aplica también en el backend.

client.js utiliza http://localhost:8080/api en desarrollo local.
Se conserva la configuración alternativa mediante window.API_BASE_URL.

## Operaciones utilizadas

| Método | Ruta | Operación |
|---|---|---|
| GET | /api/categorias | Consultar y filtrar por activo |
| POST | /api/categorias | Registrar |
| PUT | /api/categorias/{id} | Editar |
| PATCH | /api/categorias/{id}/estado | Activar o desactivar |

## Ejecución local

1. Configurar .env a partir de .env.example.
2. Iniciar MySQL con docker compose up -d mysql.
3. Cargar las variables de .env en la terminal del backend.
4. Desde backend, ejecutar mvnw.cmd spring-boot:run.
5. Abrir login.html mediante Live Server.
6. Iniciar sesión como administrador.
7. Abrir categorias.html en el mismo origen del navegador.

Si cambia el puerto local de MySQL, ajustar DB_PORT y DB_URL
en .env. No subir .env al repositorio.

## Pruebas automatizadas

Resultado local del 24 de septiembre de 2026:
- Categorías: 29 pruebas, cero fallos y cero errores.
- Suite completa del backend: 156 pruebas, cero fallos y cero errores.

Las pruebas verifican el controlador y servicio de categorías,
las restricciones de acceso y que las solicitudes rechazadas
no lleguen al servicio.

Las pruebas automatizadas del backend no sustituyen la validación
manual de la interfaz.

## Integración con el equipo

- El enlace del panel debe apuntar a categorias.html.
- Coordinar los cambios del cliente HTTP compartido con Javier.
- La navegación a otros apartados depende de que sus pantallas
  estén integradas.
- El CRUD de tarifas queda fuera de esta entrega.