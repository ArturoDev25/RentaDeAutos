package com.rentadeautos.modules.audit.dto;

import java.time.LocalDateTime;

public record AuditDetailDTO(
    Long id,
    String idFormateado,
    LocalDateTime fechaHora,
    String usuarioNombre,
    String modulo,
    String accion,
    String descripcion,
    String direccionIp,
    String resultado,
    String valoresAnteriores,
    String valoresNuevos
) {}
