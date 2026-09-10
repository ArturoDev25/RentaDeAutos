package com.rentadeautos.modules.auth.repository;

import com.rentadeautos.modules.auth.model.UsuarioApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a la tabla usuarios_app.
 * Spring Data JPA genera la implementación en tiempo de ejecución.
 */
@Repository
public interface UsuarioAppRepository extends JpaRepository<UsuarioApp, Long> {

    Optional<UsuarioApp> findByCorreo(String correo);
}