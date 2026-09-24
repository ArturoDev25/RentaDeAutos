package com.rentadeautos.modules.audit.repository;

import com.rentadeautos.modules.audit.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    @Query("SELECT COUNT(DISTINCT a.usuario.id) FROM Auditoria a WHERE a.usuario IS NOT NULL")
    long countDistinctUsuariosActivos();

    long countByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    long countByResultadoIgnoreCase(String resultado);

    @Query("SELECT DISTINCT a.entidad FROM Auditoria a ORDER BY a.entidad ASC")
    List<String> findDistinctEntidades();

    @Query("SELECT DISTINCT a.accion FROM Auditoria a ORDER BY a.accion ASC")
    List<String> findDistinctAcciones();
}
