package com.rentadeautos.modules.reservation.service;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.reservation.dto.ReservacionRequest;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservacionServiceTest {
    @Mock ReservacionRepository reservaciones;
    @Mock UsuarioAppRepository usuarios;
    @Mock JdbcTemplate jdbc;
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
}
