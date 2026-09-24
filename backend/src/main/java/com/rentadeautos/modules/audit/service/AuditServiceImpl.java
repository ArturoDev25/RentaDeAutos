package com.rentadeautos.modules.audit.service;

import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.audit.repository.AuditRepository;
import com.rentadeautos.modules.audit.repository.AuditSpecification;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;
    private final UsuarioAppRepository usuarioRepository;

    public AuditServiceImpl(AuditRepository auditRepository, UsuarioAppRepository usuarioRepository) {
        this.auditRepository = auditRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Page<AuditResponseDTO> listar(Pageable pageable, String q, Long usuarioId, String modulo, String accion) {
        Specification<Auditoria> spec = AuditSpecification.build(q, usuarioId, modulo, accion);
        return auditRepository.findAll(spec, pageable).map(this::mapToResponseDTO);
    }

    @Override
    public AuditDetailDTO obtenerDetalle(Long id) {
        return auditRepository.findById(id)
                .map(this::mapToDetailDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de auditoría no encontrado"));
    }

    @Override
    public AuditMetricsDTO obtenerMetricas() {
        long totalRegistros = auditRepository.count();
        long usuariosActivos = auditRepository.countDistinctUsuariosActivos();
        
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);
        long accionesHoy = auditRepository.countByFechaHoraBetween(inicioDia, finDia);
        
        long intentosFallidos = auditRepository.countByResultadoIgnoreCase("FALLIDO");
        
        return new AuditMetricsDTO(totalRegistros, usuariosActivos, accionesHoy, intentosFallidos);
    }

    @Override
    public AuditFilterOptionsDTO obtenerOpcionesFiltro() {
        List<AuditFilterOptionsDTO.UsuarioOpcionDTO> usuarios = usuarioRepository.findAll().stream()
                .map(u -> new AuditFilterOptionsDTO.UsuarioOpcionDTO(u.getId(), u.getNombre()))
                .collect(Collectors.toList());
                
        List<String> modulos = auditRepository.findDistinctEntidades();
        List<String> acciones = auditRepository.findDistinctAcciones();
        
        return new AuditFilterOptionsDTO(usuarios, modulos, acciones);
    }
    
    @Override
    @Transactional
    public void registrarEvento(Long usuarioId, String accion, String entidad, Long entidadId, String resultado, String valoresAnteriores, String valoresNuevos, String direccionIp) {
        UsuarioApp usuario = null;
        if (usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }
        
        Auditoria auditoria = new Auditoria(
            usuario, accion, entidad, entidadId, resultado, 
            valoresAnteriores, valoresNuevos, direccionIp, LocalDateTime.now()
        );
        
        auditRepository.save(auditoria);
    }

    private AuditResponseDTO mapToResponseDTO(Auditoria a) {
        String usuarioNombre = a.getUsuario() != null ? a.getUsuario().getNombre() : "Sistema";
        String resultadoDisplay = "FALLIDO".equalsIgnoreCase(a.getResultado()) ? "Fallida" : "Exitosa";
        String idFormateado = "AUD-" + String.format("%04d", a.getId());
        String descripcion = String.format("%s en %s", a.getAccion(), a.getEntidad());
        
        return new AuditResponseDTO(
            a.getId(), idFormateado, a.getFechaHora(), usuarioNombre, 
            a.getEntidad(), a.getAccion(), descripcion, a.getDireccionIp(), resultadoDisplay
        );
    }

    private AuditDetailDTO mapToDetailDTO(Auditoria a) {
        String usuarioNombre = a.getUsuario() != null ? a.getUsuario().getNombre() : "Sistema";
        String resultadoDisplay = "FALLIDO".equalsIgnoreCase(a.getResultado()) ? "Fallida" : "Exitosa";
        String idFormateado = "AUD-" + String.format("%04d", a.getId());
        String descripcion = String.format("%s en %s", a.getAccion(), a.getEntidad());
        
        return new AuditDetailDTO(
            a.getId(), idFormateado, a.getFechaHora(), usuarioNombre, 
            a.getEntidad(), a.getAccion(), descripcion, a.getDireccionIp(), resultadoDisplay,
            a.getValoresAnteriores(), a.getValoresNuevos()
        );
    }
}
