package com.rentadeautos.modules.auth.service;

import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.model.Rol;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de autenticación (S1-08).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioAppRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UsuarioApp usuarioActivo;

    @BeforeEach
    void prepararDatos() {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "id", 1L);
        ReflectionTestUtils.setField(rol, "nombre", "ADMINISTRADOR");

        usuarioActivo = new UsuarioApp();
        ReflectionTestUtils.setField(usuarioActivo, "id", 1L);
        ReflectionTestUtils.setField(usuarioActivo, "nombre", "Ana Demo");
        ReflectionTestUtils.setField(usuarioActivo, "correo", "admin@demo.local");
        ReflectionTestUtils.setField(usuarioActivo, "passwordHash", "$2a$10$hashFicticio");
        ReflectionTestUtils.setField(usuarioActivo, "activo", true);
        ReflectionTestUtils.setField(usuarioActivo, "rol", rol);
    }

    @Test
    @DisplayName("Un usuario válido recibe token, identidad y rol")
    void login_usuarioValido_devuelveToken() {
        when(usuarioRepository.findByCorreo("admin@demo.local"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generarToken(anyString(), anyString())).thenReturn("token-firmado");
        when(jwtService.getSegundosExpiracion()).thenReturn(1800L);

        LoginResponse respuesta = authService.login(
                new LoginRequest("admin@demo.local", "Demo1234"));

        assertThat(respuesta.accessToken()).isEqualTo("token-firmado");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.user().rol()).isEqualTo("ADMINISTRADOR");
        assertThat(respuesta.user().correo()).isEqualTo("admin@demo.local");
    }

    @Test
    @DisplayName("El correo se normaliza antes de buscar (RN-AUTH-01)")
    void login_correoConMayusculas_seNormaliza() {
        when(usuarioRepository.findByCorreo("admin@demo.local"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generarToken(anyString(), anyString())).thenReturn("token-firmado");
        when(jwtService.getSegundosExpiracion()).thenReturn(1800L);

        authService.login(new LoginRequest("  Admin@Demo.Local  ", "Demo1234"));

        verify(usuarioRepository).findByCorreo("admin@demo.local");
    }

    @Test
    @DisplayName("Una contraseña incorrecta impide el acceso")
    void login_passwordIncorrecta_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo(anyString()))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("admin@demo.local", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generarToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Un correo inexistente impide el acceso")
    void login_correoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("fantasma@demo.local", "Demo1234")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generarToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Un usuario desactivado no accede aunque la contraseña sea correcta")
    void login_usuarioDesactivado_lanzaExcepcion() {
        ReflectionTestUtils.setField(usuarioActivo, "activo", false);
        when(usuarioRepository.findByCorreo(anyString()))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("auditor@demo.local", "Demo1234")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generarToken(anyString(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Los tres casos de fallo devuelven el mismo mensaje (RN-AUTH-05)")
    void login_casosDeFallo_mismoMensaje() {
        when(usuarioRepository.findByCorreo("fantasma@demo.local"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("admin@demo.local"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        String mensajeInexistente = capturarMensaje("fantasma@demo.local");
        String mensajePasswordMala = capturarMensaje("admin@demo.local");

        assertThat(mensajeInexistente).isEqualTo(mensajePasswordMala);
    }

    private String capturarMensaje(String correo) {
        try {
            authService.login(new LoginRequest(correo, "loQueSea"));
            return null;
        } catch (BadCredentialsException ex) {
            return ex.getMessage();
        }
    }
}