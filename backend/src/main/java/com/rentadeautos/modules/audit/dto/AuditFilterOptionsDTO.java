package com.rentadeautos.modules.audit.dto;

import java.util.List;

public record AuditFilterOptionsDTO(
    List<UsuarioOpcionDTO> usuarios,
    List<String> modulos,
    List<String> acciones
) {
    public record UsuarioOpcionDTO(Long id, String nombre) {}
}
