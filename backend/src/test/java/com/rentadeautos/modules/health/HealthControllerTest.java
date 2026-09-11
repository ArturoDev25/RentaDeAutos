package com.rentadeautos.modules.health;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del endpoint GET /api/v1/health.
 */
@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtService jwtService;

    /**
     * El filtro de autenticación consulta este repositorio.
     * En esta prueba web se utiliza un mock para no depender de MySQL.
     */
    @MockBean
    private UsuarioAppRepository usuarioAppRepository;

    @Test
    @DisplayName("GET /api/v1/health debe retornar HTTP 200 OK")
    void health_shouldReturn200() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/health debe incluir status UP")
    void health_shouldReturnStatusUp() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe indicar base de datos disponible")
    void health_shouldReturnDatabaseUp_whenDbResponds() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health debe indicar base de datos no disponible")
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
    @DisplayName("GET /api/v1/health debe incluir timestamp")
    void health_shouldReturnTimestamp() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class)))
                .thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timestamp").exists())
                .andExpect(jsonPath("$.data.timestamp").isString());
    }
}