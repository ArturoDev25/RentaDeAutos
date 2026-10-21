
package com.rentadeautos.modules.auth.security;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.model.Rol;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UsuarioAppRepository usuarioRepository;
    private ClienteRepository clienteRepository;
    private JwtAuthenticationFilter filtro;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        String secreto =
                "clave-segura-de-prueba-para-firmar-tokens-jwt-123456";
        jwtService = new JwtService(secreto, 30);
        usuarioRepository = mock(UsuarioAppRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        filtro = new JwtAuthenticationFilter(
                jwtService, usuarioRepository, clienteRepository);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }
    @Test
    void clienteActivoDebeAutenticarseConRolCliente() throws Exception {
        Cliente cliente = mock(Cliente.class);
        when(cliente.getId()).thenReturn(7L);
        when(cliente.getActivo()).thenReturn(true);
        // Simular que el cliente existe en el repositorio
        when(clienteRepository.findById(7L))
        .thenReturn(Optional.of(cliente));
        // Generar un token JWT con el subject "cliente:7" y rol "CLIENTE"
        String token = jwtService.generarToken("cliente:7", "CLIENTE");
        ejecutarFiltro(token);
        var autenticacion =
        SecurityContextHolder.getContext().getAuthentication();
        // Verificar que la autenticación no sea nula y tenga el principal y rol correctos
        assertNotNull(autenticacion);
        assertEquals("cliente:7", autenticacion.getPrincipal());
        assertTrue(autenticacion.getAuthorities().stream()
                  .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
        verify(clienteRepository).findById(7L);
        verifyNoInteractions(usuarioRepository);
}
    @Test
    void clienteInexistenteNoDebeAutenticarse() throws Exception {
        when(clienteRepository.findById(7L))
                .thenReturn(Optional.empty());

        ejecutarFiltro(
                jwtService.generarToken("cliente:7", "CLIENTE"));

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());
    }
    @Test
    void clienteInactivoNoDebeAutenticarse() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setActivo(false);

        when(clienteRepository.findById(7L))
                .thenReturn(Optional.of(cliente));

        ejecutarFiltro(
                jwtService.generarToken("cliente:7", "CLIENTE"));

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());
    }
    @Test
    void subjectInvalidoNoDebeAutenticarse() throws Exception {
        ejecutarFiltro(
                jwtService.generarToken("cliente:abc", "CLIENTE"));

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(clienteRepository);
    }
    @Test
    void subjectClienteConIdCeroNoDebeAutenticarse() throws Exception {
        ejecutarFiltro(
                jwtService.generarToken("cliente:0", "CLIENTE"));

        assertNull(
                SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void tokenSinRolClienteDebeBuscarUsuarioDelPersonal()
            throws Exception {
        when(usuarioRepository.findByCorreo("admin@prueba.com"))
                .thenReturn(Optional.empty());

        ejecutarFiltro(
                jwtService.generarToken("admin@prueba.com", "ADMIN"));

        verify(usuarioRepository).findByCorreo("admin@prueba.com");
        verifyNoInteractions(clienteRepository);
        assertNull(
                SecurityContextHolder.getContext().getAuthentication());
    }

    private void ejecutarFiltro(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
