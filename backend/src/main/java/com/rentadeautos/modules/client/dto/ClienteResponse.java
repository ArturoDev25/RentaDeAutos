package com.rentadeautos.modules.client.dto;

import com.rentadeautos.modules.client.model.Cliente;

import java.time.LocalDate;

public record ClienteResponse(
        Long id,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        String numeroLicencia,
        LocalDate licenciaVencimiento,
        String direccion,
        Boolean activo
) {

    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getApellidos(),
                cliente.getCorreo(), cliente.getTelefono(), cliente.getNumeroLicencia(),
                cliente.getLicenciaVencimiento(), cliente.getDireccion(), cliente.getActivo());
    }
}