package com.rentadeautos.modules.devolucion.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.devolucion.dto.DevolucionRequest;
import com.rentadeautos.modules.devolucion.dto.DevolucionResponse;
import com.rentadeautos.modules.devolucion.exception.DevolucionException;
import com.rentadeautos.modules.devolucion.model.Devolucion;
import com.rentadeautos.modules.devolucion.repository.DevolucionRepository;
import com.rentadeautos.modules.rental.model.Entrega;
import com.rentadeautos.modules.rental.repository.EntregaRepository;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * S3-19: casos CP-07 y CP-08 del flujo de devolucion.
 * Escenario controlado: la renta dura 2 dias pactados y se devuelve a tiempo
 * (sin atraso), por lo que el total esperado es exactamente 2 x 500.00 = 1000.00.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevolucionConsistenciaTest {

    @Mock DevolucionRepository devoluciones;
    @Mock EntregaRepository entregas;
    @Mock ReservacionRepository reservaciones;
    @Mock VehiculoRepository vehiculos;
    @Mock UsuarioAppRepository usuarios;
    @Mock AuditService auditoria;
    @InjectMocks DevolucionService servicio;

    private Reservacion reservacion;
    private Vehiculo vehiculo;

    @BeforeEach
    void escenarioEnCurso() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agente@ejemplo.test", null));
        UsuarioApp usuario = new UsuarioApp();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(usuarios.findByCorreo("agente@ejemplo.test")).thenReturn(Optional.of(usuario));

        LocalDateTime ahora = LocalDateTime.now();

        Entrega entrega = new Entrega();
        ReflectionTestUtils.setField(entrega, "id", 50L);
        entrega.setReservacionId(10L);
        entrega.setFechaEntrega(ahora.minusDays(1));
        entrega.setKilometrajeSalida(new BigDecimal("15000.0"));

        reservacion = new Reservacion();
        ReflectionTestUtils.setField(reservacion, "id", 10L);
        reservacion.setVehiculoId(3L);
        reservacion.setEstado("EN_CURSO");
        reservacion.setFechaInicio(ahora.minusDays(1));
        reservacion.setFechaFin(ahora.plusDays(1));
        reservacion.setTarifaDia(new BigDecimal("500.00"));

        vehiculo = new Vehiculo();
        ReflectionTestUtils.setField(vehiculo, "id", 3L);
        vehiculo.setEstado(EstadoVehiculo.RENTADO);
        vehiculo.setKilometraje(new BigDecimal("15000.0"));
        vehiculo.setPlaca("ABC-123");

        when(entregas.findById(50L)).thenReturn(Optional.of(entrega));
        when(devoluciones.existsByEntregaId(50L)).thenReturn(false);
        when(reservaciones.findByIdParaActualizar(10L)).thenReturn(Optional.of(reservacion));
        when(vehiculos.findByIdParaActualizar(3L)).thenReturn(Optional.of(vehiculo));
        when(devoluciones.saveAndFlush(any())).thenAnswer(inv -> {
            Devolucion d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 200L);
            return d;
        });
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private DevolucionRequest request(String kmEntrada) {
        return new DevolucionRequest(50L, new BigDecimal(kmEntrada), new BigDecimal("80.00"),
                "Buen estado general", BigDecimal.ZERO, null, false);
    }

    @Test
    @DisplayName("CP-07: devolucion valida cierra la renta y libera el vehiculo")
    void cp07DevolucionValidaCierraRentaYLiberaVehiculo() {
        DevolucionResponse respuesta = servicio.registrar(request("15500.0"), "10.0.0.5");

        // Respuesta de la API
        assertEquals(10L, respuesta.reservacionId());
        assertEquals("FINALIZADA", respuesta.estadoReservacion());
        assertEquals("DISPONIBLE", respuesta.estadoVehiculo());
        assertEquals(0, new BigDecimal("1000.00").compareTo(respuesta.totalFinal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(respuesta.cargoAtraso()));

        // Estados finales de los datos
        assertEquals("FINALIZADA", reservacion.getEstado());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
        assertEquals(0, new BigDecimal("15500.0").compareTo(vehiculo.getKilometraje()));

        // Persistencia y auditoria
        verify(devoluciones).saveAndFlush(any());
        verify(reservaciones).saveAndFlush(reservacion);
        verify(vehiculos).saveAndFlush(vehiculo);
        verify(auditoria).registrarEvento(eq(7L), eq("DEVOLVER_VEHICULO"), eq("Devolucion"),
                eq(200L), eq("EXITOSO"), contains("EN_CURSO"), contains("FINALIZADA"),
                eq("10.0.0.5"));
    }

    @Test
    @DisplayName("CP-08: kilometraje inconsistente impide cerrar la renta y los datos no cambian")
    void cp08KilometrajeInconsistenteNoCierraLaRenta() {
        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(request("14999.0"), "10.0.0.5"));

        assertEquals(400, error.getStatus().value());
        assertTrue(error.getMessage().contains("RN-08"));

        // Los datos permanecen consistentes
        assertEquals("EN_CURSO", reservacion.getEstado());
        assertEquals(EstadoVehiculo.RENTADO, vehiculo.getEstado());
        assertEquals(0, new BigDecimal("15000.0").compareTo(vehiculo.getKilometraje()));

        // Nada se persiste ni se audita
        verify(devoluciones, never()).saveAndFlush(any());
        verify(reservaciones, never()).saveAndFlush(any());
        verify(vehiculos, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    @DisplayName("CP-08 (limite): kilometraje de entrada igual al de salida si se permite")
    void cp08KilometrajeIgualAlDeSalidaEsValido() {
        DevolucionResponse respuesta = servicio.registrar(request("15000.0"), "10.0.0.5");

        assertEquals("FINALIZADA", respuesta.estadoReservacion());
        assertEquals("DISPONIBLE", respuesta.estadoVehiculo());
    }
}
