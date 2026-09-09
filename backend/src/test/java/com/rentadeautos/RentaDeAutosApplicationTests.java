package com.rentadeautos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración base: verifica que el contexto de Spring
 * carga correctamente usando H2 en memoria (perfil "test").
 *
 * <p>Corre sin necesitar una instancia de MySQL real gracias al
 * perfil {@code test} definido en {@code application-test.yml}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class RentaDeAutosApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto de Spring carga sin lanzar excepción, el test pasa.
    }
}
