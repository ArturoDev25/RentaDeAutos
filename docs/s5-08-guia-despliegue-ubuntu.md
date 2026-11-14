# S5-08 - Guía de despliegue en Ubuntu con Docker

## 1. Versión y alcance

Base documental y commit desplegado reportado:
`3b4380d062de6154ad1f71c6ceb9d3faadfcf883`.

Esta guía conserva el procedimiento de referencia para Ubuntu solicitado
en S5-08. El despliegue real se realizó en Debian; no se acredita una
ejecución en Ubuntu.

Datos proporcionados por el responsable del servidor:

| Dato | Valor reportado |
|---|---|
| Servidor | srv01 |
| Sistema operativo | Debian; versión no proporcionada |
| Fecha de despliegue | 2026-11-04 |
| Docker / Docker Compose | 29.9.0 / 5.6.0 |
| Ruta del proyecto | `~/RentaDeAutos` |
| Rama y estado de Git | `main`, limpio |
| Comando utilizado | `docker compose up -d --build` |
| Configuración | `.env` y `docker-compose.override.yml` |
| Perfil Spring | `prod` |
| Puertos publicados | MySQL 3307, backend 8080 y frontend 8081 |
| Acceso por Tailscale | `http://100.80.219.96:8081` |

Gustavo comprobó login, dashboard y health UP mediante
`https://srv01.tailb2f071.ts.net/login.html`.

El contenido del override y la configuración del acceso HTTPS no fueron
proporcionados. El override tiene fecha de modificación del 6 de noviembre;
queda pendiente confirmar cuándo se recrearon los servicios con esos ajustes.

S5-07 fue aprobada según confirmación de Gustavo. Falta adjuntar el reporte
MySQL y vincularlo con el commit desplegado.

Los resultados del ensayo se registran en
[el acta de verificación](s5-08-verificacion-y-ensayo.md).

## 2. Requisitos

- Servidor Ubuntu accesible para el responsable del despliegue.
- Docker Engine y plugin Docker Compose instalados; Git, curl y OpenSSL.
- Acceso al repositorio y a los registros de imágenes durante la construcción.
- Espacio para imágenes y volumen MySQL, y puertos disponibles.
- Acceso desde los equipos de la demostración a la IP y puerto del frontend.
- Credenciales y secreto JWT elegidos para el entorno; nunca subir `.env`.

Java y Maven se incluyen en la etapa de construcción del Dockerfile;
no se necesitan en el host para arrancar mediante Compose.
El servidor real utiliza Debian con Docker 29.9.0 y Compose 5.6.0.
Para una instalación nueva en Ubuntu, comprobar sus versiones y requisitos
antes de ejecutar el procedimiento.

En la terminal Ubuntu:

```bash
cat /etc/os-release
docker --version
docker compose version
docker info
git --version
```

Si Docker no está instalado o el usuario no tiene permiso para usarlo, completar
esa preparación con el responsable de S5-06 antes de continuar.

## 3. Obtener y fijar la versión

Para una instalación nueva:

```bash
git clone https://github.com/ArturoDev25/RentaDeAutos.git
cd RentaDeAutos
git fetch origin
```

Seleccionar el commit aprobado para la presentación. Este ejemplo fija la versión
revisada documentalmente, no acredita su aprobación contra MySQL:

```bash
git switch --detach 3b4380d062de6154ad1f71c6ceb9d3faadfcf883
git rev-parse HEAD
git status --short
```

Si el equipo ya tiene una instalación, no volver a clonar ni cambiar su versión
sin revisar sus archivos locales, configuración y respaldo. Un commit diferente
requiere comprobar qué cambió y actualizar esta guía.

## 4. Configuración de `.env`

En una instalación nueva, sin sobrescribir un archivo existente:

```bash
test -f .env || cp .env.example .env
chmod 600 .env
nano .env
```

| Variable | Uso en el Compose revisado |
|---|---|
| `DB_NAME` | Base de datos, por ejemplo `rentadeautos` |
| `DB_USER` | `root` en este Compose; no crea automáticamente otro usuario |
| `DB_PASSWORD` | Contraseña MySQL elegida localmente; no conservar `root` |
| `DB_PORT` | Puerto publicado del host; 3306 por defecto, 3307 si está ocupado |
| `BACKEND_PORT` | Puerto publicado del backend; 8080 por defecto |
| `FRONTEND_PORT` | Puerto publicado del frontend; 80 por defecto |
| `JWT_SECRET` | Clave aleatoria de firma, generada localmente |
| `JWT_EXPIRATION_MINUTES` | Duración de sesión; 30 por defecto |

Generar la clave JWT y copiarla a `.env` sin compartir su salida:

```bash
openssl rand -base64 48
```

Compose carga `.env`; no hace falta ejecutar `source .env`. El backend utiliza
`mysql:3306` dentro de la red Docker aunque el puerto publicado sea 3307. No usar
`localhost` como host MySQL desde el contenedor backend. Compose construye `DB_URL`.
Cambiar la contraseña del `.env` no cambia la de un volumen MySQL ya inicializado;
para una instalación existente coordinar el cambio con el administrador.

## 5. Comprobaciones previas de zona horaria y datos demo

### Zona horaria

El PR S5-07 #163 reporta posibles falsos traslapes cuando la JVM no usa UTC.
El Compose revisado fija UTC en MySQL pero no explícitamente en el backend.
Comprobar cómo lo resolvió S5-06. Si no hay un ajuste, puede prepararse este override
**como propuesta a validar**, sin afirmar que ya se utilizó en el servidor:

```yaml
# compose.utc.yaml
services:
  backend:
    environment:
      TZ: UTC
      JAVA_TOOL_OPTIONS: "-Duser.timezone=UTC"
```

