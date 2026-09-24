package com.rentadeautos.modules.vehicle.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.vehicle.dto.FiltroVehiculos;
import com.rentadeautos.modules.vehicle.dto.VehiculoRequest;
import com.rentadeautos.modules.vehicle.dto.VehiculoResponse;
import com.rentadeautos.modules.vehicle.exception.VehiculoDuplicadoException;
import com.rentadeautos.modules.vehicle.exception.VehiculoInvalidoException;
import com.rentadeautos.modules.vehicle.exception.VehiculoNoEncontradoException;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.service.VehiculoService;
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
 * Comprueba los endpoints de la gestión de vehículos (S2-06),
 * incluidos los permisos por rol definidos en SecurityConfig.
 */
@WebMvcTest(VehiculoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class VehiculoControllerTest {

    private static final String URL = "/api/v1/vehiculos";

    private static final String CUERPO_VALIDO = """
            {
              "categoriaId": 1,
              "placa": "FAK-1234",
              "vin": "3N1AB7AP0FY123456",
              "marca": "Nissan",
              "modelo": "Versa",
              "anio": 2022,
              "color": "Blanco",
              "kilometraje": 15000.0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehiculoService vehiculoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioAppRepository usuarioRepository;

    private VehiculoResponse versa(EstadoVehiculo estado) {
        return new VehiculoResponse(1L, 1L, "Compacto", "FAK-1234", "3N1AB7AP0FY123456",
                "Nissan", "Versa", 2022, "Blanco", new BigDecimal("15000.0"), estado);
    }

    /** Cuerpo válido con un solo campo reemplazado. */
    private String cuerpoCon(String campo, String valorJson) {
        return CUERPO_VALIDO.replaceFirst(
                "\"" + campo + "\": [^,\\n]+", "\"" + campo + "\": " + valorJson);
    }

    // ---------- Consultar ----------

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("Cualquier rol puede consultar el listado de vehículos")
    void listarDevuelveVehiculos() throws Exception {
        when(vehiculoService.listar(FiltroVehiculos.sinFiltros()))
                .thenReturn(List.of(versa(EstadoVehiculo.DISPONIBLE)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].placa").value("FAK-1234"));
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Se puede filtrar el listado por estado y categoría")
    void listarConFiltros() throws Exception {
        FiltroVehiculos filtro =
                new FiltroVehiculos(null, EstadoVehiculo.DISPONIBLE, 1L, null, null);
        when(vehiculoService.listar(filtro))
                .thenReturn(List.of(versa(EstadoVehiculo.DISPONIBLE)));

        mockMvc.perform(get(URL).param("estado", "DISPONIBLE").param("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].estado").value("DISPONIBLE"));

        verify(vehiculoService).listar(filtro);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("Se puede buscar por texto y rango de años (S2-07)")
    void buscarPorTextoYAnios() throws Exception {
        FiltroVehiculos filtro = new FiltroVehiculos("versa", null, null, 2020, 2024);
        when(vehiculoService.listar(filtro))
                .thenReturn(List.of(versa(EstadoVehiculo.DISPONIBLE)));

        mockMvc.perform(get(URL)
                        .param("q", "versa")
                        .param("anioDesde", "2020")
                        .param("anioHasta", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelo").value("Versa"));

        verify(vehiculoService).listar(filtro);
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Una búsqueda inválida devuelve 400")
    void buscarInvalidoDevuelve400() throws Exception {
        FiltroVehiculos filtro = new FiltroVehiculos(null, null, null, 2024, 2020);
        when(vehiculoService.listar(filtro)).thenThrow(new VehiculoInvalidoException(
                "El año inicial no puede ser mayor que el año final"));

        mockMvc.perform(get(URL).param("anioDesde", "2024").param("anioHasta", "2020"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El año inicial no puede ser mayor que el año final"));
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Un año que no es número devuelve 400")
    void buscarConAnioNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get(URL).param("anioDesde", "dos mil"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Filtrar por un estado que no existe devuelve 400")
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(URL).param("estado", "VOLANDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Se puede consultar un vehículo por id")
    void obtenerDevuelveVehiculo() throws Exception {
        when(vehiculoService.obtener(1L)).thenReturn(versa(EstadoVehiculo.DISPONIBLE));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.vin").value("3N1AB7AP0FY123456"))
                .andExpect(jsonPath("$.data.categoriaNombre").value("Compacto"));
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Consultar un vehículo inexistente devuelve 404")
    void obtenerInexistenteDevuelve404() throws Exception {
        when(vehiculoService.obtener(99L)).thenThrow(new VehiculoNoEncontradoException());

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vehículo no encontrado"));
    }

    // ---------- Registrar ----------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede registrar un vehículo válido y queda DISPONIBLE")
    void crearDevuelve201() throws Exception {
        when(vehiculoService.crear(any(VehiculoRequest.class)))
                .thenReturn(versa(EstadoVehiculo.DISPONIBLE));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("DISPONIBLE"));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("El supervisor también puede registrar vehículos")
    void supervisorPuedeCrear() throws Exception {
        when(vehiculoService.crear(any(VehiculoRequest.class)))
                .thenReturn(versa(EstadoVehiculo.DISPONIBLE));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar sin placa devuelve 400")
    void crearSinPlacaDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("placa", "\"  \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con un VIN que no tiene 17 caracteres devuelve 400")
    void crearConVinCortoDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("vin", "\"3N1AB7AP0FY\"")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con un VIN que contiene I, O o Q devuelve 400")
    void crearConVinConLetrasProhibidasDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("vin", "\"3N1AB7AP0FY12345O\"")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con kilometraje negativo devuelve 400")
    void crearConKilometrajeNegativoDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("kilometraje", "-5")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con un año anterior a 2000 devuelve 400")
    void crearConAnioAntiguoDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("anio", "1995")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar sin categoría devuelve 400")
    void crearSinCategoriaDevuelve400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCon("categoriaId", "null")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con una categoría inactiva devuelve 400")
    void crearConCategoriaInactivaDevuelve400() throws Exception {
        when(vehiculoService.crear(any(VehiculoRequest.class)))
                .thenThrow(new VehiculoInvalidoException("La categoría indicada está inactiva"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La categoría indicada está inactiva"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con placa repetida devuelve 409")
    void crearConPlacaDuplicadaDevuelve409() throws Exception {
        when(vehiculoService.crear(any(VehiculoRequest.class)))
                .thenThrow(VehiculoDuplicadoException.placa());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(VehiculoDuplicadoException.PLACA_DUPLICADA));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Registrar con VIN repetido devuelve 409")
    void crearConVinDuplicadoDevuelve409() throws Exception {
        when(vehiculoService.crear(any(VehiculoRequest.class)))
                .thenThrow(VehiculoDuplicadoException.vin());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(VehiculoDuplicadoException.VIN_DUPLICADO));
    }

    // ---------- Editar ----------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Se puede editar un vehículo")
    void editarDevuelve200() throws Exception {
        when(vehiculoService.editar(eq(1L), any(VehiculoRequest.class)))
                .thenReturn(versa(EstadoVehiculo.DISPONIBLE));

        mockMvc.perform(put(URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelo").value("Versa"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Editar un vehículo inexistente devuelve 404")
    void editarInexistenteDevuelve404() throws Exception {
        when(vehiculoService.editar(eq(99L), any(VehiculoRequest.class)))
                .thenThrow(new VehiculoNoEncontradoException());

        mockMvc.perform(put(URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isNotFound());
    }

    // ---------- Cambiar estado ----------

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Se puede cambiar el estado de un vehículo")
    void cambiarEstadoDevuelve200() throws Exception {
        when(vehiculoService.cambiarEstado(1L, EstadoVehiculo.MANTENIMIENTO))
                .thenReturn(versa(EstadoVehiculo.MANTENIMIENTO));

        mockMvc.perform(patch(URL + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "estado": "MANTENIMIENTO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("MANTENIMIENTO"));

        verify(vehiculoService).cambiarEstado(1L, EstadoVehiculo.MANTENIMIENTO);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Cambiar a un estado que no existe devuelve 400")
    void cambiarEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(patch(URL + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "estado": "VOLANDO" }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Cambiar estado sin indicar el valor devuelve 400")
    void cambiarEstadoSinValorDevuelve400() throws Exception {
        mockMvc.perform(patch(URL + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    // ---------- Permisos ----------

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Un agente no puede registrar vehículos (403)")
    void agenteNoPuedeCrear() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("Un auditor no puede editar vehículos (403)")
    void auditorNoPuedeEditar() throws Exception {
        mockMvc.perform(put(URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @WithMockUser(roles = "AGENTE")
    @DisplayName("Un agente no puede cambiar el estado (403)")
    void agenteNoPuedeCambiarEstado() throws Exception {
        mockMvc.perform(patch(URL + "/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "estado": "BAJA" }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    @DisplayName("Usuario no autenticado recibe 401")
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("No existe eliminación física de vehículos")
    void noSePuedeEliminarUnVehiculo() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() >= 400,
                        "DELETE no debe ser una operación exitosa"));

        verifyNoInteractions(vehiculoService);
    }
}
