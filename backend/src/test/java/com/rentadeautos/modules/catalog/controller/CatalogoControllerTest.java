package com.rentadeautos.modules.catalog.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.catalog.repository.CatalogoRepository;
import com.rentadeautos.modules.catalog.dto.VehiculoCatalogoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CatalogoControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CatalogoRepository catalogo;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;
    private static final String RUTA = "/api/v1/catalogo/vehiculos";

    @Test void consultaPublicaSoloExponeDatosDelCatalogo() throws Exception {
        when(catalogo.listar(any())).thenReturn(List.of(new VehiculoCatalogoResponse(
                1L, "Nissan", "Kicks", 2024, "Blanco", 2L, "SUV", new BigDecimal("850.00"))));
        mvc.perform(get(RUTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoria").value("SUV"))
                .andExpect(jsonPath("$.data[0].tarifaDia").value(850.0))
                .andExpect(jsonPath("$.data[0].placa").doesNotExist())
                .andExpect(jsonPath("$.data[0].vin").doesNotExist())
                .andExpect(jsonPath("$.data[0].kilometraje").doesNotExist());
    }

    @Test void sinResultadosDevuelve200YListaVacia() throws Exception {
        when(catalogo.listar(any())).thenReturn(List.of());
        mvc.perform(get(RUTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void niAdministradorPuedeEscribirPorElCatalogo(String metodo) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), RUTA)
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogo);
    }
}
