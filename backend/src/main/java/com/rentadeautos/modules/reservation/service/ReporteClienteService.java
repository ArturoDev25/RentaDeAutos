package com.rentadeautos.modules.reservation.service;

import com.rentadeautos.modules.reservation.dto.*;
import com.rentadeautos.modules.reservation.exception.ReservacionException;
import com.rentadeautos.modules.reservation.repository.ReporteClienteRepository;
import com.rentadeautos.modules.reservation.repository.ReporteClienteRepository.OperacionClienteFila;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReporteClienteService {
    public static final String AVISO_IMPORTES =
            "Los montos son estimados; el cobro final se registra al devolver el vehículo.";

    private static final Set<String> PERIODOS_VALIDOS = Set.of("7D", "30D", "3M", "1A");
    private static final String[] MESES_CORTOS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final ReporteClienteRepository repositorio;

    public ReporteClienteService(ReporteClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ReporteClienteResumenResponse generarResumen(Long clienteId, String periodoParam) {
        String periodo = (periodoParam == null || periodoParam.isBlank())
                ? "3M"
                : periodoParam.strip().toUpperCase(Locale.ROOT);
        if (!PERIODOS_VALIDOS.contains(periodo)) {
            throw new ReservacionException(HttpStatus.BAD_REQUEST,
                    "Periodo inválido. Valores permitidos: 7D, 30D, 3M, 1A");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioActual = calcularInicioPeriodo(ahora, periodo, 1);
        LocalDateTime inicioAnterior = calcularInicioPeriodo(ahora, periodo, 2);

        List<OperacionClienteFila> todas = repositorio.listarOperacionesDesde(clienteId, inicioAnterior);

        List<OperacionClienteFila> actuales = todas.stream()
                .filter(op -> !op.fechaReferencia().isBefore(inicioActual))
                .toList();
        List<OperacionClienteFila> anteriores = todas.stream()
                .filter(op -> op.fechaReferencia().isBefore(inicioActual)
                        && !op.fechaReferencia().isBefore(inicioAnterior))
                .toList();

        Metricas mActual = calcularMetricas(actuales);
        Metricas mAnterior = calcularMetricas(anteriores);

        VariacionKpiResponse variacion = new VariacionKpiResponse(
                variacionPorcentual(BigDecimal.valueOf(mActual.totalRentas), BigDecimal.valueOf(mAnterior.totalRentas)),
                variacionPorcentual(mActual.gastoTotal, mAnterior.gastoTotal),
                variacionPorcentual(BigDecimal.valueOf(mActual.diasRentados), BigDecimal.valueOf(mAnterior.diasRentados)),
                variacionPorcentual(mActual.costoPromedioDia, mAnterior.costoPromedioDia)
        );

        List<SeriePeriodoItem> serie = construirSerie(actuales, ahora.toLocalDate(), periodo);
        List<EstadoResumenItem> distribucion = construirDistribucionEstados(actuales);
        List<HistorialReporteItem> historial = construirHistorial(actuales);

        return new ReporteClienteResumenResponse(
                periodo,
                mActual.totalRentas,
                mActual.gastoTotal,
                mActual.diasRentados,
                mActual.costoPromedioDia,
                variacion,
                serie,
                distribucion,
                historial,
                AVISO_IMPORTES
        );
    }

    private LocalDateTime calcularInicioPeriodo(LocalDateTime base, String periodo, int multiplicador) {
        return switch (periodo) {
            case "7D" -> base.minusDays(7L * multiplicador);
            case "30D" -> base.minusDays(30L * multiplicador);
            case "3M" -> base.minusMonths(3L * multiplicador);
            case "1A" -> base.minusYears(1L * multiplicador);
            default -> base.minusMonths(3L * multiplicador);
        };
    }

    private record Metricas(long totalRentas, BigDecimal gastoTotal, long diasRentados, BigDecimal costoPromedioDia) {}

    private Metricas calcularMetricas(List<OperacionClienteFila> operaciones) {
        List<OperacionClienteFila> rentas = operaciones.stream()
                .filter(op -> "EN_CURSO".equals(op.estado()) || "FINALIZADA".equals(op.estado()))
                .toList();
        long totalRentas = rentas.size();
        BigDecimal gastoTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long diasRentados = 0L;

        for (OperacionClienteFila r : rentas) {
            BigDecimal importe = r.totalFinal() != null ? r.totalFinal()
                    : (r.totalEstimado() != null ? r.totalEstimado() : BigDecimal.ZERO);
            gastoTotal = gastoTotal.add(importe);
            diasRentados += calcularDias(r);
        }

        BigDecimal costoPromedioDia = diasRentados > 0
                ? gastoTotal.divide(BigDecimal.valueOf(diasRentados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new Metricas(totalRentas, gastoTotal.setScale(2, RoundingMode.HALF_UP), diasRentados, costoPromedioDia);
    }

    private long calcularDias(OperacionClienteFila op) {
        LocalDateTime inicio = op.fechaEntrega() != null ? op.fechaEntrega() : op.fechaInicio();
        LocalDateTime fin = op.fechaDevolucionReal() != null ? op.fechaDevolucionReal() : op.fechaFin();
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            return 1L;
        }
        long segundos = Duration.between(inicio, fin).getSeconds();
        return Math.max(1L, 1L + (segundos - 1L) / 86400L);
    }

    private BigDecimal variacionPorcentual(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return actual != null && actual.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("100.0")
                    : new BigDecimal("0.0");
        }
        return actual.subtract(anterior)
                .multiply(new BigDecimal("100"))
                .divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private List<SeriePeriodoItem> construirSerie(List<OperacionClienteFila> actuales, LocalDate hoy, String periodo) {
        List<OperacionClienteFila> noCanceladas = actuales.stream()
                .filter(op -> !"CANCELADA".equals(op.estado()))
                .toList();

        if ("7D".equals(periodo)) {
            List<SeriePeriodoItem> items = new ArrayList<>(7);
            for (int i = 6; i >= 0; i--) {
                LocalDate dia = hoy.minusDays(i);
                long cuenta = noCanceladas.stream()
                        .filter(op -> op.fechaReferencia().toLocalDate().equals(dia))
                        .count();
                items.add(new SeriePeriodoItem(dia.format(FORMATO_DIA_MES), cuenta));
            }
            return items;
        }

        if ("30D".equals(periodo)) {
            List<SeriePeriodoItem> items = new ArrayList<>(4);
            for (int sem = 4; sem >= 1; sem--) {
                LocalDate finSem = hoy.minusDays((sem - 1) * 7L);
                LocalDate iniSem = hoy.minusDays(sem * 7L - 1L);
                long cuenta = noCanceladas.stream()
                        .filter(op -> {
                            LocalDate f = op.fechaReferencia().toLocalDate();
                            return !f.isBefore(iniSem) && !f.isAfter(finSem);
                        })
                        .count();
                items.add(new SeriePeriodoItem("Sem " + (5 - sem), cuenta));
            }
            return items;
        }

        int meses = "1A".equals(periodo) ? 12 : 6;
        List<SeriePeriodoItem> items = new ArrayList<>(meses);
        YearMonth mesActual = YearMonth.from(hoy);
        for (int i = meses - 1; i >= 0; i--) {
            YearMonth ym = mesActual.minusMonths(i);
            long cuenta = noCanceladas.stream()
                    .filter(op -> YearMonth.from(op.fechaReferencia()).equals(ym))
                    .count();
            String etiqueta = MESES_CORTOS[ym.getMonthValue() - 1];
            items.add(new SeriePeriodoItem(etiqueta, cuenta));
        }
        return items;
    }

    private List<EstadoResumenItem> construirDistribucionEstados(List<OperacionClienteFila> actuales) {
        long finalizadas = actuales.stream().filter(o -> "FINALIZADA".equals(o.estado())).count();
        long activas = actuales.stream().filter(o -> "EN_CURSO".equals(o.estado())).count();
        long reservadas = actuales.stream()
                .filter(o -> "PENDIENTE".equals(o.estado()) || "CONFIRMADA".equals(o.estado()))
                .count();
        long canceladas = actuales.stream().filter(o -> "CANCELADA".equals(o.estado())).count();
        long total = finalizadas + activas + reservadas + canceladas;

        return List.of(
                new EstadoResumenItem("FINALIZADA", finalizadas, porcentaje(finalizadas, total)),
                new EstadoResumenItem("EN_CURSO", activas, porcentaje(activas, total)),
                new EstadoResumenItem("RESERVADA", reservadas, porcentaje(reservadas, total)),
                new EstadoResumenItem("CANCELADA", canceladas, porcentaje(canceladas, total))
        );
    }

    private BigDecimal porcentaje(long parte, long total) {
        if (total <= 0) {
            return new BigDecimal("0.0");
        }
        return BigDecimal.valueOf(parte * 100)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private List<HistorialReporteItem> construirHistorial(List<OperacionClienteFila> actuales) {
        return actuales.stream().map(op -> {
            boolean esRenta = "EN_CURSO".equals(op.estado()) || "FINALIZADA".equals(op.estado());
            String prefijo = esRenta ? "REN-" : "RES-";
            String folio = prefijo + String.format("%04d", op.reservacionId());
            String vehiculo = ((op.marca() != null ? op.marca() : "") + " "
                    + (op.modelo() != null ? op.modelo() : "")
                    + (op.anio() != null ? " " + op.anio() : "")).strip();
            LocalDateTime inicio = op.fechaEntrega() != null ? op.fechaEntrega() : op.fechaInicio();
            LocalDateTime fin = op.fechaDevolucionReal() != null ? op.fechaDevolucionReal() : op.fechaFin();
            return new HistorialReporteItem(
                    op.reservacionId(),
                    folio,
                    vehiculo.isEmpty() ? "Vehículo #" + op.reservacionId() : vehiculo,
                    op.categoria(),
                    inicio,
                    fin,
                    calcularDias(op),
                    op.totalEstimado(),
                    op.totalFinal(),
                    op.estado()
            );
        }).toList();
    }
}
