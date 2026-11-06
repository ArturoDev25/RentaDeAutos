package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.catalog.controller.CatalogoController;
import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_catalogo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Import(TarifaService.class)
class CatalogoDemoInitializerTest {
    @Autowired CategoriaRepository categorias;
    @Autowired VehiculoRepository vehiculos;
    @Autowired TarifaRepository tarifas;
    @Autowired TarifaService servicio;
    @Autowired JdbcTemplate jdbc;
    private CatalogoDemoInitializer seed;

    @BeforeEach void preparar() {
        // Se ejecuta dentro de la transacción del caso, no al arrancar el contexto.
        seed = new CatalogoDemoInitializer(categorias, vehiculos, tarifas, servicio);
    }

    @Test void publicaCincoVehiculosConTarifaVigenteYEntregaLaFotoOriginal() throws Exception {
        seed.run();
        var catalogo = new CatalogoRepository(jdbc);
        var lista = catalogo.listar(LocalDateTime.now());
        assertEquals(5, lista.size());
        assertTrue(lista.stream().allMatch(v -> v.tieneImagen() && v.tarifaDia().signum() > 0));
        assertEquals(3, categorias.count());
        assertEquals(3, tarifas.count());
        var tsuru = vehiculos.findByPlacaIgnoreCase("S504-DEMO-01").orElseThrow();
        var respuesta = new CatalogoController(catalogo).imagen(tsuru.getId());
        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(MediaType.IMAGE_JPEG, respuesta.getHeaders().getContentType());
        try (var foto = new ClassPathResource("demo/fotos/Tsuru.jpg").getInputStream()) {
            assertArrayEquals(foto.readAllBytes(), respuesta.getBody());
        }
    }

    @Test void repetirConservaVehiculosYNoCambiaEstado() {
        seed.run();
        var vehiculo = vehiculos.findByPlacaIgnoreCase("S504-DEMO-01").orElseThrow();
        vehiculo.setEstado(EstadoVehiculo.MANTENIMIENTO);
        vehiculos.saveAndFlush(vehiculo);
        seed.run();
        assertEquals(5, vehiculos.count());
        assertEquals(3, categorias.count());
        assertEquals(3, tarifas.count());
        assertEquals(EstadoVehiculo.MANTENIMIENTO,
                vehiculos.findById(vehiculo.getId()).orElseThrow().getEstado());
    }

    @Test void conservaTarifaVigentePersonalizada() {
        var categoria = categoriaDemo();
        servicio.crear(new TarifaRequest(categoria.getId(), new BigDecimal("777.00"),
                BigDecimal.ZERO, LocalDate.now().minusDays(1), null));
        seed.run();
        var visibles = new CatalogoRepository(jdbc).listar(LocalDateTime.now(), categoria.getId(), null, null);
        assertEquals(1, visibles.size());
        assertEquals(new BigDecimal("777.00"), visibles.get(0).tarifaDia());
        assertEquals(3, tarifas.count());
    }

    @Test void completaVigenciaActualSinTraslaparTarifaFutura() {
        var categoria = categoriaDemo();
        LocalDate futura = LocalDate.now().plusDays(10);
        servicio.crear(new TarifaRequest(categoria.getId(), new BigDecimal("777.00"),
                BigDecimal.ZERO, futura, null));
        seed.run();
        var vigencias = tarifas.buscarTraslapes(categoria.getId(), LocalDate.now(), null);
        assertEquals(2, vigencias.size());
        var actual = vigencias.stream().filter(t -> !t.getFechaInicio().isAfter(LocalDate.now()))
                .findFirst().orElseThrow();
        assertEquals(futura.minusDays(1), actual.getFechaFin());
    }

    private Categoria categoriaDemo() {
        var categoria = new Categoria();
        categoria.setNombre("Demo Económico");
        categoria.setActivo(true);
        categoria.setDepositoBase(BigDecimal.ZERO);
        return categorias.saveAndFlush(categoria);
    }
}
