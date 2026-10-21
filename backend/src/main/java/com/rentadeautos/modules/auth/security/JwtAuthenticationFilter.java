
package com.rentadeautos.modules.auth.security;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
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
    private final ClienteRepository clienteRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioAppRepository usuarioRepository,
            ClienteRepository clienteRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
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

                var contenido = claims.get();
                String rolToken = contenido.get("rol", String.class);
                String subject = contenido.getSubject();

                if ("CLIENTE".equals(rolToken)) {
                    autenticarCliente(subject);
                } else {
                    usuarioRepository.findByCorreo(subject)
                            .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                            .filter(usuario -> usuario.getRol() != null)
                            .filter(usuario ->
                                    Boolean.TRUE.equals(usuario.getRol().getActivo()))
                            .ifPresent(this::registrarAutenticacion);
                }
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

    private void autenticarCliente(String subject) {
        if (subject == null || !subject.matches("cliente:[1-9]\\d*")) {
            return;
        }

        try {
            Long clienteId = Long.parseLong(
                    subject.substring("cliente:".length()));

            clienteRepository.findById(clienteId)
                    .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
                    .ifPresent(this::registrarAutenticacionCliente);

        } catch (NumberFormatException e) {
            // Un ID inválido no debe autenticar al cliente.
        }
    }

    private void registrarAutenticacionCliente(Cliente cliente) {
        var autoridades =
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));

        var autenticacion = new UsernamePasswordAuthenticationToken(
                "cliente:" + cliente.getId(),
                null,
                autoridades
        );

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
