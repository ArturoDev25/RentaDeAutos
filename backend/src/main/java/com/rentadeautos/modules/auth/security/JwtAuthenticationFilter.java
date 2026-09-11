package com.rentadeautos.modules.auth.security;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioAppRepository usuarioRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioAppRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String encabezado = request.getHeader("Authorization");

        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            String token = encabezado.substring(PREFIJO.length());
            Optional<Claims> claims = jwtService.validarToken(token);

            if (claims.isPresent()
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String correo = claims.get().getSubject();

                usuarioRepository.findByCorreo(correo)
                        .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                        .filter(usuario -> usuario.getRol() != null)
                        .filter(usuario ->
                                Boolean.TRUE.equals(usuario.getRol().getActivo()))
                        .ifPresent(this::registrarAutenticacion);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void registrarAutenticacion(UsuarioApp usuario) {
        String rol = usuario.getRol().getNombre();

        var autoridades =
                List.of(new SimpleGrantedAuthority("ROLE_" + rol));

        var autenticacion = new UsernamePasswordAuthenticationToken(
                usuario.getCorreo(),
                null,
                autoridades
        );

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}