package com.rentadeautos.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.auth.dto.ClienteRegistroRequest;
import com.rentadeautos.modules.auth.exception.ClienteRegistroDuplicadoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.SQLException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteRegistroServiceTest {
    @Mock ClienteRepository clientes;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private ClienteRegistroService servicio;

    @BeforeEach void preparar() {
        servicio = new ClienteRegistroService(clientes, encoder);
    }

    private ClienteRegistroRequest datos(String password) {
        return new ClienteRegistroRequest(" Ana ", " Prueba ", " ANA@example.test ",
                password, "8441234567", " lic-s4-02 ", LocalDate.now().plusYears(1), null);
    }

    @Test void altaActivaConNormalizacionYBCryptSinRecortarPassword() {
        when(clientes.saveAndFlush(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            assertEquals("ana@example.test", c.getCorreo());
            assertEquals("LIC-S4-02", c.getNumeroLicencia());
            assertEquals("Ana", c.getNombre());
            assertTrue(c.getActivo());
            assertNotEquals(" Clave123! ", c.getPasswordHash());
            assertTrue(encoder.matches(" Clave123! ", c.getPasswordHash()));
            assertFalse(encoder.matches("Clave123!", c.getPasswordHash()));
            ReflectionTestUtils.setField(c, "id", 7L);
            return c;
        });
        var respuesta = servicio.registrar(datos(" Clave123! "));
        assertEquals(7L, respuesta.id());
        assertTrue(respuesta.activo());
        verify(clientes).existsByCorreoIgnoreCase("ana@example.test");
        verify(clientes).existsByNumeroLicenciaIgnoreCase("LIC-S4-02");
    }

    @Test void correoExistenteSeRechazaSinModificarCliente() {
        when(clientes.existsByCorreoIgnoreCase("ana@example.test")).thenReturn(true);
        assertThrows(ClienteRegistroDuplicadoException.class, () -> servicio.registrar(datos("Clave123!")));
        verify(clientes, never()).saveAndFlush(any());
        verify(clientes, never()).findByCorreoIgnoreCase(any());
    }

    @Test void licenciaExistenteSeRechazaSinGuardar() {
        when(clientes.existsByNumeroLicenciaIgnoreCase("LIC-S4-02")).thenReturn(true);
        assertThrows(ClienteRegistroDuplicadoException.class, () -> servicio.registrar(datos("Clave123!")));
        verify(clientes, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"uq_clientes_correo", "clientes.uq_clientes_licencia", "PUBLIC.UQ_CLIENTES_CORREO_INDEX_1"})
    void duplicadoConcurrenteSeTraduceSinRevelarSql(String constraint) {
        var causa = new ConstraintViolationException("SQL privado", new SQLException("duplicado"), constraint);
        when(clientes.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("detalle privado", causa));
        var error = assertThrows(ClienteRegistroDuplicadoException.class,
                () -> servicio.registrar(datos("Clave123!")));
        assertEquals("No se pudo completar el registro con los datos proporcionados.", error.getMessage());
        assertNull(error.getCause());
    }

    @Test void otroErrorDeIntegridadNoSeDisfrazaComoDuplicado() {
        var error = new DataIntegrityViolationException("otro error");
        when(clientes.saveAndFlush(any())).thenThrow(error);
        assertSame(error, assertThrows(DataIntegrityViolationException.class,
                () -> servicio.registrar(datos("Clave123!"))));
    }

    @Test void dtoNoExponePasswordAlSerializarNiEnToString() throws Exception {
        var datos = datos("Clave123!");
        var mapper = new ObjectMapper().findAndRegisterModules();
        String salida = mapper.writeValueAsString(datos);
        assertFalse(salida.contains("Clave123!"));
        assertFalse(salida.contains("password"));
        assertFalse(datos.toString().contains("Clave123!"));
    }
}
