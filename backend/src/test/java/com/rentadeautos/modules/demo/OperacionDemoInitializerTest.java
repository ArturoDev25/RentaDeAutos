package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.devolucion.repository.DevolucionRepository;
import com.rentadeautos.modules.rental.repository.EntregaRepository;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_operacion;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Import(TarifaService.class)
class OperacionDemoInitializerTest {

    private OperacionDemoInitializer seed;
    @Autowired ClienteRepository clientes;
    @Autowired VehiculoRepository vehiculos;
    @Autowired CategoriaRepository categorias;
    @Autowired ReservacionRepository reservaciones;
    @Autowired EntregaRepository entregas;
    @Autowired DevolucionRepository devoluciones;
    @Autowired UsuarioAppRepository usuarios;
    @Autowired TarifaRepository tarifas;
    @Autowired TarifaService servicio;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepararDatos() {
        seed = new OperacionDemoInitializer(clientes, vehiculos, reservaciones, entregas, devoluciones, usuarios, tarifas, jdbc);
        new ClientelaDemoInitializer(clientes, new BCryptPasswordEncoder(4), "Demo-S504-test-2026").run();
        new FlotaDemoInitializer(categorias, tarifas, vehiculos, servicio).run();
    }

    private long contarEstado(String estado) {
        return reservaciones.findAll().stream().filter(r -> estado.equals(r.getEstado())).count();
    }

    private long contarVehiculos(EstadoVehiculo estado) {
        return vehiculos.findAll().stream().filter(v -> v.getEstado() == estado).count();
    }

    @Test
    void cargaCincuentaYCuatroReservacionesDelCiclo() {
        seed.run();

        assertEquals(54, reservaciones.count());
        assertEquals(18, contarEstado("EN_CURSO"));
        assertEquals(14, contarEstado("FINALIZADA"));
        assertEquals(12, contarEstado("CONFIRMADA"));
        assertEquals(6, contarEstado("PENDIENTE"));
        assertEquals(4, contarEstado("CANCELADA"));

        assertEquals(32, entregas.count(), "entregas = en curso (18) + finalizadas (14)");
        assertEquals(14, devoluciones.count(), "devoluciones = solo finalizadas");

        assertEquals(18, contarVehiculos(EstadoVehiculo.RENTADO));
        assertEquals(12, contarVehiculos(EstadoVehiculo.RESERVADO));

        for (Reservacion r : reservaciones.findAll()) {
            assertTrue(r.getObservaciones().startsWith(OperacionDemoInitializer.SENTINEL));
            assertNotNull(r.getCreadoPorId());
            assertTrue(r.getTotalEstimado().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(r.getFechaFin().isAfter(r.getFechaInicio()));
        }
    }

    @Test
    void esIdempotente() {
        seed.run();
        seed.run();
        assertEquals(54, reservaciones.count());
    }

    @Test
    void sinVehiculosDisponiblesLanzaError() {
        vehiculos.findAll().forEach(v -> {
            v.setEstado(EstadoVehiculo.BAJA);
            vehiculos.save(v);
        });
        vehiculos.flush();
        assertThrows(IllegalStateException.class, seed::run);
        assertEquals(0, reservaciones.count());
    }
    @Test void noTocaClientesNiVehiculosAjenos() {
        var cat = categorias.findAll().get(0);
        var v = new Vehiculo(); v.setCategoria(cat); v.setPlaca("AJENO-001");
        v.setVin("3MXRNT99999999999"); v.setMarca("Manual"); v.setModelo("Ajeno");
        v.setAnio(2024); v.setKilometraje(new BigDecimal("1234.0")); v.setEstado(EstadoVehiculo.DISPONIBLE);
        v = vehiculos.saveAndFlush(v);
        var c = new Cliente(); c.setNombre("Manual"); c.setApellidos("Ajeno"); c.setCorreo("manual@example.test");
        c.setTelefono("0000000000"); c.setNumeroLicencia("LIC-AJENA");
        c.setLicenciaVencimiento(LocalDate.now().plusYears(2)); c.setActivo(true);
        c = clientes.saveAndFlush(c);
        seed.run();
        Long ajeno = c.getId(); Long auto = v.getId();
        assertTrue(reservaciones.findAll().stream().noneMatch(r -> r.getClienteId().equals(ajeno) || r.getVehiculoId().equals(auto)));
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculos.findById(auto).orElseThrow().getEstado());
    }

    @Test void reservaCopiaLaTarifaConsultadaEnLaBase() {
        tarifas.findAll().forEach(t -> { t.setPrecioDia(new BigDecimal("987.00")); tarifas.save(t); });
        tarifas.flush(); seed.run();
        assertTrue(reservaciones.findAll().stream().allMatch(r -> r.getTarifaDia().compareTo(new BigDecimal("987.00")) == 0));
    }

    @Test void sinTarifaNoCreaReservaciones() {
        tarifas.deleteAll(); tarifas.flush();
        assertThrows(IllegalStateException.class, seed::run);
        assertEquals(0, reservaciones.count());
    }

    @Test void kilometrajeActualCoincideConCadaDevolucion() {
        seed.run();
        for (var d : devoluciones.findAll()) {
            var e = entregas.findById(d.getEntregaId()).orElseThrow();
            var r = reservaciones.findById(e.getReservacionId()).orElseThrow();
            var v = vehiculos.findById(r.getVehiculoId()).orElseThrow();
            assertEquals(0, v.getKilometraje().compareTo(d.getKilometrajeEntrada()));
            assertTrue(d.getKilometrajeEntrada().compareTo(e.getKilometrajeSalida()) >= 0);
        }
    }

    @Test void registroAjenoConTraslapeSeRechazaSinCrearOtro() {
        var v = vehiculos.findAll().stream().filter(FlotaDemoInitializer::aptoParaEscenario)
                .sorted(java.util.Comparator.comparing(Vehiculo::getPlaca)).findFirst().orElseThrow();
        var c = clientes.findAll().stream().filter(Cliente::getActivo).findFirst().orElseThrow();
        var r = new Reservacion(); r.setClienteId(c.getId()); r.setVehiculoId(v.getId());
        r.setCreadoPorId(usuarios.findByCorreo("agente@demo.local").orElseThrow().getId());
        r.setFechaInicio(LocalDate.now().minusDays(10).atStartOfDay());
        r.setFechaFin(LocalDate.now().plusDays(10).atStartOfDay());
        r.setEstado("PENDIENTE"); r.setTarifaDia(new BigDecimal("10")); r.setTotalEstimado(new BigDecimal("200"));
        reservaciones.saveAndFlush(r);
        assertThrows(IllegalStateException.class, seed::run);
        assertEquals(1, reservaciones.count());
    }

    @Test void reponeEscenarioFaltanteSinDuplicarLosDemas() {
        seed.run();
        var eliminable = reservaciones.findAll().stream().filter(r -> "CANCELADA".equals(r.getEstado())).findFirst().orElseThrow();
        reservaciones.delete(eliminable); reservaciones.flush();
        seed.run();
        assertEquals(54, reservaciones.count());
        assertEquals(32, entregas.count());
    }

}
