package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** S5-04, primer avance: ocho clientes ficticios; no modifica clientes existentes. */
@Component
@Order(30)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class ClienteDemoInitializer implements CommandLineRunner {
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final String password;

    private record Datos(String nombre, String apellidos) { }
    private static final List<Datos> DATOS = List.of(
            new Datos("Ana", "Torres Demo"), new Datos("Luis", "Mendoza Demo"),
            new Datos("Sofía", "Ramírez Demo"), new Datos("Diego", "Herrera Demo"),
            new Datos("Valeria", "Castro Demo"), new Datos("Carlos", "Navarro Demo"),
            new Datos("Elena", "Ríos Demo"), new Datos("Mateo", "Vargas Demo"));

    public ClienteDemoInitializer(ClienteRepository clientes, PasswordEncoder encoder,
            @Value("${app.demo.seed.password:}") String password) {
        this.clientes = clientes;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_SEED_PASSWORD requiere al menos 12 caracteres y máximo 72 bytes UTF-8");
        }
        // Comprobar todas las identidades antes de insertar. Una coincidencia parcial no se adopta.
        List<Integer> faltantes = new ArrayList<>();
        for (int numero = 1; numero <= DATOS.size(); numero++) {
            String correo = correo(numero);
            String licencia = licencia(numero);
            var existente = clientes.findByCorreoIgnoreCase(correo);
            if (existente.isPresent()) {
                if (!licencia.equalsIgnoreCase(existente.get().getNumeroLicencia())) {
                    throw new IllegalStateException("Colisión de identidad demo: " + correo);
                }
            } else {
                if (clientes.existsByNumeroLicenciaIgnoreCase(licencia)) {
                    throw new IllegalStateException("Colisión de licencia demo: " + licencia);
                }
                faltantes.add(numero);
            }
        }
        if (faltantes.isEmpty()) {
            return;
        }
        String hash = encoder.encode(password);
        LocalDate vencimiento = LocalDate.now().plusYears(3);
        for (int numero : faltantes) {
            Datos datos = DATOS.get(numero - 1);
            Cliente cliente = new Cliente();
            cliente.setNombre(datos.nombre());
            cliente.setApellidos(datos.apellidos());
            cliente.setCorreo(correo(numero));
            cliente.setNumeroLicencia(licencia(numero));
            cliente.setTelefono("000000000" + numero);
            cliente.setDireccion("Dirección ficticia S5-04, sin domicilio real");
            cliente.setLicenciaVencimiento(vencimiento);
            cliente.setPasswordHash(hash);
            cliente.setActivo(true);
            clientes.save(cliente);
        }
        clientes.flush();
    }

    private static String correo(int numero) {
        return "demo%02d@s504.example".formatted(numero);
    }

    private static String licencia(int numero) {
        return "S504-DEMO-%02d".formatted(numero);
    }
}
