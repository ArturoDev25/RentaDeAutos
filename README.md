# RentaDeAutos

Aplicación web para administrar la operación básica de una empresa de renta de
autos. El sistema centraliza clientes, vehículos, disponibilidad,
reservaciones, entregas, devoluciones, incidencias y trazabilidad de las
acciones realizadas por el personal.

---

## 🗄️ Infraestructura de Base de Datos

El backend se conecta a **MySQL 8.0** mediante un pool HikariCP configurado en
[`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml).
La aplicación completa se ejecuta mediante Docker Compose, que levanta MySQL,
el backend Spring Boot y el frontend Nginx.

### Variables de entorno

Las variables se configuran en `.env`, creado a partir de
[`.env.example`](.env.example):

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_NAME` | Nombre de la base de datos | `rentadeautos` |
| `DB_PORT` | Puerto de MySQL publicado en el host | `3306` |
| `BACKEND_PORT` | Puerto del backend publicado en el host | `8080` |
| `FRONTEND_PORT` | Puerto del frontend publicado en el host | `80` |
| `JWT_SECRET` | Secreto para firmar tokens JWT | `genera-una-clave-segura` |
| `JWT_EXPIRATION_MINUTES` | Duración de los tokens JWT | `30` |

`DB_URL` no necesita configurarse manualmente. Docker Compose la construye
usando el host interno `mysql`; por ese motivo no debe utilizarse `localhost`
para la conexión del backend dentro de Docker.

El archivo `.env` está excluido del repositorio y no debe contenerse en
commits. Para crear la configuración local:

```powershell
cp .env.example .env
```

Después, reemplaza al menos `DB_PASSWORD` y `JWT_SECRET` por valores seguros.

### Quick Start — Docker Compose

Desde la raíz del repositorio:

```powershell
# Levantar MySQL, backend y frontend
docker compose up -d

# Verificar el estado de los servicios
docker compose ps

# Consultar los logs
docker compose logs -f
```

El backend se compila dentro de su imagen con Maven, se ejecuta con el perfil
`prod` y utiliza el archivo `.jar` generado. MySQL cuenta con un healthcheck y
el backend espera a que la base de datos esté disponible antes de iniciar.
Flyway ejecuta automáticamente las migraciones al arrancar.

URLs por defecto:

- Frontend: <http://localhost>
- Backend: <http://localhost:8080>
- Health check: <http://localhost/api/v1/health>

Respuesta esperada del health check:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "database": "UP"
  }
}
```

El frontend utiliza Nginx para servir los archivos estáticos y reenviar las
solicitudes `/api` al backend mediante la red interna de Docker.

Para detener los contenedores sin eliminar los datos:

```powershell
docker compose down
```

Para detenerlos y eliminar también el volumen de MySQL:

```powershell
docker compose down -v
```

### Esquema DDL y migraciones

Las migraciones versionadas de Flyway se encuentran en
[`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration).
La migración inicial define las tablas principales del sistema, incluyendo
`roles`, `usuarios_app`, `clientes`, `categorias`, `tarifas`, `vehiculos`,
`reservaciones`, `entregas`, `devoluciones`, `incidencias` y `auditoria`.

📖 **Guías relacionadas:**

- [`docs/database-setup.md`](docs/database-setup.md)
- [`docs/modelo-datos.md`](docs/modelo-datos.md)

---

## 🧪 Tests automatizados y CI

Las pruebas del backend usan H2 en memoria, por lo que no requieren iniciar
MySQL ni Docker. GitHub Actions las ejecuta automáticamente en cada `push` y
`pull request` mediante
[`.github/workflows/ci.yml`](.github/workflows/ci.yml).

Para ejecutarlas manualmente desde la raíz en Windows PowerShell:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
```

El resultado esperado es `BUILD SUCCESS`, con cero fallos y errores. Para más
opciones, consulta [`backend/src/test/README.md`](backend/src/test/README.md).

El frontend cuenta con pruebas Node.js. Desde la carpeta
[`renta-autos-app`](renta-autos-app):

```powershell
node --test
```

---

## 🔐 Autenticación

El sistema utiliza JWT para autenticación y BCrypt para el manejo de
contraseñas. Requiere las variables `JWT_SECRET` y
`JWT_EXPIRATION_MINUTES`, configuradas en `.env`.

📖 **Guía completa:** [`docs/autenticacion.md`](docs/autenticacion.md)

## Reservaciones del cliente — S4-06

El cliente autenticado puede crear y consultar sus reservaciones, cancelar las
permitidas y consultar su historial de rentas de solo lectura.

Contrato, permisos, migración V6 y guía de comprobación:
[`docs/reservaciones-cliente-api.md`](docs/reservaciones-cliente-api.md).

## Aprobación de clientes del portal — S5-09

Las cuentas creadas desde el portal quedan pendientes hasta que el personal las
aprueba o rechaza en **Clientes pendientes**. Estados, endpoints, permisos y
auditoría:
[`docs/s5-09-aprobacion-clientes.md`](docs/s5-09-aprobacion-clientes.md).

## Otras funcionalidades documentadas

- [Catálogo del cliente](docs/catalogo-cliente-api.md)
- [Vehículos](docs/vehiculos-api.md)
- [Tarifas](docs/tarifas-api.md)
