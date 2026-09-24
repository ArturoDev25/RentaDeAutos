package com.rentadeautos.modules.vehicle.repository;

import com.rentadeautos.modules.vehicle.model.EstadoVehiculo;
import com.rentadeautos.modules.vehicle.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Acceso a datos de los vehículos.
 */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);

    boolean existsByVinIgnoreCase(String vin);

    boolean existsByVinIgnoreCaseAndIdNot(String vin, Long id);

    /**
     * Busca vehículos con su categoría (S2-07). Los criterios nulos se ignoran.
     *
     * <p>{@code patron} ya llega en minúsculas, rodeado de {@code %} y con los
     * comodines del usuario escapados con {@code !}, para que "50%" o "A_B"
     * se busquen como texto literal.</p>
     */
    @Query("""
            SELECT v FROM Vehiculo v
            JOIN FETCH v.categoria c
            WHERE (:estado IS NULL OR v.estado = :estado)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:anioDesde IS NULL OR v.anio >= :anioDesde)
              AND (:anioHasta IS NULL OR v.anio <= :anioHasta)
              AND (:patron IS NULL
                   OR LOWER(v.placa)  LIKE :patron ESCAPE '!'
                   OR LOWER(v.vin)    LIKE :patron ESCAPE '!'
                   OR LOWER(v.marca)  LIKE :patron ESCAPE '!'
                   OR LOWER(v.modelo) LIKE :patron ESCAPE '!')
            ORDER BY v.id ASC
            """)
    List<Vehiculo> buscar(@Param("patron") String patron,
                          @Param("estado") EstadoVehiculo estado,
                          @Param("categoriaId") Long categoriaId,
                          @Param("anioDesde") Integer anioDesde,
                          @Param("anioHasta") Integer anioHasta);
}
