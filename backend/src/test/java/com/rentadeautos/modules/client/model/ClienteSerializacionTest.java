package com.rentadeautos.modules.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteSerializacionTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("La serialización JSON de Cliente NUNCA debe incluir passwordHash ni su valor BCrypt")
    void serializacionClienteNoIncluyePasswordHash() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombre("Carlos");
        cliente.setApellidos("Mendoza");
        cliente.setCorreo("carlos.mendoza@email.com");
        cliente.setPasswordHash("$2a$10$w8qI.O7a4LpS77t68x9wfehC3z6Nn4ZcM1s2rL/4Q7w90/u8P6pfe");
        cliente.setTelefono("8441234567");
        cliente.setNumeroLicencia("COA-98765");
        cliente.setLicenciaVencimiento(LocalDate.now().plusYears(2));
        cliente.setActivo(true);

        String json = objectMapper.writeValueAsString(cliente);

        assertFalse(json.contains("passwordHash"), "El JSON resultante de Cliente no debe contener el campo passwordHash");
        assertFalse(json.contains("password_hash"), "El JSON resultante de Cliente no debe contener el campo password_hash");
        assertFalse(json.contains("$2a$10$"), "El JSON resultante de Cliente no debe contener el valor del hash BCrypt");
    }

    @Test
    @DisplayName("El método toString() de Cliente no debe imprimir el passwordHash por seguridad en logs")
    void toStringNoImprimePasswordHash() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Laura");
        cliente.setApellidos("García");
        cliente.setCorreo("laura@email.com");
        cliente.setPasswordHash("$2a$10$ficticioHashDePrueba1234567890abcdefghijklmnopqrstu");

        String str = cliente.toString();

        assertFalse(str.contains(cliente.getPasswordHash()), "toString() de Cliente nunca debe exponer el hash de contraseña");
        assertFalse(str.contains("passwordHash"), "toString() de Cliente no debe listar el atributo passwordHash");
        assertTrue(str.contains("tieneCuentaPortal=true"), "toString() debe indicar el estado informativo tieneCuentaPortal");
    }

    @Test
    @DisplayName("ClienteResponse no debe tener ningún componente o campo relacionado con contraseña")
    void clienteResponseNoTieneCampoDePassword() {
        RecordComponent[] components = ClienteResponse.class.getRecordComponents();
        boolean hasPassword = Arrays.stream(components)
                .anyMatch(c -> c.getName().toLowerCase().contains("password") || c.getName().toLowerCase().contains("pass"));

        assertFalse(hasPassword, "El DTO ClienteResponse de la API no debe exponer campos de contraseña");
    }
}
