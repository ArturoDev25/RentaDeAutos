package com.rentadeautos.modules.audit.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3-17 (#85) — Catálogo único de valores de la bitácora para el ciclo operativo
 * (confirmar → entregar → devolver).
 *
 * <p>Centraliza acciones, entidades y resultados para que los servicios no repitan
 * cadenas sueltas y para que la pantalla de auditoría pueda filtrarlos de forma
 * consistente.</p>
 */
public final class AuditoriaOperativa {

    // ── Resultados permitidos por chk_auditoria_resultado (V1__init_schema.sql) ──
    public static final String EXITOSO = "EXITOSO";
    public static final String FALLIDO = "FALLIDO";

    // ── Acciones ──
    public static final String CREAR_RESERVACION     = "CREAR_RESERVACION";
    public static final String EDITAR_RESERVACION    = "EDITAR_RESERVACION";
    public static final String CANCELAR_RESERVACION  = "CANCELAR_RESERVACION";
    public static final String CONFIRMAR_RESERVACION = "CONFIRMAR_RESERVACION";
    public static final String ENTREGAR_VEHICULO     = "ENTREGAR_VEHICULO";
    public static final String DEVOLVER_VEHICULO     = "DEVOLVER_VEHICULO";
    // S5-09: revisión de cuentas registradas desde el portal.
    public static final String APROBAR_CLIENTE       = "APROBAR_CLIENTE";
    public static final String RECHAZAR_CLIENTE      = "RECHAZAR_CLIENTE";

    // ── Entidades ──
    public static final String RESERVACION = "RESERVACION";
    public static final String ENTREGA     = "ENTREGA";
    public static final String DEVOLUCION  = "DEVOLUCION";
    public static final String CLIENTE     = "CLIENTE";

    private AuditoriaOperativa() {
    }

    /**
     * Construye el snapshot de un intento rechazado por una regla de negocio.
     * Solo incluye el código HTTP, el mensaje funcional (nunca credenciales) y
     * la referencia que identifica el intento.
     */
    public static Map<String, Object> fallo(String referencia, Object referenciaId,
            int codigoHttp, String motivo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(referencia, referenciaId);
        m.put("codigoHttp", codigoHttp);
        m.put("motivo", motivo);
        return m;
    }
}
