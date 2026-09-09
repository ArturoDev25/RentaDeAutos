# Configuración de Base de Datos — Sistema de Renta de Autos

Guía paso a paso para reproducir el entorno de base de datos MySQL local y verificar la conectividad.

---

## Prerrequisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Java | 21 | `java -version` |
| Maven | 3.9+ | o usar `./mvnw` incluido |
| Docker Desktop | 24+ | Para Método A |
| MySQL Server | 8.0 | Solo para Método B |

---

## Método A — Docker Compose (Recomendado)

El método más rápido y reproducible. Levanta MySQL 8.0 con el esquema
oficial auto-inicializado sin configuración adicional.

### Paso 1 — Configura tus variables de entorno

```bash
# Desde la raíz del repositorio
cp .env.example .env
```

Edita `.env` si necesitas cambiar contraseña u otro valor. Los valores por
defecto son suficientes para desarrollo local.

### Paso 2 — Levanta el contenedor

```bash
docker compose up -d
```

El contenedor:
- Descarga `mysql:8.0` (solo la primera vez)
- Crea el volumen persistente `mysql_data`
- Ejecuta `V1__init_schema.sql` automáticamente (solo si el volumen está vacío)
- Expone MySQL en `localhost:3306`

### Paso 3 — Verifica que MySQL esté listo

```bash
docker compose ps
# La columna STATUS debe mostrar: Up (healthy)
```

O con logs:

```bash
docker compose logs -f mysql
# Espera ver: /usr/sbin/mysqld: ready for connections
```

### Paso 4 — Inicia el backend

```bash
cd backend
./mvnw spring-boot:run
```

### Comandos útiles de Docker Compose

```bash
docker compose stop          # Detiene el contenedor (preserva datos)
docker compose start         # Reanuda el contenedor detenido
docker compose down          # Detiene y elimina el contenedor (preserva volumen)
docker compose down -v       # ⚠️ Detiene, elimina contenedor Y volumen (borra datos)
docker compose logs mysql    # Ver logs del contenedor
```

---

## Método B — MySQL Nativo / Workbench

Para quienes tengan MySQL instalado localmente o usen MySQL Workbench.

### Paso 1 — Crea la base de datos e importa el esquema

**Opción B.1 — Desde la línea de comandos MySQL:**

```bash
mysql -u root -p < V1__init_schema.sql
```

**Opción B.2 — Desde MySQL Workbench:**

1. Abre MySQL Workbench y conéctate a tu servidor local.
2. Menú → **Server** → **Data Import**.
3. Selecciona **Import from Self-Contained File**.
4. Navega a `V1__init_schema.sql` en la raíz del repositorio.
5. Haz clic en **Start Import**.

### Paso 2 — Verifica el esquema

```sql
USE rentadeautos;
SHOW TABLES;
-- Debe listar 11 tablas:
-- auditoria, categorias, clientes, devoluciones,
-- entregas, incidencias, reservaciones, roles,
-- tarifas, usuarios_app, vehiculos
```

### Paso 3 — Configura las variables de entorno

```bash
cp .env.example .env
```

Ajusta `DB_USER`, `DB_PASSWORD` según tu instalación local de MySQL.

### Paso 4 — Inicia el backend

```bash
cd backend
./mvnw spring-boot:run
```

---

## Variables de Entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL de conexión JDBC completa | `jdbc:mysql://localhost:3306/rentadeautos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | *(vacío)* |
| `DB_PORT` | Puerto del contenedor MySQL | `3306` |
| `DB_NAME` | Nombre de la base de datos | `rentadeautos` |

> **Nota:** El archivo `.env` está en `.gitignore`. Nunca lo subas al repositorio.
> Usa `.env.example` como referencia y crea tu propio `.env` local.

---

## Verificación con el Endpoint de Salud

Una vez que MySQL y el backend estén corriendo, verifica la conectividad:

```bash
curl -s http://localhost:8080/api/v1/health | json_pp
```

### Respuesta esperada (todo UP)

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "database": "UP",
    "timestamp": "2026-09-08T03:30:00Z"
  }
}
```

### Respuesta cuando MySQL no está disponible

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "database": "DOWN",
    "timestamp": "2026-09-08T03:30:00Z"
  }
}
```

> `database: DOWN` indica que el backend inició pero no puede alcanzar MySQL.
> Revisa que el contenedor esté healthy: `docker compose ps`.

---

## Ejecución de Tests (sin MySQL)

Los tests están desacoplados de MySQL mediante H2 en memoria:

```bash
cd backend
./mvnw clean compile   # Compilar
./mvnw test            # Ejecutar tests (usa H2, no requiere MySQL)
```

Ambos comandos deben terminar en **BUILD SUCCESS**.

---

## Esquema de la Base de Datos

El script `V1__init_schema.sql` (raíz del repositorio) crea las siguientes tablas:

| # | Tabla | Descripción |
|---|---|---|
| 1 | `roles` | Catálogo de roles del sistema |
| 2 | `usuarios_app` | Personal que opera el sistema |
| 3 | `clientes` | Clientes que reservan vehículos |
| 4 | `categorias` | Clasificación de vehículos |
| 5 | `tarifas` | Precios por categoría y período |
| 6 | `vehiculos` | Vehículos disponibles para renta |
| 7 | `reservaciones` | Solicitudes de renta por período |
| 8 | `entregas` | Registro de entrega del vehículo |
| 9 | `devoluciones` | Registro de devolución del vehículo |
| 10 | `incidencias` | Daños, fallas y multas |
| 11 | `auditoria` | Historial de acciones del sistema |
