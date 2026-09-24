package com.rentadeautos.modules.vehicle.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.vehicle.dto.CategoriaRequest;
import com.rentadeautos.modules.vehicle.dto.CategoriaResponse;
import com.rentadeautos.modules.vehicle.exception.CategoriaDuplicadaException;
import com.rentadeautos.modules.vehicle.exception.CategoriaNoEncontradaException;
import com.rentadeautos.modules.vehicle.service.CategoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba los endpoints del catálogo de categorías (S2-08).
 */
@WebMvcTest(CategoriaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoriaControllerTest {

    private static final String CUERPO_VALIDO = """
            {
              "nombre": "SUV",
              "descripcion": "Camionetas familiares",
              "depositoBase": 3000.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoriaService categoriaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioAppRepository usuarioRepository;

    private CategoriaResponse suv(boolean activo) {
        return new CategoriaResponse(
                1L, "SUV", "Camionetas familiares", new BigDecimal("3000.00"), activo);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se pueden consultar las categorías")
    void listarDevuelveCategorias() throws Exception {
        when(categoriaService.listar(null)).thenReturn(List.of(suv(true)));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nombre").value("SUV"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede filtrar el listado por estado activo")
    void listarFiltraPorActivo() throws Exception {
        when(categoriaService.listar(true)).thenReturn(List.of(suv(true)));

        mockMvc.perform(get("/api/categorias").param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activo").value(true));

        verify(categoriaService).listar(true);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede consultar una categoría por id")
    void obtenerDevuelveCategoria() throws Exception {
        when(categoriaService.obtener(1L)).thenReturn(suv(true));

        mockMvc.perform(get("/api/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nombre").value("SUV"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Consultar una categoría inexistente devuelve 404")
    void obtenerInexistenteDevuelve404() throws Exception {
        when(categoriaService.obtener(99L))
                .thenThrow(new CategoriaNoEncontradaException());

        mockMvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede crear una categoría válida")
    void crearDevuelve201() throws Exception {
        when(categoriaService.crear(any(CategoriaRequest.class))).thenReturn(suv(true));

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Crear sin nombre devuelve 400")
    void crearSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "  ",
                                  "depositoBase": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(categoriaService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Crear con depósito negativo devuelve 400")
    void crearConDepositoNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "SUV",
                                  "depositoBase": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoriaService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Crear con nombre repetido devuelve 409")
    void crearDuplicadaDevuelve409() throws Exception {
        when(categoriaService.crear(any(CategoriaRequest.class)))
                .thenThrow(new CategoriaDuplicadaException());

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Ya existe una categoría con ese nombre"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede editar una categoría")
    void editarDevuelve200() throws Exception {
        when(categoriaService.editar(eq(1L), any(CategoriaRequest.class)))
                .thenReturn(suv(true));

        mockMvc.perform(put("/api/categorias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("SUV"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Editar una categoría inexistente devuelve 404")
    void editarInexistenteDevuelve404() throws Exception {
        when(categoriaService.editar(eq(99L), any(CategoriaRequest.class)))
                .thenThrow(new CategoriaNoEncontradaException());

        mockMvc.perform(put("/api/categorias/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede desactivar una categoría")
    void cambiarEstadoDesactiva() throws Exception {
        when(categoriaService.cambiarEstado(1L, false)).thenReturn(suv(false));

        mockMvc.perform(patch("/api/categorias/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(false));

        verify(categoriaService).cambiarEstado(1L, false);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Cambiar estado sin indicar el valor devuelve 400")
    void cambiarEstadoSinValorDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/categorias/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoriaService);
    }

    @Test
    @DisplayName("Usuario no autenticado recibe 401")
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("No existe eliminación física de categorías")
    void noSePuedeEliminarUnaCategoria() throws Exception {
        mockMvc.perform(delete("/api/categorias/1"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() >= 400,
                        "DELETE no debe ser una operación exitosa"));

        verifyNoInteractions(categoriaService);
    }

    @ParameterizedTest(name = "{0} no puede operar el catálogo de categorías")
@ValueSource(strings = {"AGENTE", "SUPERVISOR", "AUDITOR"})
void otrosRolesNoPuedenOperarCategorias(String rol) throws Exception {
    mockMvc.perform(get("/api/categorias")
                    .with(user("usuario-prueba").roles(rol)))
            .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/categorias/1")
                    .with(user("usuario-prueba").roles(rol)))
            .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/categorias")
                    .with(user("usuario-prueba").roles(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO))
            .andExpect(status().isForbidden());

    mockMvc.perform(put("/api/categorias/1")
                    .with(user("usuario-prueba").roles(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO))
            .andExpect(status().isForbidden());

    mockMvc.perform(patch("/api/categorias/1/estado")
                    .with(user("usuario-prueba").roles(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activo\":false}"))
            .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/categorias/1")
                    .with(user("usuario-prueba").roles(rol)))
            .andExpect(status().isForbidden());

    verifyNoInteractions(categoriaService);
}

@Test
void sinSesionNoPuedeModificarCategorias() throws Exception {
    mockMvc.perform(post("/api/categorias")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO))
            .andExpect(status().isUnauthorized());

    mockMvc.perform(put("/api/categorias/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO))
            .andExpect(status().isUnauthorized());

    mockMvc.perform(patch("/api/categorias/1/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activo\":false}"))
            .andExpect(status().isUnauthorized());

    mockMvc.perform(delete("/api/categorias/1"))
            .andExpect(status().isUnauthorized());

    verifyNoInteractions(categoriaService);
}
}
