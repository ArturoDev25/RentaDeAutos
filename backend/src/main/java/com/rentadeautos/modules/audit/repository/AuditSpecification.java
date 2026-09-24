package com.rentadeautos.modules.audit.repository;

import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AuditSpecification {

    private AuditSpecification() { }

    public static Specification<Auditoria> busquedaGeneral(String q) {
        if (q == null || q.isBlank()) {
            return Specification.where(null);
        }
        String patron = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<Auditoria, UsuarioApp> usuarioJoin = root.join("usuario", JoinType.LEFT);
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.like(cb.lower(root.get("accion")), patron));
            predicados.add(cb.like(cb.lower(root.get("entidad")), patron));
            predicados.add(cb.like(cb.lower(root.get("direccionIp")), patron));
            predicados.add(cb.like(cb.lower(usuarioJoin.get("nombre")), patron));
            return cb.or(predicados.toArray(new Predicate[0]));
        };
    }

    public static Specification<Auditoria> porUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Auditoria> porModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("entidad")), modulo.toLowerCase());
    }

    public static Specification<Auditoria> porTipoAccion(String tipoAccion) {
        if (tipoAccion == null || tipoAccion.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("accion")), tipoAccion.toLowerCase());
    }

    public static Specification<Auditoria> build(String q, Long usuarioId, String modulo, String tipoAccion) {
        return Specification.where(busquedaGeneral(q))
                .and(porUsuario(usuarioId))
                .and(porModulo(modulo))
                .and(porTipoAccion(tipoAccion));
    }
}
