package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class TarifaRepositoryTest {
    @Autowired private TestEntityManager em;
    @Autowired private TarifaRepository tarifas;
    private Categoria categoria;
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);

    @BeforeEach void categoria() {
        categoria = new Categoria();
        categoria.setNombre("SUV");
        em.persistAndFlush(categoria);
    }

    private Long guardar(LocalDate fin, boolean activo) {
        Tarifa tarifa = new Tarifa();
        tarifa.setCategoria(categoria);
        tarifa.setPrecioDia(new BigDecimal("850.00"));
        tarifa.setFechaInicio(inicio);
        tarifa.setFechaFin(fin);
        tarifa.setActivo(activo);
        em.persistAndFlush(tarifa);
        return tarifa.getId();
    }

    @Test void limitesInclusivosSeTraslapan() {
        guardar(inicio.plusDays(9), true);
        assertEquals(1, tarifas.buscarTraslapes(categoria.getId(), inicio.plusDays(9), inicio.plusDays(19)).size());
    }

    @Test void diaSiguienteNoSeTraslapa() {
        guardar(inicio.plusDays(9), true);
        assertTrue(tarifas.buscarTraslapes(categoria.getId(), inicio.plusDays(10), null).isEmpty());
    }

    @Test void vigenciaAbiertaBloqueaPeriodosFuturos() {
        guardar(null, true);
        assertEquals(1, tarifas.buscarTraslapes(categoria.getId(), inicio.plusYears(1), null).size());
    }

    @Test void tarifaInactivaNoBloquea() {
        guardar(null, false);
        assertTrue(tarifas.buscarTraslapes(categoria.getId(), inicio, null).isEmpty());
    }

    @Test void otraCategoriaNoBloquea() {
        guardar(null, true);
        Categoria otra = new Categoria();
        otra.setNombre("Compacto");
        em.persistAndFlush(otra);
        assertTrue(tarifas.buscarTraslapes(otra.getId(), inicio, null).isEmpty());
    }

    @Test void excluyeLaPropiaTarifa() {
        Long id = guardar(null, true);
        assertTrue(tarifas.buscarTraslapesExcluyendo(categoria.getId(), inicio, null, id).isEmpty());
        assertEquals(categoria.getId(), tarifas.consultarCategoriaId(id).orElseThrow());
        assertEquals(id, tarifas.bloquearPorId(id).orElseThrow().getId());
    }

    @Test void exclusionNoOcultaOtraTarifa() {
        Long propia = guardar(inicio.plusDays(9), true);
        Long otra = guardar(null, true);
        var resultado = tarifas.buscarTraslapesExcluyendo(categoria.getId(), inicio, null, propia);
        assertEquals(1, resultado.size());
        assertEquals(otra, resultado.get(0).getId());
    }
}
