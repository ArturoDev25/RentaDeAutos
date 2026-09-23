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
     * Lista los vehículos con su categoría; los filtros nulos se ignoran.
     */
    @Query("""
            SELECT v FROM Vehiculo v
            JOIN FETCH v.categoria c
            WHERE (:estado IS NULL OR v.estado = :estado)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
            ORDER BY v.id ASC
            """)
    List<Vehiculo> buscar(@Param("estado") EstadoVehiculo estado,
                          @Param("categoriaId") Long categoriaId);
}
