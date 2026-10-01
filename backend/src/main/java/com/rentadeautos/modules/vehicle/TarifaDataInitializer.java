package com.rentadeautos.modules.vehicle;

import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/** Datos ficticios de tarifas, después de cargar categorías y vehículos demo. */
@Component
@Profile({"dev", "default"})
@Order(20)
public class TarifaDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(TarifaDataInitializer.class);
    private static final Map<String, BigDecimal> PRECIOS = Map.of(
            "economico", new BigDecimal("500.00"), "suv", new BigDecimal("850.00"),
            "pickup", new BigDecimal("1000.00"), "lujo", new BigDecimal("1800.00"));
    private final CategoriaRepository categorias;
    private final TarifaRepository tarifas;
    private final TarifaService servicio;

    public TarifaDataInitializer(CategoriaRepository categorias, TarifaRepository tarifas,
            TarifaService servicio) {
        this.categorias = categorias;
        this.tarifas = tarifas;
        this.servicio = servicio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate hoy = LocalDate.now();
        int creadas = 0;
        // Orden consistente con los bloqueos del CRUD al cambiar de categoría.
        for (Categoria referencia : categorias.findAll(Sort.by("id"))) {
            Categoria categoria = categorias.bloquearPorId(referencia.getId()).orElseThrow();
            if (!Boolean.TRUE.equals(categoria.getActivo())) {
                continue;
            }
            var existentes = tarifas.buscarTraslapes(categoria.getId(), hoy, null);
            long vigentesHoy = existentes.stream().filter(t -> !t.getFechaInicio().isAfter(hoy)).count();
            if (vigentesHoy > 1) {
                throw new IllegalStateException("Hay varias tarifas vigentes para la categoría "
                        + categoria.getId() + "; corregir el catálogo antes de cargar la demo");
            }
            if (vigentesHoy == 1) {
                continue;
            }
            LocalDate fin = existentes.stream().map(t -> t.getFechaInicio())
                    .min(Comparator.naturalOrder()).map(fecha -> fecha.minusDays(1)).orElse(null);
            String clave = Normalizer.normalize(categoria.getNombre(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
            BigDecimal precio = PRECIOS.getOrDefault(clave, new BigDecimal("850.00"));
            servicio.crear(new TarifaRequest(categoria.getId(), precio,
                    new BigDecimal("100.00"), hoy, fin));
            creadas++;
        }
        log.info("Datos demo ficticios: se crearon {} tarifas; se conservaron las existentes", creadas);
    }
}
