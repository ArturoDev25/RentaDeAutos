package com.rentadeautos.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:registro_s402_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Transactional
class ClienteRegistroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClienteRepository clientes;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    private static final String RUTA = "/api/auth/cliente/registro";

    private Map<String, Object> datos() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Ana");
        datos.put("apellidos", "Registro");
        datos.put("correo", " NUEVO-S402@example.test ");
        datos.put("password", "Clave123!");
        datos.put("telefono", "8441234567");
        datos.put("numeroLicencia", "LIC-REGISTRO-S402");
        datos.put("licenciaVencimiento", LocalDate.now().plusYears(1).toString());
        return datos;
    }

    @Test void registraPersisteBCryptEIniciaSesionComoCliente() throws Exception {
        long antes = clientes.count();
        var datos = datos();
        // No puede elegir rol ni desactivar la cuenta desde el cuerpo.
        datos.put("rol", "ADMINISTRADOR");
        datos.put("activo", false);
        datos.put("passwordHash", "hash-controlado-por-solicitante");
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        em.clear();
        Cliente guardado = clientes.findByCorreoIgnoreCase("nuevo-s402@example.test").orElseThrow();
        assertEquals(antes + 1, clientes.count());
        assertTrue(guardado.getActivo());
        assertTrue(encoder.matches("Clave123!", guardado.getPasswordHash()));
        assertNotEquals("Clave123!", guardado.getPasswordHash());
        mvc.perform(post("/api/auth/cliente/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nuevo-s402@example.test\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.accessToken", not(emptyOrNullString())))
                .andExpect(content().string(not(containsString(guardado.getPasswordHash()))));
    }

    @Test void duplicadoNoReemplazaCuentaDeMostradorNiLaActiva() throws Exception {
        Cliente existente = new Cliente();
        existente.setNombre("Mostrador");
        existente.setApellidos("Original");
        existente.setCorreo("nuevo-s402@example.test");
        existente.setTelefono("8440000000");
        existente.setNumeroLicencia("LIC-ORIGINAL-S402");
        existente.setLicenciaVencimiento(LocalDate.now().plusYears(1));
        existente.setActivo(false);
        clientes.saveAndFlush(existente);
        long antes = clientes.count();
        Long id = existente.getId();
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se pudo completar el registro con los datos proporcionados."));
        em.clear();
        Cliente despues = clientes.findById(id).orElseThrow();
        assertEquals(antes, clientes.count());
        assertNull(despues.getPasswordHash());
        assertFalse(despues.getActivo());
        assertEquals("Mostrador", despues.getNombre());
        assertEquals("LIC-ORIGINAL-S402", despues.getNumeroLicencia());
    }

    @Test void datosInvalidosNoCreanFilas() throws Exception {
        long antes = clientes.count();
        var datos = datos();
        datos.put("password", "123");
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isBadRequest());
        assertEquals(antes, clientes.count());
    }
}
