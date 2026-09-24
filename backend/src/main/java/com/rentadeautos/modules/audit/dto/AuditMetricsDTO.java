package com.rentadeautos.modules.audit.dto;

public record AuditMetricsDTO(
    long totalRegistros,
    long usuariosActivos,
    long accionesHoy,
    long intentosFallidos
) {}
