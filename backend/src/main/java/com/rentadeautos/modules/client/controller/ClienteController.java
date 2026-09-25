package com.rentadeautos.modules.client.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.client.dto.ClienteRequest;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import com.rentadeautos.modules.client.dto.FiltroClientes;
import com.rentadeautos.modules.client.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(ApiResponse.success(
                clienteService.listar(new FiltroClientes(q, activo))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(
            @Valid @RequestBody ClienteRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(clienteService.crear(peticion)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> editar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest peticion) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.editar(id, peticion)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.desactivar(id)));
    }
}