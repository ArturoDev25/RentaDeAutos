package com.rentadeautos.modules.client.repository;

import com.rentadeautos.modules.client.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:client_repo_v5_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ClienteRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClienteRepository clienteRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Cliente crearClienteBase(String nombre, String correo, String licencia) {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setApellidos("Prueba");
        c.setCorreo(correo);
        c.setTelefono("8440001122");
        c.setNumeroLicencia(licencia);
        c.setLicenciaVencimiento(LocalDate.now().plusYears(1));
        c.setActivo(true);
        return c;
    }

    @Test
    @DisplayName("Cliente de mostrador sin contraseña se guarda y recupera con password_hash = NULL")
    void clienteMostradorSeGuardaConPasswordNull() {
        Cliente cliente = crearClienteBase("Juan Mostrador", "juan.mostrador@test.com", "LIC-MOST-01");
        cliente.setPasswordHash(null);

        Cliente guardado = clienteRepository.saveAndFlush(cliente);
        em.clear();

        Cliente recuperado = clienteRepository.findById(guardado.getId()).orElseThrow();
        assertNull(recuperado.getPasswordHash(), "El cliente de mostrador debe tener passwordHash = NULL");
        assertFalse(recuperado.tieneCuentaPortal(), "Un cliente con passwordHash NULL no tiene cuenta de portal");
    }

    @Test
    @DisplayName("Cliente con cuenta de portal se guarda con hash BCrypt y se recupera con findByCorreoIgnoreCase")
    void clientePortalConBCryptYBusquedaPorCorreo() {
        String rawPassword = "PasswordSeguro2026!";
        String encoded = passwordEncoder.encode(rawPassword);

        Cliente cliente = crearClienteBase("Elena Portal", "elena.portal@test.com", "LIC-PORT-02");
        cliente.setPasswordHash(encoded);

        clienteRepository.saveAndFlush(cliente);
        em.clear();

        // Búsqueda insensible a mayúsculas
        Optional<Cliente> opt = clienteRepository.findByCorreoIgnoreCase("ELENA.PORTAL@TEST.COM");
        assertTrue(opt.isPresent(), "Debe encontrar el cliente buscando por correo en mayúsculas");

        Cliente recuperado = opt.get();
        assertNotNull(recuperado.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, recuperado.getPasswordHash()),
                "El hash BCrypt almacenado debe coincidir con la contraseña original mediante matches()");
        assertTrue(recuperado.tieneCuentaPortal());
        assertTrue(clienteRepository.existsByCorreoIgnoreCase("elena.portal@test.com"));
    }

    @Test
    @DisplayName("Prueba negativa de unicidad: Intentar insertar o actualizar dos clientes con el mismo correo lanza DataIntegrityViolationException")
    void rechazaInsercionOActualizacionConCorreoDuplicado() {
        Cliente c1 = crearClienteBase("Primero", "duplicado@test.com", "LIC-DUP-01");
        clienteRepository.saveAndFlush(c1);

        Cliente c2 = crearClienteBase("Segundo", "duplicado@test.com", "LIC-DUP-02");
        assertThrows(DataIntegrityViolationException.class, () -> {
            clienteRepository.saveAndFlush(c2);
        }, "Debe violar la restricción UNIQUE uq_clientes_correo");
    }

    @Test
    @DisplayName("Múltiples clientes de mostrador pueden coexistir con correo = NULL")
    void multiplesClientesConCorreoNull() {
        Cliente c1 = crearClienteBase("Sin Correo Uno", null, "LIC-NOCORREO-01");
        Cliente c2 = crearClienteBase("Sin Correo Dos", null, "LIC-NOCORREO-02");

        clienteRepository.saveAndFlush(c1);
        clienteRepository.saveAndFlush(c2);
        em.clear();

        assertTrue(clienteRepository.existsById(c1.getId()));
        assertTrue(clienteRepository.existsById(c2.getId()));
    }

    @Test
    @DisplayName("Prueba negativa CHECK: Si se provee password_hash no nulo pero correo es NULL, debe violar chk_clientes_acceso_requiere_correo")
    void rechazaPasswordHashSinCorreo() {
        Cliente c = crearClienteBase("Invalido", null, "LIC-CHECK-CORREO");
        c.setPasswordHash(passwordEncoder.encode("PasswordValido123"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            clienteRepository.saveAndFlush(c);
        }, "Debe violar chk_clientes_acceso_requiere_correo al intentar registrar password sin correo");
    }

    @Test
    @DisplayName("Prueba negativa CHECK: Si el password_hash es una cadena corta (< 50 caracteres) o vacía, debe violar chk_clientes_password_hash")
    void rechazaPasswordHashCortoOFicticio() {
        Cliente c = crearClienteBase("InvalidoHash", "invalido@test.com", "LIC-CHECK-HASH");
        c.setPasswordHash("12345678"); // Menor a 50 caracteres

        assertThrows(DataIntegrityViolationException.class, () -> {
            clienteRepository.saveAndFlush(c);
        }, "Debe violar chk_clientes_password_hash al intentar guardar un hash menor a 50 caracteres");
    }
}
