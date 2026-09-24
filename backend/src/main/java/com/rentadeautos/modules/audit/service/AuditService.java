package com.rentadeautos.modules.audit.service;

import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    Page<AuditResponseDTO> listar(Pageable pageable, String q, Long usuarioId, String modulo, String accion);
    AuditDetailDTO obtenerDetalle(Long id);
    AuditMetricsDTO obtenerMetricas();
    AuditFilterOptionsDTO obtenerOpcionesFiltro();
    
    // Método interno para registrar eventos (sin exponer en REST)
    void registrarEvento(Long usuarioId, String accion, String entidad, Long entidadId, String resultado, String valoresAnteriores, String valoresNuevos, String direccionIp);
}
