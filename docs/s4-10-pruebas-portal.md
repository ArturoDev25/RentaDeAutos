# S4-10 — Pruebas y Verificación de "Mis reservaciones", "Mis rentas", Reportes y Ayuda

## 1. Resumen de componentes implementados

### Backend (`com.rentadeautos.modules.reservation` y `security`)
- **`ReservacionClienteResponse` y `RentaClienteResponse`:** Enriquecidos con metadatos del vehículo (`marca`, `modelo`, `anio`, `categoria`) e indicador `puedeCancelar` en reservaciones, manteniendo constructores compatibles hacia atrás para no romper pruebas de S4-06 ni S4-11.
- **`ReservacionService.cancelarDesdePortal`:** Valida que `fechaInicio` sea estrictamente posterior a la fecha/hora actual (`409 Conflict` si ya inició o pasó), además de estado `PENDIENTE` o `CONFIRMADA` y pertenencia al cliente autenticado.
- **`ReporteClienteController` / `ReporteClienteService` / `ReporteClienteRepository`:** Nuevo endpoint `GET /api/v1/cliente/reportes/resumen?periodo=7D|30D|3M|1A` que calcula KPIs propios del cliente, variaciones frente al periodo anterior, serie para gráfica de barras, distribución por estado, tabla de historial y el aviso explícito de importes estimados.
- **`SecurityConfig`:** Asegura `GET /api/v1/cliente/reportes/resumen` exclusivamente para `ROLE_CLIENTE` y cierra `/api/v1/cliente/**` con `denyAll()`.

### Frontend (`renta-autos-app/`)
- **`mis-reservaciones.html` + `js/mis-reservaciones.js`:** Tarjetas de reservaciones propias, modal de detalles, cancelación con confirmación únicamente cuando `puedeCancelar === true`, estado vacío y reintento ante error.
- **`mis-rentas.html` + `js/mis-rentas.js`:** Historial de rentas (`EN_CURSO` y `FINALIZADA`) en modo solo lectura, filtros por estado (*Todas*, *Renta activa*, *Finalizadas*), barra de progreso de días transcurridos en rentas activas y modal informativo.
- **`reportes-cliente.html` + `js/reportes-cliente.js`:** Selector de periodo (*7 días*, *30 días*, *3 meses*, *1 año*), 4 tarjetas KPI con porcentaje de variación, gráfica de barras, gráfica de dona SVG, tabla de historial, aviso de importes estimados y exportación CSV generada en el navegador.
- **`ayuda.html`:** Centro de ayuda con acordeón de 6 preguntas frecuentes alineadas con las reglas reales del sistema y modal de contacto de soporte en Sucursal Saltillo.
- **`js/portal-cuenta.js` + `css/portal-cuenta.css`:** Guardia de sesión (`requerirCliente`), interceptor de `401` (`apiCliente` limpia `localStorage.usuarioSesion` y redirige a `login.html?redirect=...`), formateadores y utilidades puras.

## 2. Ejecución de pruebas automatizadas

### Pruebas de Backend (JUnit 5 + Spring Boot Test)
```powershell
cd backend
.\mvnw.cmd test "-Dtest=ReservacionClienteRepositoryTest,ReservacionClienteControllerTest,ReservacionPortalAltaTest,ReporteClienteRepositoryTest,ReporteClienteControllerTest"
```

### Pruebas Unitarias de Utilidades JS (`portal-cuenta.js`)
Verifica cálculo de folios (`RES-XXXX`, `REN-XXXX`), días cobrados (RN-02), barra de progreso de renta activa, reglas de cancelabilidad y sanitización contra inyección de fórmulas en CSV.
```powershell
node --test scratch/portal-cuenta.test.js
```
*(O cualquier archivo de prueba que importe `renta-autos-app/js/portal-cuenta.js`).*

## 3. Lista de verificación manual en navegador

1. **Sesión expirada / Sin sesión:** Abrir `mis-reservaciones.html`, `mis-rentas.html` o `reportes-cliente.html` sin sesión; verificar redirección inmediata a `login.html?redirect=<pagina>`.
2. **Aislamiento entre clientes:** Iniciar sesión con Cliente A y Cliente B en navegadores separados; confirmar que ninguno visualiza ni puede consultar/cancelar operaciones del otro (`404` en IDs ajenos).
3. **Solo lectura en Mis rentas:** Confirmar que en `mis-rentas.html` no existen botones para cancelar, modificar fechas ni cerrar rentas.
4. **Aviso de importes estimados en Reportes:** Verificar la presencia del banner informativo *"Los montos son estimados; el cobro final se registra al devolver el vehículo."* y probar la descarga del archivo CSV.
