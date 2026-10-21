package com.rentadeautos.modules.auth.service;

import com.rentadeautos.modules.auth.dto.ClienteRegistroRequest;
import com.rentadeautos.modules.auth.dto.ClienteRegistroResponse;
import com.rentadeautos.modules.auth.exception.ClienteRegistroDuplicadoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import jakarta.validation.Valid;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;

@Service
@Validated
public class ClienteRegistroService {
    private final ClienteRepository clientes;
    private final PasswordEncoder passwordEncoder;

    public ClienteRegistroService(ClienteRepository clientes, PasswordEncoder passwordEncoder) {
        this.clientes = clientes;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ClienteRegistroResponse registrar(@Valid ClienteRegistroRequest datos) {
        if (clientes.existsByCorreoIgnoreCase(datos.correo())
                || clientes.existsByNumeroLicenciaIgnoreCase(datos.numeroLicencia())) {
            throw new ClienteRegistroDuplicadoException();
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(datos.nombre());
        cliente.setApellidos(datos.apellidos());
        cliente.setCorreo(datos.correo());
        cliente.setTelefono(datos.telefono());
        cliente.setNumeroLicencia(datos.numeroLicencia());
        cliente.setLicenciaVencimiento(datos.licenciaVencimiento());
        cliente.setDireccion(datos.direccion());
        cliente.setActivo(true);
        cliente.setPasswordHash(passwordEncoder.encode(datos.password()));

        try {
            cliente = clientes.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException error) {
            // La restricción UNIQUE también protege registros concurrentes.
            // Solo traducir las dos restricciones conocidas de datos duplicados.
            if (esDuplicado(error)) {
                throw new ClienteRegistroDuplicadoException();
            }
            throw error;
        }
        return new ClienteRegistroResponse(cliente.getId(), cliente.getNombre(),
                cliente.getApellidos(), cliente.getCorreo(), Boolean.TRUE.equals(cliente.getActivo()));
    }

    private boolean esDuplicado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                String nombre = violacion.getConstraintName();
                if (nombre != null) {
                    nombre = nombre.toLowerCase(Locale.ROOT);
                    if (nombre.contains("uq_clientes_correo") || nombre.contains("uq_clientes_licencia")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
