package com.rentadeautos.modules.devolucion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentadeautos.modules.audit.service.AuditService;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.devolucion.dto.DevolucionRequest;
import com.rentadeautos.modules.devolucion.dto.DevolucionResponse;
import com.rentadeautos.modules.devolucion.exception.DevolucionException;
import com.rentadeautos.modules.devolucion.model.Devolucion;
import com.rentadeautos.modules.devolucion.repository.DevolucionRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3-10 — Devolución de vehículo.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Valida que la entrega exista y no tenga devolución previa.
 *   <li>Valida RN-08: kilometrajeEntrada ≥ kilometrajeSalida de la entrega.
 *   <li>Calcula subtotal (tarifa_dia × días reales), cargoAtraso y totalFinal.
 *   <li>Persiste {@link Devolucion}.
 *   <li>Transición Reservación → FINALIZADA.
 *   <li>Transición Vehículo: actualiza kilometraje → DISPONIBLE o MANTENIMIENTO.
 *   <li>Registra en auditoría.
 * </ol>
 * Todo ocurre dentro de una única transacción: cualquier fallo hace rollback completo.</p>
 */
@Service
public class DevolucionService {

    static final String ACCION_AUDITORIA   = "DEVOLVER_VEHICULO";
    static final String ESTADO_FINALIZADA  = "FINALIZADA";
    static final String ESTADO_EN_CURSO    = "EN_CURSO";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DevolucionRepository devoluciones;
    private final EntregaRepository    entregas;
    private final ReservacionRepository reservaciones;
    private final VehiculoRepository   vehiculos;
    private final UsuarioAppRepository usuarios;
    private final AuditService         auditoria;

    public DevolucionService(
            DevolucionRepository devoluciones,
            EntregaRepository entregas,
            ReservacionRepository reservaciones,
            VehiculoRepository vehiculos,
            UsuarioAppRepository usuarios,
            AuditService auditoria) {
        this.devoluciones  = devoluciones;
        this.entregas      = entregas;
        this.reservaciones = reservaciones;
        this.vehiculos     = vehiculos;
        this.usuarios      = usuarios;
        this.auditoria     = auditoria;
    }

    // ── Operación principal ────────────────────────────────────────────────────

