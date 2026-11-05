package com.rentadeautos.modules.client.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.audit.service.AuditoriaOperativa;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import com.rentadeautos.modules.client.exception.ClienteEstadoAprobacionException;
import com.rentadeautos.modules.client.exception.ClienteNoEncontradoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** S5-09: reglas de aprobación y rechazo (repositorio y auditoría simulados). */
@ExtendWith(MockitoExtension.class)
class ClienteAprobacionServiceTest {

    @Mock ClienteRepository clientes;
    @Mock AuditService auditoria;

    private ClienteAprobacionService servicio;
    private Cliente pendiente;

    @BeforeEach
    void preparar() {
        servicio = new ClienteAprobacionService(clientes, auditoria);
        pendiente = new Cliente();
        ReflectionTestUtils.setField(pendiente, "id", 7L);
        pendiente.setNombre("Ana");
        pendiente.setApellidos("Pérez");
        pendiente.setCorreo("ana@example.test");
        pendiente.setTelefono("8441234567");
        pendiente.setNumeroLicencia("LIC-S509");
        pendiente.setLicenciaVencimiento(LocalDate.now().plusYears(1));
        pendiente.setPasswordHash("$2a$10$hash-que-nunca-debe-ir-a-la-bitacora-xxxxxxxxxxxxxxxx");
        pendiente.setActivo(false);
        pendiente.setEstadoAprobacion(EstadoAprobacionCliente.PENDIENTE);
    }

    @Test
    @DisplayName("Lista solo las cuentas PENDIENTE")
    void listarPendientesConsultaSoloPendientes() {
        when(clientes.findByEstadoAprobacionOrderByIdAsc(EstadoAprobacionCliente.PENDIENTE))
                .thenReturn(List.of(pendiente));

        List<ClienteResponse> lista = servicio.listarPendientes();

        assertEquals(1, lista.size());
        assertEquals(EstadoAprobacionCliente.PENDIENTE, lista.get(0).estadoAprobacion());
        assertFalse(lista.get(0).activo());
    }

    @Test
    @DisplayName("Aprobar: la cuenta queda APROBADO y activa, y se audita como EXITOSO")
    void aprobarActivaLaCuentaYAudita() {
        when(clientes.findByIdParaRevision(7L)).thenReturn(Optional.of(pendiente));
        when(clientes.saveAndFlush(pendiente)).thenReturn(pendiente);

        ClienteResponse respuesta = servicio.aprobar(7L);

        assertEquals(EstadoAprobacionCliente.APROBADO, respuesta.estadoAprobacion());
        assertTrue(respuesta.activo());
        assertNotNull(pendiente.getFechaRevision());

        ArgumentCaptor<Object> antes = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> despues = ArgumentCaptor.forClass(Object.class);
        verify(auditoria).registrarEvento(isNull(), eq(AuditoriaOperativa.APROBAR_CLIENTE),
                eq(AuditoriaOperativa.CLIENTE), eq(7L), eq(AuditoriaOperativa.EXITOSO),
                antes.capture(), despues.capture(), isNull());
        assertEquals(EstadoAprobacionCliente.PENDIENTE, ((Map<?, ?>) antes.getValue()).get("estadoAprobacion"));
        assertEquals(EstadoAprobacionCliente.APROBADO, ((Map<?, ?>) despues.getValue()).get("estadoAprobacion"));
        assertFalse(despues.getValue().toString().contains("$2a$10$"), "La bitácora no debe llevar credenciales");
    }

    @Test
    @DisplayName("Rechazar: la cuenta queda RECHAZADO e inactiva; el motivo va a la auditoría")
    void rechazarMantieneInactivaYAuditaMotivo() {
        when(clientes.findByIdParaRevision(7L)).thenReturn(Optional.of(pendiente));
        when(clientes.saveAndFlush(pendiente)).thenReturn(pendiente);

        ClienteResponse respuesta = servicio.rechazar(7L, "  Licencia ilegible  ");

        assertEquals(EstadoAprobacionCliente.RECHAZADO, respuesta.estadoAprobacion());
        assertFalse(respuesta.activo());

        ArgumentCaptor<Object> despues = ArgumentCaptor.forClass(Object.class);
        verify(auditoria).registrarEvento(isNull(), eq(AuditoriaOperativa.RECHAZAR_CLIENTE),
                eq(AuditoriaOperativa.CLIENTE), eq(7L), eq(AuditoriaOperativa.EXITOSO),
                any(), despues.capture(), isNull());
        assertEquals("Licencia ilegible", ((Map<?, ?>) despues.getValue()).get("motivo"));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAprobacionCliente.class, names = {"APROBADO", "RECHAZADO"})
    @DisplayName("Una cuenta ya revisada no se vuelve a revisar: 409 y auditoría FALLIDO")
    void cuentaYaRevisadaNoSeModifica(EstadoAprobacionCliente estado) {
        pendiente.setEstadoAprobacion(estado);
        boolean activoAntes = estado == EstadoAprobacionCliente.APROBADO;
        pendiente.setActivo(activoAntes);
        when(clientes.findByIdParaRevision(7L)).thenReturn(Optional.of(pendiente));

        assertThrows(ClienteEstadoAprobacionException.class, () -> servicio.aprobar(7L));
        assertThrows(ClienteEstadoAprobacionException.class, () -> servicio.rechazar(7L, null));

        assertEquals(estado, pendiente.getEstadoAprobacion());
        assertEquals(activoAntes, pendiente.getActivo());
        verify(clientes, never()).saveAndFlush(any());
        verify(auditoria, times(2)).registrarEvento(isNull(), any(), eq(AuditoriaOperativa.CLIENTE),
                eq(7L), eq(AuditoriaOperativa.FALLIDO), any(), any(), isNull());
    }

    @Test
    @DisplayName("Un cliente de mostrador (sin estado de aprobación) no se puede aprobar ni rechazar")
    void clienteDeMostradorNoEntraAlFlujo() {
        pendiente.setEstadoAprobacion(null);
        pendiente.setActivo(true);
        when(clientes.findByIdParaRevision(7L)).thenReturn(Optional.of(pendiente));

        assertThrows(ClienteEstadoAprobacionException.class, () -> servicio.rechazar(7L, null));
        assertTrue(pendiente.getActivo());
        verify(clientes, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Cliente inexistente → 404 sin auditar")
    void clienteInexistente() {
        when(clientes.findByIdParaRevision(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> servicio.aprobar(99L));
        verifyNoInteractions(auditoria);
    }
}
