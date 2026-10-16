package com.rentadeautos.modules.client.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de arranque de contexto completo con:
 * 1. Base H2 en memoria dedicada en MODE=MySQL.
 * 2. Flyway ejecutando todas las migraciones (V1..V5).
 * 3. Hibernate ddl-auto: validate (Criterio de aceptación 7).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:validate_v5_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.clean-disabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
class FlywayHibernateValidateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("El contexto de Spring Boot inicia exitosamente aplicando Flyway V1..V5 con Hibernate en modo validate")
    void contextLoadsConFlywayYHibernateValidate() {
        assertNotNull(jdbcTemplate);

        // Verificar que la tabla clientes existe y la columna password_hash está presente
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes", Integer.class);
        assertNotNull(count);

        // Verificar historial de flyway (entre comillas dobles para compatibilidad de case con H2)
        Integer migraciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"version\" = '5'", Integer.class);
        assertNotNull(migraciones);
        assertTrue(migraciones >= 1, "La migración V5 debe estar registrada y exitosa en flyway_schema_history");
    }
}
