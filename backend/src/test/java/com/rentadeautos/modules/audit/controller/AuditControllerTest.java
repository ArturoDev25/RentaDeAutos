package com.rentadeautos.modules.audit.controller;

import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.service.AuditService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioAppRepository usuarioAppRepository;

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("Rol AUDITOR puede consultar el listado y devuelve 200")
    void listar_rolAUDITOR_devuelve200() throws Exception {
        Page page = new PageImpl<>(List.of());
        when(auditService.listar(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Usuario sin sesion recibe 401")
    void listar_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Rol AGENTE recibe 403 al intentar consultar")
    void listar_rolAGENTE_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Rol SUPERVISOR puede consultar métricas")
    void metrics_rolSUPERVISOR_devuelve200() throws Exception {
        when(auditService.obtenerMetricas()).thenReturn(new AuditMetricsDTO(10, 2, 5, 0));

        mockMvc.perform(get("/api/v1/audit/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRegistros").value(10));
    }
    
    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("La API de auditoría rechaza métodos de escritura")
    void post_siempreDevuelve403_o_405() throws Exception {
        mockMvc.perform(post("/api/v1/audit"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() >= 400,
                        "POST no debe ser una operación exitosa"));
    }
}
