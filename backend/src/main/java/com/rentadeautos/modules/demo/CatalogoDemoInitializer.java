package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.vehicle.dto.TarifaRequest;
import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import com.rentadeautos.modules.vehicle.repository.CategoriaRepository;
import com.rentadeautos.modules.vehicle.repository.TarifaRepository;
import com.rentadeautos.modules.vehicle.repository.VehiculoRepository;
import com.rentadeautos.modules.vehicle.service.TarifaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

@Component
@Order(40)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class CatalogoDemoInitializer implements CommandLineRunner {
    private enum Clase {
        ECONOMICO("Demo Económico", "2000.00", "500.00"),
        SUV("Demo SUV", "3500.00", "850.00"),
        LUJO("Demo Lujo", "15000.00", "1800.00");

        final String nombre;
        final BigDecimal deposito;
        final BigDecimal precio;
        Clase(String nombre, String deposito, String precio) {
            this.nombre = nombre;
            this.deposito = new BigDecimal(deposito);
            this.precio = new BigDecimal(precio);
        }
    }

    private record Datos(Clase clase, String marca, String modelo, int anio, String foto) { }
    private static final List<Datos> DATOS = List.of(
            new Datos(Clase.ECONOMICO, "Nissan", "Tsuru", 2017, "Tsuru.jpg"),
            new Datos(Clase.SUV, "Audi", "Q7", 2023, "Audiq7.jpg"),
            new Datos(Clase.LUJO, "Tesla", "Model 3", 2024, "tesla.jpg"),
            new Datos(Clase.LUJO, "BMW", "Serie 3", 2022, "bmw.jpg"),
            new Datos(Clase.LUJO, "Ferrari", "F8 Tributo", 2022, "Ferrari.jpg"));

    private final CategoriaRepository categorias;
    private final VehiculoRepository vehiculos;
    private final TarifaRepository tarifas;
    private final TarifaService servicioTarifas;

    public CatalogoDemoInitializer(CategoriaRepository categorias, VehiculoRepository vehiculos,
            TarifaRepository tarifas, TarifaService servicioTarifas) {
        this.categorias = categorias;
        this.vehiculos = vehiculos;
        this.tarifas = tarifas;
        this.servicioTarifas = servicioTarifas;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate hoy = LocalDate.now();
        var grupos = new EnumMap<Clase, Categoria>(Clase.class);
        for (Clase clase : Clase.values()) {
            Categoria categoria = categorias.findByNombreIgnoreCase(clase.nombre)
                    .orElseGet(() -> crearCategoria(clase));
            asegurarTarifa(categoria, clase, hoy);
            grupos.put(clase, categoria);
        }
        for (int i = 0; i < DATOS.size(); i++) {
            String placa = String.format(Locale.ROOT, "S504-DEMO-%02d", i + 1);
            String vin = String.format(Locale.ROOT, "S504DEMA%09d", i + 1);
            var existente = vehiculos.findByPlacaIgnoreCase(placa);
            if (existente.isPresent()) {
                if (!vin.equalsIgnoreCase(existente.get().getVin())) {
                    throw new IllegalStateException("Colisión de placa demo: " + placa);
                }
                continue;
            }
            if (vehiculos.existsByVinIgnoreCase(vin)) {
                throw new IllegalStateException("Colisión de VIN demo: " + vin);
            }

            Datos datos = DATOS.get(i);
            Vehiculo vehiculo = new Vehiculo();
            vehiculo.setCategoria(grupos.get(datos.clase()));
            vehiculo.setPlaca(placa);
            vehiculo.setVin(vin);
            vehiculo.setMarca(datos.marca());
            vehiculo.setModelo(datos.modelo());
            vehiculo.setAnio(datos.anio());
            vehiculo.setColor("Blanco");
            vehiculo.setKilometraje(new BigDecimal("12000.0"));
            vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
            vehiculo.setImagen(cargarFoto(datos.foto()));
            vehiculos.save(vehiculo);
        }
        vehiculos.flush();
    }

    private Categoria crearCategoria(Clase clase) {
        Categoria categoria = new Categoria();
        categoria.setNombre(clase.nombre);
        categoria.setDescripcion("Categoría ficticia S5-04");
        categoria.setDepositoBase(clase.deposito);
        categoria.setActivo(true);
        return categorias.saveAndFlush(categoria);
    }

    private void asegurarTarifa(Categoria referencia, Clase clase, LocalDate hoy) {
        Categoria categoria = categorias.bloquearPorId(referencia.getId()).orElseThrow();
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalStateException("La categoría demo está inactiva: " + clase.nombre);
        }
        var existentes = tarifas.buscarTraslapes(categoria.getId(), hoy, null);
        long vigentes = existentes.stream().filter(t -> !t.getFechaInicio().isAfter(hoy)).count();
        if (vigentes > 1) {
            throw new IllegalStateException("Más de una tarifa vigente en " + clase.nombre);
        }
        if (vigentes == 1) {
            return;
        }
        LocalDate fin = existentes.stream().map(t -> t.getFechaInicio()).min(LocalDate::compareTo)
                .map(fecha -> fecha.minusDays(1)).orElse(null);
        servicioTarifas.crear(new TarifaRequest(categoria.getId(), clase.precio,
                new BigDecimal("100.00"), hoy, fin));
    }

    private String cargarFoto(String nombre) {
        try (var archivo = new ClassPathResource("demo/fotos/" + nombre).getInputStream()) {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(archivo.readAllBytes());
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo cargar la foto demo: " + nombre, error);
        }
    }
}
