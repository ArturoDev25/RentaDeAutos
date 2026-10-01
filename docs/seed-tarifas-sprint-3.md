# S3-07 — Seed de tarifas de demostración

## Objetivo

Garantizar una tarifa vigente hoy para cada categoría activa del entorno demo.
Los importes son ficticios; no representan precios comerciales acordados.
No se crean categorías adicionales ni se modifican vehículos o reservaciones.

## Ejecución

`TarifaDataInitializer` corre automáticamente al arrancar Spring Boot en el perfil
`dev` o en el perfil implícito `default`. No corre con el perfil `test` por sí solo,
ni con un perfil `prod` por sí solo. No habilitar `dev`/`default` para datos reales.

Requiere Java 21, MySQL accesible, las variables de conexión y `JWT_SECRET` según
las guías de instalación y autenticación existentes. Docker no es obligatorio.
Desde `backend`, con las variables ya configuradas en la terminal:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Orden de carga:

1. `VehiculoDataInitializer`, `@Order(10)`: asegura categorías y vehículos demo.
2. `TarifaDataInitializer`, `@Order(20)`: revisa categorías y carga tarifas faltantes.

No requiere una migración nueva: reutiliza la tabla `tarifas` y `TarifaService`.
Los IDs se obtienen de los registros existentes; no se supone que SUV tenga ID 2.

## Datos ficticios

| Categoría | Precio diario | Cargo de atraso diario |
|---|---:|---:|
| Económico | $500.00 | $100.00 |
| SUV | $850.00 | $100.00 |
| Pickup | $1,000.00 | $100.00 |
| Lujo | $1,800.00 | $100.00 |
| Otra categoría activa | $850.00 | $100.00 |

Los nombres se comparan sin acentos ni diferencias de mayúsculas. Una categoría
inactiva no recibe tarifa. Para una categoría adicional se utiliza el precio
ficticio de respaldo; debe revisarse antes de usarlo fuera de una demo.

## Vigencia y repetición

La fecha inicial es el día del arranque, según la zona horaria del servidor.

- Si ya existe una tarifa activa vigente hoy, se conserva con su precio y fechas.
- Si no hay tarifa vigente hoy ni tarifas activas futuras, se crea sin fecha final.
- Si existe una tarifa futura, la nueva termina el día anterior a la primera futura.
- Los registros históricos e inactivos se conservan y no bloquean el periodo actual.
- Reiniciar con la misma cobertura no duplica tarifas.
- Si hoy coinciden varias tarifas activas, el seed falla con un mensaje y revierte
  su transacción; no intenta decidir qué precio es correcto ni borrar registros.

Se reutilizan los bloqueos por categoría y la validación de traslapes del CRUD.
La tarifa de demo puede volver a crearse en un arranque posterior si se desactiva
la que cubría hoy y queda un hueco: el seed busca mantener la cobertura demo.
No se garantizan todos los periodos futuros; la cobertura obligatoria es hoy.

Mensaje esperado en el log:

```text
Datos demo ficticios: se crearon N tarifas; se conservaron las existentes
```

En una base nueva con las cuatro categorías demo, N debe ser 4. Si todas ya tienen
cobertura vigente hoy, N debe ser 0; con otras categorías activas puede variar.

## Verificación automatizada

Desde `backend`:

```powershell
.\mvnw.cmd "-Dtest=TarifaDataInitializerTest" test
.\mvnw.cmd test
```

Los seis casos nuevos usan H2 y verifican:

1. Cobertura de las cuatro categorías y repetición sin duplicados.
2. Conservación de una tarifa vigente previamente capturada.
3. Creación hasta el día anterior a una tarifa futura.
4. Exclusión de categorías inactivas.
5. Conservación de tarifas históricas e inactivas.
6. Precio de respaldo para una categoría adicional.

Resultado reportado por el desarrollador el 1 de octubre de 2026: 304 casos en
la suite completa, 0 fallos, 0 errores y 0 omitidos; BUILD SUCCESS. La comprobación
manual con MySQL descrita abajo sigue pendiente de ejecución y evidencia.

## Guion de demostración con MySQL

1. Arrancar el backend con perfil `dev` y comprobar el mensaje del seed.
2. Autenticarse como Administrador. Consultar `GET /api/v1/tarifas` y comprobar
   cobertura de cada categoría activa para hoy.
3. Detener y reiniciar el backend. Consultar de nuevo: los IDs de las tarifas
   vigentes y sus precios deben conservarse; el log debe indicar 0 nuevas si
   no se cambiaron categorías ni tarifas y sigue existiendo cobertura hoy.
4. Para mostrar el costo, elegir un cliente activo y un vehículo disponible de
   categoría SUV. Verificar que la tarifa que aplica a la fecha inicial sea $850;
   el seed conserva una tarifa preexistente aunque tenga otro precio.
5. Crear una reservación con inicio hoy a las 15:00 y fin tres días después a las
   15:00, sin otro periodo ocupado para ese vehículo. Usar fecha y hora reales
   elegidas por el operador; respetar las validaciones del módulo de reservaciones.
6. Comprobar `tarifaDia = 850.00` y `totalEstimado = 2550.00` si la tarifa aplicable
   es la ficticia de SUV: 3 días × $850. No incluye atraso, combustible ni daños.
   Si se conserva otro precio, el esperado es 3 × ese precio.
7. Guardar evidencia de la consulta, el reinicio y el resultado de la reservación,
   sin incluir tokens ni contraseñas. Adjuntarla al PR para completar la revisión.

### Consultas SQL de apoyo (solo lectura)

Categorías activas sin exactamente una tarifa vigente hoy; el resultado esperado
es cero filas:

```sql
SELECT c.id, c.nombre, COUNT(t.id) AS tarifas_vigentes
FROM categorias c
LEFT JOIN tarifas t ON t.categoria_id = c.id AND t.activo = TRUE
  AND t.fecha_inicio <= CURRENT_DATE
  AND (t.fecha_fin IS NULL OR t.fecha_fin >= CURRENT_DATE)
WHERE c.activo = TRUE
GROUP BY c.id, c.nombre
HAVING COUNT(t.id) <> 1;
```

Importes y vigencias de la cobertura actual:

```sql
SELECT c.nombre, t.id, t.precio_dia, t.cargo_atraso_dia,
       t.fecha_inicio, t.fecha_fin
FROM tarifas t
JOIN categorias c ON c.id = t.categoria_id
WHERE c.activo = TRUE AND t.activo = TRUE
  AND t.fecha_inicio <= CURRENT_DATE
  AND (t.fecha_fin IS NULL OR t.fecha_fin >= CURRENT_DATE)
ORDER BY c.nombre;
```

Estas consultas usan la fecha de MySQL; verificar que coincida con la fecha del
servidor Java durante la demo, especialmente cerca de medianoche.
