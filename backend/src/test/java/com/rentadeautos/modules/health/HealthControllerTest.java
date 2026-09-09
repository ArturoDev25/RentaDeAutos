package com.rentadeautos.modules.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del endpoint GET /api/v1/health.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health debe retornar HTTP 200 OK")
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir status 'UP' en el body")
    void health_shouldReturnStatusUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir timestamp en el body")
    void health_shouldReturnTimestamp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.timestamp").exists())
               .andExpect(jsonPath("$.data.timestamp").isString());
    }
}
