package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {
    @Mock private TarifaRepository tarifas;
    @InjectMocks private TarifaService servicio;

    private Tarifa tarifa(boolean activo) {
        Categoria categoria = new Categoria();
        categoria.setNombre("SUV");
        Tarifa tarifa = new Tarifa();
        tarifa.setCategoria(categoria);
        tarifa.setPrecioDia(new BigDecimal("850.00"));
        tarifa.setFechaInicio(LocalDate.of(2026, 10, 1));
        tarifa.setActivo(activo);
        return tarifa;
    }

    @Test void listarIncluyeHistorialYDatosDeCategoria() {
        when(tarifas.findAll(Sort.by(Sort.Direction.DESC, "fechaInicio", "id")))
                .thenReturn(List.of(tarifa(true), tarifa(false)));
        var resultado = servicio.listar();
        assertEquals(2, resultado.size());
        assertEquals("SUV", resultado.get(0).categoriaNombre());
        assertEquals(new BigDecimal("850.00"), resultado.get(0).precioDia());
        assertFalse(resultado.get(1).activo());
    }

    @Test void listarSinDatosDevuelveListaVacia() {
        when(tarifas.findAll(Sort.by(Sort.Direction.DESC, "fechaInicio", "id")))
                .thenReturn(List.of());
        assertTrue(servicio.listar().isEmpty());
    }

    @Test void obtenerConservaVigenciaAbiertaYCargo() {
        when(tarifas.findById(1L)).thenReturn(Optional.of(tarifa(true)));
        var resultado = servicio.obtener(1L);
        assertEquals(LocalDate.of(2026, 10, 1), resultado.fechaInicio());
        assertNull(resultado.fechaFin());
        assertEquals(BigDecimal.ZERO, resultado.cargoAtrasoDia());
    }

    @Test void obtenerInexistenteLanzaErrorControlado() {
        when(tarifas.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TarifaNoEncontradaException.class, () -> servicio.obtener(99L));
    }
}
