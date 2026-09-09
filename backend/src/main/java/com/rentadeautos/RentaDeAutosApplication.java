package com.rentadeautos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal de la aplicación Renta de Autos.
 *
 * <p>DataSource y JPA se auto-configuran desde {@code application.yml}.
 * La conexión a MySQL se gestiona vía HikariCP y se valida contra el
 * esquema oficial definido en {@code V1__init_schema.sql} (S1-04).</p>
 */
@SpringBootApplication
public class RentaDeAutosApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentaDeAutosApplication.class, args);
    }
}