    @Transactional
    public DevolucionResponse registrar(DevolucionRequest datos, String direccionIp) {

        Long actorId = usuarioActualId();

        // 1. Obtener y validar la entrega.
        Entrega entrega = entregas.findById(datos.entregaId())
                .orElseThrow(() -> new DevolucionException(HttpStatus.NOT_FOUND,
                        "Entrega no encontrada: " + datos.entregaId()));

        // 2. Verificar que no exista devolución previa (409 Conflict).
        if (devoluciones.existsByEntregaId(entrega.getId())) {
            throw new DevolucionException(HttpStatus.CONFLICT,
                    "La entrega " + entrega.getId() + " ya tiene una devolución registrada");
        }

        // 3. Obtener reservación (con bloqueo FOR UPDATE) y validar que esté EN_CURSO.
        Reservacion reservacion = reservaciones.findByIdParaActualizar(entrega.getReservacionId())
                .orElseThrow(() -> new DevolucionException(HttpStatus.NOT_FOUND,
                        "Reservación no encontrada para la entrega " + entrega.getId()));

        if (!ESTADO_EN_CURSO.equals(reservacion.getEstado())) {
            throw new DevolucionException(HttpStatus.CONFLICT,
                    "Solo se puede devolver un vehículo de una reservación EN_CURSO "
                    + "(estado actual: " + reservacion.getEstado() + ")");
        }

        // 4. Obtener vehículo con bloqueo FOR UPDATE para actualización.
        Vehiculo vehiculo = vehiculos.findByIdParaActualizar(reservacion.getVehiculoId())
                .orElseThrow(() -> new DevolucionException(HttpStatus.CONFLICT,
                        "El vehículo de la reservación no existe"));

        // 5. RN-08: kilometrajeEntrada ≥ kilometrajeSalida (400 Bad Request).
        BigDecimal kmEntrada = datos.kilometrajeEntrada();
        BigDecimal kmSalida  = entrega.getKilometrajeSalida();
        if (kmEntrada.compareTo(kmSalida) < 0) {
            throw new DevolucionException(HttpStatus.BAD_REQUEST,
                    "RN-08: el kilometraje de entrada (" + kmEntrada.toPlainString()
                    + ") no puede ser menor al de salida (" + kmSalida.toPlainString() + ")");
        }

        // 6. Instantánea de estados antes del cambio (para auditoría).
        Map<String, Object> antes = capturarEstadoAntes(reservacion, vehiculo, entrega);

        // 7. Cálculo de costos.
        LocalDateTime ahora           = LocalDateTime.now();
        BigDecimal    tarifaDia       = reservacion.getTarifaDia();
        LocalDateTime fechaFinPactada = reservacion.getFechaFin();

        // Subtotal por los días PACTADOS (inicio -> fin pactada), NO por los días reales:
        // los días de atraso se cobran aparte (cargoAtraso) y no deben contarse dos veces.
        long horasPactadas = ChronoUnit.HOURS.between(reservacion.getFechaInicio(), fechaFinPactada);
        long diasPactados  = (horasPactadas + 23) / 24; // ceil sin BigDecimal
        if (diasPactados < 1) diasPactados = 1;

        BigDecimal subtotal = tarifaDia.multiply(BigDecimal.valueOf(diasPactados))
                                       .setScale(2, RoundingMode.HALF_UP);

        // Cargo por atraso: días extra después de la fecha fin pactada.
        BigDecimal cargoAtraso = BigDecimal.ZERO;
        if (ahora.isAfter(fechaFinPactada)) {
            long horasAtraso = ChronoUnit.HOURS.between(fechaFinPactada, ahora);
            long diasAtraso  = (horasAtraso + 23) / 24;
            // Recargo = tarifa_dia × días de atraso (mismo precio, sin penalización adicional
            // a menos que la tarifa incluya cargo_atraso_dia; aquí se usa tarifa_dia
            // por consistencia con el modelo simplificado de la issue).
            cargoAtraso = tarifaDia.multiply(BigDecimal.valueOf(diasAtraso))
                                   .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cargoDanos = datos.cargoDanos() != null
                ? datos.cargoDanos().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalFinal = subtotal.add(cargoAtraso).add(cargoDanos);

        // 8. Persistir la devolución.
        Devolucion devolucion = new Devolucion();
        devolucion.setEntregaId(entrega.getId());
        devolucion.setRecibidoPorId(actorId);
        devolucion.setFechaDevolucion(ahora);
        devolucion.setKilometrajeEntrada(kmEntrada);
        devolucion.setCombustibleEntrada(datos.combustibleEntrada());
        devolucion.setCondicionEntrada(datos.condicionEntrada().strip());
        devolucion.setCargoAtraso(cargoAtraso);
        devolucion.setCargoDanos(cargoDanos);
        devolucion.setTotalFinal(totalFinal);
        devolucion.setObservaciones(
                datos.observaciones() == null || datos.observaciones().isBlank()
                        ? null : datos.observaciones().strip());
        Devolucion guardada = devoluciones.saveAndFlush(devolucion);

        // 9. Transición de Reservación → FINALIZADA.
        reservacion.setEstado(ESTADO_FINALIZADA);
        reservaciones.saveAndFlush(reservacion);

        // 10. Transición de Vehículo: actualizar kilometraje y estado.
        vehiculo.setKilometraje(kmEntrada);
        EstadoVehiculo nuevoEstadoVehiculo = determinarEstadoVehiculo(
                datos.requiereMantenimiento(), cargoDanos);
        vehiculo.setEstado(nuevoEstadoVehiculo);
        vehiculos.saveAndFlush(vehiculo);

        // 11. Auditoría dentro de la misma transacción.
        Map<String, Object> despues = capturarEstadoDespues(guardada, reservacion, vehiculo,
                subtotal, nuevoEstadoVehiculo);
        auditoria.registrarEvento(actorId, ACCION_AUDITORIA, "Devolucion", guardada.getId(),
                "EXITOSO", aJson(antes), aJson(despues), direccionIp);

        return DevolucionResponse.desde(
                guardada,
                reservacion.getId(),
                vehiculo.getPlaca(),
                subtotal,
                nuevoEstadoVehiculo.name(),
                ESTADO_FINALIZADA);
    }

    // ── Métodos auxiliares ─────────────────────────────────────────────────────

    /**
     * Determina el estado del vehículo al finalizar la renta.
     * Pasa a MANTENIMIENTO cuando el agente lo solicita explícitamente
     * (requiereMantenimiento) o cuando se registró un cargo por daños (> 0).
     * Ya NO se infiere del texto de la condición: "Sin daños" contiene la
     * palabra "daño" y antes mandaba por error un vehículo sano a mantenimiento.
     */
    static EstadoVehiculo determinarEstadoVehiculo(Boolean requiereMantenimiento, BigDecimal cargoDanos) {
        boolean hayCargoDanos = cargoDanos != null && cargoDanos.compareTo(BigDecimal.ZERO) > 0;
        if (Boolean.TRUE.equals(requiereMantenimiento) || hayCargoDanos) {
            return EstadoVehiculo.MANTENIMIENTO;
        }
        return EstadoVehiculo.DISPONIBLE;
    }

    private Long usuarioActualId() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> new DevolucionException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no corresponde a un usuario válido"))
                .getId();
    }

    private Map<String, Object> capturarEstadoAntes(
            Reservacion r, Vehiculo v, Entrega e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reservacionId",    r.getId());
        m.put("estadoReservacion", r.getEstado());
        m.put("vehiculoId",       v.getId());
        m.put("estadoVehiculo",   v.getEstado().name());
        m.put("kilometrajeVehiculo", v.getKilometraje());
        m.put("entregaId",        e.getId());
        m.put("kilometrajeSalida", e.getKilometrajeSalida());
        return m;
    }

    private Map<String, Object> capturarEstadoDespues(
            Devolucion d, Reservacion r, Vehiculo v,
            BigDecimal subtotal, EstadoVehiculo nuevoEstado) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("devolucionId",     d.getId());
        m.put("entregaId",        d.getEntregaId());
        m.put("reservacionId",    r.getId());
        m.put("estadoReservacion", ESTADO_FINALIZADA);
        m.put("vehiculoId",       v.getId());
        m.put("estadoVehiculo",   nuevoEstado.name());
        m.put("kilometrajeEntrada", d.getKilometrajeEntrada());
        m.put("combustibleEntrada", d.getCombustibleEntrada());
        m.put("subtotal",         subtotal);
        m.put("cargoAtraso",      d.getCargoAtraso());
        m.put("cargoDanos",       d.getCargoDanos());
        m.put("totalFinal",       d.getTotalFinal());
        return m;
    }

    private static String aJson(Map<String, Object> valores) {
        try {
            return JSON.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría", e);
        }
    }
}
