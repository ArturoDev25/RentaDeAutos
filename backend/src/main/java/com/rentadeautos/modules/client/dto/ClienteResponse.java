package com.rentadeautos.modules.client.dto;

import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        String numeroLicencia,
        LocalDate licenciaVencimiento,
        String direccion,
        Boolean activo,
        // S5-09: PENDIENTE, APROBADO, RECHAZADO o null (alta en mostrador).
        EstadoAprobacionCliente estadoAprobacion,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaRevision
) {

    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getApellidos(),
                cliente.getCorreo(), cliente.getTelefono(), cliente.getNumeroLicencia(),
                cliente.getLicenciaVencimiento(), cliente.getDireccion(), cliente.getActivo(),
                cliente.getEstadoAprobacion(), cliente.getFechaRegistro(), cliente.getFechaRevision());
    }
}
