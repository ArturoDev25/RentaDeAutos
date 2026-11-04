package com.rentadeautos.modules.auth.exception;

import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;

/**
 * S5-09: el cliente escribió bien su contraseña, pero su cuenta del portal sigue
 * pendiente de aprobación o fue rechazada. Solo se lanza cuando la contraseña
 * coincide, así que no revela a terceros si un correo está registrado.
 */
public class CuentaClienteNoAprobadaException extends RuntimeException {

    public static final String MENSAJE_PENDIENTE =
            "Tu cuenta está pendiente de aprobación. Podrás iniciar sesión cuando el personal la apruebe.";
    public static final String MENSAJE_RECHAZADA =
            "Tu solicitud de cuenta fue rechazada. Comunícate con la sucursal para más información.";

    private final EstadoAprobacionCliente estado;

    public CuentaClienteNoAprobadaException(EstadoAprobacionCliente estado) {
        super(estado == EstadoAprobacionCliente.RECHAZADO ? MENSAJE_RECHAZADA : MENSAJE_PENDIENTE);
        this.estado = estado;
    }

    public EstadoAprobacionCliente getEstado() {
        return estado;
    }
}
