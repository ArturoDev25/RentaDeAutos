package com.rentadeautos.modules.vehicle;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = "spring.flyway.enabled=false")
@Import({TarifaDataInitializer.class, TarifaService.class})
class TarifaDataInitializerTest {
    @Autowired private TestEntityManager em;
    @Autowired private TarifaRepository tarifas;
    @Autowired private TarifaDataInitializer seed;

    private Categoria categoria(String nombre, boolean activo) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setActivo(activo);
        return em.persistAndFlush(c);
    }

    private Tarifa tarifa(Categoria c, LocalDate desde, LocalDate hasta, boolean activo) {
        Tarifa t = new Tarifa();
        t.setCategoria(c);
        t.setPrecioDia(new BigDecimal("777.00"));
        t.setFechaInicio(desde);
        t.setFechaFin(hasta);
        t.setActivo(activo);
        return em.persistAndFlush(t);
    }

    @Test void cargaCuatroCategoriasYRepetirNoDuplica() {
        categoria("Económico", true);
        Categoria suv = categoria("SUV", true);
        categoria("Pickup", true);
        categoria("Lujo", true);
        seed.run();
        seed.run();
        assertEquals(4, tarifas.count());
        var vigentes = tarifas.buscarTraslapes(suv.getId(), LocalDate.now(), LocalDate.now());
        assertEquals(1, vigentes.size());
        assertEquals(new BigDecimal("850.00"), vigentes.get(0).getPrecioDia());
        assertEquals(new BigDecimal("100.00"), vigentes.get(0).getCargoAtrasoDia());
    }

    @Test void conservaTarifaVigenteCapturadaPreviamente() {
        Categoria c = categoria("SUV", true);
        Tarifa existente = tarifa(c, LocalDate.now().minusDays(1), null, true);
        seed.run();
        assertEquals(1, tarifas.count());
        assertEquals(new BigDecimal("777.00"), tarifas.findById(existente.getId()).orElseThrow().getPrecioDia());
    }

    @Test void respetaTarifaFuturaYCompletaSoloElHueco() {
        Categoria c = categoria("SUV", true);
        LocalDate futura = LocalDate.now().plusDays(10);
        tarifa(c, futura, null, true);
        seed.run();
        seed.run();
        assertEquals(2, tarifas.count());
        Tarifa hoy = tarifas.buscarTraslapes(c.getId(), LocalDate.now(), LocalDate.now()).get(0);
        assertEquals(futura.minusDays(1), hoy.getFechaFin());
        assertEquals(1, tarifas.buscarTraslapes(c.getId(), futura, futura).size());
    }

    @Test void categoriaInactivaNoRecibeTarifa() {
        categoria("SUV", false);
        seed.run();
        assertEquals(0, tarifas.count());
    }

    @Test void historicasEInactivasSeConservanSinBloquearLaDemo() {
        Categoria c = categoria("SUV", true);
        tarifa(c, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), true);
        tarifa(c, LocalDate.now(), null, false);
        seed.run();
        assertEquals(3, tarifas.count());
        assertEquals(1, tarifas.buscarTraslapes(c.getId(), LocalDate.now(), LocalDate.now()).size());
    }

    @Test void categoriaAdicionalRecibePrecioFicticioDeRespaldo() {
        Categoria c = categoria("Minivan", true);
        seed.run();
        assertEquals(new BigDecimal("850.00"),
                tarifas.buscarTraslapes(c.getId(), LocalDate.now(), LocalDate.now()).get(0).getPrecioDia());
    }
}
