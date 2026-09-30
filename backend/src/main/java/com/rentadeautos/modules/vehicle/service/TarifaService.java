package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.TarifaResponse;
import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.exception.TarifaException;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import org.springframework.http.HttpStatus;
import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TarifaService {
    private final TarifaRepository tarifas;

    private final CategoriaRepository categorias;

    public TarifaService(TarifaRepository tarifas, CategoriaRepository categorias) {
        this.tarifas = tarifas;
        this.categorias = categorias;
    }

    @Transactional
    public TarifaResponse crear(TarifaRequest datos) {
        if (datos.fechaFin() != null && datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw new TarifaException(HttpStatus.BAD_REQUEST,
                    "La fecha final no puede ser anterior a la inicial");
        }
        // Bloquear la categoría antes de consultar traslapes protege también el primer alta.
        var categoria = categorias.bloquearPorId(datos.categoriaId())
                .orElseThrow(() -> new TarifaException(HttpStatus.BAD_REQUEST,
                        "La categoría no existe"));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new TarifaException(HttpStatus.BAD_REQUEST, "La categoría está inactiva");
        }
        if (!tarifas.buscarTraslapes(datos.categoriaId(), datos.fechaInicio(), datos.fechaFin()).isEmpty()) {
            throw new TarifaException(HttpStatus.CONFLICT,
                    "Ya existe una tarifa activa para la categoría en ese periodo");
        }
        Tarifa tarifa = new Tarifa();
        tarifa.setCategoria(categoria);
        tarifa.setPrecioDia(datos.precioDia());
        tarifa.setCargoAtrasoDia(datos.cargoAtrasoDia());
        tarifa.setFechaInicio(datos.fechaInicio());
        tarifa.setFechaFin(datos.fechaFin());
        return TarifaResponse.desde(tarifas.saveAndFlush(tarifa));
    }

    /** Incluye tarifas activas e inactivas para consultar el historial. */
    @Transactional(readOnly = true)
    public List<TarifaResponse> listar() {
        return tarifas.findAll(Sort.by(Sort.Direction.DESC, "fechaInicio", "id"))
                .stream().map(TarifaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public TarifaResponse obtener(Long id) {
        return TarifaResponse.desde(tarifas.findById(id)
                .orElseThrow(TarifaNoEncontradaException::new));
    }
}
