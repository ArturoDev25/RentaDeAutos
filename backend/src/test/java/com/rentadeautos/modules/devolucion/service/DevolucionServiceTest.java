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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link DevolucionService} (S3-10).
 * Usa Mockito puro — sin Spring Context — para máxima velocidad.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevolucionServiceTest {

    @Mock DevolucionRepository devoluciones;
    @Mock EntregaRepository    entregas;
    @Mock ReservacionRepository reservaciones;
    @Mock VehiculoRepository   vehiculos;
    @Mock UsuarioAppRepository usuarios;
    @Mock AuditService         auditoria;

    @InjectMocks DevolucionService servicio;

    // ── Objetos del escenario base ────────────────────────────────────────────

    private Entrega      entrega;
    private Reservacion  reservacion;
    private Vehiculo     vehiculo;

    /** Fecha fin pactada: ayer → la renta ya está en tiempo normal (sin atraso). */
    private static final LocalDateTime FECHA_FIN_PACTADA =
            LocalDateTime.now().minusDays(1);

    /** Fecha de entrega: hace 2 días (la renta duró 2 días sin atraso). */
    private static final LocalDateTime FECHA_ENTREGA =
            LocalDateTime.now().minusDays(2);

    @BeforeEach
    void escenarioValido() {
        // Sesión simulada.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agente@ejemplo.test", null));
        UsuarioApp usuario = new UsuarioApp();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(usuarios.findByCorreo("agente@ejemplo.test")).thenReturn(Optional.of(usuario));

        // Entrega base.
        entrega = new Entrega();
        ReflectionTestUtils.setField(entrega, "id", 50L);
        entrega.setReservacionId(10L);
        entrega.setFechaEntrega(FECHA_ENTREGA);
        entrega.setKilometrajeSalida(new BigDecimal("15000.0"));

        // Reservación en curso.
        reservacion = new Reservacion();
        ReflectionTestUtils.setField(reservacion, "id", 10L);
        reservacion.setVehiculoId(3L);
        reservacion.setEstado("EN_CURSO");
        reservacion.setFechaInicio(LocalDateTime.now().minusDays(2));
        reservacion.setFechaFin(FECHA_FIN_PACTADA);
        reservacion.setTarifaDia(new BigDecimal("500.00"));

        // Vehículo rentado.
        vehiculo = new Vehiculo();
        ReflectionTestUtils.setField(vehiculo, "id", 3L);
        vehiculo.setEstado(EstadoVehiculo.RENTADO);
        vehiculo.setKilometraje(new BigDecimal("15000.0"));
        vehiculo.setPlaca("ABC-123");

        // Configurar mocks.
        when(entregas.findById(50L)).thenReturn(Optional.of(entrega));
        when(devoluciones.existsByEntregaId(50L)).thenReturn(false);
        // Orden de bloqueo: vehiculoId (sin bloqueo) → vehículo FOR UPDATE → reservación FOR UPDATE.
        when(reservaciones.findVehiculoIdById(10L)).thenReturn(Optional.of(3L));
        when(vehiculos.findByIdParaActualizar(3L)).thenReturn(Optional.of(vehiculo));
        when(reservaciones.findByIdParaActualizar(10L)).thenReturn(Optional.of(reservacion));

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DevolucionRequest request(String km, boolean requiereMantenimiento) {
        return new DevolucionRequest(
                50L,
                new BigDecimal(km),
                new BigDecimal("80.00"),
                "Buen estado general",
                BigDecimal.ZERO,
                null,
                requiereMantenimiento);
    }

    private void assertNadaGuardado() {
        verify(devoluciones, never()).saveAndFlush(any());
        verify(reservaciones, never()).saveAndFlush(any());
        verify(vehiculos, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    // ── Casos exitosos ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Devolución válida: cálculo correcto y vehículo pasa a DISPONIBLE")
    void devolucionValidaCalculaCorrectamenteYVehiculoDisponible() {
        DevolucionResponse respuesta = servicio.registrar(request("15500.0", false), "10.0.0.5");

        // Verificar respuesta.
        assertEquals(200L, respuesta.id());
        assertEquals(10L,  respuesta.reservacionId());
        assertEquals("ABC-123", respuesta.vehiculoPlaca());
        assertEquals("DISPONIBLE",  respuesta.estadoVehiculo());
        assertEquals("FINALIZADA",  respuesta.estadoReservacion());
        // totalFinal = subtotal + 0 + 0 = subtotal > 0
        assertTrue(respuesta.totalFinal().compareTo(BigDecimal.ZERO) > 0);
        // cargoAtraso = 0 porque la fecha fin ya pasó hace 1 día pero la entrega fue hace 2
        // (el cálculo depende de la lógica de días reales; al menos no debe ser negativo)
        assertTrue(respuesta.cargoAtraso().compareTo(BigDecimal.ZERO) >= 0);

        // Verificar transiciones de estado.
        assertEquals("FINALIZADA", reservacion.getEstado());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
        assertEquals(new BigDecimal("15500.0"), vehiculo.getKilometraje());

        // Verificar persistencia.
        verify(devoluciones).saveAndFlush(any());
        verify(reservaciones).saveAndFlush(reservacion);
        verify(vehiculos).saveAndFlush(vehiculo);
    }

    @Test
    @DisplayName("Cargo por daños sin flag de mantenimiento: vehículo queda DISPONIBLE")
    void cargoDanosSinFlagNoMandaAMantenimiento() {
        DevolucionRequest req = new DevolucionRequest(
                50L,
                new BigDecimal("15200.0"),
                new BigDecimal("60.00"),
                "Golpe en puerta delantera izquierda",
                new BigDecimal("1500.00"),
                null,
                false); // requiereMantenimiento=false: ni el texto ni el cargo deciden el estado

        DevolucionResponse respuesta = servicio.registrar(req, "10.0.0.5");

        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
        assertEquals(new BigDecimal("1500.00"), respuesta.cargoDanos());
        assertEquals("FINALIZADA", reservacion.getEstado());
    }

    @Test
    @DisplayName("requiereMantenimiento=true: vehículo pasa a MANTENIMIENTO aunque condición sea normal")
    void requiereMantenimientoTrueFuerzaMantenimiento() {
        servicio.registrar(request("15100.0", true), "10.0.0.1");
        assertEquals(EstadoVehiculo.MANTENIMIENTO, vehiculo.getEstado());
    }

    @Test
    @DisplayName("Recargo por atraso se aplica cuando la devolución es posterior a la fecha pactada")
    void recargoPorAtrasoSeCuandoDevolucionEsTardia() {
        // Fecha fin ya pasó hace 1 día (configurado en el escenario base).
        // El cargo de atraso debe ser > 0.
        DevolucionResponse respuesta = servicio.registrar(request("15200.0", false), "10.0.0.5");
        assertTrue(respuesta.cargoAtraso().compareTo(BigDecimal.ZERO) > 0,
                "Se esperaba cargo por atraso > 0 porque la devolución es posterior a la fecha pactada");
        // totalFinal >= cargoAtraso
        assertTrue(respuesta.totalFinal().compareTo(respuesta.cargoAtraso()) >= 0);
    }

    @Test
    @DisplayName("Cargo por daños se acumula al total final")
    void cargoDanosSeAcumulaAlTotal() {
        DevolucionRequest req = new DevolucionRequest(
                50L, new BigDecimal("15100.0"), new BigDecimal("70.00"),
                "Sin daños visibles", new BigDecimal("800.00"), null, false);

        DevolucionResponse respuesta = servicio.registrar(req, "10.0.0.5");

        assertEquals(new BigDecimal("800.00"), respuesta.cargoDanos());
        assertTrue(respuesta.totalFinal().compareTo(new BigDecimal("800.00")) >= 0);
    }

    @Test
    @DisplayName("AuditService.registrarEvento es invocado con la acción correcta")
    void auditServiceEsInvocadoConAccionCorrecta() {
        servicio.registrar(request("15100.0", false), "192.168.1.1");

        ArgumentCaptor<String> antesCaptor   = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> despuesCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditoria).registrarEvento(
                eq(7L),
                eq("DEVOLVER_VEHICULO"),
                eq("Devolucion"),
                eq(200L),
                eq("EXITOSO"),
                antesCaptor.capture(),
                despuesCaptor.capture(),
                eq("192.168.1.1"));

        // El JSON anterior debe contener el estado EN_CURSO.
        assertTrue(antesCaptor.getValue().contains("\"estadoReservacion\":\"EN_CURSO\""));
        // El JSON posterior debe contener FINALIZADA y DISPONIBLE.
        assertTrue(despuesCaptor.getValue().contains("\"estadoReservacion\":\"FINALIZADA\""));
        assertTrue(despuesCaptor.getValue().contains("\"estadoVehiculo\":\"DISPONIBLE\""));
    }

    // ── Casos de error ────────────────────────────────────────────────────────

    @Test
    @DisplayName("RN-08: kilometraje entrada < salida → 400 Bad Request y nada se guarda")
    void rn08KilometrajeEntradaMenorQueScalidaLanzaExcepcionYNoGuarda() {
        // Salida = 15000.0; entrada = 14999.0 (menor)
        DevolucionRequest req = new DevolucionRequest(
                50L, new BigDecimal("14999.0"), new BigDecimal("80.00"),
                "Sin daños", null, null, false);

        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(req, null));

        assertEquals(400, error.getStatus().value());
        assertTrue(error.getMessage().contains("RN-08"));
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Entrega inexistente → 404 Not Found")
    void entregaInexistenteDevuelve404() {
        when(entregas.findById(50L)).thenReturn(Optional.empty());
        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(request("15100.0", false), null));
        assertEquals(404, error.getStatus().value());
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Doble devolución → 409 Conflict")
    void devolvidaDobleVezLanza409() {
        when(devoluciones.existsByEntregaId(50L)).thenReturn(true);
        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(request("15100.0", false), null));
        assertEquals(409, error.getStatus().value());
        assertNadaGuardado();
    }

    @Test
    @DisplayName("Reservación no EN_CURSO → 409 Conflict")
    void reservacionNoEnCursoLanza409() {
        reservacion.setEstado("FINALIZADA");
        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(request("15100.0", false), null));
        assertEquals(409, error.getStatus().value());
        assertNadaGuardado();
    }

    @Test
    @DisplayName("determinarEstadoVehiculo: requiereMantenimiento=false → DISPONIBLE")
    void determinarEstadoSinDanioEsDisponible() {
        assertEquals(EstadoVehiculo.DISPONIBLE,
                DevolucionService.determinarEstadoVehiculo(false));
    }

    @Test
    @DisplayName("determinarEstadoVehiculo: requiereMantenimiento=null → DISPONIBLE")
    void determinarEstadoNullEsDisponible() {
        assertEquals(EstadoVehiculo.DISPONIBLE,
                DevolucionService.determinarEstadoVehiculo(null));
    }

    @Test
    @DisplayName("determinarEstadoVehiculo: requiereMantenimiento=true → MANTENIMIENTO siempre")
    void determinarEstadoRequiereMantenimientoEsMantenimiento() {
        assertEquals(EstadoVehiculo.MANTENIMIENTO,
                DevolucionService.determinarEstadoVehiculo(true));
    }

    @Test
    @DisplayName("Regresión: condición 'Sin daños' sin cargo ni flag → DISPONIBLE (ya no va a mantenimiento)")
    void condicionSinDanosNoMandaAMantenimiento() {
        DevolucionRequest req = new DevolucionRequest(
                50L, new BigDecimal("15100.0"), new BigDecimal("70.00"),
                "Sin daños", BigDecimal.ZERO, null, false);

        DevolucionResponse respuesta = servicio.registrar(req, "10.0.0.5");

        assertEquals("DISPONIBLE", respuesta.estadoVehiculo());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
    }

    // ── Fórmula exacta (Bug 2) ────────────────────────────────────────────────

    @Test
    @DisplayName("Fórmula exacta: 3 días pactados + 2 de atraso, tarifa 400 → 1200 + 800 + 0 = 2000")
    void formulaExactaSinDobleCobro() {
        LocalDateTime base = LocalDateTime.now();
        reservacion.setFechaInicio(base.minusDays(5)); // 3 días pactados (72 h exactas)
        reservacion.setFechaFin(base.minusDays(2));    // 2 días de atraso
        reservacion.setTarifaDia(new BigDecimal("400.00"));

        DevolucionResponse respuesta = servicio.registrar(request("15300.0", false), "10.0.0.5");

        assertEquals(new BigDecimal("1200.00"), respuesta.subtotal());    // solo días pactados
        assertEquals(new BigDecimal("800.00"),  respuesta.cargoAtraso()); // solo días excedentes
        assertEquals(new BigDecimal("0.00"),    respuesta.cargoDanos());
        assertEquals(new BigDecimal("2000.00"), respuesta.totalFinal());
    }

    @Test
    @DisplayName("Fórmula exacta: mismos datos + cargo por daños 350 → total 2350")
    void formulaExactaConDanos() {
        LocalDateTime base = LocalDateTime.now();
        reservacion.setFechaInicio(base.minusDays(5));
        reservacion.setFechaFin(base.minusDays(2));
        reservacion.setTarifaDia(new BigDecimal("400.00"));
        DevolucionRequest req = new DevolucionRequest(
                50L, new BigDecimal("15300.0"), new BigDecimal("70.00"),
                "Rayón leve en defensa", new BigDecimal("350.00"), null, false);

        DevolucionResponse respuesta = servicio.registrar(req, "10.0.0.5");

        assertEquals(new BigDecimal("1200.00"), respuesta.subtotal());
        assertEquals(new BigDecimal("800.00"),  respuesta.cargoAtraso());
        assertEquals(new BigDecimal("350.00"),  respuesta.cargoDanos());
        assertEquals(new BigDecimal("2350.00"), respuesta.totalFinal());
    }

    // ── Orden de bloqueo (Bug 6) ──────────────────────────────────────────────

    @Test
    @DisplayName("Bloqueo: primero vehículo y después reservación (mismo orden que EntregaService)")
    void bloqueaVehiculoAntesQueReservacion() {
        servicio.registrar(request("15100.0", false), "10.0.0.5");

        org.mockito.InOrder orden = inOrder(reservaciones, vehiculos);
        orden.verify(reservaciones).findVehiculoIdById(10L);
        orden.verify(vehiculos).findByIdParaActualizar(3L);
        orden.verify(reservaciones).findByIdParaActualizar(10L);
        verify(reservaciones, never()).findById(anyLong());
        verify(vehiculos, never()).findById(anyLong());
    }

    @Test
    @DisplayName("La reservación cambió de vehículo entre lectura y bloqueo → 409 y nada se guarda")
    void reservacionCambioDeVehiculoLanza409() {
        reservacion.setVehiculoId(99L); // tras el bloqueo apunta a otro vehículo
        DevolucionException error = assertThrows(DevolucionException.class,
                () -> servicio.registrar(request("15100.0", false), null));
        assertEquals(409, error.getStatus().value());
        assertNadaGuardado();
    }
}
