package com.rentadeautos.modules.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.auth.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S5-09: flujo completo con HTTP simulado, JWT real, Spring Security y Flyway (H2).
 * Registro pendiente → revisión del personal → inicio de sesión del cliente.
 *
 * <p>El personal usa las cuentas demo de V3, ya confirmadas en la base. Sus tokens se
 * firman con el JwtService real (el filtro valida firma, usuario y rol activos) en
 * lugar de pasar por /api/auth/login, porque ese login actualiza ultimo_acceso y
 * dejaría la fila bloqueada para la auditoría FALLIDO, que usa su propia transacción.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL_PREFIX:jdbc:h2:mem:}aprobacion_s509_db${TEST_DB_URL_SUFFIX:;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE}",
        "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
        "spring.datasource.username=${TEST_DB_USER:sa}",
        "spring.datasource.password=${TEST_DB_PASSWORD:}",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClienteAprobacionIntegrationTest {

    private static final String REGISTRO = "/api/auth/cliente/registro";
    private static final String LOGIN_CLIENTE = "/api/auth/cliente/login";
    private static final String PENDIENTES = "/api/v1/clientes/pendientes";
    private static final String PASSWORD_CLIENTE = "Clave-Portal-509";
    private static final String ADMIN = "admin@demo.local";
    private static final String AGENTE = "agente@demo.local";
    private static final String SUPERVISOR = "supervisor@demo.local";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    // ── Registro pendiente ─────────────────────────────────────────────────

    @Test
    @DisplayName("El registro del portal deja la cuenta inactiva, PENDIENTE y sin poder iniciar sesión")
    void registroQuedaPendienteYNoPuedeIniciarSesion() throws Exception {
        long id = registrar("pendiente");

        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM clientes WHERE id = ?", Boolean.class, id));
        assertEquals("PENDIENTE", estado(id));
        loginCliente("pendiente")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("pendiente de aprobación")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Contraseña incorrecta de una cuenta pendiente: 401 genérico, no revela el estado")
    void pendienteConPasswordIncorrectaRecibeMensajeGenerico() throws Exception {
        registrar("generico");
        mvc.perform(post(LOGIN_CLIENTE).contentType("application/json")
                        .content(json.writeValueAsString(Map.of(
                                "correo", correo("generico"), "password", "Otra-Clave-000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    // ── Consulta de pendientes ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /pendientes devuelve solo las cuentas pendientes con los datos para revisarlas")
    void listaDePendientesMuestraDatosParaRevision() throws Exception {
        long id = registrar("lista");
        long rechazado = registrar("lista-rechazada");
        jdbc.update("UPDATE clientes SET estado_aprobacion = 'RECHAZADO' WHERE id = ?", rechazado);
        em.clear();

        JsonNode lista = cuerpo(mvc.perform(get(PENDIENTES).header("Authorization", tokenPersonal(AGENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))).path("data");

        JsonNode fila = buscar(lista, id);
        assertNotNull(fila, "El cliente pendiente debe aparecer en la lista");
        assertEquals("PENDIENTE", fila.path("estadoAprobacion").asText());
        assertEquals(correo("lista"), fila.path("correo").asText());
        assertEquals("S509-LISTA", fila.path("numeroLicencia").asText());
        assertFalse(fila.path("activo").asBoolean());
        assertTrue(fila.path("fechaRegistro").isTextual(), "Debe incluir la fecha de solicitud");
        assertFalse(fila.has("passwordHash"));
        assertNull(buscar(lista, rechazado), "Las cuentas rechazadas no aparecen como pendientes");
    }

    // ── Aprobación ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {ADMIN, AGENTE, SUPERVISOR})
    @DisplayName("Admin, Agente y Supervisor aprueban: la cuenta queda activa y el cliente inicia sesión")
    void personalAutorizadoApruebaYElClienteEntra(String personal) throws Exception {
        String sufijo = "aprobar-" + personal.substring(0, personal.indexOf('@'));
        long id = registrar(sufijo);

        mvc.perform(patch("/api/v1/clientes/" + id + "/aprobar").header("Authorization", tokenPersonal(personal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.estadoAprobacion").value("APROBADO"))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.fechaRevision", not(emptyOrNullString())));
        em.clear();

        assertEquals("APROBADO", estado(id));
        loginCliente(sufijo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.accessToken", not(emptyOrNullString())));

        // Ya no aparece en la lista de pendientes.
        JsonNode lista = cuerpo(mvc.perform(get(PENDIENTES).header("Authorization", tokenPersonal(personal)))
                .andExpect(status().isOk())).path("data");
        assertNull(buscar(lista, id));

        // Auditoría: quién aprobó, qué cliente y resultado.
        Map<String, Object> evento = jdbc.queryForMap("""
                SELECT a.accion, a.resultado, u.correo
                FROM auditoria a JOIN usuarios_app u ON u.id = a.usuario_id
                WHERE a.entidad = 'CLIENTE' AND a.entidad_id = ?
                """, id);
        assertEquals("APROBAR_CLIENTE", evento.get("ACCION"));
        assertEquals("EXITOSO", evento.get("RESULTADO"));
        assertEquals(personal, evento.get("CORREO"));
    }

    @Test
    @DisplayName("Aprobar dos veces la misma cuenta responde 409")
    void aprobarDosVecesResponde409() throws Exception {
        long id = registrar("doble");
        String token = tokenPersonal(ADMIN);
        mvc.perform(patch("/api/v1/clientes/" + id + "/aprobar").header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/clientes/" + id + "/rechazar").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ya no está pendiente")));
        em.clear();
        assertEquals("APROBADO", estado(id));
    }

    @Test
    @DisplayName("Aprobar un cliente inexistente responde 404")
    void aprobarInexistenteResponde404() throws Exception {
        mvc.perform(patch("/api/v1/clientes/999999/aprobar").header("Authorization", tokenPersonal(ADMIN)))
                .andExpect(status().isNotFound());
    }

    // ── Rechazo ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rechazar: la cuenta permanece inhabilitada, se distingue de una pendiente y se audita")
    void rechazarMantieneLaCuentaInhabilitada() throws Exception {
        long id = registrar("rechazar");

        mvc.perform(patch("/api/v1/clientes/" + id + "/rechazar").header("Authorization", tokenPersonal(SUPERVISOR))
                        .contentType("application/json").content("{\"motivo\":\"Licencia ilegible\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estadoAprobacion").value("RECHAZADO"))
                .andExpect(jsonPath("$.data.activo").value(false));
        em.clear();

        assertEquals("RECHAZADO", estado(id));
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM clientes WHERE id = ?", Boolean.class, id));
        loginCliente("rechazar")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("rechazada")))
                .andExpect(jsonPath("$.data").doesNotExist());

        // La consulta general de clientes distingue la cuenta rechazada.
        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", tokenPersonal(AGENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estadoAprobacion").value("RECHAZADO"));

        String valoresNuevos = jdbc.queryForObject("""
                SELECT valores_nuevos FROM auditoria
                WHERE entidad = 'CLIENTE' AND entidad_id = ? AND accion = 'RECHAZAR_CLIENTE'
                  AND resultado = 'EXITOSO'
                """, String.class, id);
        assertTrue(valoresNuevos.contains("Licencia ilegible"));
        assertFalse(valoresNuevos.contains(PASSWORD_CLIENTE));
    }

    @Test
    @DisplayName("Una cuenta rechazada no se puede reactivar desde Clientes (saltaría la aprobación)")
    void rechazadaNoSePuedeReactivar() throws Exception {
        long id = registrar("reactivar");
        String token = tokenPersonal(ADMIN);
        mvc.perform(patch("/api/v1/clientes/" + id + "/rechazar").header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/clientes/" + id + "/activar").header("Authorization", token))
                .andExpect(status().isBadRequest());
        em.clear();
        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT activo FROM clientes WHERE id = ?", Boolean.class, id));
    }

    // ── Permisos ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("El Auditor no puede consultar pendientes, aprobar ni rechazar (403)")
    void auditorRecibe403() throws Exception {
        long id = registrar("auditor");
        String token = tokenAuditor();

        mvc.perform(get(PENDIENTES).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/clientes/" + id + "/aprobar").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/clientes/" + id + "/rechazar").header("Authorization", token))
                .andExpect(status().isForbidden());
        em.clear();
        assertEquals("PENDIENTE", estado(id));
    }

    @Test
    @DisplayName("Un cliente del portal no puede aprobar cuentas (403) y sin token se responde 401")
    void clienteYAnonimoNoPuedenAprobar() throws Exception {
        long aprobado = registrar("cliente-aprobado");
        mvc.perform(patch("/api/v1/clientes/" + aprobado + "/aprobar").header("Authorization", tokenPersonal(ADMIN)))
                .andExpect(status().isOk());
        String tokenCliente = "Bearer " + cuerpo(loginCliente("cliente-aprobado").andExpect(status().isOk()))
                .path("data").path("accessToken").asText();
        long id = registrar("objetivo");

        mvc.perform(get(PENDIENTES).header("Authorization", tokenCliente)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/clientes/" + id + "/aprobar").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/clientes/" + id + "/rechazar").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/clientes/" + id + "/aprobar")).andExpect(status().isUnauthorized());
        mvc.perform(get(PENDIENTES)).andExpect(status().isUnauthorized());
        em.clear();
        assertEquals("PENDIENTE", estado(id));
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private String correo(String sufijo) {
        return "s509-" + sufijo + "@example.test";
    }

    private long registrar(String sufijo) throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Cliente");
        datos.put("apellidos", "S509 " + sufijo);
        datos.put("correo", correo(sufijo));
        datos.put("password", PASSWORD_CLIENTE);
        datos.put("telefono", "8441234567");
        datos.put("numeroLicencia", "S509-" + sufijo);
        datos.put("licenciaVencimiento", LocalDate.now().plusYears(2).toString());
        long id = cuerpo(mvc.perform(post(REGISTRO).contentType("application/json")
                        .content(json.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activo").value(false))
                .andExpect(jsonPath("$.data.estadoAprobacion").value("PENDIENTE")))
                .path("data").path("id").asLong();
        em.clear();
        return id;
    }

    private ResultActions loginCliente(String sufijo) throws Exception {
        return mvc.perform(post(LOGIN_CLIENTE).contentType("application/json")
                .content(json.writeValueAsString(Map.of("correo", correo(sufijo), "password", PASSWORD_CLIENTE))));
    }

    /** Token real del personal: el filtro JWT valida firma, usuario activo y rol activo. */
    private String tokenPersonal(String correo) {
        String rol = jdbc.queryForObject("""
                SELECT r.nombre FROM usuarios_app u JOIN roles r ON r.id = u.rol_id
                WHERE u.correo = ?
                """, String.class, correo);
        return "Bearer " + jwt.generarToken(correo, rol);
    }

    /** El auditor demo de V3 está inactivo; se crea uno activo solo para esta prueba. */
    private String tokenAuditor() {
        long rol = jdbc.queryForObject("SELECT id FROM roles WHERE nombre = 'AUDITOR'", Long.class);
        jdbc.update("""
                INSERT INTO usuarios_app (rol_id, nombre, correo, password_hash, activo)
                VALUES (?, 'Auditor S5-09', 'auditor-s509@example.test', ?, TRUE)
                """, rol, encoder.encode("Clave-Auditor-509"));
        return tokenPersonal("auditor-s509@example.test");
    }

    private String estado(long id) {
        return jdbc.queryForObject("SELECT estado_aprobacion FROM clientes WHERE id = ?", String.class, id);
    }

    private static JsonNode buscar(JsonNode lista, long id) {
        for (JsonNode fila : lista) {
            if (fila.path("id").asLong() == id) {
                return fila;
            }
        }
        return null;
    }

    private JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
