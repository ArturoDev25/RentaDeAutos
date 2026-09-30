package com.rentadeautos.modules.vehicle.service;

import com.rentadeautos.modules.vehicle.dto.TarifaResponse;
import com.rentadeautos.modules.vehicle.exception.TarifaNoEncontradaException;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TarifaService {
    private final TarifaRepository tarifas;

    public TarifaService(TarifaRepository tarifas) {
        this.tarifas = tarifas;
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
