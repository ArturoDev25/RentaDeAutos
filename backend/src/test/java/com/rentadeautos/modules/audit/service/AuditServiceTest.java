package com.rentadeautos.modules.audit.service;

import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.audit.repository.AuditRepository;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private UsuarioAppRepository usuarioRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    @DisplayName("obtenerMetricas llama a todos los conteos y retorna DTO")
    void obtenerMetricas_devuelveCuatroKPIs() {
        when(auditRepository.count()).thenReturn(100L);
        when(auditRepository.countDistinctUsuariosActivos()).thenReturn(5L);
        when(auditRepository.countByFechaHoraBetween(any(), any())).thenReturn(15L);
        when(auditRepository.countByResultadoIgnoreCase("FALLIDO")).thenReturn(2L);

        AuditMetricsDTO result = auditService.obtenerMetricas();

        assertEquals(100L, result.totalRegistros());
        assertEquals(5L, result.usuariosActivos());
        assertEquals(15L, result.accionesHoy());
        assertEquals(2L, result.intentosFallidos());
    }

    @Test
    @DisplayName("listar sin filtros retorna página mapeada correctamente")
    void listar_sinFiltros_devuelvePaginado() {
        Auditoria mockAudit = new Auditoria(null, "Editar", "Vehículos", 1L, "EXITOSO", null, null, "127.0.0.1", LocalDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(mockAudit, "id", 50L);
        
        Page<Auditoria> pageMock = new PageImpl<>(List.of(mockAudit));
        when(auditRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(pageMock);

        Page<AuditResponseDTO> result = auditService.listar(PageRequest.of(0, 10), null, null, null, null);

        assertEquals(1, result.getContent().size());
        AuditResponseDTO dto = result.getContent().get(0);
        assertEquals("AUD-0050", dto.idFormateado());
        assertEquals("Sistema", dto.usuarioNombre()); // Null user = "Sistema"
        assertEquals("Exitosa", dto.resultado()); // EXITOSO -> Exitosa
    }

    @Test
    @DisplayName("obtenerDetalle lanza 404 si ID no existe")
    void obtenerDetalle_inexistente_lanzaException() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> auditService.obtenerDetalle(99L));
    }

    @Test
    @DisplayName("registrarEvento guarda entidad en repositorio")
    void registrarEvento_guardaEnBD() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new UsuarioApp()));

        auditService.registrarEvento(1L, "Crear", "Clientes", 10L, "EXITOSO", "{}", "{}", "IP");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditRepository).save(captor.capture());
        assertEquals("Crear", captor.getValue().getAccion());
        assertNotNull(captor.getValue().getUsuario());
    }
}
