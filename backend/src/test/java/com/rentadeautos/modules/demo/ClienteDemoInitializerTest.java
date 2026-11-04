package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_clientes;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Import(ClienteDemoInitializerTest.Claves.class)
class ClienteDemoInitializerTest {
    @TestConfiguration
    static class Claves {
        @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }
    }
    private ClienteDemoInitializer seed;
    @Autowired ClienteRepository clientes;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void prepararSeedSinEjecutarloDuranteElArranque() {
        // La transacción del test engloba la carga; el runner no se registra como bean.
        seed = new ClienteDemoInitializer(clientes, encoder, "Demo-S504-test-2026");
    }

    private Cliente insertar(String correo, String licencia) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Registro previo");
        cliente.setApellidos("Conservado");
        cliente.setCorreo(correo);
        cliente.setNumeroLicencia(licencia);
        cliente.setTelefono("0000000000");
        cliente.setLicenciaVencimiento(LocalDate.now().plusYears(1));
        cliente.setActivo(true);
        return clientes.saveAndFlush(cliente);
    }

    @Test void creaOchoClientesActivosConCredencialesHashYLicenciaVigente() {
        seed.run();
        assertEquals(8, clientes.count());
        for (Cliente cliente : clientes.findAll()) {
            assertTrue(cliente.getCorreo().endsWith("@s504.example"));
            assertTrue(cliente.getNumeroLicencia().startsWith("S504-DEMO-"));
            assertTrue(cliente.getActivo());
            assertTrue(cliente.getLicenciaVencimiento().isAfter(LocalDate.now()));
            assertNotEquals("Demo-S504-test-2026", cliente.getPasswordHash());
            assertTrue(encoder.matches("Demo-S504-test-2026", cliente.getPasswordHash()));
        }
    }

    @Test void repetirNoDuplicaNiCambiaPassword() {
        seed.run();
        String hash = clientes.findByCorreoIgnoreCase("demo01@s504.example").orElseThrow().getPasswordHash();
        seed.run();
        assertEquals(8, clientes.count());
        assertEquals(hash, clientes.findByCorreoIgnoreCase("demo01@s504.example").orElseThrow().getPasswordHash());
    }

    @Test void conservaRegistrosAjenosAlSeed() {
        var previo = insertar("manual@example.test", "LIC-MANUAL");
        seed.run();
        assertEquals(9, clientes.count());
        var conservado = clientes.findById(previo.getId()).orElseThrow();
        assertEquals("Registro previo", conservado.getNombre());
        assertNull(conservado.getPasswordHash());
    }

    @Test void noRevierteCambiosSobreClientesDemoExistentes() {
        seed.run();
        var editado = clientes.findByCorreoIgnoreCase("demo01@s504.example").orElseThrow();
        editado.setNombre("Editado para la presentación");
        editado.setActivo(false);
        clientes.saveAndFlush(editado);
        seed.run();
        var conservado = clientes.findById(editado.getId()).orElseThrow();
        assertEquals("Editado para la presentación", conservado.getNombre());
        assertFalse(conservado.getActivo());
    }

    @Test void colisionDeCorreoSeDetectaAntesDeInsertarClientes() {
        insertar("demo08@s504.example", "LIC-AJENA");
        assertThrows(IllegalStateException.class, () -> seed.run());
        assertEquals(1, clientes.count());
    }

    @Test void colisionDeLicenciaSeDetectaAntesDeInsertarClientes() {
        insertar("manual@example.test", "S504-DEMO-08");
        assertThrows(IllegalStateException.class, () -> seed.run());
        assertEquals(1, clientes.count());
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "corta"})
    void passwordInvalidoNoInsertaDatos(String password) {
        var invalido = new ClienteDemoInitializer(clientes, encoder, password);
        assertThrows(IllegalStateException.class, () -> invalido.run());
        assertEquals(0, clientes.count());
    }
}
