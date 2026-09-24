package com.rentadeautos.modules.reservation.service;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.reservation.dto.ReservacionRequest;
import com.rentadeautos.modules.reservation.dto.ReservacionResponse;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ReservacionService {
    private final ReservacionRepository reservaciones;
    private final UsuarioAppRepository usuarios;
    private final JdbcTemplate jdbc;

    public ReservacionService(ReservacionRepository reservaciones,
            UsuarioAppRepository usuarios, JdbcTemplate jdbc) {
        this.reservaciones = reservaciones;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ReservacionResponse> listar() {
        return reservaciones.findAllByOrderByFechaInicioDesc().stream()
                .map(ReservacionResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ReservacionResponse obtener(Long id) {
        return ReservacionResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> opciones() {
        var clientes = jdbc.query("SELECT id, nombre, apellidos FROM clientes WHERE activo = TRUE ORDER BY apellidos, nombre",
                (rs, fila) -> Map.of("id", rs.getLong("id"),
                        "nombre", rs.getString("nombre") + " " + rs.getString("apellidos")));
        var vehiculos = jdbc.query("SELECT id, marca, modelo, placa FROM vehiculos "
                        + "WHERE estado IN ('DISPONIBLE', 'RESERVADO') ORDER BY marca, modelo, placa",
                (rs, fila) -> Map.of("id", rs.getLong("id"), "nombre",
                        rs.getString("marca") + " " + rs.getString("modelo") + " · " + rs.getString("placa")));
        return Map.of("clientes", clientes, "vehiculos", vehiculos);
    }

    @Transactional
    public ReservacionResponse crear(ReservacionRequest datos) {
        validarFechas(datos);
        validarCliente(datos.clienteId());
        Long categoriaId = bloquearYValidarVehiculo(datos.vehiculoId());
        validarDisponibilidad(datos, -1L);
        BigDecimal tarifa = tarifaVigente(categoriaId, datos.fechaInicio().toLocalDate());
        validarTotal(datos, tarifa);

        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        Long creador = usuarios.findByCorreo(correo)
                .orElseThrow(() -> new ReservacionException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no corresponde a un usuario válido"))
                .getId();
        Reservacion r = new Reservacion();
        r.setCreadoPorId(creador);
        aplicar(r, datos, tarifa);
        return ReservacionResponse.desde(reservaciones.saveAndFlush(r));
    }

    @Transactional
    public ReservacionResponse editar(Long id, ReservacionRequest datos) {
        validarFechas(datos);
        Reservacion r = buscar(id);
        validarEditable(r);
        validarCliente(datos.clienteId());
        Long categoriaId = bloquearYValidarVehiculo(datos.vehiculoId());
        validarDisponibilidad(datos, id);
        // RN-07: la tarifa aceptada no cambia si el catálogo cambia.
        // Al sustituir el vehículo se acepta la tarifa vigente de su categoría.
        BigDecimal tarifa = r.getVehiculoId().equals(datos.vehiculoId())
                ? r.getTarifaDia()
                : tarifaVigente(categoriaId, datos.fechaInicio().toLocalDate());
        validarTotal(datos, tarifa);
        aplicar(r, datos, tarifa);
        return ReservacionResponse.desde(reservaciones.saveAndFlush(r));
    }

    @Transactional
    public ReservacionResponse cancelar(Long id) {
        Reservacion r = buscar(id);
        validarEditable(r);
        r.setEstado("CANCELADA");
        return ReservacionResponse.desde(reservaciones.saveAndFlush(r));
    }

    private Reservacion buscar(Long id) {
        return reservaciones.findById(id).orElseThrow(() ->
                new ReservacionException(HttpStatus.NOT_FOUND, "Reservación no encontrada"));
    }

    private void validarEditable(Reservacion r) {
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(r.getEstado())) {
            throw new ReservacionException(HttpStatus.CONFLICT,
                    "Solo se pueden editar o cancelar reservaciones pendientes o confirmadas");
        }
    }

    private void validarFechas(ReservacionRequest datos) {
        if (!datos.fechaFin().isAfter(datos.fechaInicio())) {
            throw new ReservacionException(HttpStatus.BAD_REQUEST,
                    "La fecha de devolución debe ser posterior al inicio (RN-02)");
        }
    }

    private void validarTotal(ReservacionRequest datos, BigDecimal tarifa) {
        BigDecimal total = tarifa.multiply(BigDecimal.valueOf(diasCobrados(datos)));
        if (total.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ReservacionException(HttpStatus.BAD_REQUEST, "El total excede el máximo permitido");
        }
    }

    private void validarCliente(Long id) {
        List<Boolean> activos = jdbc.query("SELECT activo FROM clientes WHERE id = ?",
                (rs, fila) -> rs.getBoolean(1), id);
        if (activos.isEmpty() || !activos.get(0)) {
            throw new ReservacionException(HttpStatus.BAD_REQUEST,
                    "El cliente no existe o está inactivo");
        }
    }

    private Long bloquearYValidarVehiculo(Long id) {
        // La fila del vehículo serializa altas y ediciones concurrentes para esta unidad.
        List<Long> categorias = jdbc.query("SELECT categoria_id FROM vehiculos WHERE id = ? "
                        + "AND estado IN ('DISPONIBLE', 'RESERVADO') FOR UPDATE",
                (rs, fila) -> rs.getLong(1), id);
        if (categorias.isEmpty()) {
            throw new ReservacionException(HttpStatus.BAD_REQUEST,
                    "El vehículo no existe o no admite reservaciones");
        }
        return categorias.get(0);
    }

    private BigDecimal tarifaVigente(Long categoriaId, LocalDate fechaInicio) {
        List<BigDecimal> precios = jdbc.query("SELECT precio_dia FROM tarifas WHERE categoria_id = ? "
                        + "AND activo = TRUE AND fecha_inicio <= ? "
                        + "AND (fecha_fin IS NULL OR fecha_fin >= ?) "
                        + "ORDER BY fecha_inicio DESC, id DESC LIMIT 2 FOR UPDATE",
                (rs, fila) -> rs.getBigDecimal(1), categoriaId, fechaInicio, fechaInicio);
        if (precios.size() != 1) {
            throw new ReservacionException(HttpStatus.CONFLICT, precios.isEmpty()
                    ? "No hay una tarifa vigente para la categoría del vehículo en la fecha de inicio"
                    : "Hay más de una tarifa vigente para la categoría en la fecha de inicio");
        }
        return precios.get(0);
    }

    private void validarDisponibilidad(ReservacionRequest datos, Long excluirId) {
        // Lectura actual con bloqueo: una consulta JPA normal podría conservar
        // una instantánea anterior a la espera del bloqueo del vehículo en MySQL.
        var traslapes = jdbc.query("SELECT id FROM reservaciones WHERE vehiculo_id = ? "
                        + "AND id <> ? AND estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO') "
                        + "AND fecha_inicio < ? AND fecha_fin > ? FOR UPDATE",
                (rs, fila) -> rs.getLong(1), datos.vehiculoId(), excluirId,
                datos.fechaFin(), datos.fechaInicio());
        if (!traslapes.isEmpty()) {
            throw new ReservacionException(HttpStatus.CONFLICT,
                    "El vehículo ya tiene una reservación para esas fechas");
        }
    }

    private void aplicar(Reservacion r, ReservacionRequest datos, BigDecimal tarifa) {
        r.setClienteId(datos.clienteId());
        r.setVehiculoId(datos.vehiculoId());
        r.setFechaInicio(datos.fechaInicio());
        r.setFechaFin(datos.fechaFin());
        r.setTarifaDia(tarifa);
        long dias = diasCobrados(datos);
        r.setTotalEstimado(r.getTarifaDia().multiply(BigDecimal.valueOf(dias)));
        r.setObservaciones(datos.observaciones() == null || datos.observaciones().isBlank()
                ? null : datos.observaciones().strip());
    }

    private long diasCobrados(ReservacionRequest datos) {
        long segundos = Duration.between(datos.fechaInicio(), datos.fechaFin()).getSeconds();
        return 1 + (segundos - 1) / 86400;
    }
}
