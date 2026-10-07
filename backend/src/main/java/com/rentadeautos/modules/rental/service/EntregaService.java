package com.rentadeautos.modules.rental.service;

import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.audit.service.AuditoriaOperativa;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.rental.dto.EntregaRequest;
import com.rentadeautos.modules.rental.dto.EntregaResponse;
import com.rentadeautos.modules.rental.exception.EntregaException;
import com.rentadeautos.modules.rental.model.Entrega;
import com.rentadeautos.modules.rental.repository.EntregaRepository;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3-09 — Entrega de vehículo.
 *
 * <p>Flujo: Reservación CONFIRMADA → EN_CURSO y Vehículo → RENTADO.
 * Todo ocurre en una sola transacción: si cualquier paso falla (incluida la
 * auditoría) no se guarda la entrega ni cambia ningún estado.</p>
 */
@Service
public class EntregaService {

    static final String ESTADO_VALIDO = "CONFIRMADA";
    static final String ESTADO_EN_CURSO = "EN_CURSO";
    static final String ACCION_AUDITORIA = AuditoriaOperativa.ENTREGAR_VEHICULO;

    private final EntregaRepository entregas;
    private final ReservacionRepository reservaciones;
    private final VehiculoRepository vehiculos;
    private final ClienteRepository clientes;
    private final UsuarioAppRepository usuarios;
    private final AuditService auditoria;

    public EntregaService(EntregaRepository entregas, ReservacionRepository reservaciones,
            VehiculoRepository vehiculos, ClienteRepository clientes,
            UsuarioAppRepository usuarios, AuditService auditoria) {
        this.entregas = entregas;
        this.reservaciones = reservaciones;
        this.vehiculos = vehiculos;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional
    public EntregaResponse registrar(EntregaRequest datos, String direccionIp) {
        Long actorId = usuarioActualId();
        Long reservacionId = datos.reservacionId();

        // 1. Bloquear primero el vehículo y después la reservación, en el mismo
        //    orden que usan crear/editar/confirmar reservación, para evitar interbloqueos.
        Long vehiculoId = reservaciones.findVehiculoIdById(reservacionId)
                .orElseThrow(() -> new EntregaException(HttpStatus.NOT_FOUND, "Reservación no encontrada"));
        Vehiculo vehiculo = vehiculos.findByIdParaActualizar(vehiculoId)
                .orElseThrow(() -> new EntregaException(HttpStatus.CONFLICT,
                        "El vehículo de la reservación no existe"));
        Reservacion reservacion = reservaciones.findByIdParaActualizar(reservacionId)
                .orElseThrow(() -> new EntregaException(HttpStatus.NOT_FOUND, "Reservación no encontrada"));
        if (!vehiculoId.equals(reservacion.getVehiculoId())) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "La reservación cambió de vehículo mientras se procesaba; intente de nuevo");
        }

        // 2. Solo una reservación válida puede iniciar la renta.
        validarReservacion(reservacion);
        validarCliente(reservacion.getClienteId());

        // 3. Validar el vehículo.
        validarVehiculo(vehiculo, reservacion);
        if (datos.kilometrajeSalida().compareTo(vehiculo.getKilometraje()) < 0) {
            throw new EntregaException(HttpStatus.BAD_REQUEST,
                    "El kilometraje de salida (" + datos.kilometrajeSalida().toPlainString()
                            + ") no puede ser menor al registrado en el vehículo ("
                            + vehiculo.getKilometraje().toPlainString() + ")");
        }

        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("reservacionId", reservacionId);
        antes.put("estadoReservacion", reservacion.getEstado());
        antes.put("vehiculoId", vehiculoId);
        antes.put("estadoVehiculo", vehiculo.getEstado().name());
        antes.put("kilometrajeVehiculo", vehiculo.getKilometraje());

        // 4. Registrar la entrega (kilometraje, combustible y condición de salida).
        Entrega entrega = new Entrega();
        entrega.setReservacionId(reservacionId);
        entrega.setEntregadoPorId(actorId);
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.setKilometrajeSalida(datos.kilometrajeSalida());
        entrega.setCombustibleSalida(datos.combustibleSalida());
        entrega.setCondicionSalida(datos.condicionSalida().strip());
        entrega.setObservaciones(datos.observaciones() == null || datos.observaciones().isBlank()
                ? null : datos.observaciones().strip());
        Entrega guardada = entregas.saveAndFlush(entrega);

