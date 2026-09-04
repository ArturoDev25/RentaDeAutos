package com.rentadeautos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integración base: verifica que el contexto de Spring
 * carga correctamente sin una base de datos activa.
 */
@SpringBootTest
class RentaDeAutosApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto de Spring carga sin lanzar excepción, el test pasa.
    }
}