Si ya existe `JAVA_TOOL_OPTIONS`, conservar sus opciones y añadir la de zona horaria.
El servidor reporta un archivo `docker-compose.override.yml`.
Con los nombres predeterminados, `docker compose` lo carga automáticamente
junto con `docker-compose.yml`.

Los comandos sin `-f` de esta guía utilizan esa configuración combinada
cuando el override está presente.

Si se añade el archivo opcional `compose.utc.yaml`, deben conservarse
también los ajustes del override existente. En ese caso:

```bash
docker compose -f docker-compose.yml -f docker-compose.override.yml -f compose.utc.yaml config --quiet
docker compose -f docker-compose.yml -f docker-compose.override.yml -f compose.utc.yaml up -d --build
```

Esta variante UTC es una propuesta pendiente de validación, no un paso
confirmado del despliegue real. No sustituir el override existente.

### Datos de demostración: diferencia que debe resolver S5-06

En la versión revisada:

- Compose activa `SPRING_PROFILES_ACTIVE=prod`.
- El seed S5-04 requiere perfil `demo`, ausencia de `prod`/`production`,
  `DEMO_SEED_ENABLED=true` y `DEMO_SEED_PASSWORD` (12 caracteres mínimo y 72 bytes
  UTF-8 máximo). Debe apuntar a una base exclusiva de demostración.
- El POM toma fotos de la carpeta hermana `renta-autos-app`, pero el contexto de
  construcción del backend solo incluye `backend`. Ese empaquetado no asegura
  disponer de las fotos requeridas por el seed.

Por ello, **arrancar el Compose base no implica cargar el seed S5-04**. No basta
con escribir variables demo en `.env` si Compose no las pasa al contenedor.
Solicitar a S5-06 el Dockerfile, perfil, variables y overrides realmente utilizados.
Conservar esos ajustes como archivos versionados revisables. No activar el seed
sobre datos reales ni combinar `prod,demo` esperando que funcione.

Consultar [seed-demostracion-sprint-5.md](seed-demostracion-sprint-5.md).
Repetir el seed conserva las aprobaciones y estados: no reinicia automáticamente
los datos usados durante la presentación. Preparar nuevas cuentas para repetir el ensayo.

## 6. Iniciar y comprobar servicios

Después de configurar el entorno y resolver los ajustes necesarios para la demo:

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=150 backend
```

Servicios: `mysql`, `backend` y `frontend`. MySQL tiene healthcheck; que los otros
contenedores estén `Up` no demuestra por sí solo que la API responda.
Flyway aplica las migraciones del artefacto al arrancar. Esta versión incluye V1 a
V7, no solo V1 a V6 como indica la descripción inicial de S5-06.

Definir el puerto elegido y comprobar desde el servidor:

```bash
FRONTEND_HTTP_PORT=8081
curl -f "http://127.0.0.1:${FRONTEND_HTTP_PORT}/api/v1/health"
```

Resultado esperado: `success: true`, `status: UP` y `database: UP`.
Nginx sirve HTML/CSS/JS y redirige `/api/` a `backend:8080` por la red interna.
Desde otra computadora, abrir `http://IP_DEL_SERVIDOR:PUERTO/login.html` sustituyendo
los valores por los reales. `localhost` en otra computadora apunta a esa computadora,
no al servidor Ubuntu.

Verificar login de personal y cliente aprobado, registro pendiente, aprobación,
catálogo con imágenes, reserva, historial y contenido del dashboard. Comprobar las
reglas de red/firewall con S5-06 si funciona localmente pero no desde otro equipo.

## 7. Detener, volver a levantar y actualizar

| Operación | Comando |
|---|---|
| Detener sin eliminar contenedores | `docker compose stop` |
| Volver a iniciar contenedores detenidos | `docker compose start` |
| Eliminar contenedores y red, conservando volumen | `docker compose down` |
| Recrearlos desde configuración existente | `docker compose up -d` |
| Reconstruir imágenes tras cambiar código | `docker compose up -d --build` |
| Consultar estado | `docker compose ps` |
| Ver registros recientes | `docker compose logs --tail=100 backend` |

**No utilizar `docker compose down -v` en el entorno que se quiere conservar:**
elimina el volumen de la base. Mantener el mismo directorio/proyecto Compose para
reutilizar el volumen esperado. `restart` no reconstruye imágenes ni aplica nuevas
variables; para esos cambios usar `up -d` y `--build` cuando corresponda.

Antes de actualizar: respaldo verificado de MySQL, commit anterior anotado, nuevo
commit aprobado y revisión de migraciones. Volver a una imagen anterior no deshace
migraciones ni cambios de datos. No usar borrado de volumen como solución a errores
Flyway, de contraseñas o de arranque.

## 8. Diagnóstico y evidencia

| Síntoma | Revisión |
|---|---|
| Puerto ocupado | Ajustar puerto del host en `.env`; el puerto interno MySQL sigue 3306 |
| Error de JWT | Revisar que la clave real esté configurada y recrear backend |
| Access denied MySQL | Comparar credenciales con las del volumen ya inicializado |
| Error Flyway | Revisar logs y migraciones; no editar migraciones ya aplicadas |
| 502 desde frontend | Revisar backend, conexión MySQL y arranque completo |
| Sin datos o fotos demo | Revisar perfil, activación del seed, base destino y recursos de la imagen |
| Pendientes no aparecen | Comprobar si ya se aprobaron/rechazaron; el seed conserva esas decisiones |

No adjuntar `.env`, tokens ni la salida completa de `docker compose config`, porque
puede contener credenciales. Usar `config --quiet` para validar sintaxis.

El Dockerfile ejecuta Maven con `-DskipTests`: construir la imagen no acredita las
pruebas. Registrar el resultado CI del **mismo commit** y la evidencia MySQL de S5-07.
