package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_flota;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Import(TarifaService.class)
class FlotaDemoInitializerTest {

    private FlotaDemoInitializer seed;
    @Autowired CategoriaRepository categorias;
    @Autowired TarifaService servicio;
    @Autowired TarifaRepository tarifas;
    @Autowired VehiculoRepository vehiculos;

    @BeforeEach
    void prepararSeed() {
        seed = new FlotaDemoInitializer(categorias, tarifas, vehiculos, servicio);
    }

    private long contarEstado(EstadoVehiculo estado) {
        return vehiculos.findAll().stream().filter(v -> v.getEstado() == estado).count();
    }

    @Test
    void creaDiezCategoriasYCientoCincuentaVehiculos() {
        seed.run();
        assertEquals(10, categorias.count());
        assertEquals(150, vehiculos.count());
        assertEquals(16, contarEstado(EstadoVehiculo.MANTENIMIENTO));
        assertEquals(8, contarEstado(EstadoVehiculo.BAJA));
        assertEquals(126, contarEstado(EstadoVehiculo.DISPONIBLE));
    }

    @Test
    void todaCategoriaTieneTarifa() {
        seed.run();
        Set<Long> idsCategorias = categorias.findAll().stream()
                .map(Categoria::getId).collect(Collectors.toSet());
        Set<Long> idsConTarifa = tarifas.findAll().stream()
                .map(t -> t.getCategoria().getId()).collect(Collectors.toSet());
        assertEquals(10, idsCategorias.size());
        assertTrue(idsConTarifa.containsAll(idsCategorias), "cada categoría debe tener tarifa");
    }

    @Test
    void losVehiculosTienenVinDe17YPlacaUnica() {
        seed.run();
        Set<String> placas = vehiculos.findAll().stream()
                .map(Vehiculo::getPlaca).collect(Collectors.toSet());
        assertEquals(150, placas.size(), "las placas deben ser únicas");
        for (Vehiculo v : vehiculos.findAll()) {
            assertEquals(17, v.getVin().length());
            assertNotNull(v.getCategoria());
        }
    }

    @Test
    void repetirNoDuplica() {
        seed.run();
        seed.run();
        assertEquals(150, vehiculos.count());
        assertEquals(10, categorias.count());
    }
    @Test void categoriaPreviaSinTarifaSeCompletaSinEditarSusDatos() {
        var cat = new Categoria(); cat.setNombre("Económico"); cat.setActivo(true);
        cat.setDepositoBase(new java.math.BigDecimal("999.00"));
        cat = categorias.saveAndFlush(cat);
        seed.run();
        assertEquals(new java.math.BigDecimal("999.00"), categorias.findById(cat.getId()).orElseThrow().getDepositoBase());
        assertEquals(1, tarifas.buscarTraslapes(cat.getId(), java.time.LocalDate.now(), java.time.LocalDate.now()).size());
    }

    @Test void conservaTarifaExistenteYCubreHuecosSinTraslapes() {
        seed.run();
        var cat = categorias.findByNombreIgnoreCase("Económico").orElseThrow();
        var tarifa = tarifas.buscarTraslapes(cat.getId(), java.time.LocalDate.now(), java.time.LocalDate.now()).get(0);
        tarifa.setPrecioDia(new java.math.BigDecimal("777.00")); tarifas.saveAndFlush(tarifa);
        seed.run();
        assertEquals(1, tarifas.buscarTraslapes(cat.getId(), java.time.LocalDate.now(), java.time.LocalDate.now()).size());
        assertEquals(new java.math.BigDecimal("777.00"), tarifas.findById(tarifa.getId()).orElseThrow().getPrecioDia());
    }

}
