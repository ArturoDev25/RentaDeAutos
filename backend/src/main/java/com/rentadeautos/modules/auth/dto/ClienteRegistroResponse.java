package com.rentadeautos.modules.auth.dto;

public record ClienteRegistroResponse(
        Long id, String nombre, String apellidos, String correo, boolean activo
) {}
