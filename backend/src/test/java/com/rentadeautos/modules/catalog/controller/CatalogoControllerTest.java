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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.*;
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
        when(catalogo.listar(any(), any(), any(), any())).thenReturn(List.of(new VehiculoCatalogoResponse(
                1L, "Nissan", "Kicks", 2024, "Blanco", 2L, "SUV", new BigDecimal("850.00"), true)));
        mvc.perform(get(RUTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoria").value("SUV"))
                .andExpect(jsonPath("$.data[0].tarifaDia").value(850.0))
                .andExpect(jsonPath("$.data[0].tieneImagen").value(true))
                .andExpect(jsonPath("$.data[0].imagen").doesNotExist())
                .andExpect(jsonPath("$.data[0].placa").doesNotExist())
                .andExpect(jsonPath("$.data[0].vin").doesNotExist())
                .andExpect(jsonPath("$.data[0].kilometraje").doesNotExist());
    }

    @Test void sinResultadosDevuelve200YListaVacia() throws Exception {
        when(catalogo.listar(any(), any(), any(), any())).thenReturn(List.of());
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
    @Test void pasaLosFiltrosCombinadosAlRepositorio() throws Exception {
        when(catalogo.listar(any(), any(), any(), any())).thenReturn(List.of());
        mvc.perform(get(RUTA).param("categoriaId", "2")
                .param("precioMin", "500.00").param("precioMax", "1000.00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        verify(catalogo).listar(any(), eq(2L), eq(new BigDecimal("500.00")),
                eq(new BigDecimal("1000.00")));
    }

    @Test void rangoInvertidoDevuelve400() throws Exception {
        mvc.perform(get(RUTA).param("precioMin", "1000").param("precioMax", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(catalogo);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "categoriaId,0", "categoriaId,-1", "categoriaId,texto",
            "categoriaId,999999999999999999999", "precioMin,-1", "precioMax,-1",
            "precioMin,0.001", "precioMax,100000000", "precioMin,texto", "precioMax,NaN"})
    void filtrosInvalidosNoConsultanLaBase(String parametro, String valor) throws Exception {
        mvc.perform(get(RUTA).param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(catalogo);
    }

    @Test void admiteCeroYLimitesIguales() throws Exception {
        when(catalogo.listar(any(), any(), any(), any())).thenReturn(List.of());
        mvc.perform(get(RUTA).param("precioMin", "0").param("precioMax", "0"))
                .andExpect(status().isOk());
        verify(catalogo).listar(any(), isNull(), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO));
    }

    @Test void errorDeBaseDevuelve500SinExponerSql() throws Exception {
        when(catalogo.listar(any(), any(), any(), any())).thenThrow(
                new org.springframework.dao.DataAccessResourceFailureException("SELECT secreto FROM tabla_interna"));
        mvc.perform(get(RUTA)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Test void imagenEsPublicaYSeEntregaComoArchivo() throws Exception {
        when(catalogo.imagen(1L)).thenReturn(Optional.of(
                "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG)));
        mvc.perform(get(RUTA + "/1/imagen"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=600")));
    }

    @Test void imagenJpgSeEntregaComoJpeg() throws Exception {
        when(catalogo.imagen(1L)).thenReturn(Optional.of(
                "data:image/jpg;base64," + Base64.getEncoder().encodeToString(PNG)));
        mvc.perform(get(RUTA + "/1/imagen"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test void vehiculoSinImagenDevuelve404() throws Exception {
        when(catalogo.imagen(5L)).thenReturn(Optional.empty());
        mvc.perform(get(RUTA + "/5/imagen")).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=",
            "data:image/png;base64,@@@",
            "https://ejemplo.com/foto.png"})
    void contenidoQueNoEsImagenValidaDevuelve404(String guardado) throws Exception {
        when(catalogo.imagen(1L)).thenReturn(Optional.of(guardado));
        mvc.perform(get(RUTA + "/1/imagen")).andExpect(status().isNotFound());
    }

    @Test void idNoPositivoNoConsultaLaBase() throws Exception {
        mvc.perform(get(RUTA + "/0/imagen")).andExpect(status().isNotFound());
        verifyNoInteractions(catalogo);
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void nadiePuedeEscribirPorLaRutaDeImagen(String metodo) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), RUTA + "/1/imagen")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogo);
    }

    @Test void otrasRutasDelCatalogoSiguenBloqueadas() throws Exception {
        mvc.perform(get(RUTA + "/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(catalogo);
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void clienteTampocoPuedeEscribirPorElCatalogo(String metodo) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), RUTA)
                .with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogo);
    }

}
