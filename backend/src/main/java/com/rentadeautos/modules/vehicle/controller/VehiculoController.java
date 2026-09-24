package com.rentadeautos.modules.vehicle.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.vehicle.dto.EstadoVehiculoRequest;
import com.rentadeautos.modules.vehicle.dto.FiltroVehiculos;
import com.rentadeautos.modules.vehicle.dto.VehiculoRequest;
import com.rentadeautos.modules.vehicle.dto.VehiculoResponse;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.service.VehiculoService;

import jakarta.validation.Valid;

/**
 * Gestión de vehículos (S2-06).
 * La ruta coincide con la reservada en SecurityConfig (S2-10):
 * todos los roles consultan; solo ADMINISTRADOR y SUPERVISOR modifican.
 * No existe endpoint de eliminación: un vehículo se da de BAJA por estado.
 */
@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    /**
     * Lista los vehículos. Todos los parámetros son opcionales y se combinan:
     * <ul>
     *   <li>{@code q}: texto a buscar en placa, VIN, marca o modelo (S2-07).</li>
     *   <li>{@code estado}: DISPONIBLE, RESERVADO, RENTADO, MANTENIMIENTO o BAJA.</li>
     *   <li>{@code categoriaId}: id de la categoría.</li>
     *   <li>{@code anioDesde} y {@code anioHasta}: rango de años (inclusive).</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehiculoResponse>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoVehiculo estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer anioDesde,
            @RequestParam(required = false) Integer anioHasta) {
        FiltroVehiculos filtro =
                new FiltroVehiculos(q, estado, categoriaId, anioDesde, anioHasta);
        return ResponseEntity.ok(ApiResponse.success(vehiculoService.listar(filtro)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehiculoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehiculoService.obtener(id)));
    }

    /** Registra un vehículo; siempre inicia en estado DISPONIBLE. */
    @PostMapping
    public ResponseEntity<ApiResponse<VehiculoResponse>> crear(
            @Valid @RequestBody VehiculoRequest peticion) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(vehiculoService.crear(peticion)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehiculoResponse>> editar(
            @PathVariable Long id,
            @Valid @RequestBody VehiculoRequest peticion) {
        return ResponseEntity.ok(ApiResponse.success(vehiculoService.editar(id, peticion)));
    }

    /** Cambia el estado operativo (DISPONIBLE, MANTENIMIENTO, BAJA...). */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<VehiculoResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoVehiculoRequest peticion) {
        return ResponseEntity.ok(
                ApiResponse.success(vehiculoService.cambiarEstado(id, peticion.estado())));
    }
}
