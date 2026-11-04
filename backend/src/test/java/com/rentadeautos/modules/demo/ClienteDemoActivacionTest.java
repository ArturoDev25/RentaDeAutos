package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ClienteDemoActivacionTest {
    @Configuration(proxyBeanMethods = false)
    @Import(ClienteDemoInitializer.class)
    static class Configuracion { }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Configuracion.class)
            .withBean(ClienteRepository.class, () -> mock(ClienteRepository.class))
            .withBean(PasswordEncoder.class, () -> new BCryptPasswordEncoder(4));

    @Test void defaultNoRegistraSeedAunqueSeActivePropiedad() {
        contexto.withPropertyValues("app.demo.seed.enabled=true")
                .run(c -> assertEquals(0, c.getBeansOfType(ClienteDemoInitializer.class).size()));
    }

    @Test void demoSinActivacionNoRegistraSeed() {
        contexto.withInitializer(c -> c.getEnvironment().setActiveProfiles("demo"))
                .run(c -> assertEquals(0, c.getBeansOfType(ClienteDemoInitializer.class).size()));
    }

    @Test void demoConActivacionRegistraSeed() {
        contexto.withInitializer(c -> c.getEnvironment().setActiveProfiles("demo"))
                .withPropertyValues("app.demo.seed.enabled=true")
                .run(c -> assertEquals(1, c.getBeansOfType(ClienteDemoInitializer.class).size()));
    }

    @Test void prodImpideSeedAunqueSeCombineConDemoYActivacion() {
        contexto.withInitializer(c -> c.getEnvironment().setActiveProfiles("demo", "prod"))
                .withPropertyValues("app.demo.seed.enabled=true")
                .run(c -> assertEquals(0, c.getBeansOfType(ClienteDemoInitializer.class).size()));
    }

    @Test void productionTambienImpideSeed() {
        contexto.withInitializer(c -> c.getEnvironment().setActiveProfiles("demo", "production"))
                .withPropertyValues("app.demo.seed.enabled=true")
                .run(c -> assertEquals(0, c.getBeansOfType(ClienteDemoInitializer.class).size()));
    }
}
