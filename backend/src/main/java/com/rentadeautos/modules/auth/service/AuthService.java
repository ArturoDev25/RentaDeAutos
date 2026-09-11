package com.rentadeautos.modules.auth.service;

import com.rentadeautos.modules.auth.dto.LoginRequest;
import com.rentadeautos.modules.auth.dto.LoginResponse;
import com.rentadeautos.modules.auth.dto.UsuarioResponse;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.auth.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Lógica de autenticación (S1-08).
 * Valida credenciales, estado del usuario y genera el token.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioAppRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioAppRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        Optional<UsuarioApp> encontrado = usuarioRepository.findByCorreo(correo);

        if (encontrado.isEmpty()) {
            log.warn("Intento de acceso fallido: usuario no encontrado");
            throw new BadCredentialsException("Correo o contraseña incorrectos");
        }

        UsuarioApp usuario = encontrado.get();

        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash())) {
            log.warn("Intento de acceso fallido: contraseña incorrecta para el usuario {}", usuario.getId());
            throw new BadCredentialsException("Correo o contraseña incorrectos");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("Intento de acceso de usuario desactivado: {}", usuario.getId());
            throw new BadCredentialsException("Correo o contraseña incorrectos");
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String rol = usuario.getRol().getNombre();
        String token = jwtService.generarToken(usuario.getCorreo(), rol);

        log.info("Acceso exitoso del usuario {}", usuario.getId());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getSegundosExpiracion(),
                aResponse(usuario)
        );
    }

    public UsuarioResponse aResponse(UsuarioApp usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().getNombre()
        );
    }
}