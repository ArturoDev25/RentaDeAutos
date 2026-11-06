package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed_s504_clientela;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
@Import(ClientelaDemoInitializerTest.Claves.class)
class ClientelaDemoInitializerTest {

    @TestConfiguration
    static class Claves {
        @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }
    }

    private static final String PASSWORD = "Demo-S504-test-2026";

    private ClientelaDemoInitializer seed;
    @Autowired ClienteRepository clientes;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void prepararSeed() {
        seed = new ClientelaDemoInitializer(clientes, encoder, PASSWORD);
    }

    private long contar(EstadoAprobacionCliente estado) {
        return clientes.findAll().stream().filter(c -> c.getEstadoAprobacion() == estado).count();
    }

    @Test
    void creaCincuentaClientesConLaDistribucionEsperada() {
        seed.run();
        assertEquals(50, clientes.count());
        assertEquals(6, contar(EstadoAprobacionCliente.PENDIENTE));
        assertEquals(4, contar(EstadoAprobacionCliente.RECHAZADO));
        // Activos = aprobados de portal + mostrador (estado nulo) = 40.
        long activos = contar(EstadoAprobacionCliente.APROBADO) + contar(null);
        assertEquals(40, activos);
    }

    @Test
    void losDatosParecenRealesNoDemo() {
        seed.run();
        for (Cliente c : clientes.findAll()) {
            assertNotNull(c.getCorreo());
            assertTrue(c.getCorreo().endsWith("@s504-ampliado.example"));
            assertFalse(c.getCorreo().toLowerCase().contains("demo"), "no debe decir 'demo'");
            assertFalse(c.getNombre().toLowerCase().contains("demo"));
            assertNotNull(c.getDireccion());
            assertTrue(c.getTelefono().length() >= 10);
        }
    }

    @Test
    void pendientesYRechazadosManejanElPortalCorrectamente() {
        seed.run();
        for (Cliente c : clientes.findAll()) {
            if (c.getEstadoAprobacion() == EstadoAprobacionCliente.PENDIENTE) {
                assertTrue(c.bloqueadoPorAprobacion());
                assertFalse(c.getActivo());
                assertTrue(encoder.matches(PASSWORD, c.getPasswordHash()));
            } else if (c.getEstadoAprobacion() == EstadoAprobacionCliente.RECHAZADO) {
                assertFalse(c.getActivo());
                assertNotNull(c.getFechaRevision());
            }
        }
    }

    @Test
    void repetirNoDuplica() {
        seed.run();
        seed.run();
        assertEquals(50, clientes.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "corta"})
    void passwordInvalidoNoInsertaDatos(String password) {
        var invalido = new ClientelaDemoInitializer(clientes, encoder, password);
        assertThrows(IllegalStateException.class, invalido::run);
        assertEquals(0, clientes.count());
    }
}
