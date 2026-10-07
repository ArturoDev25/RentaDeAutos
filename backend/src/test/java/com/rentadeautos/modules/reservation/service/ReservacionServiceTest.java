package com.rentadeautos.modules.reservation.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.reservation.dto.ReservacionRequest;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservacionServiceTest {
    @Mock ReservacionRepository reservaciones;
    @Mock UsuarioAppRepository usuarios;
    @Mock JdbcTemplate jdbc;
    @Mock AuditService auditoria;
    @InjectMocks ReservacionService servicio;

    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 10, 0);

    @AfterEach void limpiarSesion() { SecurityContextHolder.clearContext(); }

    private ReservacionRequest datos(LocalDateTime fin) {
        return new ReservacionRequest(2L, 3L, inicio, fin, "Prueba");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void referenciasValidas() {
        doReturn(List.of(true)).when(jdbc).query(startsWith("SELECT activo"), any(RowMapper.class), eq(2L));
        doReturn(List.of(4L)).when(jdbc).query(startsWith("SELECT categoria_id"), any(RowMapper.class), eq(3L));
    }

    @Test void rn02RechazaDevolucionAnteriorSinGuardar() {
        ReservacionException error = assertThrows(ReservacionException.class,
                () -> servicio.crear(datos(inicio.minusHours(1))));
        assertEquals(400, error.getStatus().value());
        verifyNoInteractions(jdbc, reservaciones);
    }

    @Test void traslapeRechazaCreacion() {
        referenciasValidas();
        doReturn(List.of(9L)).when(jdbc).query(startsWith("SELECT id FROM reservaciones"),
                any(RowMapper.class), eq(3L), eq(-1L), any(), any());
        ReservacionException error = assertThrows(ReservacionException.class,
                () -> servicio.crear(datos(inicio.plusDays(1))));
        assertEquals(409, error.getStatus().value());
        verify(reservaciones, never()).saveAndFlush(any());
    }

    @Test void fraccionDeDiaCobraDosDiasYRegistraActor() {
        referenciasValidas();
        doReturn(List.of()).when(jdbc).query(startsWith("SELECT id FROM reservaciones"),
                any(RowMapper.class), eq(3L), eq(-1L), any(), any());
        doReturn(List.of(new BigDecimal("120.00"))).when(jdbc).query(startsWith("SELECT precio_dia"),
                any(RowMapper.class), eq(4L), eq(inicio.toLocalDate()), eq(inicio.toLocalDate()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@ejemplo.test", null));
        UsuarioApp usuario = new UsuarioApp();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(usuarios.findByCorreo("admin@ejemplo.test")).thenReturn(Optional.of(usuario));
        when(reservaciones.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = servicio.crear(datos(inicio.plusHours(25)));

        assertEquals(new BigDecimal("240.00"), respuesta.totalEstimado());
        assertEquals(7L, respuesta.creadoPorId());
        assertEquals("PENDIENTE", respuesta.estado());
    }

    @Test void sinTarifaVigenteImpideCrear() {
        referenciasValidas();
        ReservacionException error = assertThrows(ReservacionException.class,
                () -> servicio.crear(datos(inicio.plusDays(1))));
        assertEquals(409, error.getStatus().value());
        verify(reservaciones, never()).saveAndFlush(any());
    }

    @Test void editarMismoVehiculoConservaTarifaHistorica() {
        referenciasValidas();
        Reservacion r = new Reservacion();
        r.setVehiculoId(3L);
        r.setEstado("PENDIENTE");
        r.setTarifaDia(new BigDecimal("850.00"));
        when(reservaciones.findById(8L)).thenReturn(Optional.of(r));
        when(reservaciones.saveAndFlush(r)).thenReturn(r);

        var respuesta = servicio.editar(8L, datos(inicio.plusDays(3)));

        assertEquals(new BigDecimal("850.00"), respuesta.tarifaDia());
        assertEquals(new BigDecimal("2550.00"), respuesta.totalEstimado());
    }

    @Test void cancelacionLiberaReservacionYNoSeRepite() {
        Reservacion r = new Reservacion();
        when(reservaciones.findById(4L)).thenReturn(Optional.of(r));
        when(reservaciones.saveAndFlush(r)).thenReturn(r);
        assertEquals("CANCELADA", servicio.cancelar(4L).estado());
        assertEquals(409, assertThrows(ReservacionException.class,
                () -> servicio.cancelar(4L)).getStatus().value());
    }

    private Reservacion pendiente() {
        Reservacion r = new Reservacion();
        r.setClienteId(2L);
        r.setVehiculoId(3L);
        r.setFechaInicio(inicio);
        r.setFechaFin(inicio.plusDays(2));
        r.setEstado("PENDIENTE");
        return r;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void vehiculoApto() {
        doReturn(List.of(4L)).when(jdbc).query(startsWith("SELECT categoria_id"), any(RowMapper.class), eq(3L));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void traslapes(List<Long> ids) {
        doReturn(ids).when(jdbc).query(startsWith("SELECT id FROM reservaciones"),
                any(RowMapper.class), eq(3L), eq(8L), any(), any());
    }


    @Test
    @SuppressWarnings("unchecked")
    void confirmarValidaCambiaEstadosYAudita() {
        Reservacion r = pendiente();
        ReflectionTestUtils.setField(r, "id", 8L);
        r.setTarifaDia(new BigDecimal("850.00"));
        r.setTotalEstimado(new BigDecimal("1700.00"));
        when(reservaciones.findById(8L)).thenReturn(Optional.of(r));
        vehiculoApto();
        traslapes(List.of());
        doReturn("DISPONIBLE").when(jdbc).queryForObject(startsWith("SELECT estado"), eq(String.class), eq(3L));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@ejemplo.test", null));
        UsuarioApp usuario = new UsuarioApp();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(usuarios.findByCorreo("admin@ejemplo.test")).thenReturn(Optional.of(usuario));
        when(reservaciones.saveAndFlush(r)).thenReturn(r);

        var respuesta = servicio.confirmar(8L);

        assertEquals("CONFIRMADA", respuesta.estado());
        verify(jdbc).update(startsWith("UPDATE vehiculos SET estado = 'RESERVADO'"), eq(3L));

        ArgumentCaptor<Object> antes = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> despues = ArgumentCaptor.forClass(Object.class);
        verify(auditoria).registrarEvento(eq(7L), eq("CONFIRMAR_RESERVACION"), eq("RESERVACION"),
                eq(8L), eq("EXITOSO"), antes.capture(), despues.capture(), isNull());

        Map<String, Object> a = (Map<String, Object>) antes.getValue();
        assertEquals(8L, a.get("reservacionId"));
        assertEquals("PENDIENTE", a.get("estado"));
        assertEquals(3L, a.get("vehiculoId"));
        assertEquals("DISPONIBLE", a.get("estadoVehiculo"));
        assertEquals(inicio, a.get("fechaInicio"));
        assertEquals(inicio.plusDays(2), a.get("fechaFin"));
        assertEquals(new BigDecimal("850.00"), a.get("tarifaDia"));

        Map<String, Object> d = (Map<String, Object>) despues.getValue();
        assertEquals("CONFIRMADA", d.get("estado"));
        assertEquals("RESERVADO", d.get("estadoVehiculo"));
        assertEquals(3L, d.get("vehiculoId"));
        // RN-07: la tarifa histórica no cambia al confirmar.
        assertEquals(new BigDecimal("850.00"), d.get("tarifaDia"));
        assertEquals(new BigDecimal("1700.00"), d.get("totalEstimado"));
    }

    @Test void confirmarRechazaSiNoEstaPendienteSinTocarNada() {
        Reservacion r = pendiente();
        r.setEstado("CONFIRMADA");
        when(reservaciones.findById(8L)).thenReturn(Optional.of(r));

        assertEquals(409, assertThrows(ReservacionException.class,
                () -> servicio.confirmar(8L)).getStatus().value());
        verifyNoInteractions(jdbc, auditoria);
        verify(reservaciones, never()).saveAndFlush(any());
    }

    @Test void confirmarRechazaTraslapeYNoCambiaEstado() {
        Reservacion r = pendiente();
        when(reservaciones.findById(8L)).thenReturn(Optional.of(r));
        vehiculoApto();
        traslapes(List.of(9L));

        assertEquals(409, assertThrows(ReservacionException.class,
                () -> servicio.confirmar(8L)).getStatus().value());

        assertEquals("PENDIENTE", r.getEstado());
        verify(reservaciones, never()).saveAndFlush(any());
        verify(jdbc, never()).update(
            startsWith("UPDATE vehiculos SET estado = 'RESERVADO'"),
            anyLong());
        verifyNoInteractions(auditoria);
    }
    
    @Test void confirmarReservacionInexistenteDevuelve404() {
        when(reservaciones.findById(99L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ReservacionException.class,
                () -> servicio.confirmar(99L)).getStatus().value());
        verifyNoInteractions(jdbc, auditoria);
    }
}
