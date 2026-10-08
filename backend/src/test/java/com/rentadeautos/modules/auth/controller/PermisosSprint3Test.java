package com.rentadeautos.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import com.rentadeautos.modules.devolucion.controller.DevolucionController;
import com.rentadeautos.modules.devolucion.dto.DevolucionRequest;
import com.rentadeautos.modules.devolucion.service.DevolucionService;
import com.rentadeautos.modules.rental.controller.EntregaController;
import com.rentadeautos.modules.rental.controller.RentaActivaController;
import com.rentadeautos.modules.rental.dto.EntregaRequest;
import com.rentadeautos.modules.rental.service.EntregaService;
import com.rentadeautos.modules.rental.service.RentaActivaService;
import com.rentadeautos.modules.reservation.controller.ReservacionController;
import com.rentadeautos.modules.reservation.dto.ReservacionRequest;
import com.rentadeautos.modules.reservation.service.ReservacionService;
import com.rentadeautos.modules.vehicle.controller.TarifaController;
import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S3-21: matriz de permisos sobre controladores y seguridad reales.
 * Los servicios estan simulados: un rechazo debe ocurrir antes de invocarlos.
 * No conecta con MySQL ni modifica usuarios o datos de demostracion.
 * La matriz esperada es explicita e independiente de SecurityConfig.
 */
