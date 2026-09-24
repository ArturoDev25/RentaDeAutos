package com.rentadeautos.modules.audit;

import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.audit.repository.AuditRepository;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile({"dev", "default"})
public class AuditDataInitializer implements CommandLineRunner {

    private final AuditRepository auditRepository;
    private final UsuarioAppRepository usuarioRepository;

    public AuditDataInitializer(AuditRepository auditRepository, UsuarioAppRepository usuarioRepository) {
        this.auditRepository = auditRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (auditRepository.count() > 0) {
            return; // Ya hay datos
        }

        UsuarioApp admin = usuarioRepository.findById(1L).orElse(null); // Ana Torres
        UsuarioApp agente = usuarioRepository.findById(2L).orElse(null); // Carlos Méndez
        UsuarioApp supervisor = usuarioRepository.findById(3L).orElse(null); // María López
        UsuarioApp auditor = usuarioRepository.findById(4L).orElse(null); // Diego Ramírez

        LocalDateTime hoy = LocalDateTime.now();
        String ipOficina = "192.168.1.100";
        String ipExterna = "200.15.42.11";

        List<Auditoria> eventos = List.of(
            // AUD-1250: Editar vehículo (Luis Ramírez / Ana)
            new Auditoria(admin, "Editar", "Vehículos", 15L, "EXITOSO", "{\"estado\": \"DISPONIBLE\"}", "{\"estado\": \"MANTENIMIENTO\"}", ipOficina, hoy.minusHours(1)),
            // AUD-1249: Crear reservación (Ana Torres)
            new Auditoria(admin, "Crear", "Reservaciones", 89L, "EXITOSO", null, "{\"clienteId\": 42, \"vehiculoId\": 15}", ipOficina, hoy.minusHours(2)),
            // AUD-1248: Inicio de sesión (Carlos Méndez)
            new Auditoria(agente, "Inicio de sesión", "Autenticación", null, "EXITOSO", null, null, ipExterna, hoy.minusHours(3)),
            // AUD-1247: Cancelar reservación (Carlos Méndez)
            new Auditoria(agente, "Cancelar", "Reservaciones", 77L, "EXITOSO", "{\"estado\": \"CONFIRMADA\"}", "{\"estado\": \"CANCELADA\"}", ipExterna, hoy.minusHours(4)),
            // AUD-1246: Crear cliente (María López)
            new Auditoria(supervisor, "Crear", "Clientes", 42L, "EXITOSO", null, "{\"nombre\": \"Juan Pérez\"}", ipOficina, hoy.minusHours(5)),
            // AUD-1245: Actualizar tarifa (Sistema)
            new Auditoria(null, "Actualizar", "Tarifas", 5L, "EXITOSO", "{\"precio_dia\": 1000}", "{\"precio_dia\": 1100}", "127.00.1", hoy.minusHours(6)),
            // AUD-1244: Eliminar vehículo (Diego Ramírez)
            new Auditoria(auditor, "Eliminar", "Vehículos", 10L, "EXITOSO", "{\"estado\": \"BAJA\"}", null, ipOficina, hoy.minusHours(7)),
            // AUD-1243: Error de login (Diego Ramírez)
            new Auditoria(auditor, "Error de login", "Autenticación", null, "FALLIDO", null, null, ipExterna, hoy.minusHours(8)),
            // AUD-1242: Inicio de sesión (Diego Ramírez)
            new Auditoria(auditor, "Inicio de sesión", "Autenticación", null, "EXITOSO", null, null, ipExterna, hoy.minusHours(9)),
            // AUD-1241: Editar vehículo (Ana Torres)
            new Auditoria(admin, "Editar", "Vehículos", 15L, "EXITOSO", "{\"estado\": \"MANTENIMIENTO\"}", "{\"estado\": \"DISPONIBLE\"}", ipOficina, hoy.minusHours(10))
        );

        auditRepository.saveAll(eventos);
    }
}
