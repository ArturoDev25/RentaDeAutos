package com.rentadeautos.modules.audit.dto;

import java.time.LocalDateTime;

public record AuditResponseDTO(
    Long id,
    String idFormateado,
    LocalDateTime fechaHora,
    String usuarioNombre,
    String modulo,
    String accion,
    String descripcion,
    String direccionIp,
    String resultado
) {}
