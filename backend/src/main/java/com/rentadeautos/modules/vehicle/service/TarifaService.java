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
        validarFechas(datos);
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

    @Transactional
    public TarifaResponse editar(Long id, TarifaRequest datos) {
        validarFechas(datos);
        Long origenId = tarifas.consultarCategoriaId(id)
                .orElseThrow(TarifaNoEncontradaException::new);
        // Orden estable para evitar bloqueos cruzados al cambiar de categoría.
        var ids = java.util.stream.Stream.of(origenId, datos.categoriaId()).distinct().sorted().toList();
        com.rentadeautos.modules.vehicle.model.Categoria destino = null;
        for (Long categoriaId : ids) {
            var categoria = categorias.bloquearPorId(categoriaId)
                    .orElseThrow(() -> new TarifaException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
            if (categoriaId.equals(datos.categoriaId())) {
                destino = categoria;
            }
        }
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new TarifaException(HttpStatus.BAD_REQUEST, "La categoría está inactiva");
        }
        Tarifa tarifa = tarifas.bloquearPorId(id).orElseThrow(TarifaNoEncontradaException::new);
        if (!tarifa.getCategoria().getId().equals(origenId)) {
            throw new TarifaException(HttpStatus.CONFLICT,
                    "La categoría de la tarifa cambió durante la operación; vuelve a intentarlo");
        }
        if (Boolean.TRUE.equals(tarifa.getActivo()) &&
                !tarifas.buscarTraslapesExcluyendo(datos.categoriaId(), datos.fechaInicio(),
                        datos.fechaFin(), id).isEmpty()) {
            throw new TarifaException(HttpStatus.CONFLICT,
                    "Ya existe una tarifa activa para la categoría en ese periodo");
        }
        tarifa.setCategoria(destino);
        tarifa.setPrecioDia(datos.precioDia());
        tarifa.setCargoAtrasoDia(datos.cargoAtrasoDia());
        tarifa.setFechaInicio(datos.fechaInicio());
        tarifa.setFechaFin(datos.fechaFin());
        // Editar el catálogo no modifica la tarifa histórica de las reservaciones.
        return TarifaResponse.desde(tarifas.saveAndFlush(tarifa));
    }

    private void validarFechas(TarifaRequest datos) {
        if (datos.fechaFin() != null && datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw new TarifaException(HttpStatus.BAD_REQUEST,
                    "La fecha final no puede ser anterior a la inicial");
        }
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
