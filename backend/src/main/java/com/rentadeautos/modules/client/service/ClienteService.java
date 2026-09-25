package com.rentadeautos.modules.client.service;

import com.rentadeautos.modules.client.dto.ClienteRequest;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import com.rentadeautos.modules.client.dto.FiltroClientes;
import com.rentadeautos.modules.client.exception.ClienteDuplicadoException;
import com.rentadeautos.modules.client.exception.ClienteInvalidoException;
import com.rentadeautos.modules.client.exception.ClienteNoEncontradoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClienteService {

    static final int LONGITUD_MAXIMA_BUSQUEDA = 50;

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return listar(FiltroClientes.sinFiltros());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(FiltroClientes filtro) {
        String texto = limpiarTexto(filtro.texto());
        if (texto != null && texto.length() > LONGITUD_MAXIMA_BUSQUEDA) {
            throw new ClienteInvalidoException("El texto de búsqueda no puede exceder "
                    + LONGITUD_MAXIMA_BUSQUEDA + " caracteres");
        }

        return clienteRepository.buscar(patronBusqueda(texto), filtro.activo()).stream()
                .map(ClienteResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.desde(buscar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest peticion) {
        String correo = normalizarCorreo(peticion.correo());
        String licencia = normalizarLicencia(peticion.numeroLicencia());
        validarLicencia(peticion.licenciaVencimiento());
        validarUnicidad(correo, licencia, null);

        Cliente cliente = new Cliente();
        aplicarDatos(cliente, peticion, correo, licencia);
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse editar(Long id, ClienteRequest peticion) {
        Cliente cliente = buscar(id);
        String correo = normalizarCorreo(peticion.correo());
        String licencia = normalizarLicencia(peticion.numeroLicencia());
        validarLicencia(peticion.licenciaVencimiento());
        validarUnicidad(correo, licencia, id);

        aplicarDatos(cliente, peticion, correo, licencia);
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse desactivar(Long id) {
        Cliente cliente = buscar(id);
        cliente.setActivo(false);
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse reactivar(Long id) {
        Cliente cliente = buscar(id);
        cliente.setActivo(true);
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(ClienteNoEncontradoException::new);
    }

    private void validarUnicidad(String correo, String licencia, Long id) {
        if (correo != null && (id == null
                ? clienteRepository.existsByCorreoIgnoreCase(correo)
                : clienteRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id))) {
            throw ClienteDuplicadoException.correo();
        }
        if (id == null
                ? clienteRepository.existsByNumeroLicenciaIgnoreCase(licencia)
                : clienteRepository.existsByNumeroLicenciaIgnoreCaseAndIdNot(licencia, id)) {
            throw ClienteDuplicadoException.licencia();
        }
    }

    private void validarLicencia(LocalDate vencimiento) {
        if (!vencimiento.isAfter(LocalDate.now())) {
            throw new ClienteInvalidoException(
                    "La fecha de vencimiento de la licencia debe ser futura");
        }
    }

    private void aplicarDatos(Cliente cliente, ClienteRequest peticion,
                              String correo, String licencia) {
        cliente.setNombre(peticion.nombre().trim());
        cliente.setApellidos(peticion.apellidos().trim());
        cliente.setCorreo(correo);
        cliente.setTelefono(peticion.telefono().trim());
        cliente.setNumeroLicencia(licencia);
        cliente.setLicenciaVencimiento(peticion.licenciaVencimiento());
        cliente.setDireccion(normalizarOpcional(peticion.direccion()));
        if (cliente.getActivo() == null) {
            cliente.setActivo(true);
        }
    }

    private String normalizarCorreo(String correo) {
        return normalizarOpcional(correo) == null ? null : correo.trim().toLowerCase();
    }

    private String normalizarLicencia(String licencia) {
        return licencia.trim().toUpperCase();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    private String limpiarTexto(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim().toLowerCase();
    }

    private String patronBusqueda(String texto) {
        if (texto == null) {
            return null;
        }
        String escapado = texto.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }
}