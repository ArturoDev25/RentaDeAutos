package com.rentadeautos.modules.reservation.migration;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EsquemaReservacionesV6Test {
    private JdbcDataSource ds;

    @BeforeEach void migrarEsquemaExistente() throws Exception {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:v6_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        // Esquema mínimo para comprobar la migración y sus claves foráneas.
        try (Connection c = ds.getConnection(); var sql = c.createStatement()) {
            sql.execute("CREATE TABLE usuarios_app (id BIGINT UNSIGNED PRIMARY KEY)");
            sql.execute("CREATE TABLE clientes (id BIGINT UNSIGNED PRIMARY KEY)");
            sql.execute("INSERT INTO usuarios_app VALUES (10)");
            sql.execute("INSERT INTO clientes VALUES (1), (2)");
            sql.execute("""
                    CREATE TABLE reservaciones (id BIGINT PRIMARY KEY,
                        cliente_id BIGINT UNSIGNED NOT NULL,
                        creado_por_id BIGINT UNSIGNED NOT NULL,
                        CONSTRAINT fk_reservaciones_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        CONSTRAINT fk_reservaciones_usuario FOREIGN KEY (creado_por_id) REFERENCES usuarios_app(id)
                            ON UPDATE CASCADE ON DELETE RESTRICT)
                    """);
            sql.execute("INSERT INTO reservaciones VALUES (1, 1, 10)");
        }
        Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("5").target("6").load().migrate();
    }

    private void insertar(String empleado, String cliente, String propietario) throws SQLException {
        try (Connection c = ds.getConnection(); var sql = c.createStatement()) {
            sql.execute("INSERT INTO reservaciones (id, cliente_id, creado_por_id, creado_por_cliente_id) VALUES (2, "
                    + propietario + ", " + empleado + ", " + cliente + ")");
        }
    }

    @Test void conservaReservaAnteriorDelEmpleado() throws Exception {
        try (Connection c = ds.getConnection(); var sql = c.createStatement();
             var r = sql.executeQuery("SELECT creado_por_id, creado_por_cliente_id FROM reservaciones WHERE id = 1")) {
            assertTrue(r.next());
            assertEquals(10L, r.getLong(1));
            assertNull(r.getObject(2));
        }
    }

    @Test void permiteClienteAutenticadoComoCreador() throws Exception {
        insertar("NULL", "1", "1");
    }

    @Test void rechazaAmbosCreadoresONinguno() {
        assertThrows(SQLException.class, () -> insertar("10", "1", "1"));
        assertThrows(SQLException.class, () -> insertar("NULL", "NULL", "1"));
    }

    @Test void rechazaClienteCreadorAjenoAlPropietario() {
        assertThrows(SQLException.class, () -> insertar("NULL", "2", "1"));
    }

    @Test void rechazaClienteCreadorInexistente() {
        assertThrows(SQLException.class, () -> insertar("NULL", "999", "999"));
    }

    // MySQL 8 rechaza un CHECK sobre columnas con llaves foráneas en cascada (error 3823).
    // H2 no lo valida, por eso se comprueba aquí que ninguna llave del cliente use CASCADE.
    @Test void llavesDelClienteNoUsanAccionesEnCascada() throws Exception {
        try (Connection c = ds.getConnection(); var sql = c.createStatement();
             var r = sql.executeQuery("SELECT CONSTRAINT_NAME, UPDATE_RULE, DELETE_RULE "
                     + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS "
                     + "WHERE CONSTRAINT_NAME IN ('FK_RESERVACIONES_CLIENTE', 'FK_RESERVACIONES_USUARIO', 'FK_RESERVACIONES_CREADOR_CLIENTE')")) {
            int encontradas = 0;
            while (r.next()) {
                encontradas++;
                assertEquals("RESTRICT", r.getString("UPDATE_RULE"), r.getString("CONSTRAINT_NAME"));
                assertEquals("RESTRICT", r.getString("DELETE_RULE"), r.getString("CONSTRAINT_NAME"));
            }
            assertEquals(3, encontradas);
        }
    }
}
