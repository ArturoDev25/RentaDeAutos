package com.rentadeautos.modules.health;

import com.rentadeautos.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador para verificar el estado de salud de la aplicación.
 *
 * <p>El endpoint {@code GET /api/v1/health} informa el estado general del
 * servicio y la conectividad activa con la base de datos MySQL mediante
 * una consulta de sondeo {@code SELECT 1}.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Endpoint de salud del sistema.
     *
     * <p>Ejecuta {@code SELECT 1} para validar la conexión activa con la BD.
     * Si la consulta falla, {@code database} se reporta como {@code "DOWN"}
     * pero el endpoint sigue respondiendo HTTP 200 para no romper
     * health-checks de infraestructura.</p>
     *
     * @return 200 OK con status, database y timestamp en ISO-8601
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        String dbStatus = checkDatabase();

        Map<String, String> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("database", dbStatus);
        healthData.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success(healthData));
    }

    /**
     * Verifica la conectividad con la base de datos ejecutando {@code SELECT 1}.
     *
     * @return {@code "UP"} si la consulta tiene éxito, {@code "DOWN"} en caso contrario
     */
    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
