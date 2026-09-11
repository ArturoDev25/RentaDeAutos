# RentaDeAutos

Aplicación web para administrar la operación básica de una empresa de renta de autos. El sistema centralizará clientes, vehículos, disponibilidad, reservaciones, entregas, devoluciones, incidencias y trazabilidad de las acciones realizadas por el personal.

---

## 🗄️ Infraestructura de Base de Datos

El backend se conecta a **MySQL 8.0** a través de un pool HikariCP configurado en `backend/src/main/resources/application.yml`.

### Variables de entorno requeridas

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL de conexión JDBC | `jdbc:mysql://localhost:3306/rentadeautos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | `root` |
| `DB_PORT` | Puerto del contenedor | `3306` |
| `DB_NAME` | Nombre de la base de datos | `rentadeautos` |

Copia la plantilla y ajusta los valores:

```bash
cp .env.example .env
```

### Quick Start — Docker Compose

```bash
# 1. Levantar MySQL 8.0 (auto-inicializa el esquema en primer arranque)
docker compose up -d

# 2. Verificar que el contenedor esté healthy
docker compose ps

# 3. Iniciar el backend
cd backend && ./mvnw spring-boot:run

# 4. Verificar la conexión
curl http://localhost:8080/api/v1/health
```

Respuesta esperada:
```json
{ "success": true, "data": { "status": "UP", "database": "UP", "timestamp": "..." } }
```

### Esquema DDL

El script `V1__init_schema.sql` (raíz del repositorio) define las 11 tablas normalizadas del sistema: `roles`, `usuarios_app`, `clientes`, `categorias`, `tarifas`, `vehiculos`, `reservaciones`, `entregas`, `devoluciones`, `incidencias` y `auditoria`.

📖 **Guía completa de setup:** [`docs/database-setup.md`](docs/database-setup.md)

---

## 🧪 Tests Automatizados

Los tests corren desacoplados de MySQL usando **H2 en memoria**. No requieren ninguna BD activa:

```bash
cd backend
./mvnw clean compile   # BUILD SUCCESS
./mvnw test            # BUILD SUCCESS (H2, sin MySQL)
```

---

## 🔐 Autenticación

Login con JWT y BCrypt. Requiere las variables `JWT_SECRET` y `JWT_EXPIRATION_MINUTES`.

📖 **Guía completa:** [`docs/autenticacion.md`](docs/autenticacion.md)