@WebMvcTest(controllers = {
    ReservacionController.class, TarifaController.class, EntregaController.class,
    DevolucionController.class, RentaActivaController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PermisosSprint3Test {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean ReservacionService reservaciones;
    @MockBean TarifaService tarifas;
    @MockBean EntregaService entregas;
    @MockBean DevolucionService devoluciones;
    @MockBean RentaActivaService rentasActivas;
    @MockBean JwtService jwt;
    @MockBean UsuarioAppRepository usuarios;

    private static final Set<String> CONSULTA = Set.of(
            "ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR");
    private static final Set<String> OPERACION = Set.of(
            "ADMINISTRADOR", "AGENTE", "SUPERVISOR");
    private static final Set<String> GESTION_TARIFAS = Set.of(
            "ADMINISTRADOR", "SUPERVISOR");

    private static final String RESERVACION = """
            {"clienteId":2,"vehiculoId":3,"fechaInicio":"2030-01-01T10:00:00",
             "fechaFin":"2030-01-03T10:00:00","observaciones":"Prueba S3-21"}
            """;
    private static final String TARIFA = """
            {"categoriaId":2,"precioDia":850.00,"cargoAtrasoDia":100.00,
             "fechaInicio":"2030-01-01","fechaFin":"2030-12-31"}
            """;
    private static final String ENTREGA = """
            {"reservacionId":8,"kilometrajeSalida":15000.0,"combustibleSalida":75.00,
             "condicionSalida":"Sin golpes","observaciones":"Prueba S3-21"}
            """;
    private static final String DEVOLUCION = """
            {"entregaId":8,"kilometrajeEntrada":15500.0,"combustibleEntrada":75.00,
             "condicionEntrada":"Sin golpes","cargoDanos":0,
             "observaciones":"Prueba S3-21","requiereMantenimiento":false}
            """;

    enum Caso {
        LISTAR_RESERVACIONES("GET", "/api/reservaciones", null, 200, CONSULTA),
        OPCIONES_RESERVACION("GET", "/api/reservaciones/opciones", null, 200, CONSULTA),
        DETALLE_RESERVACION("GET", "/api/reservaciones/8", null, 200, CONSULTA),
        CREAR_RESERVACION("POST", "/api/reservaciones", RESERVACION, 201, OPERACION),
        EDITAR_RESERVACION("PUT", "/api/reservaciones/8", RESERVACION, 200, OPERACION),
        CONFIRMAR_RESERVACION("POST", "/api/reservaciones/8/confirmar", null, 200, OPERACION),
        CANCELAR_RESERVACION("PATCH", "/api/reservaciones/8/cancelar", null, 200, OPERACION),
        LISTAR_TARIFAS("GET", "/api/v1/tarifas", null, 200, CONSULTA),
        DETALLE_TARIFA("GET", "/api/v1/tarifas/8", null, 200, CONSULTA),
        CREAR_TARIFA("POST", "/api/v1/tarifas", TARIFA, 201, GESTION_TARIFAS),
        EDITAR_TARIFA("PUT", "/api/v1/tarifas/8", TARIFA, 200, GESTION_TARIFAS),
        DESACTIVAR_TARIFA("PATCH", "/api/v1/tarifas/8/desactivar", null, 200, GESTION_TARIFAS),
        REGISTRAR_ENTREGA("POST", "/api/v1/entregas", ENTREGA, 201, OPERACION),
        CONSULTAR_ENTREGA("GET", "/api/v1/entregas/reservacion/8", null, 200, CONSULTA),
        REGISTRAR_DEVOLUCION("POST", "/api/v1/devoluciones", DEVOLUCION, 201, OPERACION),
        LISTAR_RENTAS_ACTIVAS("GET", "/api/v1/rentas/activas", null, 200, CONSULTA);

        final String metodo;
        final String ruta;
        final String cuerpo;
        final int estadoPermitido;
        final Set<String> rolesPermitidos;

        Caso(String metodo, String ruta, String cuerpo, int estadoPermitido,
                Set<String> rolesPermitidos) {
            this.metodo = metodo;
            this.ruta = ruta;
            this.cuerpo = cuerpo;
            this.estadoPermitido = estadoPermitido;
            this.rolesPermitidos = rolesPermitidos;
        }
    }

    static Stream<Arguments> matrizDeRoles() {
        // CLIENTE y ROL_DESCONOCIDO son autoridades de prueba sin acceso al panel.
        return Arrays.stream(Caso.values()).flatMap(caso -> Stream.of(
                "ADMINISTRADOR", "AGENTE", "SUPERVISOR", "AUDITOR",
                "CLIENTE", "ROL_DESCONOCIDO").map(rol -> Arguments.of(caso, rol)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("matrizDeRoles")
    void comprobarPermisosYAccesoAlServicio(Caso caso, String rol) throws Exception {
        boolean permitido = caso.rolesPermitidos.contains(rol);
        var resultado = mvc.perform(solicitud(caso).with(user("operador").roles(rol)))
                .andExpect(status().is(permitido ? caso.estadoPermitido : 403))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(permitido));

        if (permitido) {
            verificarOperacion(caso);
            verifyNoMoreInteractions(reservaciones, tarifas, entregas, devoluciones, rentasActivas);
        } else {
            resultado.andExpect(jsonPath("$.message").value("No tiene permisos para esta operación"));
            verificarServiciosSinInvocar();
        }
    }

    @ParameterizedTest(name = "Sin sesion: {0} -> 401")
    @EnumSource(Caso.class)
    void sinAutenticacionNoAccedeNiInvocaServicios(Caso caso) throws Exception {
        mvc.perform(solicitud(caso))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No autenticado"));
        verificarServiciosSinInvocar();
    }

    @ParameterizedTest(name = "Sin autoridades: {0} -> 403")
    @EnumSource(Caso.class)
    void autenticadoSinAutoridadesNoAccede(Caso caso) throws Exception {
        mvc.perform(solicitud(caso)
                        .with(user("sin-permisos").authorities(Collections.emptyList())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No tiene permisos para esta operación"));
        verificarServiciosSinInvocar();
    }

    @ParameterizedTest(name = "Token rechazado: {0} -> 401")
    @EnumSource(Caso.class)
    void tokenRechazadoNoAccedeNiBuscaUsuario(Caso caso) throws Exception {
        // Simula el resultado del validador; no prueba criptografia ni expiracion JWT.
        when(jwt.validarToken("token-invalido-s3-21")).thenReturn(Optional.empty());

        mvc.perform(solicitud(caso).header("Authorization", "Bearer token-invalido-s3-21"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No autenticado"));

        verify(jwt).validarToken("token-invalido-s3-21");
        verifyNoInteractions(usuarios);
        verificarServiciosSinInvocar();
    }

    private MockHttpServletRequestBuilder solicitud(Caso caso) {
        var solicitud = request(HttpMethod.valueOf(caso.metodo), caso.ruta);
        solicitud.with(req -> { req.setRemoteAddr("127.0.0.1"); return req; });
        if (caso.cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(caso.cuerpo);
        }
        return solicitud;
    }

    private void verificarServiciosSinInvocar() {
        // Ninguna ruta de escritura de estos controladores debe alcanzar su servicio.
        verifyNoInteractions(reservaciones, tarifas, entregas, devoluciones, rentasActivas);
    }

    private void verificarOperacion(Caso caso) throws Exception {
        // Verifica el metodo y sus argumentos, no solo un HTTP 2xx.
        switch (caso) {
            case LISTAR_RESERVACIONES -> verify(reservaciones).listar();
            case OPCIONES_RESERVACION -> verify(reservaciones).opciones();
            case DETALLE_RESERVACION -> verify(reservaciones).obtener(8L);
            case CREAR_RESERVACION -> verify(reservaciones).crear(
                    mapper.readValue(RESERVACION, ReservacionRequest.class));
            case EDITAR_RESERVACION -> verify(reservaciones).editar(8L,
                    mapper.readValue(RESERVACION, ReservacionRequest.class));
            case CONFIRMAR_RESERVACION -> verify(reservaciones).confirmar(8L);
            case CANCELAR_RESERVACION -> verify(reservaciones).cancelar(8L);
            case LISTAR_TARIFAS -> verify(tarifas).listar();
            case DETALLE_TARIFA -> verify(tarifas).obtener(8L);
            case CREAR_TARIFA -> verify(tarifas).crear(mapper.readValue(TARIFA, TarifaRequest.class));
            case EDITAR_TARIFA -> verify(tarifas).editar(8L, mapper.readValue(TARIFA, TarifaRequest.class));
            case DESACTIVAR_TARIFA -> verify(tarifas).desactivar(8L);
            case REGISTRAR_ENTREGA -> verify(entregas).registrar(
                    mapper.readValue(ENTREGA, EntregaRequest.class), "127.0.0.1");
            case CONSULTAR_ENTREGA -> verify(entregas).obtenerPorReservacion(8L);
            case REGISTRAR_DEVOLUCION -> verify(devoluciones).registrar(
                    mapper.readValue(DEVOLUCION, DevolucionRequest.class), "127.0.0.1");
            case LISTAR_RENTAS_ACTIVAS -> verify(rentasActivas).listar();
        }
    }
}
