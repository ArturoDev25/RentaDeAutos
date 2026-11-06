package com.rentadeautos.modules.demo;

import com.rentadeautos.modules.client.model.Cliente;
import com.rentadeautos.modules.client.model.EstadoAprobacionCliente;
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
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * S5-04: clientela de demostración amplia y con apariencia real. Genera cincuenta
 * clientes ficticios con nombres, correos, teléfonos y domicilios verosímiles (no "demo"):
 *
 * <ul>
 *   <li>40 activos (una parte de mostrador sin portal, otra aprobada con portal);</li>
 *   <li>6 PENDIENTES de aprobación (con cuenta de portal; alimentan S5-09);</li>
 *   <li>4 RECHAZADOS (cuenta inhabilitada).</li>
 * </ul>
 *
 * <p>No modifica clientes existentes; una coincidencia parcial de correo/licencia
 * provoca error antes de insertar. Idempotente y transaccional. Gated por perfil
 * {@code demo} + {@code app.demo.seed.enabled}.</p>
 */
@Component
@Order(52)
@Profile("demo & !prod & !production")
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class ClientelaDemoInitializer implements CommandLineRunner {

    private static final int ACTIVOS = 40;
    private static final int PENDIENTES = 6;
    private static final int RECHAZADOS = 4;
    private static final int TOTAL = ACTIVOS + PENDIENTES + RECHAZADOS; // 50

    private static final String[] NOMBRES = {
            "José", "María", "Juan", "Guadalupe", "Francisco", "Alejandro", "Luis", "Fernanda",
            "Ricardo", "Gabriela", "Carlos", "Ana", "Jorge", "Patricia", "Miguel", "Laura",
            "Roberto", "Sofía", "Daniel", "Mariana", "Eduardo", "Verónica", "Arturo", "Paola",
            "Héctor", "Diana", "Raúl", "Andrea", "Sergio", "Claudia" };
    private static final String[] APELLIDOS = {
            "García", "Martínez", "Hernández", "López", "González", "Rodríguez", "Pérez",
            "Sánchez", "Ramírez", "Cruz", "Flores", "Gómez", "Díaz", "Reyes", "Morales",
            "Jiménez", "Torres", "Vázquez", "Mendoza", "Castillo", "Romero", "Álvarez", "Ruiz",
            "Herrera", "Aguilar", "Medina", "Vargas", "Castro", "Guerrero", "Rojas" };
    private static final String[] DOMINIOS = { "s504-ampliado.example" };
    private static final String[] ESTADOS = {
            "NL", "JC", "CM", "GT", "PL", "VZ", "QT", "SP", "CH", "MH" };
    private static final String[] CALLES = {
            "Reforma", "Juárez", "Hidalgo", "Morelos", "Madero", "Allende", "Zaragoza",
            "Independencia", "5 de Mayo", "Insurgentes" };
    private static final String[] COLONIAS = {
            "Centro", "Del Valle", "Roma Norte", "Las Palmas", "San Pedro", "Jardines",
            "Lindavista", "Mitras", "Obispado", "Tecnológico" };
    private static final String[] CIUDADES = {
            "Monterrey", "Guadalajara", "Ciudad de México", "Puebla", "Querétaro", "Saltillo",
            "León", "Mérida", "Toluca", "Torreón" };

    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final String password;

    private record Persona(String nombre, String apellidos, String correo, String telefono,
            String licencia, String direccion, LocalDate vencimiento,
            EstadoAprobacionCliente estado, boolean portal, boolean activo) { }

    public ClientelaDemoInitializer(ClienteRepository clientes, PasswordEncoder encoder,
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
            throw new IllegalStateException(
                    "DEMO_SEED_PASSWORD requiere al menos 12 caracteres y máximo 72 bytes UTF-8");
        }
        List<Persona> personas = generarPersonas();

        // Verificar identidades antes de insertar ninguna.
        List<Persona> faltantes = new ArrayList<>();
        for (Persona p : personas) {
            var existente = clientes.findByCorreoIgnoreCase(p.correo());
            if (existente.isPresent()) {
                if (!p.licencia().equalsIgnoreCase(existente.get().getNumeroLicencia())) {
                    throw new IllegalStateException("Colisión de identidad demo: " + p.correo());
                }
            } else {
                if (clientes.existsByNumeroLicenciaIgnoreCase(p.licencia())) {
                    throw new IllegalStateException("Colisión de licencia demo: " + p.licencia());
                }
                faltantes.add(p);
            }
        }
        if (faltantes.isEmpty()) {
            return;
        }
        String hash = encoder.encode(password);
        for (Persona p : faltantes) {
            Cliente cliente = new Cliente();
            cliente.setNombre(p.nombre());
            cliente.setApellidos(p.apellidos());
            cliente.setCorreo(p.correo());
            cliente.setTelefono(p.telefono());
            cliente.setNumeroLicencia(p.licencia());
            cliente.setDireccion(p.direccion());
            cliente.setLicenciaVencimiento(p.vencimiento());
            cliente.setActivo(p.activo());
            cliente.setEstadoAprobacion(p.estado());
            if (p.portal()) {
                cliente.setPasswordHash(hash);
            }
            if (p.estado() == EstadoAprobacionCliente.RECHAZADO
                    || p.estado() == EstadoAprobacionCliente.APROBADO) {
                cliente.setFechaRevision(LocalDateTime.now());
            }
            clientes.save(cliente);
        }
        clientes.flush();
    }

    private List<Persona> generarPersonas() {
        List<Persona> lista = new ArrayList<>(TOTAL);
        for (int k = 0; k < TOTAL; k++) {
            String nombre = NOMBRES[k % NOMBRES.length];
            String ap1 = APELLIDOS[k % APELLIDOS.length];
            String ap2 = APELLIDOS[(k * 3 + 7) % APELLIDOS.length];
            String apellidos = ap1 + " " + ap2;
            String correo = correo(nombre, ap1, k);
            String telefono = String.format(Locale.ROOT, "000%07d", k + 1);
            String licencia = String.format(Locale.ROOT, "S504-CLI-%03d", k + 1);
            String direccion = "Calle " + CALLES[k % CALLES.length] + " " + (100 + k * 3)
                    + ", Col. " + COLONIAS[k % COLONIAS.length] + ", " + CIUDADES[k % CIUDADES.length];
            LocalDate vencimiento = LocalDate.now().plusYears(1 + (k % 5)).plusMonths(k % 12);

            EstadoAprobacionCliente estado;
            boolean portal;
            boolean activo;
            if (k < ACTIVOS) {
                // Activos: una parte aprobada por el portal, el resto de mostrador.
                if (k % 3 == 0) {
                    estado = EstadoAprobacionCliente.APROBADO;
                    portal = true;
                } else {
                    estado = null; // mostrador, sin cuenta de portal
                    portal = false;
                }
                activo = true;
            } else if (k < ACTIVOS + PENDIENTES) {
                estado = EstadoAprobacionCliente.PENDIENTE;
                portal = true;
                activo = false;
            } else {
                estado = EstadoAprobacionCliente.RECHAZADO;
                portal = true;
                activo = false;
            }
            lista.add(new Persona(nombre, apellidos, correo, telefono, licencia, direccion,
                    vencimiento, estado, portal, activo));
        }
        return lista;
    }

    private static String correo(String nombre, String apellido, int k) {
        String n = normalizar(nombre);
        String a = normalizar(apellido);
        String base = switch (k % 3) {
            case 0 -> n + "." + a;
            case 1 -> n.charAt(0) + a;
            default -> n + a.charAt(0);
        };
        return base + (10 + k) + "@" + DOMINIOS[k % DOMINIOS.length];
    }

    static boolean perteneceAlSeed(Cliente cliente) {
        return generarIdentidades().contains(cliente.getNumeroLicencia() + "|" + cliente.getCorreo());
    }

    private static java.util.Set<String> generarIdentidades() {
        var identidades = new java.util.HashSet<String>();
        for (int k = 0; k < TOTAL; k++) {
            identidades.add(String.format(Locale.ROOT, "S504-CLI-%03d", k + 1)
                    + "|" + correo(NOMBRES[k % NOMBRES.length], APELLIDOS[k % APELLIDOS.length], k));
        }
        return identidades;
    }

    /** Minúsculas, sin acentos ni espacios, para formar correos. */
    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
