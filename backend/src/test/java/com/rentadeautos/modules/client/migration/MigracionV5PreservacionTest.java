package com.rentadeautos.modules.client.migration;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigracionV5PreservacionTest {

    @Test
    @DisplayName("Ejecuta Flyway V1..V3, inserta clientes legados (mostrador), migra a V5 y verifica preservación íntegra con password_hash = NULL")
    void migraHastaV3InsertaClientesYVerificaPreservacionEnV5() throws Exception {
        String dbUrl = "jdbc:h2:mem:migracion_v5_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(dbUrl);
        ds.setUser("sa");
        ds.setPassword("");

        // 1. Ejecutar Flyway hasta versión 3 (estado anterior a V5)
        Flyway flywayV3 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("3")
                .load();
        flywayV3.migrate();

        // 2. Insertar registros existentes simulando operaciones de mostrador previas a V5
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO clientes (nombre, apellidos, correo, telefono, numero_licencia, licencia_vencimiento, direccion, activo)
                    VALUES (?, ?, ?, ?, ?, DATEADD('YEAR', 2, CURRENT_DATE), ?, TRUE)
                    """)) {
                // Cliente 1: con correo
                ps.setString(1, "Mario");
                ps.setString(2, "Bros");
                ps.setString(3, "mario@nintendo.com");
                ps.setString(4, "8441112233");
                ps.setString(5, "LIC-MARIO-01");
                ps.setString(6, "Reino Champinon");
                ps.executeUpdate();

                // Cliente 2: sin correo (NULL) de mostrador
                ps.setString(1, "Luigi");
                ps.setString(2, "Bros");
                ps.setNull(3, java.sql.Types.VARCHAR);
                ps.setString(4, "8444445566");
                ps.setString(5, "LIC-LUIGI-02");
                ps.setString(6, "Mansion Embrujada");
                ps.executeUpdate();
            }
        }

        // 3. Ejecutar Flyway aplicando V5 (migración objetivo)
        Flyway flywayV5 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("5")
                .load();
        flywayV5.migrate();

        // 4. Verificar integridad y cero pérdida de datos
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM clientes")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "La cantidad total de registros de clientes debe ser exactamente 2 (cero pérdida)");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT id, nombre, apellidos, correo, password_hash FROM clientes ORDER BY id ASC")) {
                // Registro 1
                assertTrue(rs.next());
                assertEquals("Mario", rs.getString("nombre"));
                assertEquals("mario@nintendo.com", rs.getString("correo"));
                assertNull(rs.getString("password_hash"),
                        "El cliente previo con correo debe tener password_hash NULL (sin contraseñas ficticias)");

                // Registro 2
                assertTrue(rs.next());
                assertEquals("Luigi", rs.getString("nombre"));
                assertNull(rs.getString("correo"), "El cliente de mostrador debe conservar su correo NULL");
                assertNull(rs.getString("password_hash"),
                        "El cliente previo sin correo debe tener password_hash NULL (sin contraseñas ficticias)");
            }
        }
    }
}
