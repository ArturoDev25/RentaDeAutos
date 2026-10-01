package com.rentadeautos.modules.rental.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.rental.dto.EntregaRequest;
import com.rentadeautos.modules.rental.exception.EntregaException;
import com.rentadeautos.modules.rental.model.Entrega;
import com.rentadeautos.modules.rental.repository.EntregaRepository;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntregaServiceTest {

    @Mock EntregaRepository entregas;
    @Mock ReservacionRepository reservaciones;
    @Mock VehiculoRepository vehiculos;
    @Mock ClienteRepository clientes;
    @Mock UsuarioAppRepository usuarios;
    @Mock AuditService auditoria;
    @InjectMocks EntregaService servicio;

    private Reservacion reservacion;
    private Vehiculo vehiculo;
    private Cliente cliente;

    @BeforeEach
    void escenarioValido() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agente@ejemplo.test", null));
        UsuarioApp usuario = new UsuarioApp();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(usuarios.findByCorreo("agente@ejemplo.test")).thenReturn(Optional.of(usuario));

        reservacion = new Reservacion();
        ReflectionTestUtils.setField(reservacion, "id", 10L);
        reservacion.setClienteId(2L);
        reservacion.setVehiculoId(3L);
        reservacion.setEstado("CONFIRMADA");
        reservacion.setFechaInicio(LocalDateTime.now().minusHours(1));
        reservacion.setFechaFin(LocalDateTime.now().plusDays(3));

        vehiculo = new Vehiculo();
        ReflectionTestUtils.setField(vehiculo, "id", 3L);
        vehiculo.setEstado(EstadoVehiculo.RESERVADO);
        vehiculo.setKilometraje(new BigDecimal("15000.0"));

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "activo", true);
        ReflectionTestUtils.setField(cliente, "licenciaVencimiento", LocalDate.now().plusYears(1));

        when(reservaciones.findVehiculoIdById(10L)).thenReturn(Optional.of(3L));
        when(vehiculos.findByIdParaActualizar(3L)).thenReturn(Optional.of(vehiculo));
        when(reservaciones.findByIdParaActualizar(10L)).thenReturn(Optional.of(reservacion));
        when(clientes.findById(2L)).thenReturn(Optional.of(cliente));
        when(entregas.existsByReservacionId(10L)).thenReturn(false);
        when(reservaciones.existsByVehiculoIdAndEstadoAndIdNot(3L, "EN_CURSO", 10L)).thenReturn(false);
        when(entregas.saveAndFlush(any())).thenAnswer(inv -> {
            Entrega e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 99L);
            return e;
        });
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private EntregaRequest datos(String kilometraje) {
        return new EntregaRequest(10L, new BigDecimal(kilometraje), new BigDecimal("75.50"),
                "  Sin golpes; rayón leve en defensa trasera  ", " ");
    }

    private void assertSinCambios() {
        verify(entregas, never()).saveAndFlush(any());
        verify(reservaciones, never()).saveAndFlush(any());
        verify(vehiculos, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void entregaValidaRegistraDatosYCambiaEstados() {
        var respuesta = servicio.registrar(datos("15020.5"), "10.0.0.5");

        ArgumentCaptor<Entrega> captor = ArgumentCaptor.forClass(Entrega.class);
        verify(entregas).saveAndFlush(captor.capture());
        Entrega guardada = captor.getValue();
        assertEquals(10L, guardada.getReservacionId());
        assertEquals(7L, guardada.getEntregadoPorId());
        assertEquals(new BigDecimal("15020.5"), guardada.getKilometrajeSalida());
        assertEquals(new BigDecimal("75.50"), guardada.getCombustibleSalida());
        assertEquals("Sin golpes; rayón leve en defensa trasera", guardada.getCondicionSalida());
        assertNull(guardada.getObservaciones());
        assertNotNull(guardada.getFechaEntrega());

        assertEquals("EN_CURSO", reservacion.getEstado());
        assertEquals(EstadoVehiculo.RENTADO, vehiculo.getEstado());
        assertEquals(new BigDecimal("15020.5"), vehiculo.getKilometraje());
        verify(reservaciones).saveAndFlush(reservacion);
        verify(vehiculos).saveAndFlush(vehiculo);

        assertEquals(99L, respuesta.id());
        assertEquals("EN_CURSO", respuesta.estadoReservacion());
        assertEquals("RENTADO", respuesta.estadoVehiculo());
    }

    @Test
    void entregaGeneraAuditoriaConEstadosAnterioresYNuevos() {
        servicio.registrar(datos("15020.5"), "10.0.0.5");

        ArgumentCaptor<String> antes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> despues = ArgumentCaptor.forClass(String.class);
        verify(auditoria).registrarEvento(eq(7L), eq("ENTREGAR_VEHICULO"), eq("Entrega"), eq(99L),
                eq("EXITOSO"), antes.capture(), despues.capture(), eq("10.0.0.5"));
        assertTrue(antes.getValue().contains("\"estadoReservacion\":\"CONFIRMADA\""));
        assertTrue(antes.getValue().contains("\"estadoVehiculo\":\"RESERVADO\""));
        assertTrue(despues.getValue().contains("\"estadoReservacion\":\"EN_CURSO\""));
        assertTrue(despues.getValue().contains("\"estadoVehiculo\":\"RENTADO\""));
        assertTrue(despues.getValue().contains("\"combustibleSalida\":75.50"));
    }

    @Test
    void reservacionInexistenteDevuelve404() {
        when(reservaciones.findVehiculoIdById(10L)).thenReturn(Optional.empty());
        EntregaException error = assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null));
        assertEquals(404, error.getStatus().value());
        assertSinCambios();
    }

    @Test
    void reservacionPendienteNoPuedeIniciarRenta() {
        reservacion.setEstado("PENDIENTE");
        EntregaException error = assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null));
        assertEquals(409, error.getStatus().value());
        assertSinCambios();
    }

    @Test
    void reservacionCanceladaOEnCursoNoPuedeIniciarRenta() {
        for (String estado : new String[] {"CANCELADA", "EN_CURSO", "FINALIZADA"}) {
            reservacion.setEstado(estado);
            assertEquals(409, assertThrows(EntregaException.class,
                    () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        }
        assertSinCambios();
    }

    @Test
    void reservacionConEntregaPreviaSeRechaza() {
        when(entregas.existsByReservacionId(10L)).thenReturn(true);
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void reservacionVencidaSeRechaza() {
        reservacion.setFechaInicio(LocalDateTime.now().minusDays(5));
        reservacion.setFechaFin(LocalDateTime.now().minusMinutes(1));
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void noSeEntregaAntesDelDiaDeInicio() {
        reservacion.setFechaInicio(LocalDateTime.now().plusDays(2));
        reservacion.setFechaFin(LocalDateTime.now().plusDays(4));
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void clienteConLicenciaVencidaNoRecibeVehiculo() {
        ReflectionTestUtils.setField(cliente, "licenciaVencimiento", LocalDate.now().minusDays(1));
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void vehiculoEnMantenimientoNoSeEntrega() {
        vehiculo.setEstado(EstadoVehiculo.MANTENIMIENTO);
        EntregaException error = assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null));
        assertEquals(409, error.getStatus().value());
        assertTrue(error.getMessage().contains("MANTENIMIENTO"));
        assertSinCambios();
    }

    @Test
    void vehiculoYaRentadoNoSeEntrega() {
        vehiculo.setEstado(EstadoVehiculo.RENTADO);
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void vehiculoConOtraRentaEnCursoNoSeEntrega() {
        when(reservaciones.existsByVehiculoIdAndEstadoAndIdNot(3L, "EN_CURSO", 10L)).thenReturn(true);
        assertEquals(409, assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("15020.5"), null)).getStatus().value());
        assertSinCambios();
    }

    @Test
    void kilometrajeMenorAlDelVehiculoSeRechaza() {
        EntregaException error = assertThrows(EntregaException.class,
                () -> servicio.registrar(datos("14999.9"), null));
        assertEquals(400, error.getStatus().value());
        assertSinCambios();
    }

    @Test
    void kilometrajeIgualAlDelVehiculoEsValido() {
        assertEquals("EN_CURSO", servicio.registrar(datos("15000.0"), null).estadoReservacion());
    }

    @Test
    void consultarEntregaInexistenteDevuelve404() {
        when(entregas.findByReservacionId(10L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(EntregaException.class,
                () -> servicio.obtenerPorReservacion(10L)).getStatus().value());
    }
}
