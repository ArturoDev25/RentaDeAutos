package com.rentadeautos.modules.auth.controller;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtAuthenticationFilter;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.auth.security.SecurityConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PermisosPorRolTest.CatalogosDePruebaController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    PermisosPorRolTest.CatalogosDePruebaController.class
})
class PermisosPorRolTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioAppRepository usuarioRepository;

    private static final String[] METODOS = {
        "GET", "POST", "PUT", "PATCH", "DELETE"
    };

    /*
     * Orden de permisos:
     * GET, POST, PUT, PATCH, DELETE.
     *
     * Esta tabla expresa los resultados esperados.
     * No toma las reglas de SecurityConfig.
     */
    static Stream<Arguments> casosPorRol() {
        List<Arguments> casos = new ArrayList<>();

        agregar(casos, "ADMINISTRADOR", "clientes",   "SSSSS");
        agregar(casos, "ADMINISTRADOR", "vehiculos",  "SSSSS");
        agregar(casos, "ADMINISTRADOR", "categorias", "SSSSS");
        agregar(casos, "ADMINISTRADOR", "tarifas",    "SSSSS");

        agregar(casos, "AGENTE", "clientes",   "SSSSN");
        agregar(casos, "AGENTE", "vehiculos",  "SNNNN");
        agregar(casos, "AGENTE", "categorias", "SNNNN");
        agregar(casos, "AGENTE", "tarifas",    "SNNNN");

        agregar(casos, "SUPERVISOR", "clientes",   "SSSSN");
        agregar(casos, "SUPERVISOR", "vehiculos",  "SSSSN");
        agregar(casos, "SUPERVISOR", "categorias", "SSSSN");
        agregar(casos, "SUPERVISOR", "tarifas",    "SSSSN");

        agregar(casos, "AUDITOR", "clientes",   "SNNNN");
        agregar(casos, "AUDITOR", "vehiculos",  "SNNNN");
        agregar(casos, "AUDITOR", "categorias", "SNNNN");
        agregar(casos, "AUDITOR", "tarifas",    "SNNNN");

        return casos.stream();
    }

    private static void agregar(
            List<Arguments> casos,
            String rol,
            String recurso,
            String permisos) {

        for (int i = 0; i < METODOS.length; i++) {
            int esperado = permisos.charAt(i) == 'S' ? 200 : 403;

            casos.add(Arguments.of(
                rol,
                METODOS[i],
                ruta(recurso, METODOS[i]),
                esperado
            ));
        }
    }

    private static String ruta(String recurso, String metodo) {
        String base = "/api/v1/" + recurso;

        // Consultar listado y crear usan la ruta base.
        // Modificar y eliminar usan el identificador.
        return switch (metodo) {
            case "GET", "POST" -> base;
            default -> base + "/5";
        };
    }

    @ParameterizedTest(name = "{0}: {1} {2} devuelve {3}")
    @MethodSource("casosPorRol")
    void comprobarPermisos(
            String rol,
            String metodo,
            String ruta,
            int esperado) throws Exception {

        var resultado = mockMvc.perform(
            request(HttpMethod.valueOf(metodo), ruta)
                .with(user("usuario-prueba").roles(rol))
        ).andExpect(status().is(esperado));

        if (esperado == 200) {
            resultado.andExpect(content().string("operacion-permitida"));
        }
    }

    static Stream<Arguments> casosSinAutenticacion() {
        List<Arguments> casos = new ArrayList<>();

        for (String recurso :
                List.of("clientes", "vehiculos", "categorias", "tarifas")) {
            for (String metodo : METODOS) {
                casos.add(Arguments.of(metodo, ruta(recurso, metodo)));
            }
        }

        return casos.stream();
    }

    @ParameterizedTest(name = "Sin sesión: {0} {1} devuelve 401")
    @MethodSource("casosSinAutenticacion")
    void rechazarSinAutenticacion(
            String metodo,
            String ruta) throws Exception {

        mockMvc.perform(
            request(HttpMethod.valueOf(metodo), ruta)
        ).andExpect(status().isUnauthorized());
    }

    /*
     * Solo existe en src/test.
     * No se incluye como controlador del sistema en producción.
     */
    @RestController
    public static class CatalogosDePruebaController {

        @RequestMapping(
            path = {
                "/api/v1/clientes",
                "/api/v1/clientes/{id}",
                "/api/v1/vehiculos",
                "/api/v1/vehiculos/{id}",
                "/api/v1/categorias",
                "/api/v1/categorias/{id}",
                "/api/v1/tarifas",
                "/api/v1/tarifas/{id}"
            },
            method = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.PATCH,
                RequestMethod.DELETE
            }
        )
        public String ejecutar() {
            return "operacion-permitida";
        }
    }
}