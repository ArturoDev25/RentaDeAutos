package com.rentadeautos.modules.portal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rentadeautos.modules.auth.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S4-11: HTTP simulado, filtros JWT, servicios y persistencia reales.
 * No usa @MockBean ni usuarios simulados con @WithMockUser.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:portal_s411_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortalClienteIntegrationTest {
    private static final String REGISTRO = "/api/auth/cliente/registro";
    private static final String LOGIN = "/api/auth/cliente/login";
    private static final String RESERVAS = "/api/v1/cliente/reservaciones";
    private static final String HISTORIAL = RESERVAS + "/historial";
    private static final String PASSWORD = "Clave-Portal-123";
    private static final long AUTO_A = 940011L;
    private static final long AUTO_B = 940012L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Value("${app.jwt.secret}") String secretoPruebas;
    private LocalDateTime inicio;

    record Cuenta(long id, String correo, String token) {}

    @BeforeEach
    void catalogoConTarifaVigente() {
        inicio = LocalDate.now().plusDays(30).atTime(10, 0);
        jdbc.update("INSERT INTO categorias (id, nombre, deposito_base, activo) VALUES (940011, 'S4-11', 1000, TRUE)");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (?, 940011, ?, ?, 'Nissan', 'Versa', 2024, 1000, 'DISPONIBLE')
                """, AUTO_A, "S411-A", "1HGCM82633A004351");
        jdbc.update("""
                INSERT INTO vehiculos (id, categoria_id, placa, vin, marca, modelo, anio, kilometraje, estado)
                VALUES (?, 940011, ?, ?, 'Nissan', 'Sentra', 2024, 1000, 'DISPONIBLE')
                """, AUTO_B, "S411-B", "1HGCM82633A004352");
        jdbc.update("""
                INSERT INTO tarifas (categoria_id, precio_dia, cargo_atraso_dia, fecha_inicio, activo)
                VALUES (940011, 850, 100, ?, TRUE)
                """, LocalDate.now().minusDays(1));
    }

    @Test
    void registroExitosoGuardaCuentaActivaBCryptSinExponerCredenciales() throws Exception {
        long antes = contar("clientes");
        JsonNode respuesta = cuerpo(enviarRegistro(datosRegistro("alta")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist()));
        long id = respuesta.path("data").path("id").asLong();
        em.clear();
        String hash = jdbc.queryForObject("SELECT password_hash FROM clientes WHERE id = ?", String.class, id);
        assertTrue(encoder.matches(PASSWORD, hash));
        assertNotEquals(PASSWORD, hash);
        assertEquals(antes + 1, contar("clientes"));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT activo FROM clientes WHERE id = ?", Boolean.class, id));
        assertFalse(respuesta.toString().contains(hash));
        assertFalse(respuesta.toString().contains(PASSWORD));
    }

    @Test
    void correoDuplicadoDevuelve409YConservaLaCuentaOriginal() throws Exception {
        Cuenta a = cuenta("duplicado");
        var antes = jdbc.queryForMap("SELECT * FROM clientes WHERE id = ?", a.id());
        long cantidad = contar("clientes");
        var datos = datosRegistro("otra");
        datos.put("correo", " " + a.correo().toUpperCase(java.util.Locale.ROOT) + " ");
        datos.put("password", "Otra-Clave-456");
        enviarRegistro(datos).andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se pudo completar el registro con los datos proporcionados."));
        em.clear();
        assertEquals(cantidad, contar("clientes"));
        assertEquals(antes, jdbc.queryForMap("SELECT * FROM clientes WHERE id = ?", a.id()));
    }

    @Test
    void loginCorrectoGeneraJwtClienteYElFiltroLoAcepta() throws Exception {
        Cuenta a = cuenta("login");
        var claims = jwt.validarToken(a.token()).orElseThrow();
        assertEquals("CLIENTE", claims.get("rol", String.class));
        assertEquals("cliente:" + a.id(), claims.getSubject());
        mvc.perform(get(RESERVAS).header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "correo"})
    void loginIncorrectoDevuelve401SinToken(String campo) throws Exception {
        Cuenta a = cuenta("login-error");
        String correo = campo.equals("correo") ? "inexistente@example.test" : a.correo();
        String password = campo.equals("password") ? "Incorrecta-123" : PASSWORD;
        mvc.perform(post(LOGIN).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("correo", correo, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    void cambiarRolEnPayloadSinFirmarNoDaAccesoAdministrativo() throws Exception {
        Cuenta a = cuenta("alterado");
        String[] partes = a.token().split("\\.");
        ObjectNode payload = (ObjectNode) json.readTree(Base64.getUrlDecoder().decode(partes[1]));
        payload.put("rol", "ADMINISTRADOR");
        String token = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.writeValueAsBytes(payload)) + "." + partes[2];
        assertTrue(jwt.validarToken(token).isEmpty());
        mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoEsRechazadoPorServicioYFiltro() throws Exception {
        Cuenta a = cuenta("expirado");
        String token = new JwtService(secretoPruebas, -1).generarToken("cliente:" + a.id(), "CLIENTE");
        assertTrue(jwt.validarToken(token).isEmpty());
        mvc.perform(get(RESERVAS).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteDesactivadoPierdeAccesoAunqueTokenNoExpire() throws Exception {
        Cuenta a = cuenta("inactivo");
        jdbc.update("UPDATE clientes SET activo = FALSE WHERE id = ?", a.id());
        em.clear();
        mvc.perform(get(RESERVAS).header("Authorization", bearer(a)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitudSinTokenNoPuedeCrearReservaciones() throws Exception {
        long antes = contar("reservaciones");
        mvc.perform(post(RESERVAS).contentType("application/json")
                        .content(json.writeValueAsString(datosReserva(AUTO_A, inicio, inicio.plusDays(1)))))
                .andExpect(status().isUnauthorized());
        assertEquals(antes, contar("reservaciones"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/clientes", "/api/v1/vehicles", "/api/v1/categorias",
            "/api/v1/tarifas", "/api/v1/audit", "/api/reservaciones", "/api/v1/rentas/activas"
    })
    void clienteRecibe403EnConsultasAdministrativas(String ruta) throws Exception {
        Cuenta a = cuenta("prohibido");
        mvc.perform(get(ruta).header("Authorization", bearer(a)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void clienteNoPuedeDarDeAltaPersonalNiCambiarEstadoDeUsuarios() throws Exception {
        Cuenta a = cuenta("personal");
        var antes = jdbc.queryForList("SELECT * FROM usuarios_app ORDER BY id");
        mvc.perform(post("/api/auth/register").header("Authorization", bearer(a))
                        .contentType("application/json").content("""
                                {"nombre":"Intruso","correo":"intruso@example.test","password":"Clave123!"}
                                """))
                .andExpect(status().isForbidden());
        long empleado = empleadoId();
        mvc.perform(patch("/api/admin/usuarios/" + empleado + "/estado")
                        .header("Authorization", bearer(a)).contentType("application/json")
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
        assertEquals(antes, jdbc.queryForList("SELECT * FROM usuarios_app ORDER BY id"));
    }

    @Test
    void personalAutorizadoConservaAccesoConJwtReal() throws Exception {
        long rol = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR'", Long.class);
        jdbc.update("""
                INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
                VALUES (?, 'Personal S4-11', 'personal-s411@example.test', ?, TRUE)
                """, rol, encoder.encode(PASSWORD));
        JsonNode respuesta = cuerpo(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("correo", "personal-s411@example.test", "password", PASSWORD))))
                .andExpect(status().isOk()));
        String token = respuesta.path("data").path("accessToken").asText();
        mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get(RESERVAS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservaValidaUsaIdentidadYPrecioDelServidorAunqueElCuerpoLosSuplante() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        var datos = datosReserva(AUTO_A, inicio, inicio.plusHours(25));
        datos.put("clienteId", b.id());
        datos.put("cliente_id", b.id());
        datos.put("creadoPorId", empleadoId());
        datos.put("creadoPorClienteId", b.id());
        datos.put("estado", "CONFIRMADA");
        datos.put("tarifaDia", 1);
        datos.put("totalEstimado", 1);
        long id = cuerpo(enviarReserva(a, datos).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.data.tarifaDia").value(850))
                .andExpect(jsonPath("$.data.totalEstimado").value(1700))
                .andExpect(jsonPath("$.data.creadoPorId").doesNotExist()))
                .path("data").path("id").asLong();
        em.clear();
        assertEquals(a.id(), jdbc.queryForObject("SELECT cliente_id FROM reservaciones WHERE id = ?", Long.class, id));
        assertEquals(a.id(), jdbc.queryForObject("SELECT creado_por_cliente_id FROM reservaciones WHERE id = ?", Long.class, id));
        assertNull(jdbc.queryForObject("SELECT creado_por_id FROM reservaciones WHERE id = ?", Long.class, id));
    }

    @Test
    void listasYDetallesAislanAmbosClientesAunqueSeEnvieOtroId() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        long ra = reservar(a, AUTO_A, inicio, inicio.plusDays(1));
        long rb = reservar(b, AUTO_B, inicio, inicio.plusDays(1));
        listaPropia(a, b.id(), ra);
        listaPropia(b, a.id(), rb);
        mvc.perform(get(RESERVAS + "/" + ra).header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(ra));
        String ajena = mvc.perform(get(RESERVAS + "/" + ra).header("Authorization", bearer(b)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inexistente = mvc.perform(get(RESERVAS + "/9223372036854775807").header("Authorization", bearer(b)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(json.readTree(inexistente), json.readTree(ajena));
        mvc.perform(get(RESERVAS + "/" + rb).header("Authorization", bearer(a)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelacionAjenaNoModificaDatos() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        long id = reservar(a, AUTO_A, inicio, inicio.plusDays(1));
        var reservaAntes = jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id);
        var autoAntes = jdbc.queryForMap("SELECT * FROM vehiculos WHERE id = ?", AUTO_A);
        mvc.perform(patch(RESERVAS + "/" + id + "/cancelar").header("Authorization", bearer(b)))
                .andExpect(status().isNotFound());
        assertEquals(reservaAntes, jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id));
        assertEquals(autoAntes, jdbc.queryForMap("SELECT * FROM vehiculos WHERE id = ?", AUTO_A));
    }

    @Test
    void cancelacionPropiaPersisteYLiberaElIntervalo() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        long id = reservar(a, AUTO_A, inicio, inicio.plusDays(1));
        mvc.perform(patch(RESERVAS + "/" + id + "/cancelar").header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estado").value("CANCELADA"));
        assertEquals("CANCELADA", jdbc.queryForObject("SELECT estado FROM reservaciones WHERE id = ?", String.class, id));
        reservar(b, AUTO_A, inicio, inicio.plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pasado", "fin-anterior", "fin-igual", "sin-inicio", "sin-fin", "formato"})
    void fechasInvalidasDevuelven400SinGuardar(String caso) throws Exception {
        Cuenta a = cuenta("fechas");
        var datos = datosReserva(AUTO_A, inicio, inicio.plusDays(1));
        switch (caso) {
            case "pasado" -> datos.put("fechaInicio", LocalDate.now().minusDays(1).atTime(10, 0).toString());
            case "fin-anterior" -> datos.put("fechaFin", inicio.minusDays(1).toString());
            case "fin-igual" -> datos.put("fechaFin", inicio.toString());
            case "sin-inicio" -> datos.remove("fechaInicio");
            case "sin-fin" -> datos.remove("fechaFin");
            case "formato" -> datos.put("fechaInicio", "fecha-invalida");
            default -> throw new IllegalArgumentException(caso);
        }
        long antes = contar("reservaciones");
        var autoAntes = jdbc.queryForMap("SELECT * FROM vehiculos WHERE id = ?", AUTO_A);
        enviarReserva(a, datos).andExpect(status().isBadRequest());
        assertEquals(antes, contar("reservaciones"));
        assertEquals(autoAntes, jdbc.queryForMap("SELECT * FROM vehiculos WHERE id = ?", AUTO_A));
    }

    @ParameterizedTest
    @CsvSource({"0,48", "12,24", "-12,12", "24,72", "-12,72"})
    void traslapesEntreClientesDevuelven409SinGuardar(long desde, long hasta) throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        long id = reservar(a, AUTO_A, inicio, inicio.plusHours(48));
        long antes = contar("reservaciones");
        var reservaAntes = jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id);
        enviarReserva(b, datosReserva(AUTO_A, inicio.plusHours(desde), inicio.plusHours(hasta)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("reservación para esas fechas")));
        assertEquals(antes, contar("reservaciones"));
        assertEquals(reservaAntes, jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id));
    }

    @Test
    void intervalosContiguosNoSeConfundenConTraslape() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        reservar(a, AUTO_A, inicio, inicio.plusDays(1));
        reservar(b, AUTO_A, inicio.plusDays(1), inicio.plusDays(2));
        assertEquals(2, contar("reservaciones"));
    }

    @Test
    void historialSoloDevuelveRentasPropiasConEntregasRealesEnBase() throws Exception {
        Cuenta a = cuenta("a");
        Cuenta b = cuenta("b");
        long ra = prepararRenta(a, AUTO_A);
        long rb = prepararRenta(b, AUTO_B);
        historialPropio(a, b.id(), ra);
        historialPropio(b, a.id(), rb);
        mvc.perform(get(RESERVAS + "/" + rb).header("Authorization", bearer(a)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void historialEsSoloLecturaYNoModificaRentas(String metodo) throws Exception {
        Cuenta a = cuenta("rentas");
        prepararRenta(a, AUTO_A);
        var reservasAntes = jdbc.queryForList("SELECT * FROM reservaciones ORDER BY id");
        var entregasAntes = jdbc.queryForList("SELECT * FROM entregas ORDER BY id");
        mvc.perform(request(HttpMethod.valueOf(metodo), HISTORIAL)
                        .header("Authorization", bearer(a)).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertEquals(reservasAntes, jdbc.queryForList("SELECT * FROM reservaciones ORDER BY id"));
        assertEquals(entregasAntes, jdbc.queryForList("SELECT * FROM entregas ORDER BY id"));
    }

    @Test
    void rentaEnCursoNoPuedeCancelarseDesdeElPortal() throws Exception {
        Cuenta a = cuenta("en-curso");
        long id = prepararRenta(a, AUTO_A);
        var antes = jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id);
        mvc.perform(patch(RESERVAS + "/" + id + "/cancelar").header("Authorization", bearer(a)))
                .andExpect(status().isConflict());
        assertEquals(antes, jdbc.queryForMap("SELECT * FROM reservaciones WHERE id = ?", id));
        assertEquals("RENTADO", jdbc.queryForObject("SELECT estado FROM vehiculos WHERE id = ?", String.class, AUTO_A));
    }

    private Map<String, Object> datosRegistro(String sufijo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Cliente");
        datos.put("apellidos", "Prueba " + sufijo);
        datos.put("correo", "s411-" + sufijo + "@example.test");
        datos.put("password", PASSWORD);
        datos.put("telefono", "8441234567");
        datos.put("numeroLicencia", "S411-" + sufijo);
        datos.put("licenciaVencimiento", LocalDate.now().plusYears(2).toString());
        return datos;
    }

    private ResultActions enviarRegistro(Map<String, Object> datos) throws Exception {
        return mvc.perform(post(REGISTRO).contentType("application/json").content(json.writeValueAsString(datos)));
    }

    private Cuenta cuenta(String sufijo) throws Exception {
        var datos = datosRegistro(sufijo);
        long id = cuerpo(enviarRegistro(datos).andExpect(status().isCreated())).path("data").path("id").asLong();
        String correo = datos.get("correo").toString();
        JsonNode login = cuerpo(mvc.perform(post(LOGIN).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("correo", correo, "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.rol").value("CLIENTE")));
        String token = login.path("data").path("accessToken").asText();
        assertFalse(token.isBlank());
        return new Cuenta(id, correo, token);
    }

    private Map<String, Object> datosReserva(long auto, LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("vehiculoId", auto);
        datos.put("fechaInicio", desde.toString());
        datos.put("fechaFin", hasta.toString());
        return datos;
    }

    private ResultActions enviarReserva(Cuenta cuenta, Map<String, Object> datos) throws Exception {
        return mvc.perform(post(RESERVAS).header("Authorization", bearer(cuenta))
                .contentType("application/json").content(json.writeValueAsString(datos)));
    }

    private long reservar(Cuenta cuenta, long auto, LocalDateTime desde, LocalDateTime hasta) throws Exception {
        return cuerpo(enviarReserva(cuenta, datosReserva(auto, desde, hasta)).andExpect(status().isCreated()))
                .path("data").path("id").asLong();
    }

    private void listaPropia(Cuenta cuenta, long otroCliente, long reserva) throws Exception {
        mvc.perform(get(RESERVAS).header("Authorization", bearer(cuenta))
                        .param("clienteId", Long.toString(otroCliente)).param("cliente_id", Long.toString(otroCliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(reserva));
    }

    // Fixture persistida para probar consulta y aislamiento; no sustituye pruebas del flujo de entrega.
    private long prepararRenta(Cuenta cuenta, long auto) throws Exception {
        long id = reservar(cuenta, auto, inicio, inicio.plusDays(1));
        LocalDateTime entrega = LocalDate.now().minusDays(1).atTime(10, 0);
        jdbc.update("UPDATE reservaciones SET estado = 'EN_CURSO', fecha_inicio = ?, fecha_fin = ? WHERE id = ?",
                entrega, entrega.plusDays(3), id);
        jdbc.update("UPDATE vehiculos SET estado = 'RENTADO' WHERE id = ?", auto);
        jdbc.update("""
                INSERT INTO entregas (reservacion_id, entregado_por_id, fecha_entrega,
                    kilometraje_salida, combustible_salida, condicion_salida)
                VALUES (?, ?, ?, 1000, 100, 'Sin daños')
                """, id, empleadoId(), entrega);
        return id;
    }

    private void historialPropio(Cuenta cuenta, long otroCliente, long reserva) throws Exception {
        mvc.perform(get(HISTORIAL).header("Authorization", bearer(cuenta))
                        .param("clienteId", Long.toString(otroCliente)).param("cliente_id", Long.toString(otroCliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reservacionId").value(reserva))
                .andExpect(jsonPath("$.data[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.data[0].entregadoPorId").doesNotExist())
                .andExpect(jsonPath("$.data[0].observaciones").doesNotExist());
    }

    private long empleadoId() {
        return jdbc.queryForObject("SELECT MIN(id) FROM usuarios_app WHERE activo = TRUE", Long.class);
    }

    private long contar(String tabla) {
        if (!List.of("clientes", "reservaciones").contains(tabla)) {
            throw new IllegalArgumentException("Tabla de prueba no permitida");
        }
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class);
    }

    private String bearer(Cuenta cuenta) {
        return "Bearer " + cuenta.token();
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
