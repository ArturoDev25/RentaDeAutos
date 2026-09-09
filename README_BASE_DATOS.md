# Entrega S1-06 — Migración inicial de la base de datos

## Archivos

- `database/setup/00_create_database.sql`: crea la base local una sola vez.
- `backend/src/main/resources/db/migration/V1__init_schema.sql`: crea las tablas, relaciones, restricciones e índices mediante Flyway.
- `backend/src/main/resources/db/migration/V2__seed_roles.sql`: registra el catálogo inicial de roles.
- `database/tests/schema_smoke_test.sql`: consultas y pruebas rápidas para reunir evidencia.

## Requisitos

- MySQL 8.0.16 o posterior.
- Proyecto Spring Boot con `flyway-mysql` y el conector de MySQL.
- Credenciales de la base proporcionadas mediante variables de entorno.

## Ejecución local

1. Crear la base:

   ```powershell
   mysql -u root -p -e "source database/setup/00_create_database.sql"
   ```

2. Configurar Spring Boot sin guardar credenciales reales en GitHub:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/rentadeautos?serverTimezone=UTC
   spring.datasource.username=${DB_USER}
   spring.datasource.password=${DB_PASSWORD}
   spring.flyway.enabled=true
   spring.jpa.hibernate.ddl-auto=validate
   ```

3. Iniciar Spring Boot. Flyway aplicará V1 y V2 automáticamente.

4. Ejecutar las comprobaciones:

   ```powershell
   mysql -u root -p -e "source database/tests/schema_smoke_test.sql"
   ```

## Reglas que quedan en la API REST

- Evitar reservaciones traslapadas dentro de una transacción.
- Comprobar que el vehículo se encuentre disponible.
- Comparar el kilometraje de devolución con el de entrega.
- Autorizar operaciones de acuerdo con el rol usando Spring Security.
- Normalizar correo, placa y VIN antes de guardar.
- Registrar en auditoría los eventos exitosos y fallidos.

## Criterio de cierre

- Flyway ejecuta V1 y V2 en una base vacía.
- Un segundo arranque no repite las migraciones.
- Existen once tablas funcionales y el historial de Flyway.
- Las PK, FK, restricciones únicas, `CHECK` e índices están presentes.
- Se adjunta evidencia de al menos una prueba correcta y cuatro negativas.
- No existen contraseñas, tokens ni credenciales reales en el repositorio.
