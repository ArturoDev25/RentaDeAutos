# Pruebas e integración continua

Esta carpeta contiene las pruebas automatizadas del backend. La configuración de
integración continua se encuentra en
[`/.github/workflows/ci.yml`](../../../.github/workflows/ci.yml).

## Ejecución automática en GitHub Actions

El workflow `CI` se ejecuta automáticamente cuando:

- Se realiza un `push` a cualquier rama.
- Se crea o actualiza un `pull request`.

El workflow realiza los siguientes pasos:

1. Descarga el código del repositorio.
2. Configura Java 21 usando Temurin.
3. Habilita la caché de dependencias de Maven.
4. Entra al directorio `backend`.
5. Ejecuta `./mvnw test`.

El resultado puede consultarse en la pestaña **Actions** del repositorio. Un
check verde indica que todas las pruebas terminaron correctamente; un check rojo
indica que alguna prueba falló o que el workflow no pudo completarse.

Las pruebas usan H2 en memoria y el perfil `test`, por lo que el workflow no
requiere iniciar MySQL ni levantar Docker.

## Ejecutar todas las pruebas manualmente

### Windows PowerShell

Desde la raíz del repositorio:

```powershell
Set-Location .\backend
.\mvnw.cmd test
```

También se puede ejecutar en una sola línea desde la raíz:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
```

### Linux, macOS o Git Bash

Desde la raíz del repositorio:

```bash
cd backend
./mvnw test
```

El resultado esperado termina con:

```text
BUILD SUCCESS
```

Y debe mostrar cero fallos y cero errores:

```text
Failures: 0, Errors: 0
```

## Ejecutar una prueba específica

Para ejecutar una clase concreta:

```powershell
.\mvnw.cmd -Dtest=HealthControllerTest test
```

En Linux, macOS o Git Bash:

```bash
./mvnw -Dtest=HealthControllerTest test
```

Para ejecutar varias clases específicas, sepáralas con comas:

```powershell
.\mvnw.cmd "-Dtest=HealthControllerTest,AuthServiceTest" test
```

## Limpieza y ejecución completa

Si se necesita eliminar los resultados de compilaciones anteriores y ejecutar
las pruebas nuevamente:

```powershell
.\mvnw.cmd clean test
```

El comando equivalente en Linux, macOS o Git Bash es:

```bash
./mvnw clean test
```

No se debe usar `-DskipTests`, porque esa opción omite precisamente las pruebas
que valida la integración continua.
