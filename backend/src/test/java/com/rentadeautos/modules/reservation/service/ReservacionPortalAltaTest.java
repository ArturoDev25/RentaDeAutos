package com.rentadeautos.modules.reservation.service;

import com.rentadeautos.modules.reservation.dto.ReservacionClienteRequest;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import(ReservacionService.class)
class ReservacionPortalAltaTest {
    @Autowired ReservacionService servicio;
    @Autowired JdbcTemplate jdbc;
    @MockBean UsuarioAppRepository usuarios;
    @MockBean AuditService auditoria;
    private LocalDateTime inicio;

    @BeforeEach void preparar() {
        inicio = LocalDateTime.now().plusDays(10).withNano(0);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente:7", null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (1, 'SUV', 0, TRUE)");
        jdbc.update("""
                INSERT INTO clientes (id, nombre, apellidos, telefono, numero_licencia, licencia_vencimiento, activo)
                VALUES (7, 'Ana', 'Pérez', '8441234567', 'DEMO7', '2030-12-31', TRUE)
                """);
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (1, 1, 'DEMO1', '1HGCM82633A004352', 'Nissan', 'Kicks', 2024, 1000, 'DISPONIBLE')
                """);
        jdbc.update("""
                INSERT INTO tarifas (id, categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, activo)
                VALUES (1, 1, 850, 100, ?, TRUE)
                """, inicio.toLocalDate());
    }

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    private ReservacionClienteRequest datos() {
        return new ReservacionClienteRequest(1L, inicio, inicio.plusHours(25), "  Portal  ");
    }

    private void rechazaSinGuardar(int codigo, ReservacionClienteRequest datos) {
        var error = assertThrows(ReservacionException.class, () -> servicio.crearDesdePortal(datos));
        assertEquals(codigo, error.getStatus().value());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones", Integer.class));
    }

    @Test void creaConClienteAutenticadoTarifaHistoricaYRedondeo() {
        var r = servicio.crearDesdePortal(datos());
        assertNotNull(r.id());
        assertEquals(7L, r.clienteId());
        assertNull(r.creadoPorId());
        assertEquals("PENDIENTE", r.estado());
        assertEquals(new BigDecimal("850.00"), r.tarifaDia());
        assertEquals(new BigDecimal("1700.00"), r.totalEstimado());
        assertEquals("Portal", r.observaciones());
        assertEquals(7L, jdbc.queryForObject("SELECT creado_por_cliente_id FROM reservaciones WHERE id = ?", Long.class, r.id()));
        verifyNoInteractions(usuarios);
    }

    @Test void traslapeNoGuardaSegundaReserva() {
        servicio.crearDesdePortal(datos());
        var error = assertThrows(ReservacionException.class, () -> servicio.crearDesdePortal(datos()));
        assertEquals(409, error.getStatus().value());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones", Integer.class));
    }

    @Test void clienteInactivoNoPuedeCrear() {
        jdbc.update("UPDATE clientes SET activo = FALSE WHERE id = 7");
        rechazaSinGuardar(400, datos());
    }

    @Test void autoDeBajaNoPuedeReservarse() {
        jdbc.update("UPDATE vehiculos SET estado = 'BAJA'");
        rechazaSinGuardar(400, datos());
    }

    @Test void sinTarifaNoGuarda() {
        jdbc.update("DELETE FROM tarifas");
        rechazaSinGuardar(409, datos());
    }

    @Test void fechasInvertidasNoGuardan() {
        rechazaSinGuardar(400, new ReservacionClienteRequest(1L, inicio, inicio.minusHours(1), null));
    }

    @Test void inicioPasadoNoGuarda() {
        rechazaSinGuardar(400, new ReservacionClienteRequest(1L, inicio.minusDays(20), inicio, null));
    }

    @Test void sinSesionNoGuarda() {
        SecurityContextHolder.clearContext();
        rechazaSinGuardar(401, datos());
    }
    @Test void cancelaPendientePropiaSinEliminarla() {
        var r = servicio.crearDesdePortal(datos());
        var cancelada = servicio.cancelarDesdePortal(r.id());
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals(r.totalEstimado(), cancelada.totalEstimado());
        assertEquals("CANCELADA", jdbc.queryForObject("SELECT estado FROM reservaciones WHERE id = ?", String.class, r.id()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones", Integer.class));
    }

    @Test void cancelarConfirmadaLiberaVehiculoSinOtrosCompromisos() {
        var r = servicio.crearDesdePortal(datos());
        jdbc.update("UPDATE reservaciones SET estado = 'CONFIRMADA' WHERE id = ?", r.id());
        jdbc.update("UPDATE vehiculos SET estado = 'RESERVADO' WHERE id = 1");
        servicio.cancelarDesdePortal(r.id());
        assertEquals("DISPONIBLE", jdbc.queryForObject("SELECT estado FROM vehiculos WHERE id = 1", String.class));
    }

    @Test void cancelarConfirmadaNoLiberaSiHayOtraConfirmada() {
        var r = servicio.crearDesdePortal(datos());
        jdbc.update("UPDATE reservaciones SET estado = 'CONFIRMADA' WHERE id = ?", r.id());
        jdbc.update("UPDATE vehiculos SET estado = 'RESERVADO' WHERE id = 1");
        jdbc.update("""
                INSERT INTO reservaciones (cliente_id, vehiculo_id, creado_por_cliente_id,
                    fecha_inicio, fecha_fin, estado, tarifa_dia, total_estimado)
                VALUES (7, 1, 7, ?, ?, 'CONFIRMADA', 850, 850)
                """, inicio.plusDays(3), inicio.plusDays(4));
        servicio.cancelarDesdePortal(r.id());
        assertEquals("RESERVADO", jdbc.queryForObject("SELECT estado FROM vehiculos WHERE id = 1", String.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"EN_CURSO", "FINALIZADA", "CANCELADA"})
    void noCancelaEstadosNoPermitidos(String estado) {
        var r = servicio.crearDesdePortal(datos());
        jdbc.update("UPDATE reservaciones SET estado = ? WHERE id = ?", estado, r.id());
        var error = assertThrows(ReservacionException.class, () -> servicio.cancelarDesdePortal(r.id()));
        assertEquals(409, error.getStatus().value());
        assertEquals(estado, jdbc.queryForObject("SELECT estado FROM reservaciones WHERE id = ?", String.class, r.id()));
    }

    @Test void clienteDistintoNoCancelaReservaAjena() {
        var r = servicio.crearDesdePortal(datos());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cliente:8", null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
        var error = assertThrows(ReservacionException.class, () -> servicio.cancelarDesdePortal(r.id()));
        assertEquals(404, error.getStatus().value());
        assertEquals("PENDIENTE", jdbc.queryForObject("SELECT estado FROM reservaciones WHERE id = ?", String.class, r.id()));
    }

    @Test void cancelacionInexistenteDevuelve404() {
        var error = assertThrows(ReservacionException.class, () -> servicio.cancelarDesdePortal(999L));
        assertEquals(404, error.getStatus().value());
    }

    @Test void cancelarSinSesionDevuelve401() {
        SecurityContextHolder.clearContext();
        var error = assertThrows(ReservacionException.class, () -> servicio.cancelarDesdePortal(1L));
        assertEquals(401, error.getStatus().value());
    }

    @Test void cancelacionNoSacaVehiculoDeMantenimiento() {
        var r = servicio.crearDesdePortal(datos());
        jdbc.update("UPDATE reservaciones SET estado = 'CONFIRMADA' WHERE id = ?", r.id());
        jdbc.update("UPDATE vehiculos SET estado = 'MANTENIMIENTO' WHERE id = 1");
        servicio.cancelarDesdePortal(r.id());
        assertEquals("MANTENIMIENTO", jdbc.queryForObject("SELECT estado FROM vehiculos WHERE id = 1", String.class));
    }

    @Test void noCancelaReservacionCuyaFechaDeInicioYaPaso() {
        var r = servicio.crearDesdePortal(datos());
        jdbc.update("UPDATE reservaciones SET fecha_inicio = ?, fecha_fin = ? WHERE id = ?",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().plusDays(1), r.id());
        var error = assertThrows(ReservacionException.class, () -> servicio.cancelarDesdePortal(r.id()));
        assertEquals(409, error.getStatus().value());
        assertEquals("PENDIENTE", jdbc.queryForObject("SELECT estado FROM reservaciones WHERE id = ?", String.class, r.id()));
    }

}
