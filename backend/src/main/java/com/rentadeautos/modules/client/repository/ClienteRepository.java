package com.rentadeautos.modules.client.repository;

import com.rentadeautos.modules.client.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByNumeroLicenciaIgnoreCase(String numeroLicencia);

    boolean existsByNumeroLicenciaIgnoreCaseAndIdNot(String numeroLicencia, Long id);

        @Query("""
                        SELECT c FROM Cliente c
                        WHERE (:activo IS NULL OR c.activo = :activo)
                            AND (:patron IS NULL
                                     OR LOWER(CONCAT(c.nombre, ' ', c.apellidos)) LIKE :patron ESCAPE '!'
                                     OR LOWER(COALESCE(c.correo, '')) LIKE :patron ESCAPE '!'
                                     OR LOWER(c.telefono) LIKE :patron ESCAPE '!'
                                     OR LOWER(c.numeroLicencia) LIKE :patron ESCAPE '!')
                        ORDER BY c.apellidos ASC, c.nombre ASC
                        """)
        List<Cliente> buscar(@Param("patron") String patron, @Param("activo") Boolean activo);
}