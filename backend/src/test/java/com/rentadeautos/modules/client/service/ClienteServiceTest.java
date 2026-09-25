package com.rentadeautos.modules.client.service;

import com.rentadeautos.modules.client.dto.ClienteRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}