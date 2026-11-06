package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import com.rentadeautos.modules.devolucion.model.Devolucion;
import com.rentadeautos.modules.devolucion.repository.DevolucionRepository;
import com.rentadeautos.modules.rental.model.Entrega;
import com.rentadeautos.modules.rental.repository.EntregaRepository;
import com.rentadeautos.modules.reservation.model.Reservacion;
import com.rentadeautos.modules.reservation.repository.ReservacionRepository;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * S5-04: ciclo operativo de demostración (el "guion"), amplio y repartido en el
 * mes. Genera 54 reservaciones sobre la flota realista ({@link FlotaDemoInitializer})
 * y la clientela realista ({@link ClientelaDemoInitializer}), con fechas relativas
 * al día de la carga:
 *
 * <ul>
 *   <li>18 EN_CURSO: rentas activas que abarcan el día de hoy (con entrega; el
 *       vehículo queda RENTADO);</li>
 *   <li>14 FINALIZADAS: rentadas y devueltas en días pasados (con entrega y
 *       devolución);</li>
 *   <li>12 CONFIRMADAS: próximas (el vehículo queda RESERVADO);</li>
 *   <li>6 PENDIENTES; 4 CANCELADAS.</li>
 * </ul>
 *
 * <p>Usa exclusivamente las identidades marcadas de la flota y clientela S5-04.
 * Cada escenario tiene su propia marca; repetirlo conserva cambios y permite
 * completar reservaciones faltantes. Consulta la tarifa vigente y rechaza
 * compromisos previos, sin adoptar registros ajenos. Transaccional.</p>
 */
