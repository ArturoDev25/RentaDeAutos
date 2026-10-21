package com.rentadeautos.modules.client.migration;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsquemaClientesV5Test {

    private static JdbcDataSource ds;

    @BeforeAll
    static void initSchema() {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:esquema_v5_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @Test
    @DisplayName("Verifica que password_hash existe, es VARCHAR(255) y permite valores NULL")
    void verificaColumnaPasswordHash() throws Exception {
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "CLIENTES", "PASSWORD_HASH")) {
                assertTrue(rs.next(), "La columna PASSWORD_HASH debe existir en la tabla CLIENTES");
                assertEquals(255, rs.getInt("COLUMN_SIZE"), "El tamaño de PASSWORD_HASH debe ser 255");
                assertEquals("YES", rs.getString("IS_NULLABLE"), "PASSWORD_HASH debe admitir NULL");
            }
        }
    }

    @Test
    @DisplayName("Verifica que correo existe, es VARCHAR(150), permite NULL y tiene constraint UNIQUE")
    void verificaColumnaCorreoYUnicidad() throws Exception {
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "CLIENTES", "CORREO")) {
                assertTrue(rs.next(), "La columna CORREO debe existir en la tabla CLIENTES");
                assertEquals(150, rs.getInt("COLUMN_SIZE"), "El tamaño de CORREO debe ser 150");
                assertEquals("YES", rs.getString("IS_NULLABLE"), "CORREO debe admitir NULL");
            }

            try (ResultSet rs = metaData.getIndexInfo(null, null, "CLIENTES", true, false)) {
                boolean uqCorreoEncontrado = false;
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if ("CORREO".equalsIgnoreCase(col)) {
                        uqCorreoEncontrado = true;
                        break;
                    }
                }
                assertTrue(uqCorreoEncontrado, "Debe existir un índice o constraint UNIQUE para la columna CORREO");
            }
        }
    }

    @Test
    @DisplayName("Verifica que las restricciones CHECK (longitud de hash y correo obligatorio con hash) estén activas")
    void verificaCheckConstraints() throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            // Inspeccionar constraints de tipo CHECK en information_schema
            try (ResultSet rs = stmt.executeQuery("""
                    SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
                    """)) {
                List<String> clauses = new ArrayList<>();
                while (rs.next()) {
                    clauses.add(rs.getString(1).toUpperCase());
                }

                boolean checkPasswordPresente = clauses.stream().anyMatch(c -> c.contains("PASSWORD_HASH") && c.contains("50"));
                boolean checkCorreoPresente = clauses.stream().anyMatch(c -> c.contains("PASSWORD_HASH") && c.contains("CORREO"));

                assertTrue(checkPasswordPresente, "Debe existir la restricción CHECK para la longitud del hash (chk_clientes_password_hash)");
                assertTrue(checkCorreoPresente, "Debe existir la restricción CHECK de requerimiento de correo si hay hash (chk_clientes_acceso_requiere_correo)");
            }
        }
    }
}
