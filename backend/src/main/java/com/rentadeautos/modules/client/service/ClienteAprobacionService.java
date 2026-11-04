package com.rentadeautos.modules.client.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.audit.service.AuditoriaOperativa;
import com.rentadeautos.modules.client.dto.ClienteResponse;
import com.rentadeautos.modules.client.exception.ClienteEstadoAprobacionException;
import com.rentadeautos.modules.client.exception.ClienteNoEncontradoException;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S5-09: revisión de las cuentas que los clientes crean desde el portal.
 *
 * <ul>
 *   <li>Aprobar: PENDIENTE → APROBADO y la cuenta queda activa (puede iniciar sesión).</li>
 *   <li>Rechazar: PENDIENTE → RECHAZADO y la cuenta permanece inactiva.</li>
 * </ul>
 * Cada acción se registra en auditoría: EXITOSO en la misma transacción (RN-09)
 * y FALLIDO cuando se intenta revisar una cuenta que ya no está pendiente.
 */
@Service
public class ClienteAprobacionService {

    static final String MENSAJE_NO_PENDIENTE =
            "La cuenta de este cliente ya no está pendiente de aprobación";

    private final ClienteRepository clientes;
    private final AuditService auditoria;

    public ClienteAprobacionService(ClienteRepository clientes, AuditService auditoria) {
        this.clientes = clientes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarPendientes() {
        return clientes.findByEstadoAprobacionOrderByIdAsc(EstadoAprobacionCliente.PENDIENTE).stream()
                .map(ClienteResponse::desde)
                .toList();
    }

    @Transactional
    public ClienteResponse aprobar(Long id) {
        return revisar(id, EstadoAprobacionCliente.APROBADO, true,
                AuditoriaOperativa.APROBAR_CLIENTE, null);
    }

    @Transactional
    public ClienteResponse rechazar(Long id, String motivo) {
        return revisar(id, EstadoAprobacionCliente.RECHAZADO, false,
                AuditoriaOperativa.RECHAZAR_CLIENTE, limpiar(motivo));
    }

    private ClienteResponse revisar(Long id, EstadoAprobacionCliente nuevoEstado, boolean activo,
                                    String accion, String motivo) {
        // Bloqueo pesimista: dos usuarios del personal no pueden revisar la misma cuenta a la vez.
        Cliente cliente = clientes.findByIdParaRevision(id)
                .orElseThrow(ClienteNoEncontradoException::new);

        if (cliente.getEstadoAprobacion() != EstadoAprobacionCliente.PENDIENTE) {
            auditoria.registrarEvento(null, accion, AuditoriaOperativa.CLIENTE, id,
                    AuditoriaOperativa.FALLIDO, snapshot(cliente, null),
                    AuditoriaOperativa.fallo("clienteId", id,
                            HttpStatus.CONFLICT.value(), MENSAJE_NO_PENDIENTE),
                    null);
            throw new ClienteEstadoAprobacionException(MENSAJE_NO_PENDIENTE);
        }

        Map<String, Object> antes = snapshot(cliente, null);
        cliente.setEstadoAprobacion(nuevoEstado);
        cliente.setActivo(activo);
        cliente.setFechaRevision(LocalDateTime.now());
        Cliente guardado = clientes.saveAndFlush(cliente);

        auditoria.registrarEvento(null, accion, AuditoriaOperativa.CLIENTE, id,
                AuditoriaOperativa.EXITOSO, antes, snapshot(guardado, motivo), null);
        return ClienteResponse.desde(guardado);
    }

    /** Datos de la cuenta para la bitácora; nunca incluye credenciales. */
    private static Map<String, Object> snapshot(Cliente cliente, String motivo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clienteId", cliente.getId());
        m.put("nombre", cliente.getNombre() + " " + cliente.getApellidos());
        m.put("correo", cliente.getCorreo());
        m.put("estadoAprobacion", cliente.getEstadoAprobacion());
        m.put("activo", cliente.getActivo());
        if (motivo != null) {
            m.put("motivo", motivo);
        }
        return m;
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
