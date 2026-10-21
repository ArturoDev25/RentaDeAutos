package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.auth.dto.ClienteRegistroRequest;
import com.rentadeautos.modules.auth.dto.ClienteRegistroResponse;
import com.rentadeautos.modules.auth.exception.ClienteRegistroDuplicadoException;
import com.rentadeautos.modules.auth.service.ClienteRegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/cliente")
public class ClienteRegistroController {
    private final ClienteRegistroService servicio;

    public ClienteRegistroController(ClienteRegistroService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<ClienteRegistroResponse>> registrar(
            @Valid @RequestBody ClienteRegistroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(servicio.registrar(datos)));
    }

    @ExceptionHandler(ClienteRegistroDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> duplicado(ClienteRegistroDuplicadoException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error.getMessage()));
    }
}
