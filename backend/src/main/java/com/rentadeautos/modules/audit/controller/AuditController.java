package com.rentadeautos.modules.audit.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import com.rentadeautos.modules.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR','SUPERVISOR')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<Page<AuditResponseDTO>> listar(
            Pageable pageable,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion) {
        return ApiResponse.success(auditService.listar(pageable, q, usuarioId, modulo, accion));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditDetailDTO> obtenerDetalle(@PathVariable Long id) {
        return ApiResponse.success(auditService.obtenerDetalle(id));
    }

    @GetMapping("/metrics")
    public ApiResponse<AuditMetricsDTO> obtenerMetricas() {
        return ApiResponse.success(auditService.obtenerMetricas());
    }

    @GetMapping("/filters")
    public ApiResponse<AuditFilterOptionsDTO> obtenerFiltros() {
        return ApiResponse.success(auditService.obtenerOpcionesFiltro());
    }
}
