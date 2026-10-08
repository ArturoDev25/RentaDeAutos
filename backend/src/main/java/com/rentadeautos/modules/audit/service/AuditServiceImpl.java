package com.rentadeautos.modules.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentadeautos.modules.audit.dto.AuditDetailDTO;
import com.rentadeautos.modules.audit.dto.AuditFilterOptionsDTO;
import com.rentadeautos.modules.audit.dto.AuditMetricsDTO;
import com.rentadeautos.modules.audit.dto.AuditResponseDTO;
import com.rentadeautos.modules.audit.model.Auditoria;
import com.rentadeautos.modules.audit.repository.AuditRepository;
import com.rentadeautos.modules.audit.repository.AuditSpecification;
import com.rentadeautos.modules.auth.model.UsuarioApp;
import com.rentadeautos.modules.auth.repository.UsuarioAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    /** Serializador de snapshots: fechas ISO-8601, BigDecimal sin notación científica. */
    static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    /** Política de cero secretos en la bitácora (doc S2-18 §2.5). */
    private static final Pattern CLAVE_SENSIBLE = Pattern.compile(
            "(?i)(password|contrasena|contraseña|passwd|hash|token|secret|jwt|authorization)");
    static final String REDACTADO = "[REDACTADO]";

    private final AuditRepository auditRepository;
    private final UsuarioAppRepository usuarioRepository;
    private final PlatformTransactionManager transactionManager;

    public AuditServiceImpl(AuditRepository auditRepository, UsuarioAppRepository usuarioRepository,
                            PlatformTransactionManager transactionManager) {
        this.auditRepository = auditRepository;
        this.usuarioRepository = usuarioRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public Page<AuditResponseDTO> listar(Pageable pageable, String q, Long usuarioId, String modulo, String accion) {
        Specification<Auditoria> spec = AuditSpecification.build(q, usuarioId, modulo, accion);
        return auditRepository.findAll(spec, pageable).map(this::mapToResponseDTO);
    }

    @Override
    public AuditDetailDTO obtenerDetalle(Long id) {
        return auditRepository.findById(id)
                .map(this::mapToDetailDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de auditoría no encontrado"));
    }

    @Override
    public AuditMetricsDTO obtenerMetricas() {
        long totalRegistros = auditRepository.count();
        long usuariosActivos = auditRepository.countDistinctUsuariosActivos();
        
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);
        long accionesHoy = auditRepository.countByFechaHoraBetween(inicioDia, finDia);
        
        long intentosFallidos = auditRepository.countByResultadoIgnoreCase("FALLIDO");
        
        return new AuditMetricsDTO(totalRegistros, usuariosActivos, accionesHoy, intentosFallidos);
    }

    @Override
    public AuditFilterOptionsDTO obtenerOpcionesFiltro() {
        List<AuditFilterOptionsDTO.UsuarioOpcionDTO> usuarios = usuarioRepository.findAll().stream()
                .map(u -> new AuditFilterOptionsDTO.UsuarioOpcionDTO(u.getId(), u.getNombre()))
                .collect(Collectors.toList());
                
        List<String> modulos = auditRepository.findDistinctEntidades();
        List<String> acciones = auditRepository.findDistinctAcciones();
        
        return new AuditFilterOptionsDTO(usuarios, modulos, acciones);
    }
    
    @Override
    @Transactional
    public void registrarEvento(Long usuarioId, String accion, String entidad, Long entidadId, String resultado,
                                Object valoresAnteriores, Object valoresNuevos, String direccionIp) {
        String resultadoNormalizado = normalizarResultado(resultado);
        // Serializar ANTES de tocar la BD: un snapshot inválido no deja registros a medias.
        String antesJson = aJson(valoresAnteriores);
        String despuesJson = aJson(valoresNuevos);
        String ip = direccionIp != null ? direccionIp : ipDePeticionActual();

        Runnable guardar = () -> {
            Auditoria auditoria = new Auditoria(
                resolverActor(usuarioId), accion, entidad, entidadId, resultadoNormalizado,
                antesJson, despuesJson, ip, LocalDateTime.now()
            );
            auditRepository.save(auditoria);
        };

        if (AuditoriaOperativa.FALLIDO.equals(resultadoNormalizado)) {
            // Un intento rechazado hace rollback de la operación de negocio; la bitácora
            // del fallo se confirma en su propia transacción para no perderse con él.
            TransactionTemplate nueva = new TransactionTemplate(transactionManager);
            nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            try {
                nueva.executeWithoutResult(estado -> guardar.run());
            } catch (RuntimeException e) {
                // No ocultar el error de negocio original; el log no incluye snapshots.
                log.warn("No se pudo registrar auditoría FALLIDO de {} sobre {}: {}",
                        accion, entidad, e.getClass().getSimpleName());
            }
        } else {
            // EXITOSO: misma transacción que la operación → todo o nada (RN-09).
            guardar.run();
        }
    }

    // ── Soporte de registrarEvento (S3-17) ──────────────────────────────────

    private static String normalizarResultado(String resultado) {
        String r = resultado == null ? "" : resultado.strip().toUpperCase(Locale.ROOT);
        if (!AuditoriaOperativa.EXITOSO.equals(r) && !AuditoriaOperativa.FALLIDO.equals(r)) {
            throw new IllegalArgumentException("Resultado de auditoría inválido: " + resultado);
        }
        return r;
    }

    /** Actor explícito; si no viene, el usuario autenticado; si no hay sesión, null ("Sistema"). */
    private UsuarioApp resolverActor(Long usuarioId) {
        if (usuarioId != null) {
            return usuarioRepository.findById(usuarioId).orElse(null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken
                || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
    }

    /** IP del request HTTP en curso (misma fuente que los controladores: getRemoteAddr). */
    private static String ipDePeticionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return null;
    }

    /**
     * Convierte un snapshot a JSON limpio. Acepta objetos (Map, record, DTO) o una cadena
     * JSON ya formada; los atributos null se conservan como null y las fechas salen en ISO-8601.
     * Cualquier clave sensible (contraseña, token, hash, secreto) se enmascara.
     */
    static String aJson(Object valores) {
        if (valores == null) {
            return null;
        }
        try {
            JsonNode nodo;
            if (valores instanceof String texto) {
                if (texto.isBlank()) {
                    return null;
                }
                try {
                    nodo = JSON.readTree(texto);
                } catch (JsonProcessingException noEsJson) {
                    nodo = TextNode.valueOf(texto); // texto plano → cadena JSON válida
                }
            } else {
                nodo = JSON.valueToTree(valores);
            }
            if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
                return null;
            }
            return JSON.writeValueAsString(enmascararSensibles(nodo));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo serializar el snapshot de auditoría", e);
        }
    }

    private static JsonNode enmascararSensibles(JsonNode nodo) {
        if (nodo instanceof ObjectNode objeto) {
            List<String> claves = new ArrayList<>();
            objeto.fieldNames().forEachRemaining(claves::add);
            for (String clave : claves) {
                if (CLAVE_SENSIBLE.matcher(clave).find()) {
                    objeto.put(clave, REDACTADO);
                } else {
                    enmascararSensibles(objeto.get(clave));
                }
            }
        } else if (nodo instanceof ArrayNode arreglo) {
            arreglo.forEach(AuditServiceImpl::enmascararSensibles);
        }
        return nodo;
    }

    private AuditResponseDTO mapToResponseDTO(Auditoria a) {
        String usuarioNombre = a.getUsuario() != null ? a.getUsuario().getNombre() : "Sistema";
        String resultadoDisplay = "FALLIDO".equalsIgnoreCase(a.getResultado()) ? "Fallida" : "Exitosa";
        String idFormateado = "AUD-" + String.format("%04d", a.getId());
        String descripcion = String.format("%s en %s", a.getAccion(), a.getEntidad());
        
        return new AuditResponseDTO(
            a.getId(), idFormateado, a.getFechaHora(), usuarioNombre, 
            a.getEntidad(), a.getAccion(), descripcion, a.getDireccionIp(), resultadoDisplay
        );
    }

    private AuditDetailDTO mapToDetailDTO(Auditoria a) {
        String usuarioNombre = a.getUsuario() != null ? a.getUsuario().getNombre() : "Sistema";
        String resultadoDisplay = "FALLIDO".equalsIgnoreCase(a.getResultado()) ? "Fallida" : "Exitosa";
        String idFormateado = "AUD-" + String.format("%04d", a.getId());
        String descripcion = String.format("%s en %s", a.getAccion(), a.getEntidad());
        
        return new AuditDetailDTO(
            a.getId(), idFormateado, a.getFechaHora(), usuarioNombre, 
            a.getEntidad(), a.getAccion(), descripcion, a.getDireccionIp(), resultadoDisplay,
            a.getValoresAnteriores(), a.getValoresNuevos()
        );
    }
}
