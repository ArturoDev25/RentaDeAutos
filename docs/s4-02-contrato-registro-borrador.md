# S4-02 — Preparación del registro de clientes

Estado: borrador de contrato y pruebas; todavía no implementado ni ejecutado.
Fecha: 2026-10-18.
Tarea: https://github.com/ArturoDev25/RentaDeAutos/issues/121
Dependencia: S4-01, PR #133, pendiente de integración al revisar este documento.
Referencia técnica revisada: commit dbedd833476046fd29bd42ad5f0900157218098d.

## Base confirmada

La documentación de S4-01 adopta cuentas de clientes en `clientes`, separadas
del personal en `usuarios_app`. Agrega `password_hash`, sin asignar credenciales
a clientes existentes. El correo ya tiene restricción de unicidad.

El registro debe solicitar los campos obligatorios del modelo. La pantalla
actual captura únicamente nombre, correo y contraseña; la conexión y ampliación
del formulario corresponde a S4-07 y debe usar el contrato acordado aquí.

## Contrato propuesto

POST /api/auth/cliente/registro
Content-Type: application/json
Acceso público, sin JWT previo.

| Campo | Validación prevista |
| --- | --- |
| nombre | Obligatorio, no blanco, máximo 100 caracteres |
| apellidos | Obligatorio, no blanco, máximo 150 caracteres |
| correo | Obligatorio, formato válido, máximo 150 caracteres, único |
| password | Obligatoria, no blanca; política de longitud pendiente de confirmar |
| telefono | Obligatorio, no blanco, máximo 20 caracteres |
| numeroLicencia | Obligatorio, no blanco, máximo 50 caracteres, único |
| licenciaVencimiento | Obligatoria, fecha ISO YYYY-MM-DD, posterior a hoy |
| direccion | Opcional, máximo 255 caracteres |

Ejemplo de estructura (no representa una cuenta creada):

```json
{
  "nombre": "Cliente",
  "apellidos": "De Prueba",
  "correo": "cliente.prueba@example.com",
  "password": "ClaveEjemplo123!",
  "telefono": "8441234567",
  "numeroLicencia": "LIC-S402-001",
  "licenciaVencimiento": "2030-12-31",
  "direccion": "Saltillo, Coahuila"
}
```

Respuestas propuestas:

- 201: cuenta creada, success=true y DTO con id, nombre, apellidos, correo y activo=true.
- 400: datos inválidos, sin guardar una cuenta.
- 409: correo o licencia duplicados, sin modificar al cliente existente.
- Ninguna respuesta incluye password, passwordHash ni detalles internos de SQL.

Mensaje genérico propuesto para conflictos de unicidad:
"No se pudo completar el registro con los datos proporcionados."
El código 409 sigue indicando conflicto; no se promete ocultar la existencia
del correo por completo. El mensaje no incluye nombres, estado de cuenta,
identificadores ni datos del cliente existente.

## Comportamiento previsto

1. Validar y normalizar los datos con reglas explícitas.
2. Normalizar el correo con trim y minúsculas usando Locale.ROOT.
3. Normalizar la licencia con trim y mayúsculas usando Locale.ROOT.
4. Comprobar unicidad de correo y licencia.
5. Codificar la contraseña con el PasswordEncoder BCrypt existente.
   No recortar ni transformar la contraseña silenciosamente.
6. Crear un nuevo Cliente activo dentro de una transacción.
7. Dejar que la restricción UNIQUE proteja también los registros concurrentes.
   Traducir los conflictos de unicidad al mismo 409 sin exponer el error SQL;
   no convertir indiscriminadamente cualquier fallo de base de datos en duplicado.
8. Responder con DTO explícito sin credenciales.

No se aceptan rol, activo ni passwordHash como decisiones del solicitante.
El cliente se crea activo por decisión del servidor. No se genera JWT al
registrar: el inicio de sesión se implementa en S4-03.
Un correo existente con password_hash=NULL también se rechaza: este endpoint
no debe apropiarse de una ficha de mostrador ni asignarle una contraseña.

## Pruebas por preparar

| Caso | Resultado esperado |
| --- | --- |
| Registro público con datos válidos | 201, cliente activo |
| Campos obligatorios ausentes, vacíos o blancos | 400, sin escritura |
| Correo inválido o longitudes excedidas | 400, sin escritura |
| Contraseña que incumple la política acordada | 400, sin escritura |
| Fecha de licencia inválida, vencida o de hoy | 400, sin escritura |
| Correo repetido, incluidas variantes de mayúsculas | 409 genérico |
| Correo de cliente de mostrador sin credenciales | 409, ficha intacta |
| Licencia repetida | 409 genérico, sin cuenta nueva |
| Conflicto UNIQUE detectado al guardar | 409 sin detalles internos |
| Contraseña persistida | Distinta del texto original; BCrypt.matches verdadero |
| Respuestas de éxito y error | Sin contraseña, hash ni contraseña rechazada |
| Campos extra de rol, activo o hash | No permiten controlar permisos ni estado |
| Clientes existentes | No se alteran datos ni contraseñas |
| Registro de personal ya existente | Conserva su comportamiento |

Capas previstas: pruebas del servicio con repositorio simulado y BCrypt real;
pruebas MockMvc con seguridad real; pruebas de persistencia sobre base de pruebas
con migración V5 para verificar unicidad y almacenamiento.
La comprobación por mocks no se presentará como prueba de persistencia real.

## Pendientes antes de cerrar la implementación

- S4-01 debe integrarse y la rama local debe actualizarse con su versión final.
- Confirmar la política de contraseña del portal. El RegisterRequest del registro
  anterior exige seis caracteres como mínimo; esto es un antecedente del código,
  no una política de Sprint 4 confirmada. Definir también el límite compatible
  con BCrypt, evitando truncamiento silencioso.
- La documentación de S4-01 señala coordinación de orden con la migración V4
  del PR #117. Comprobar el historial final antes de migrar la base local.
- Acordar este contrato con la integración del frontend S4-07.
- Ejecutar las pruebas específicas y la suite completa después de implementar.

Este documento no registra pruebas aprobadas de S4-02.
