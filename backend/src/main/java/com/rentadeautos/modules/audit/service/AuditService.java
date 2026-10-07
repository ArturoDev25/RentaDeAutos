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
    /**
     * Registra un evento en la bitácora (RN-09).
     *
     * @param usuarioId         actor; si es {@code null} se toma del usuario autenticado en
     *                          {@code SecurityContextHolder} y, si no hay sesión, queda como "Sistema".
     * @param resultado         {@code EXITOSO} (se guarda en la transacción de la operación) o
     *                          {@code FALLIDO} (se guarda en transacción propia para sobrevivir al rollback).
     * @param valoresAnteriores snapshot previo: cualquier objeto serializable (Map, record, DTO) o
     *                          una cadena JSON ya formada. {@code null} se guarda como NULL.
     * @param valoresNuevos     snapshot posterior, mismas reglas que {@code valoresAnteriores}.
     * @param direccionIp       IP de origen; si es {@code null} se toma de la petición HTTP en curso.
     */
    void registrarEvento(Long usuarioId, String accion, String entidad, Long entidadId, String resultado,
                         Object valoresAnteriores, Object valoresNuevos, String direccionIp);
}
