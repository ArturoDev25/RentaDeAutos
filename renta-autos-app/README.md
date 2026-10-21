# RentaAutos - Sistema de Renta de Vehículos

Este proyecto es el frontend de una aplicación de renta de autos inspirada en la experiencia de Airbnb.

## Cómo ejecutar el proyecto localmente

No necesitas instalar ninguna dependencia. Para ver la aplicación:
1. Abre la carpeta `renta-autos-app` en tu editor de código (recomendado: VS Code).
2. Haz doble clic en el archivo `index.html` para abrir la página principal en tu navegador.
3. Haz doble clic en `login.html` para ver la pantalla de acceso.

## Estructura de Archivos

- `index.html`: Inicio del portal del cliente: buscador y vehículos disponibles.
- `catalogo.html`: Catálogo con filtros por categoría y precio diario.
- `vehiculo.html`: Detalle del vehículo (`?id=`) y botón para iniciar la reservación.
- `login.html`: Interfaz de inicio de sesión.
- `css/style.css`: Contiene todos los estilos globales, colores y diseño responsivo.
- `css/portal.css`: Estilos del portal del cliente.
- `js/portal.js`: Funciones compartidas del portal (consulta del catálogo, tarjetas, estados de carga, vacío y error, sesión).
- `js/inicio.js`, `js/catalogo.js`, `js/vehiculo.js`: Lógica de cada página del portal.
- `js/auth.js`: Controla la validación del formulario de login y la redirección.
- `assets/`: Carpeta destinada a imágenes y recursos visuales.

## Tecnologías utilizadas
- **HTML5**: Estructura semántica.
- **CSS3**: Diseño moderno usando Flexbox y Grid.
- **JavaScript (Vanilla)**: Lógica de cliente sin frameworks.

## Instrucciones para el equipo
Los vehículos del portal ya no se escriben en el HTML. Inicio, catálogo y detalle los obtienen de `GET /api/v1/catalogo/vehiculos` (ver `docs/catalogo-cliente-api.md`). Para que aparezca un auto en el portal debe estar `DISPONIBLE`, con categoría activa y una tarifa vigente.

Para probar el portal, levanta el backend en `http://localhost:8080` y abre `index.html`. Si el backend está apagado se muestra el mensaje de error de conexión con la opción de reintentar.
