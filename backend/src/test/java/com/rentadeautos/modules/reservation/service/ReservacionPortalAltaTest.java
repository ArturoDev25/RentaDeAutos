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
}
