package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.exception.TarifaException;
import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarifaBajaServiceTest {
    @Mock private TarifaRepository tarifas;
    @Mock private CategoriaRepository categorias;
    @InjectMocks private TarifaService servicio;

    private Tarifa preparar(boolean activo) {
        Categoria c = new Categoria();
        ReflectionTestUtils.setField(c, "id", 2L);
        c.setNombre("SUV");
        Tarifa t = new Tarifa();
        ReflectionTestUtils.setField(t, "id", 7L);
        t.setCategoria(c);
        t.setPrecioDia(new BigDecimal("850.00"));
        t.setFechaInicio(LocalDate.of(2026, 10, 1));
        t.setActivo(activo);
        when(tarifas.consultarCategoriaId(7L)).thenReturn(Optional.of(2L));
        when(categorias.bloquearPorId(2L)).thenReturn(Optional.of(c));
        when(tarifas.bloquearPorId(7L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test void desactivaSinBorrarNiCambiarPrecioOVigencia() {
        Tarifa t = preparar(true);
        when(tarifas.saveAndFlush(t)).thenReturn(t);
        var resultado = servicio.desactivar(7L);
        assertFalse(resultado.activo());
        assertEquals(7L, resultado.id());
        assertEquals(new BigDecimal("850.00"), resultado.precioDia());
        assertEquals(LocalDate.of(2026, 10, 1), resultado.fechaInicio());
        var orden = inOrder(categorias, tarifas);
        orden.verify(tarifas).consultarCategoriaId(7L);
        orden.verify(categorias).bloquearPorId(2L);
        orden.verify(tarifas).bloquearPorId(7L);
        orden.verify(tarifas).saveAndFlush(t);
        verify(tarifas, never()).delete(any());
        verify(tarifas, never()).deleteById(any());
    }

    @Test void bajaRepetidaConservaEstadoSinGuardarDeNuevo() {
        preparar(false);
        assertFalse(servicio.desactivar(7L).activo());
        verify(tarifas, never()).saveAndFlush(any());
    }

    @Test void inexistenteNoGuarda() {
        when(tarifas.consultarCategoriaId(99L)).thenReturn(Optional.empty());
        assertThrows(TarifaNoEncontradaException.class, () -> servicio.desactivar(99L));
        verifyNoInteractions(categorias);
        verify(tarifas, never()).saveAndFlush(any());
    }

    @Test void categoriaCambiadaDuranteLaBajaRechazaOperacion() {
        Tarifa t = preparar(true);
        ReflectionTestUtils.setField(t.getCategoria(), "id", 3L);
        assertThrows(TarifaException.class, () -> servicio.desactivar(7L));
        assertTrue(t.getActivo());
        verify(tarifas, never()).saveAndFlush(any());
    }
}
