package com.rentadeautos.modules.vehicle;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos de demostración de vehículos (S2-03).
 *
 * <p>Solo corre en los perfiles de desarrollo ("dev" y "default"), nunca en
 * pruebas. Es idempotente: una categoría se crea solo si no existe otra con
 * el mismo nombre y un vehículo solo si su placa y su VIN no están
 * registrados, así que reiniciar el backend no duplica nada ni toca los
 * vehículos capturados a mano.</p>
 */
@Component
@Profile({"dev", "default"})
public class VehiculoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VehiculoDataInitializer.class);

    private final CategoriaRepository categoriaRepository;
    private final VehiculoRepository vehiculoRepository;

    public VehiculoDataInitializer(CategoriaRepository categoriaRepository,
                                   VehiculoRepository vehiculoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    private record CategoriaDemo(String nombre, String descripcion, String deposito) { }

    private record VehiculoDemo(String categoria, String placa, String vin, String marca,
                                String modelo, int anio, String color, String kilometraje,
                                EstadoVehiculo estado) { }

    private static final List<CategoriaDemo> CATEGORIAS = List.of(
            new CategoriaDemo("Económico", "Autos compactos de bajo consumo", "2000.00"),
            new CategoriaDemo("SUV", "Camionetas familiares", "3500.00"),
            new CategoriaDemo("Pickup", "Camionetas de carga", "4000.00"),
            new CategoriaDemo("Lujo", "Vehículos premium y deportivos", "15000.00")
    );

    private static final List<VehiculoDemo> VEHICULOS = List.of(
            new VehiculoDemo("Económico", "ABC-1234", "1HGCM82633A004352", "Toyota", "Corolla", 2024, "Blanco", "12500.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("SUV", "DEF-4567", "3N1CP5DV1PL123456", "Nissan", "Kicks", 2023, "Rojo", "28400.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("Pickup", "GHI-7890", "1FTER4FH5RLA12345", "Ford", "Ranger", 2024, "Gris", "9800.0", EstadoVehiculo.RENTADO),
            new VehiculoDemo("Económico", "JKL-0123", "9BGEB69H0PG123456", "Chevrolet", "Onix", 2023, "Azul", "35100.0", EstadoVehiculo.MANTENIMIENTO),
            new VehiculoDemo("SUV", "MNO-3456", "JM3KFBCL1R0123456", "Mazda", "CX-5", 2024, "Blanco", "7600.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("Económico", "PQR-6789", "3N1EB31S0ZK123456", "Nissan", "Tsuru", 2017, "Blanco", "98500.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("Lujo", "STU-9012", "WA1LAAF78KD012345", "Audi", "Q7", 2023, "Negro", "18200.0", EstadoVehiculo.RESERVADO),
            new VehiculoDemo("Lujo", "VWX-2345", "WBA5R1C05LFH12345", "BMW", "Serie 3", 2022, "Blanco", "31000.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("Lujo", "YZA-5678", "5YJ3E1EA8PF123456", "Tesla", "Model 3", 2024, "Blanco", "5400.0", EstadoVehiculo.RENTADO),
            new VehiculoDemo("Lujo", "BCD-8901", "ZFF79ALA8L0123456", "Ferrari", "F8 Tributo", 2022, "Rojo", "3200.0", EstadoVehiculo.MANTENIMIENTO),
            new VehiculoDemo("Pickup", "EFG-1122", "3TMCZ5AN1PM123456", "Toyota", "Tacoma", 2023, "Plata", "22700.0", EstadoVehiculo.DISPONIBLE),
            new VehiculoDemo("Económico", "HJK-3344", "3VWFE21C04M123456", "Volkswagen", "Vento", 2021, "Gris", "54300.0", EstadoVehiculo.BAJA)
    );

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Categoria> categorias = asegurarCategorias();

        int creados = 0;
        for (VehiculoDemo demo : VEHICULOS) {
            if (vehiculoRepository.existsByPlacaIgnoreCase(demo.placa())
                    || vehiculoRepository.existsByVinIgnoreCase(demo.vin())) {
                continue;
            }

            Vehiculo vehiculo = new Vehiculo();
            vehiculo.setCategoria(categorias.get(clave(demo.categoria())));
            vehiculo.setPlaca(demo.placa());
            vehiculo.setVin(demo.vin());
            vehiculo.setMarca(demo.marca());
            vehiculo.setModelo(demo.modelo());
            vehiculo.setAnio(demo.anio());
            vehiculo.setColor(demo.color());
            vehiculo.setKilometraje(new BigDecimal(demo.kilometraje()));
            vehiculo.setEstado(demo.estado());
            vehiculoRepository.save(vehiculo);
            creados++;
        }

        if (creados > 0) {
            log.info("Datos demo: se registraron {} vehículos de demostración", creados);
        }
    }

    /** Devuelve las categorías indexadas por {@link #clave(String)}, creando las demo que falten. */
    private Map<String, Categoria> asegurarCategorias() {
        Map<String, Categoria> existentes = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(c -> clave(c.getNombre()),
                        Function.identity(), (a, b) -> a));

        for (CategoriaDemo demo : CATEGORIAS) {
            existentes.computeIfAbsent(clave(demo.nombre()), nueva -> {
                Categoria categoria = new Categoria();
                categoria.setNombre(demo.nombre());
                categoria.setDescripcion(demo.descripcion());
                categoria.setDepositoBase(new BigDecimal(demo.deposito()));
                categoria.setActivo(true);
                return categoriaRepository.save(categoria);
            });
        }
        return existentes;
    }

    /**
     * Clave de comparación sin acentos ni mayúsculas, igual que la collation
     * de MySQL: así "Economico" capturado a mano se reutiliza en lugar de
     * intentar crear "Económico" y chocar con la restricción UNIQUE.
     */
    private static String clave(String nombre) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