@Component
@Order(70)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class OperacionDemoInitializer implements CommandLineRunner {

    static final String SENTINEL = "[S504-OP:";

    private static final int EN_CURSO = 18;
    private static final int FINALIZADAS = 14;
    private static final int CONFIRMADAS = 12;
    private static final int PENDIENTES = 6;
    private static final int CANCELADAS = 4;
    private static final int TOTAL = EN_CURSO + FINALIZADAS + CONFIRMADAS + PENDIENTES + CANCELADAS; // 54

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final ReservacionRepository reservaciones;
    private final EntregaRepository entregas;
    private final DevolucionRepository devoluciones;
    private final UsuarioAppRepository usuarios;
    private final TarifaRepository tarifas;
    private final JdbcTemplate jdbc;

    public OperacionDemoInitializer(ClienteRepository clientes, VehiculoRepository vehiculos,
            ReservacionRepository reservaciones, EntregaRepository entregas,
            DevolucionRepository devoluciones, UsuarioAppRepository usuarios, TarifaRepository tarifas, JdbcTemplate jdbc) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.reservaciones = reservaciones;
        this.entregas = entregas;
        this.devoluciones = devoluciones;
        this.usuarios = usuarios;
        this.tarifas = tarifas;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long agente = staff("agente@demo.local");
        Long supervisor = staff("supervisor@demo.local");
        List<Vehiculo> flota = vehiculos.findAll().stream()
                .filter(FlotaDemoInitializer::aptoParaEscenario)
                .sorted(Comparator.comparing(Vehiculo::getPlaca)).limit(TOTAL).toList();
        List<Long> activos = clientes.findAll().stream()
                .filter(ClientelaDemoInitializer::perteneceAlSeed)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()) && !c.bloqueadoPorAprobacion())
                .sorted(Comparator.comparing(Cliente::getNumeroLicencia)).map(Cliente::getId).toList();
        if (flota.size() != TOTAL || activos.isEmpty()) {
            throw new IllegalStateException("Se requieren 54 vehículos propios de la flota y clientes demo activos");
        }
        LocalDateTime base = LocalDate.now().atTime(10, 0);
        for (int i = 0; i < TOTAL; i++) {
            Vehiculo referencia = flota.get(i);
            // Orden estable de bloqueo y lectura actual del escenario (MySQL REPEATABLE READ).
            var v = vehiculos.findByIdParaActualizar(referencia.getId()).orElseThrow();
            String marca = SENTINEL + String.format(java.util.Locale.ROOT, "%02d]", i + 1);
            var existentes = jdbc.query("SELECT cliente_id, vehiculo_id FROM reservaciones WHERE observaciones = ? FOR UPDATE",
                    (rs, fila) -> List.of(rs.getLong(1), rs.getLong(2)), marca);
            if (!existentes.isEmpty()) {
                if (existentes.size() != 1 || !v.getId().equals(existentes.get(0).get(1))
                        || clientes.findById(existentes.get(0).get(0)).filter(ClientelaDemoInitializer::perteneceAlSeed).isEmpty()) {
                    throw new IllegalStateException("Colisión de escenario " + marca);
                }
                continue;
            }
            if (v.getEstado() != EstadoVehiculo.DISPONIBLE) {
                throw new IllegalStateException("No se modifica el vehículo comprometido " + v.getPlaca());
            }
            String estado;
            LocalDateTime inicio, fin;
            if (i < EN_CURSO) {
                estado = "EN_CURSO"; inicio = base.minusDays(1L + i % 4); fin = base.plusDays(1L + i % 6);
            } else if (i < EN_CURSO + FINALIZADAS) {
                int n = i - EN_CURSO;
                estado = "FINALIZADA"; inicio = base.minusDays(6L + n * 2); fin = inicio.plusDays(2L + n % 3);
            } else if (i < EN_CURSO + FINALIZADAS + CONFIRMADAS) {
                int n = i - EN_CURSO - FINALIZADAS;
                estado = "CONFIRMADA"; inicio = base.plusDays(1L + n * 2); fin = inicio.plusDays(2L + n % 4);
            } else if (i < TOTAL - CANCELADAS) {
                estado = "PENDIENTE"; inicio = base.plusDays(2L + (i % 6) * 3); fin = inicio.plusDays(2);
            } else {
                estado = "CANCELADA"; inicio = base.minusDays(4L + i % 4 * 3); fin = inicio.plusDays(2);
            }
            var conflictos = jdbc.query("""
                    SELECT id FROM reservaciones WHERE vehiculo_id = ?
                    AND (estado = 'EN_CURSO' OR (estado IN ('PENDIENTE','CONFIRMADA')
                        AND fecha_inicio < ? AND fecha_fin > ?)) FOR UPDATE
                    """, (rs, fila) -> rs.getLong(1), v.getId(), fin, inicio);
            if (!conflictos.isEmpty()) throw new IllegalStateException("Traslape previo en " + v.getPlaca());
            var precios = tarifas.buscarTraslapes(v.getCategoria().getId(), inicio.toLocalDate(), inicio.toLocalDate());
            if (precios.size() != 1) throw new IllegalStateException("Se requiere una tarifa vigente para " + v.getPlaca());
            BigDecimal precio = precios.get(0).getPrecioDia();
            Reservacion r = new Reservacion();
            r.setClienteId(activos.get(i % activos.size())); r.setVehiculoId(v.getId());
            r.setCreadoPorId(agente); r.setFechaInicio(inicio); r.setFechaFin(fin);
            r.setEstado(estado); r.setTarifaDia(precio);
            r.setTotalEstimado(precio.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(inicio, fin))));
            r.setObservaciones(marca);
            r = reservaciones.saveAndFlush(r);
            if ("CONFIRMADA".equals(estado)) marcar(v, EstadoVehiculo.RESERVADO);
            if ("EN_CURSO".equals(estado) || "FINALIZADA".equals(estado)) {
                BigDecimal entrada = v.getKilometraje();
                BigDecimal salida = "FINALIZADA".equals(estado)
                        ? entrada.subtract(new BigDecimal("620.0")).max(BigDecimal.ZERO) : entrada;
                Entrega e = crearEntrega(r.getId(), agente, inicio, salida);
                if ("EN_CURSO".equals(estado)) marcar(v, EstadoVehiculo.RENTADO);
                else {
                    // Historial sintético: el odómetro actual coincide con la devolución más reciente.
                    crearDevolucion(e.getId(), supervisor, fin, entrada, r.getTotalEstimado());
                    v.setKilometraje(entrada);
                    vehiculos.save(v);
                }
            }
        }
        reservaciones.flush();
    }

    private Entrega crearEntrega(Long reservacionId, Long staffId, LocalDateTime fecha, BigDecimal kmSalida) {
        Entrega e = new Entrega();
        e.setReservacionId(reservacionId);
        e.setEntregadoPorId(staffId);
        e.setFechaEntrega(fecha);
        e.setKilometrajeSalida(kmSalida);
        e.setCombustibleSalida(new BigDecimal("100.00"));
        e.setCondicionSalida("Entrega demo S5-04: vehículo limpio, sin daños aparentes.");
        return entregas.save(e);
    }

    private void crearDevolucion(Long entregaId, Long staffId, LocalDateTime fecha,
            BigDecimal kmEntrada, BigDecimal totalFinal) {
        Devolucion d = new Devolucion();
        d.setEntregaId(entregaId);
        d.setRecibidoPorId(staffId);
        d.setFechaDevolucion(fecha);
        d.setKilometrajeEntrada(kmEntrada);
        d.setCombustibleEntrada(new BigDecimal("55.00"));
        d.setCondicionEntrada("Devolución demo S5-04: recibido conforme.");
        d.setCargoAtraso(BigDecimal.ZERO);
        d.setCargoDanos(BigDecimal.ZERO);
        d.setTotalFinal(totalFinal);
        devoluciones.save(d);
    }

    private void marcar(Vehiculo vehiculo, EstadoVehiculo estado) {
        vehiculo.setEstado(estado);
        vehiculos.save(vehiculo);
    }

    private Long staff(String correo) {
        var usuario = usuarios.findByCorreo(correo)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()) && u.getRol() != null
                        && Boolean.TRUE.equals(u.getRol().getActivo())
                        && List.of("ADMINISTRADOR", "AGENTE", "SUPERVISOR").contains(u.getRol().getNombre()))
                .orElseThrow(() -> new IllegalStateException("Falta operador demo activo autorizado: " + correo));
        return usuario.getId();
    }
}
