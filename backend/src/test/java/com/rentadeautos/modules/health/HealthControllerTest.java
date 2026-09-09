package com.rentadeautos.modules.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del endpoint GET /api/v1/health.
 *
 * <p>Usa {@code @WebMvcTest} para cargar sólo la capa web y
 * {@code @MockBean} para simular {@link JdbcTemplate} sin BD real.
 * El perfil {@code "test"} garantiza aislamiento total.</p>
 */
@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock de JdbcTemplate necesario porque HealthController lo inyecta
     * y @WebMvcTest sólo carga la capa web (no auto-configura DataSource).
     */
    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /api/v1/health debe retornar HTTP 200 OK")
    void health_shouldReturn200() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir status 'UP' en el body")
    void health_shouldReturnStatusUp() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir database 'UP' cuando BD responde")
    void health_shouldReturnDatabaseUp_whenDbResponds() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir database 'DOWN' cuando BD no responde")
    void health_shouldReturnDatabaseDown_whenDbFails() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.status").value("UP"))
               .andExpect(jsonPath("$.data.database").value("DOWN"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir timestamp en el body")
    void health_shouldReturnTimestamp() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.timestamp").exists())
               .andExpect(jsonPath("$.data.timestamp").isString());
    }
}
