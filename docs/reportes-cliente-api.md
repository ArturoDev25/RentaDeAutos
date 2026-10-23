# S4-10 — API de Reportes del Cliente

## Identidad y aislamiento de datos

El endpoint de reportes del cliente requiere `Authorization: Bearer <token>` perteneciente a un cliente activo autenticado mediante `POST /api/auth/cliente/login` (subject `cliente:<id>`, rol `ROLE_CLIENTE`).

- El identificador `clienteId` se extrae exclusivamente de `Principal.getName()` (`cliente:<id>`).
- Cualquier parámetro externo (`clienteId`, `cliente_id`) enviado en la URL de consulta es ignorado por completo.
- El personal interno (`ADMINISTRADOR`, `AGENTE`, `SUPERVISOR`, `AUDITOR`) recibe `403 Forbidden` al intentar acceder a `/api/v1/cliente/reportes/**`.
- Las solicitudes sin sesión o con token expirado reciben `401 Unauthorized`.
- Solo se permite el método `GET` sobre `/api/v1/cliente/reportes/resumen`; cualquier intento de escritura (`POST`, `PUT`, `PATCH`, `DELETE`) es rechazado con `403 Forbidden` en `SecurityConfig`.

## Contrato HTTP

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/cliente/reportes/resumen` | `periodo` (opcional: `7D`, `30D`, `3M`, `1A`; por defecto `3M`) | `200 OK` con `ApiResponse<ReporteClienteResumenResponse>` |

### Ejemplo de respuesta (`200 OK`)

```json
{
  "success": true,
  "data": {
    "periodo": "3M",
    "totalRentas": 2,
    "gastoTotalEstimado": 4450.00,
    "diasRentados": 5,
    "costoPromedioDia": 890.00,
    "variacionPeriodoAnterior": {
      "rentasPct": 100.0,
      "gastoPct": 100.0,
      "diasPct": 100.0,
      "costoPromedioDiaPct": 100.0
    },
    "seriePorPeriodo": [
      { "etiqueta": "May", "cantidad": 0 },
      { "etiqueta": "Jun", "cantidad": 0 },
      { "etiqueta": "Jul", "cantidad": 0 },
      { "etiqueta": "Ago", "cantidad": 0 },
      { "etiqueta": "Sep", "cantidad": 0 },
      { "etiqueta": "Oct", "cantidad": 2 }
    ],
    "distribucionEstado": [
      { "estado": "FINALIZADA", "cantidad": 1, "porcentaje": 50.0 },
      { "estado": "EN_CURSO", "cantidad": 1, "porcentaje": 50.0 },
      { "estado": "RESERVADA", "cantidad": 0, "porcentaje": 0.0 },
      { "estado": "CANCELADA", "cantidad": 0, "porcentaje": 0.0 }
    ],
    "historial": [
      {
        "reservacionId": 2,
        "folio": "REN-0002",
        "vehiculo": "Nissan Kicks 2024",
        "categoria": "SUV",
        "fechaInicio": "2026-10-18T10:00:00",
        "fechaFin": "2026-10-21T10:00:00",
        "dias": 3,
        "totalEstimado": 2550.00,
        "totalFinal": null,
        "estado": "EN_CURSO"
      }
    ],
    "avisoImportes": "Los montos son estimados; el cobro final se registra al devolver el vehículo."
  }
}
```

### Reglas de negocio de los indicadores

1. **Total de rentas, gasto y días rentados:** Contabilizan las operaciones propias del cliente en estado `EN_CURSO` o `FINALIZADA` dentro del periodo seleccionado.
2. **Importes estimados vs. finales:** Para las rentas `FINALIZADA` con devolución registrada se toma `totalFinal`; para las rentas `EN_CURSO` se toma `totalEstimado` e incluye siempre la leyenda `avisoImportes` aclarando que los montos son estimados hasta el cierre en sucursal.
3. **Variación porcentual:** Compara el periodo seleccionado (`[ahora - P, ahora]`) contra el periodo inmediato anterior (`[ahora - 2P, ahora - P)`).
4. **Periodo inválido:** Si `periodo` no pertenece a `{7D, 30D, 3M, 1A}`, el servicio responde `400 Bad Request`.
