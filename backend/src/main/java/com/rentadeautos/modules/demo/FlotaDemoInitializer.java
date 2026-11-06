package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Tarifa;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * S5-04: catálogo de demostración amplio y realista. Crea diez categorías con su
 * tarifa vigente y una flota de 150 vehículos repartidos entre ellas, con marcas
 * y modelos reales. Deja 16 vehículos en MANTENIMIENTO y 8 dados de BAJA; el
 * resto quedan DISPONIBLES (las rentas activas y reservaciones los pasan a
 * RENTADO/RESERVADO después, en {@link OperacionDemoInitializer}).
 *
 * <p>No usa fotos (se asignan aparte si se desea). Idempotente: reutiliza las
 * categorías por nombre y conserva los vehículos con placa y VIN coincidentes. Transaccional.
 * Gated por perfil {@code demo} + {@code app.demo.seed.enabled}.</p>
 */
@Component
@Order(55)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class FlotaDemoInitializer implements CommandLineRunner {

    private final CategoriaRepository categorias;
    private final TarifaRepository tarifas;
    private final VehiculoRepository vehiculos;
    private final TarifaService servicioTarifas;

    /** Categoría demo: nombre, depósito, precio por día y cuántos vehículos genera. */
    private record Cat(String nombre, String deposito, String precio, int cantidad, String[] modelos) { }

    private static final List<Cat> CATALOGO = List.of(
            new Cat("Económico", "1500.00", "450.00", 20, new String[] {
                    "Nissan|March", "Chevrolet|Aveo", "Kia|Rio", "Hyundai|Grand i10",
                    "Suzuki|Ignis", "Renault|Kwid", "Volkswagen|Gol", "Chevrolet|Beat" }),
            new Cat("Compacto", "2000.00", "600.00", 20, new String[] {
                    "Nissan|Versa", "Volkswagen|Virtus", "Chevrolet|Onix", "Kia|Forte",
                    "Hyundai|Accent", "Mazda|Mazda 2", "Toyota|Yaris", "Seat|Ibiza" }),
            new Cat("Sedán", "2500.00", "800.00", 20, new String[] {
                    "Toyota|Corolla", "Honda|Civic", "Mazda|Mazda 3", "Nissan|Sentra",
                    "Volkswagen|Jetta", "Hyundai|Elantra", "Kia|K3", "Chevrolet|Cavalier" }),
            new Cat("Familiar", "3000.00", "900.00", 12, new String[] {
                    "Toyota|Avanza", "Suzuki|Ertiga", "Kia|Carens", "Chevrolet|Spin",
                    "Mitsubishi|Xpander", "Dodge|Journey" }),
            new Cat("SUV", "4000.00", "1100.00", 24, new String[] {
                    "Mazda|CX-5", "Honda|CR-V", "Toyota|RAV4", "Nissan|X-Trail",
                    "Volkswagen|Tiguan", "Hyundai|Tucson", "Kia|Sportage", "Jeep|Compass",
                    "Chevrolet|Tracker" }),
            new Cat("Pickup", "3500.00", "1000.00", 16, new String[] {
                    "Toyota|Hilux", "Ford|Ranger", "Chevrolet|Silverado", "RAM|1500",
                    "Nissan|Frontier", "Volkswagen|Amarok", "Toyota|Tacoma" }),
            new Cat("Minivan", "4000.00", "1200.00", 10, new String[] {
                    "Chrysler|Pacifica", "Honda|Odyssey", "Kia|Carnival",
                    "Toyota|Sienna", "Volkswagen|Caddy" }),
            new Cat("Eléctrico", "6000.00", "1500.00", 10, new String[] {
                    "Tesla|Model 3", "Nissan|Leaf", "BYD|Dolphin", "Chevrolet|Bolt",
                    "Hyundai|Ioniq 5", "Kia|EV6" }),
            new Cat("Deportivo", "10000.00", "2500.00", 8, new String[] {
                    "Ford|Mustang", "Chevrolet|Camaro", "Dodge|Challenger",
                    "Nissan|Z", "Toyota|GR Supra", "BMW|Z4" }),
            new Cat("Lujo", "15000.00", "3200.00", 10, new String[] {
                    "Mercedes-Benz|Clase C", "BMW|Serie 3", "Audi|A4", "Lexus|ES",
                    "Volvo|S60", "Genesis|G70", "Jaguar|XE" }));

    private static final String[] COLORES = {
            "Blanco", "Negro", "Gris Oxford", "Plata", "Azul Acero",
            "Rojo", "Arena", "Gris Grafito", "Verde Militar" };
    private static final String[] PREFIJOS_PLACA = {
            "NLX", "JAL", "CMX", "GTO", "PUE", "VRZ", "QRO", "SLP", "CHH", "MIC" };

    public FlotaDemoInitializer(CategoriaRepository categorias, TarifaRepository tarifas,
            VehiculoRepository vehiculos, TarifaService servicioTarifas) {
        this.categorias = categorias;
        this.tarifas = tarifas;
        this.vehiculos = vehiculos;
        this.servicioTarifas = servicioTarifas;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate inicioTarifa = LocalDate.now().withDayOfMonth(1).minusMonths(6);
        Set<String> placasExistentes = new HashSet<>();
        for (Vehiculo v : vehiculos.findAll()) {
            if (v.getPlaca() != null) {
                placasExistentes.add(v.getPlaca().toUpperCase(Locale.ROOT));
            }
        }

        int indice = 0;
        for (Cat cat : CATALOGO) {
            Categoria categoria = categorias.findByNombreIgnoreCase(cat.nombre())
                    .orElseGet(() -> crearCategoriaConTarifa(cat, inicioTarifa));
            asegurarTarifas(categoria, cat, inicioTarifa);
            List<Vehiculo> nuevos = new ArrayList<>();
            for (int n = 0; n < cat.cantidad(); n++) {
                indice++;
                String placa = placa(indice);
                if (placasExistentes.contains(placa.toUpperCase(Locale.ROOT))) {
                    var existente = vehiculos.findByPlacaIgnoreCase(placa).orElseThrow();
                    if (!vin(indice).equalsIgnoreCase(existente.getVin())) {
                        throw new IllegalStateException("Colisión de placa de flota demo: " + placa);
                    }
                    continue;
                }
                if (vehiculos.existsByVinIgnoreCase(vin(indice))) {
                    throw new IllegalStateException("Colisión de VIN de flota demo");
                }
                String[] modelo = cat.modelos()[n % cat.modelos().length].split("\\|");
                Vehiculo vehiculo = new Vehiculo();
                vehiculo.setCategoria(categoria);
                vehiculo.setPlaca(placa);
                vehiculo.setVin(vin(indice));
                vehiculo.setMarca(modelo[0]);
                vehiculo.setModelo(modelo[1]);
                vehiculo.setAnio(2017 + (indice % 9));
                vehiculo.setColor(COLORES[indice % COLORES.length]);
                vehiculo.setKilometraje(new BigDecimal(10000 + (indice * 373 % 90000) + ".0"));
                vehiculo.setEstado(estadoInicial(indice));
                nuevos.add(vehiculo);
            }
            vehiculos.saveAll(nuevos);
        }
        vehiculos.flush();
    }

    private Categoria crearCategoriaConTarifa(Cat cat, LocalDate inicioTarifa) {
        Categoria categoria = new Categoria();
        categoria.setNombre(cat.nombre());
        categoria.setDescripcion("Categoría " + cat.nombre() + " (flota de demostración S5-04)");
        categoria.setDepositoBase(new BigDecimal(cat.deposito()));
        categoria.setActivo(true);
        categoria = categorias.saveAndFlush(categoria);

        return categoria;
    }

    private void asegurarTarifas(Categoria referencia, Cat cat, LocalDate inicio) {
        var categoria = categorias.bloquearPorId(referencia.getId()).orElseThrow();
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalStateException("Categoría inactiva: " + categoria.getNombre());
        }
        var existentes = tarifas.buscarTraslapes(categoria.getId(), inicio, null).stream()
                .sorted(java.util.Comparator.comparing(Tarifa::getFechaInicio)).toList();
        for (int i = 1; i < existentes.size(); i++) {
            var previo = existentes.get(i - 1);
            if (previo.getFechaFin() == null || !existentes.get(i).getFechaInicio().isAfter(previo.getFechaFin())) {
                throw new IllegalStateException("Tarifas activas traslapadas en " + categoria.getNombre());
            }
        }
        LocalDate cursor = inicio;
        LocalDate anteriorFin = null;
        boolean anterior = false;
        for (Tarifa tarifa : existentes) {
            if (anterior && (anteriorFin == null || !tarifa.getFechaInicio().isAfter(anteriorFin))) {
                throw new IllegalStateException("Tarifas activas traslapadas en " + categoria.getNombre());
            }
            if (tarifa.getFechaInicio().isAfter(cursor)) {
                servicioTarifas.crear(new TarifaRequest(categoria.getId(), new BigDecimal(cat.precio()),
                        new BigDecimal(cat.precio()), cursor, tarifa.getFechaInicio().minusDays(1)));
            }
            if (tarifa.getFechaFin() == null) return;
            cursor = tarifa.getFechaFin().plusDays(1);
            anteriorFin = tarifa.getFechaFin();
            anterior = true;
        }
        servicioTarifas.crear(new TarifaRequest(categoria.getId(), new BigDecimal(cat.precio()),
                new BigDecimal(cat.precio()), cursor, null));
    }

    static boolean perteneceAlSeed(Vehiculo vehiculo) {
        for (int i = 1; i <= 150; i++) {
            if (placa(i).equals(vehiculo.getPlaca()) && vin(i).equals(vehiculo.getVin())) return true;
        }
        return false;
    }

    static boolean aptoParaEscenario(Vehiculo vehiculo) {
        for (int i = 1; i <= 150; i++) {
            if (placa(i).equals(vehiculo.getPlaca()) && vin(i).equals(vehiculo.getVin())) {
                return estadoInicial(i) == EstadoVehiculo.DISPONIBLE;
            }
        }
        return false;
    }

    /** 16 en mantenimiento (múltiplos de 9) y 8 de baja (múltiplos de 17); el resto disponibles. */
    private static EstadoVehiculo estadoInicial(int indice) {
        if (indice % 9 == 0) {
            return EstadoVehiculo.MANTENIMIENTO;
        }
        if (indice % 17 == 0) {
            return EstadoVehiculo.BAJA;
        }
        return EstadoVehiculo.DISPONIBLE;
    }

    private static String placa(int indice) {
        return String.format(Locale.ROOT, "S504-FLT-%03d", indice);
    }

    private static String vin(int indice) {
        return String.format(Locale.ROOT, "3MXRNT%011d", indice); // 6 + 11 = 17 caracteres
    }
}
