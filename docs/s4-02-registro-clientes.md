# S4-02 — Registro de clientes

Implementación preparada sobre main c95bdbb, con S4-01 (migración V5) y
S4-03 (login de clientes) integrados. Este documento sustituye al contrato
borrador como referencia de la implementación.

## Contrato

`POST /api/auth/cliente/registro` es público y recibe JSON:

```json
{
  "nombre": "Ana",
  "apellidos": "Prueba",
  "correo": "ana@example.test",
  "password": "Clave123!",
  "telefono": "8441234567",
  "numeroLicencia": "LIC-PORTAL-01",
  "licenciaVencimiento": "2030-12-31",
  "direccion": "Domicilio de prueba"
}
```

La fecha de vencimiento debe ser posterior al día de ejecución.
Dirección es opcional. Los demás campos son obligatorios para guardar
un cliente conforme al modelo existente.

| Campo | Regla |
|---|---|
| nombre | No vacío, máximo 100 caracteres |
| apellidos | No vacío, máximo 150 caracteres |
| correo | No vacío, formato de correo, máximo 150 caracteres |
| password | No vacío, mínimo 6 caracteres, máximo 72 bytes UTF-8 |
| telefono | No vacío, máximo 20 caracteres |
| numeroLicencia | No vacío, máximo 50 caracteres |
| licenciaVencimiento | Fecha válida posterior a hoy |
| direccion | Opcional, máximo 255 caracteres |

Política adoptada para esta implementación: contraseña obligatoria,
no compuesta únicamente por espacios, con mínimo seis caracteres y
máximo 72 bytes en UTF-8. Se conserva el mínimo del registro existente.
No se exigen mayúsculas, números ni símbolos. La contraseña no se
recorta ni se normaliza.

Se recortan espacios externos de los campos de texto excepto password.
Correo se normaliza a minúsculas y licencia a mayúsculas con Locale.ROOT.
La contraseña se conserva exactamente como se recibió y se codifica con
el PasswordEncoder BCrypt ya configurado en SecurityConfig.

## Respuestas

201:

```json
{
  "success": true,
  "data": {
    "id": 7,
    "nombre": "Ana",
    "apellidos": "Prueba",
    "correo": "ana@example.test",
    "activo": true
  }
}
```

No se devuelve contraseña, hash ni token. Después se usa el login
existente POST /api/auth/cliente/login.

- 400: campos inválidos, obligatorios ausentes o JSON malformado.
- 409: correo o licencia ya registrados; siempre el mismo mensaje:
  "No se pudo completar el registro con los datos proporcionados."
- Otros fallos de integridad conservan el manejo general de errores;
  no se disfrazan como duplicados.

No se exponen el correo existente, su estado, identificador, hash o SQL
en el mensaje de conflicto. La diferencia entre 201 y 409 es observable
por diseño del criterio de aceptación; el mensaje genérico no elimina
por sí solo esa posibilidad de inferencia.

## Persistencia y seguridad

- Se crea un registro nuevo en clientes, activo=true.
- No se crea personal en usuarios_app. Rol, id, activo y passwordHash
  no son campos editables del DTO público.
- Un correo existente, incluso de un cliente de mostrador sin contraseña
  o desactivado, se rechaza; no se sobrescribe ni se reactiva.
- Se verifica correo y licencia antes de guardar. saveAndFlush también
  fuerza la verificación de las restricciones UNIQUE de la base.
- Los conflictos concurrentes de uq_clientes_correo y uq_clientes_licencia
  se traducen al mismo 409. La transacción revierte el alta fallida.
- Solo se abre POST de la ruta exacta. El alta de personal continúa
  restringida a ADMINISTRADOR.
- No se modifican migraciones existentes ni se implementa la interfaz
  del portal, que corresponde a otra tarea.

## Pruebas incluidas

ClienteRegistroControllerTest:
alta pública 201, respuesta sin credenciales, obligatorios, blancos,
formato de correo, longitudes, contraseña en caracteres y bytes,
licencia vigente, JSON inválido, duplicado 409 y rutas protegidas.

ClienteRegistroServiceTest:
normalización, contraseña BCrypt real y sin recortar, cuenta activa,
duplicados sin guardar, simulación de colisión UNIQUE concurrente,
otros errores de integridad y DTO sin contraseña en JSON/toString.

ClienteRegistroIntegrationTest:
contexto real de Spring con filtros de seguridad, H2 independiente,
migraciones Flyway y Hibernate validate. Prueba el registro y posterior
login CLIENTE, persistencia del hash, rechazo sin modificar una cuenta de
mostrador y validación sin crear filas. Cada prueba revierte sus datos.

La simulación de colisiones del servicio no es una prueba de carga con
dos transacciones concurrentes en MySQL.

## Ejecución en Windows CMD

Desde backend:

```bat
mvnw.cmd -Dtest=ClienteRegistroControllerTest,ClienteRegistroServiceTest,ClienteRegistroIntegrationTest test
mvnw.cmd test
```

Estas pruebas usan H2 y no requieren levantar Docker.

Resultados de ejecución — 20 de octubre de 2026:

- Pruebas específicas de registro: 43 ejecutadas, 0 fallos,
  0 errores y 0 omitidas. BUILD SUCCESS.
- Suite completa: 670 ejecutadas, 0 fallos,
  0 errores y 0 omitidas. BUILD SUCCESS.

Las pruebas automatizadas validan el registro público, los datos
obligatorios, el rechazo de duplicados, el almacenamiento con BCrypt,
la respuesta sin credenciales y el inicio de sesión del cliente creado.
