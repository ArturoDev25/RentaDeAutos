package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.Categoria;
import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ejecuta la consulta de búsqueda de vehículos (S2-07) contra una base H2
 * en memoria, para comprobar que el JPQL filtra correctamente.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class VehiculoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    private Categoria compacto;
    private Categoria suv;

    @BeforeEach
    void cargarDatos() {
        compacto = categoria("Compacto");
        suv = categoria("SUV");

        vehiculo(compacto, "FAK-1234", "3N1AB7AP0FY123456", "Nissan", "Versa", 2022,
                EstadoVehiculo.DISPONIBLE);
        vehiculo(compacto, "TOY-5001", "JTDBR32E720123456", "Toyota", "Corolla", 2019,
                EstadoVehiculo.DISPONIBLE);
        vehiculo(suv, "HON-7788", "5J6RW2H89NL123456", "Honda", "CR-V", 2024,
                EstadoVehiculo.MANTENIMIENTO);

        em.flush();
        em.clear();
    }

    private Categoria categoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDepositoBase(new BigDecimal("1000.00"));
        categoria.setActivo(true);
        return em.persist(categoria);
    }

    private void vehiculo(Categoria categoria, String placa, String vin, String marca,
                          String modelo, int anio, EstadoVehiculo estado) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setCategoria(categoria);
        vehiculo.setPlaca(placa);
        vehiculo.setVin(vin);
        vehiculo.setMarca(marca);
        vehiculo.setModelo(modelo);
        vehiculo.setAnio(anio);
        vehiculo.setKilometraje(new BigDecimal("1000.0"));
        vehiculo.setEstado(estado);
        em.persist(vehiculo);
    }

    private List<String> placas(List<Vehiculo> vehiculos) {
        return vehiculos.stream().map(Vehiculo::getPlaca).toList();
    }

    @Test
    @DisplayName("Sin filtros devuelve todos los vehículos")
    void sinFiltros() {
        assertEquals(3, vehiculoRepository.buscar(null, null, null, null, null).size());
    }

    @Test
    @DisplayName("El texto busca en la marca sin distinguir mayúsculas")
    void textoEnMarca() {
        assertEquals(List.of("FAK-1234"),
                placas(vehiculoRepository.buscar("%nissan%", null, null, null, null)));
    }

    @Test
    @DisplayName("El texto busca en el modelo")
    void textoEnModelo() {
        assertEquals(List.of("TOY-5001"),
                placas(vehiculoRepository.buscar("%corolla%", null, null, null, null)));
    }

    @Test
    @DisplayName("El texto busca en la placa y el VIN")
    void textoEnPlacaYVin() {
        assertEquals(List.of("HON-7788"),
                placas(vehiculoRepository.buscar("%hon-77%", null, null, null, null)));
        assertEquals(List.of("TOY-5001"),
                placas(vehiculoRepository.buscar("%jtdbr%", null, null, null, null)));
    }

    @Test
    @DisplayName("Filtra por estado")
    void porEstado() {
        assertEquals(List.of("HON-7788"), placas(vehiculoRepository.buscar(
                null, EstadoVehiculo.MANTENIMIENTO, null, null, null)));
    }

    @Test
    @DisplayName("Filtra por categoría y rango de años combinados")
    void porCategoriaYAnios() {
        assertEquals(List.of("FAK-1234"), placas(vehiculoRepository.buscar(
                null, null, compacto.getId(), 2020, 2024)));
    }

    @Test
    @DisplayName("Un comodín escapado se busca como texto literal")
    void comodinEscapado() {
        // "%!%%" es lo que genera el servicio cuando el usuario escribe "%".
        assertTrue(vehiculoRepository.buscar("%!%%", null, null, null, null).isEmpty());
    }

    @Test
    @DisplayName("Si ningún vehículo coincide devuelve una lista vacía")
    void sinCoincidencias() {
        assertTrue(vehiculoRepository.buscar("%tesla%", null, null, null, null).isEmpty());
    }
}
