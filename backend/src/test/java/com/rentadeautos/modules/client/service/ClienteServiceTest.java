package com.rentadeautos.modules.client.service;

import com.rentadeautos.modules.client.dto.ClienteRequest;
import com.rentadeautos.modules.client.dto.FiltroClientes;
import com.rentadeautos.modules.client.exception.ClienteDuplicadoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequest peticion() {
        return new ClienteRequest(" Ana ", " Lopez ", " ANA@MAIL.COM ",
                "5551234567", " lic-42 ", LocalDate.now().plusDays(30), " Casa ");
    }

    @Test
    void crearNormalizaDatosYActivaCliente() {
        when(clienteRepository.saveAndFlush(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.crear(peticion());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        Cliente cliente = captor.getValue();
        assertEquals("ana@mail.com", cliente.getCorreo());
        assertEquals("LIC-42", cliente.getNumeroLicencia());
        assertEquals("Ana", cliente.getNombre());
        assertEquals("Lopez", cliente.getApellidos());
        assertEquals(true, cliente.getActivo());
    }

    @Test
    void crearRechazaCorreoDuplicado() {
        when(clienteRepository.existsByCorreoIgnoreCase("ana@mail.com")).thenReturn(true);

        assertThrows(ClienteDuplicadoException.class, () -> clienteService.crear(peticion()));
    }

    @Test
    void desactivarConservaRegistroYLoMarcaInactivo() {
        Cliente cliente = new Cliente();
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.saveAndFlush(cliente)).thenReturn(cliente);

        clienteService.desactivar(7L);

        assertFalse(cliente.getActivo());
        verify(clienteRepository).saveAndFlush(cliente);
    }

    @Test
    void reactivarMarcaClienteComoActivo() {
        Cliente cliente = new Cliente();
        cliente.setActivo(false);
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.saveAndFlush(cliente)).thenReturn(cliente);

        clienteService.reactivar(7L);

        assertEquals(true, cliente.getActivo());
        verify(clienteRepository).saveAndFlush(cliente);
    }

    @Test
    void listarAplicaTextoYEstadoActivo() {
        when(clienteRepository.buscar("%ana!!!%!_%", true)).thenReturn(List.of());

        clienteService.listar(new FiltroClientes(" Ana!%_ ", true));

        verify(clienteRepository).buscar("%ana!!!%!_%", true);
    }

    @Test
    void listarRechazaTextoDemasiadoLargo() {
        String texto = "a".repeat(51);

        assertThrows(RuntimeException.class,
                () -> clienteService.listar(new FiltroClientes(texto, null)));
    }

    @Test
    void editarConservaPasswordHashExistente() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Original");
        cliente.setApellidos("Original");
        cliente.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqr");
        
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.saveAndFlush(cliente)).thenReturn(cliente);

        clienteService.editar(10L, peticion());

        assertEquals("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqr", cliente.getPasswordHash(),
                "La edición de cliente por personal de mostrador no debe sobreescribir ni borrar el passwordHash");
        verify(clienteRepository).saveAndFlush(cliente);
    }

    @Test
    void crearDesdeMostradorDejaPasswordHashNull() {
        when(clienteRepository.saveAndFlush(any(Cliente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        clienteService.crear(peticion());

        org.mockito.ArgumentCaptor<Cliente> captor = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getPasswordHash(),
                "El alta de mostrador debe crear el cliente sin passwordHash (NULL), sin contraseñas ficticias");
    }
}