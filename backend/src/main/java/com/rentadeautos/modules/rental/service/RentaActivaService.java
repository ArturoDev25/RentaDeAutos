package com.rentadeautos.modules.rental.service;

import com.rentadeautos.modules.rental.dto.RentaActivaResponse;
import com.rentadeautos.modules.rental.repository.RentaActivaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RentaActivaService {

    private final RentaActivaRepository repositorio;

    public RentaActivaService(RentaActivaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<RentaActivaResponse> listar() {
        return repositorio.listar();
    }
}
