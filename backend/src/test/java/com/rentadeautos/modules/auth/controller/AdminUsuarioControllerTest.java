package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba la autorización y el cambio de estado de usuarios.
 */
@WebMvcTest(AdminUsuarioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioAppRepository usuarioRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "admin@rentadeautos.com",
            roles = "ADMINISTRADOR")
    @DisplayName("Administrador puede desactivar un usuario")
    void administradorPuedeDesactivarUsuario() throws Exception {
        UsuarioApp usuario = mock(UsuarioApp.class);

        UsuarioResponse respuesta = new UsuarioResponse(
                5L,
                "Agente de prueba",
                "agente@rentadeautos.com",
                "AGENTE"
        );

        when(usuarioRepository.findById(5L))
                .thenReturn(Optional.of(usuario));
        when(authService.aResponse(usuario))
                .thenReturn(respuesta);

        mockMvc.perform(patch("/api/admin/usuarios/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.rol").value("AGENTE"));

        verify(usuario).setActivo(false);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @WithMockUser(username = "agente@rentadeautos.com",
            roles = "AGENTE")
    @DisplayName("Agente no puede desactivar usuarios")
    void agenteNoPuedeDesactivarUsuario() throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "supervisor@rentadeautos.com",
            roles = "SUPERVISOR")
    @DisplayName("Supervisor no puede desactivar usuarios")
    void supervisorNoPuedeDesactivarUsuario() throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "auditor@rentadeautos.com",
            roles = "AUDITOR")
    @DisplayName("Auditor no puede desactivar usuarios")
    void auditorNoPuedeDesactivarUsuario() throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario no autenticado recibe 401")
    void usuarioNoAutenticadoRecibe401() throws Exception {
        mockMvc.perform(patch("/api/admin/usuarios/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@rentadeautos.com",
            roles = "ADMINISTRADOR")
    @DisplayName("Usuario inexistente devuelve 404")
    void usuarioInexistenteDevuelve404() throws Exception {
        when(usuarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/admin/usuarios/99/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Usuario no encontrado"));
    }
}