        // 5. Cambiar estados: Reservación → EN_CURSO, Vehículo → RENTADO.
        reservacion.setEstado(ESTADO_EN_CURSO);
        reservaciones.saveAndFlush(reservacion);
        vehiculo.setEstado(EstadoVehiculo.RENTADO);
        vehiculo.setKilometraje(datos.kilometrajeSalida());
        vehiculos.saveAndFlush(vehiculo);

        // 6. Auditoría dentro de la misma transacción.
        Map<String, Object> despues = new LinkedHashMap<>();
        despues.put("entregaId", guardada.getId());
        despues.put("reservacionId", reservacionId);
        despues.put("estadoReservacion", ESTADO_EN_CURSO);
        despues.put("vehiculoId", vehiculoId);
        despues.put("estadoVehiculo", EstadoVehiculo.RENTADO.name());
        despues.put("kilometrajeSalida", guardada.getKilometrajeSalida());
        despues.put("combustibleSalida", guardada.getCombustibleSalida());
        despues.put("condicionSalida", guardada.getCondicionSalida());
        despues.put("fechaEntrega", guardada.getFechaEntrega());
        auditoria.registrarEvento(actorId, ACCION_AUDITORIA, AuditoriaOperativa.ENTREGA, guardada.getId(),
                AuditoriaOperativa.EXITOSO, antes, despues, direccionIp);

        return EntregaResponse.desde(guardada, vehiculoId, ESTADO_EN_CURSO,
                EstadoVehiculo.RENTADO.name());
    }

    @Transactional(readOnly = true)
    public EntregaResponse obtenerPorReservacion(Long reservacionId) {
        Entrega entrega = entregas.findByReservacionId(reservacionId)
                .orElseThrow(() -> new EntregaException(HttpStatus.NOT_FOUND,
                        "La reservación no tiene una entrega registrada"));
        Reservacion r = reservaciones.findById(reservacionId)
                .orElseThrow(() -> new EntregaException(HttpStatus.NOT_FOUND, "Reservación no encontrada"));
        String estadoVehiculo = vehiculos.findById(r.getVehiculoId())
                .map(v -> v.getEstado().name()).orElse(null);
        return EntregaResponse.desde(entrega, r.getVehiculoId(), r.getEstado(), estadoVehiculo);
    }

    private void validarReservacion(Reservacion r) {
        if (!ESTADO_VALIDO.equals(r.getEstado())) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "Solo se puede entregar un vehículo de una reservación CONFIRMADA (estado actual: "
                            + r.getEstado() + ")");
        }
        if (entregas.existsByReservacionId(r.getId())) {
            throw new EntregaException(HttpStatus.CONFLICT, "La reservación ya tiene una entrega registrada");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (!r.getFechaFin().isAfter(ahora)) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "La reservación ya venció; no puede iniciar la renta");
        }
        // No se entrega antes del día de inicio: el auto podría seguir comprometido
        // con otra reservación que termina justo antes.
        if (ahora.toLocalDate().isBefore(r.getFechaInicio().toLocalDate())) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "La entrega solo puede hacerse a partir del día de inicio de la reservación ("
                            + r.getFechaInicio().toLocalDate() + ")");
        }
    }

    private void validarCliente(Long clienteId) {
        Cliente cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new EntregaException(HttpStatus.CONFLICT, "El cliente de la reservación no existe"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new EntregaException(HttpStatus.CONFLICT, "El cliente de la reservación está inactivo");
        }
        if (cliente.getLicenciaVencimiento() == null
                || cliente.getLicenciaVencimiento().isBefore(LocalDate.now())) {
            throw new EntregaException(HttpStatus.CONFLICT, "La licencia del cliente está vencida");
        }
    }

    private void validarVehiculo(Vehiculo v, Reservacion r) {
        EstadoVehiculo estado = v.getEstado();
        if (estado != EstadoVehiculo.RESERVADO && estado != EstadoVehiculo.DISPONIBLE) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "El vehículo no puede entregarse porque está en estado " + estado);
        }
        if (reservaciones.existsByVehiculoIdAndEstadoAndIdNot(v.getId(), ESTADO_EN_CURSO, r.getId())) {
            throw new EntregaException(HttpStatus.CONFLICT,
                    "El vehículo tiene otra renta en curso");
        }
    }

    private Long usuarioActualId() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> new EntregaException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no corresponde a un usuario válido"))
                .getId();
    }
}
