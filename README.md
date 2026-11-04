# RentaDeAutos 

Aplicación web para administrar la operación básica de una empresa de renta de autos. El sistema centralizará clientes, vehículos, disponibilidad, reservaciones, entregas, devoluciones, incidencias y trazabilidad de las acciones realizadas por el personal.

---

## 🗄️ Infraestructura de Base de Datos

El backend se conecta a **MySQL 8.0** a través de un pool HikariCP configurado en `backend/src/main/resources/application.yml`.

### Variables de entorno requeridas

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | `root` |
| `DB_NAME` | Nombre de la base de datos | `rentadeautos` |
| `DB_PORT` | Puerto de MySQL publicado en el host | `3306` |
| `BACKEND_PORT` | Puerto del backend publicado en el host | `8080` |
| `FRONTEND_PORT` | Puerto del frontend publicado en el host | `80` |
| `JWT_SECRET` | Secreto para firmar tokens JWT | `genera-una-clave-segura` |
| `JWT_EXPIRATION_MINUTES` | Duración de los tokens JWT | `30` |

Dentro de Docker, Compose construye automáticamente `DB_URL` con el host
interno `mysql`; no debe configurarse con `localhost`.

Copia la plantilla y ajusta los valores:

```bash
cp .env.example .env
```

### Quick Start — Docker Compose

```bash
# 1. Crear la configuración local (el archivo .env no se sube al repositorio)
cp .env.example .env

# 2. Reemplazar JWT_SECRET y DB_PASSWORD en .env

# 3. Levantar MySQL, backend y frontend
docker compose up -d

# 4. Verificar los tres servicios
docker compose ps

# 5. Verificar el backend a través del frontend/Nginx
curl http://localhost/api/v1/health
```

Respuesta esperada:
```json
{ "success": true, "data": { "status": "UP", "database": "UP", "timestamp": "..." } }
```

El backend se construye dentro de su imagen con Maven y se ejecuta con el perfil
`prod`. Su conexión usa el nombre de servicio `mysql`, mientras que el frontend
usa Nginx para servir los archivos estáticos y reenviar `/api` al backend.
Flyway ejecuta las migraciones al iniciar el backend.

### Esquema DDL

El script `V1__init_schema.sql` (raíz del repositorio) define las 11 tablas normalizadas del sistema: `roles`, `usuarios_app`, `clientes`, `categorias`, `tarifas`, `vehiculos`, `reservaciones`, `entregas`, `devoluciones`, `incidencias` y `auditoria`.

📖 **Guía completa de setup:** [`docs/database-setup.md`](docs/database-setup.md)

---

## 🧪 Tests automatizados y CI

Las pruebas usan **H2 en memoria**, por lo que no requieren MySQL ni Docker.
GitHub Actions las ejecuta automáticamente en cada `push` y `pull request`
mediante [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

Para ejecutarlas manualmente desde la raíz del repositorio:

```bash
cd backend
./mvnw test
```

En Windows PowerShell:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
```

El resultado esperado es `BUILD SUCCESS`, con cero fallos y errores. Para más
opciones, consulta [`backend/src/test/README.md`](backend/src/test/README.md).

---

## 🔐 Autenticación

Login con JWT y BCrypt. Requiere las variables `JWT_SECRET` y `JWT_EXPIRATION_MINUTES`.

📖 **Guía completa:** [`docs/autenticacion.md`](docs/autenticacion.md)

## Reservaciones del cliente — S4-06

El cliente autenticado puede crear y consultar sus reservaciones, cancelar las
permitidas y consultar su historial de rentas de solo lectura.

Contrato, permisos, migración V6 y guía de comprobación:
[docs/reservaciones-cliente-api.md](docs/reservaciones-cliente-api.md).

## Aprobación de clientes del portal — S5-09

Las cuentas creadas desde el portal quedan pendientes hasta que el personal
las aprueba o rechaza en **Clientes pendientes**. Estados, endpoints, permisos
y auditoría: [docs/s5-09-aprobacion-clientes.md](docs/s5-09-aprobacion-clientes.md).
