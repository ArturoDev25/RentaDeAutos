package com.rentadeautos.modules.catalog.controller;

import com.rentadeautos.common.dto.ApiResponse;
import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/catalogo/vehiculos")
public class CatalogoController {
    private static final Pattern DATA_URL = Pattern.compile(
            "^data:(image/(?:png|jpe?g|webp|gif));base64,([A-Za-z0-9+/=\\s]+)$");

    private final CatalogoRepository catalogo;

    public CatalogoController(CatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public ApiResponse<List<VehiculoCatalogoResponse>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax) {
        if (categoriaId != null && categoriaId <= 0) {
            throw new IllegalArgumentException("categoriaId debe ser positivo");
        }
        validarPrecio(precioMin, "precioMin");
        validarPrecio(precioMax, "precioMax");
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new IllegalArgumentException("precioMin no puede ser mayor que precioMax");
        }
        return ApiResponse.success(catalogo.listar(LocalDateTime.now(), categoriaId, precioMin, precioMax));
    }

    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id) {
        if (id <= 0) {
            return ResponseEntity.notFound().build();
        }
        return catalogo.imagen(id)
                .flatMap(CatalogoController::decodificar)
                .map(foto -> ResponseEntity.ok()
                        .contentType(foto.tipo())
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                        .body(foto.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private record Foto(MediaType tipo, byte[] bytes) {
    }

    // Solo se sirven data URL de imagen válidas; cualquier otro contenido se trata como sin foto.
    private static Optional<Foto> decodificar(String dataUrl) {
        Matcher m = DATA_URL.matcher(dataUrl);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            String tipo = m.group(1).equals("image/jpg") ? "image/jpeg" : m.group(1);
            byte[] bytes = Base64.getMimeDecoder().decode(m.group(2));
            return bytes.length == 0 ? Optional.empty()
                    : Optional.of(new Foto(MediaType.parseMediaType(tipo), bytes));
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
    }

    private void validarPrecio(BigDecimal precio, String campo) {
        if (precio != null && (precio.signum() < 0
                || precio.compareTo(new BigDecimal("99999999.99")) > 0
                || precio.stripTrailingZeros().scale() > 2)) {
            throw new IllegalArgumentException(campo
                    + " debe estar entre 0 y 99999999.99 y tener como máximo dos decimales");
        }
    }
}
