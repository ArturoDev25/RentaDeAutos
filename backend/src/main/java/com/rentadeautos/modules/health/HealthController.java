package com.rentadeautos.modules.health;

import com.rentadeautos.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador para verificar el estado de salud de la aplicación.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * Endpoint de salud del sistema.
     *
     * @return 200 OK con status "UP" y timestamp actual en ISO-8601
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success(healthData));
    }
}
