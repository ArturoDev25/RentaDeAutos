package com.rentadeautos.modules.auth.service;

import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.exception.CuentaClienteNoAprobadaException;
import com.rentadeautos.modules.auth.security.JwtService;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** S4-03: pruebas del login de cliente (BCrypt y JWT reales, repositorio simulado). */
@ExtendWith(MockitoExtension.class)
class ClienteLoginServiceTest {

    private static final String PASSWORD = "Clave-Segura-123";

    @Mock ClienteRepository clientes;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwt = new JwtService("secreto-de-prueba-de-al-menos-32-caracteres-ok", 30);

    private ClienteLoginService servicio;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        servicio = new ClienteLoginService(clientes, encoder, jwt);

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", 7L);
        cliente.setNombre("Ana");
        cliente.setApellidos("Pérez");
        cliente.setCorreo("ana@demo.local");
        cliente.setPasswordHash(encoder.encode(PASSWORD));
        cliente.setActivo(true);
    }

    @Test
    @DisplayName("Credenciales válidas: devuelve token JWT con rol CLIENTE")
    void credencialesValidasDevuelvenTokenConRolCliente() {
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        Optional<LoginResponse> respuesta = servicio.login(new LoginRequest("ana@demo.local", PASSWORD));

        assertTrue(respuesta.isPresent());
        assertEquals("Bearer", respuesta.get().tokenType());
        assertEquals(1800, respuesta.get().expiresIn());
        assertNotNull(respuesta.get().user());

        Claims claims = jwt.validarToken(respuesta.get().accessToken()).orElseThrow();
        assertEquals("CLIENTE", claims.get("rol", String.class));
        assertEquals("cliente:7", claims.getSubject());
        // No parece un correo: nunca puede coincidir con un usuario del personal.
        assertFalse(claims.getSubject().contains("@"));
    }

    @Test
    @DisplayName("El correo se normaliza (espacios y mayúsculas)")
    void correoSeNormaliza() {
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("  ANA@Demo.Local  ", PASSWORD)).isPresent());
    }

    @Test
    @DisplayName("Contraseña incorrecta: se rechaza")
    void passwordIncorrectaSeRechaza() {
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", "otra-clave")).isEmpty());
    }

    @Test
    @DisplayName("Correo no registrado: se rechaza")
    void correoNoRegistradoSeRechaza() {
        when(clientes.findByCorreoIgnoreCase("nadie@demo.local")).thenReturn(Optional.empty());

        assertTrue(servicio.login(new LoginRequest("nadie@demo.local", PASSWORD)).isEmpty());
    }

    @Test
    @DisplayName("Cuenta inactiva: no puede iniciar sesión aunque la contraseña sea correcta")
    void cuentaInactivaNoPuedeEntrar() {
        cliente.setActivo(false);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", PASSWORD)).isEmpty());
    }

    @Test
    @DisplayName("Cliente de mostrador sin cuenta de portal (password_hash nulo): se rechaza")
    void clienteSinCuentaDePortalSeRechaza() {
        cliente.setPasswordHash(null);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", PASSWORD)).isEmpty());
    }

    // ── S5-09: aprobación de cuentas del portal ──

    @Test
    @DisplayName("S5-09: cuenta pendiente con contraseña correcta → excepción con mensaje de pendiente")
    void cuentaPendienteNoPuedeEntrarYSeExplicaElMotivo() {
        cliente.setActivo(false);
        cliente.setEstadoAprobacion(EstadoAprobacionCliente.PENDIENTE);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        CuentaClienteNoAprobadaException error = assertThrows(CuentaClienteNoAprobadaException.class,
                () -> servicio.login(new LoginRequest("ana@demo.local", PASSWORD)));
        assertEquals(EstadoAprobacionCliente.PENDIENTE, error.getEstado());
        assertEquals(CuentaClienteNoAprobadaException.MENSAJE_PENDIENTE, error.getMessage());
    }

    @Test
    @DisplayName("S5-09: cuenta rechazada con contraseña correcta → excepción con mensaje de rechazo")
    void cuentaRechazadaNoPuedeEntrar() {
        cliente.setActivo(false);
        cliente.setEstadoAprobacion(EstadoAprobacionCliente.RECHAZADO);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        CuentaClienteNoAprobadaException error = assertThrows(CuentaClienteNoAprobadaException.class,
                () -> servicio.login(new LoginRequest("ana@demo.local", PASSWORD)));
        assertEquals(CuentaClienteNoAprobadaException.MENSAJE_RECHAZADA, error.getMessage());
    }

    @Test
    @DisplayName("S5-09: cuenta pendiente con contraseña incorrecta → respuesta genérica (no revela el estado)")
    void cuentaPendienteConPasswordIncorrectaNoRevelaEstado() {
        cliente.setActivo(false);
        cliente.setEstadoAprobacion(EstadoAprobacionCliente.PENDIENTE);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", "otra-clave")).isEmpty());
    }

    @Test
    @DisplayName("S5-09: cuenta aprobada y activa → inicia sesión")
    void cuentaAprobadaPuedeEntrar() {
        cliente.setActivo(true);
        cliente.setEstadoAprobacion(EstadoAprobacionCliente.APROBADO);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", PASSWORD)).isPresent());
    }

    @Test
    @DisplayName("S5-09: cuenta aprobada pero desactivada después → se conserva el bloqueo por inactiva")
    void cuentaAprobadaPeroInactivaNoPuedeEntrar() {
        cliente.setActivo(false);
        cliente.setEstadoAprobacion(EstadoAprobacionCliente.APROBADO);
        when(clientes.findByCorreoIgnoreCase("ana@demo.local")).thenReturn(Optional.of(cliente));

        assertTrue(servicio.login(new LoginRequest("ana@demo.local", PASSWORD)).isEmpty());
    }
}
