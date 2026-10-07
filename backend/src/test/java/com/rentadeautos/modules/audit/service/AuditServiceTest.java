package com.rentadeautos.modules.audit.service;

import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.audit.repository.AuditRepository;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AuditServiceImpl auditService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

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
    @DisplayName("registrarEvento guarda entidad en repositorio con actor explícito")
    void registrarEvento_guardaEnBD() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new UsuarioApp()));

        auditService.registrarEvento(1L, "Crear", "Clientes", 10L, "EXITOSO", "{}", "{}", "IP");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditRepository).save(captor.capture());
        assertEquals("Crear", captor.getValue().getAccion());
        assertNotNull(captor.getValue().getUsuario());
    }

    @Test
    @DisplayName("registrarEvento serializa objetos/Maps a JSON limpio preservando tipos y nulos")
    void registrarEvento_serializaSnapshotsObjetosAJson() {
        Map<String, Object> anterior = new LinkedHashMap<>();
        anterior.put("reservacionId", 15L);
        anterior.put("estado", "PENDIENTE");
        anterior.put("tarifaDia", new BigDecimal("450.50"));
        anterior.put("observaciones", null);

        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("reservacionId", 15L);
        nuevo.put("estado", "CONFIRMADA");
        nuevo.put("estadoVehiculo", "RESERVADO");
        nuevo.put("fechaConfirmacion", LocalDateTime.of(2026, 10, 6, 14, 30, 0));

        auditService.registrarEvento(null, "CONFIRMAR_RESERVACION", "RESERVACION", 15L,
                "EXITOSO", anterior, nuevo, "192.168.1.50");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();

        assertEquals("CONFIRMAR_RESERVACION", guardada.getAccion());
        assertEquals("RESERVACION", guardada.getEntidad());
        assertEquals(15L, guardada.getEntidadId());
        assertEquals("EXITOSO", guardada.getResultado());
        assertEquals("192.168.1.50", guardada.getDireccionIp());
        assertNotNull(guardada.getFechaHora());

        // Snapshot anterior serializado
        String jsonAntes = guardada.getValoresAnteriores();
        assertNotNull(jsonAntes);
        assertTrue(jsonAntes.contains("\"reservacionId\":15"));
        assertTrue(jsonAntes.contains("\"estado\":\"PENDIENTE\""));
        assertTrue(jsonAntes.contains("\"tarifaDia\":450.5") || jsonAntes.contains("\"tarifaDia\":450.50"));
        assertTrue(jsonAntes.contains("\"observaciones\":null"));

        // Snapshot nuevo serializado
        String jsonDespues = guardada.getValoresNuevos();
        assertNotNull(jsonDespues);
        assertTrue(jsonDespues.contains("\"estado\":\"CONFIRMADA\""));
        assertTrue(jsonDespues.contains("\"estadoVehiculo\":\"RESERVADO\""));
        assertTrue(jsonDespues.contains("\"fechaConfirmacion\":\"2026-10-06T14:30:00\""));
    }

    @Test
    @DisplayName("registrarEvento extrae actor autenticado automáticamente de SecurityContext si usuarioId es null")
    void registrarEvento_resuelveActorDesdeSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agente@rentadeautos.test", null));

        UsuarioApp usuarioAgente = new UsuarioApp();
        when(usuarioRepository.findByCorreo("agente@rentadeautos.test")).thenReturn(Optional.of(usuarioAgente));

        auditService.registrarEvento(null, "ENTREGAR_VEHICULO", "ENTREGA", 99L, "EXITOSO", null, null, null);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditRepository).save(captor.capture());
        assertSame(usuarioAgente, captor.getValue().getUsuario());
    }

    @Test
    @DisplayName("registrarEvento enmascara secretos y credenciales en snapshots (cero passwords en logs)")
    void registrarEvento_enmascaraCredenciales() {
        Map<String, Object> payload = Map.of(
                "usuario", "admin",
                "password", "Secreto123!",
                "token", "eyJhbGciOi..."
        );

        auditService.registrarEvento(null, "LOGIN", "AUTENTICACION", null, "EXITOSO", null, payload, null);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditRepository).save(captor.capture());
        String json = captor.getValue().getValoresNuevos();

        assertTrue(json.contains("\"password\":\"[REDACTADO]\""));
        assertTrue(json.contains("\"token\":\"[REDACTADO]\""));
        assertFalse(json.contains("Secreto123!"));
    }
}
