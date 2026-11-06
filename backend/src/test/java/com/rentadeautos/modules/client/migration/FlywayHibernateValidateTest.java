package com.rentadeautos.modules.client.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de arranque de contexto completo con:
 * 1. Base H2 en memoria dedicada en MODE=MySQL (o MySQL real con las variables TEST_DB_*).
 * 2. Flyway ejecutando todas las migraciones.
 * 3. Hibernate ddl-auto: validate (Criterio de aceptación 7).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL_PREFIX:jdbc:h2:mem:}validate_v5_db${TEST_DB_URL_SUFFIX:;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE}",
        "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
        "spring.datasource.username=${TEST_DB_USER:sa}",
        "spring.datasource.password=${TEST_DB_PASSWORD:}",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.clean-disabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
class FlywayHibernateValidateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("El contexto de Spring Boot inicia exitosamente aplicando Flyway con Hibernate en modo validate")
    void contextLoadsConFlywayYHibernateValidate() {
        assertNotNull(jdbcTemplate);

        // Verificar que la tabla clientes existe
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes", Integer.class);
        assertNotNull(count);

        // Historial de Flyway consultado por la API de Flyway: funciona igual en H2 y en MySQL
        long migraciones = Arrays.stream(flyway.info().applied())
                .filter(m -> m.getVersion() != null && "5".equals(m.getVersion().getVersion()))
                .filter(m -> m.getState().isApplied())
                .count();
        assertTrue(migraciones >= 1, "La migración V5 debe estar registrada y exitosa en el historial de Flyway");
    }
}
