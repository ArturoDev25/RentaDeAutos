package com.rentadeautos.modules.client.model;

/**
 * S5-09: estado de revisión de una cuenta creada desde el portal.
 *
 * <p>Un cliente dado de alta en mostrador (o registrado antes de S5-09) no tiene
 * estado de aprobación ({@code null}) y no participa en este flujo.</p>
 */
public enum EstadoAprobacionCliente {
    /** Registrado en el portal; espera revisión del personal. No puede iniciar sesión. */
    PENDIENTE,
    /** Aprobado por el personal; la cuenta queda activa. */
    APROBADO,
    /** Rechazado por el personal; la cuenta permanece inhabilitada. */
    RECHAZADO
}
