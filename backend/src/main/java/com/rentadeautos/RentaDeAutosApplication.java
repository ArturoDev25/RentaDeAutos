package com.rentadeautos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Punto de entrada principal de la aplicación Renta de Autos.
 *
 * <p>DataSourceAutoConfiguration e HibernateJpaAutoConfiguration se excluyen
 * temporalmente para permitir que la aplicación arranque sin una base de datos activa.
 * Estas exclusiones se eliminarán en la issue S1-04 al configurar la conexión real a MySQL.</p>
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class RentaDeAutosApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentaDeAutosApplication.class, args);
    }
}
