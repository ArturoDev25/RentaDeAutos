// portal-cuenta.js - Utilidades compartidas de cuenta del cliente (S4-10)

const PortalCuenta = (() => {

    const ROLES_PERSONAL = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR', 'AUDITOR'];

    function leerSesion() {
        if (typeof localStorage === 'undefined') return null;
        try {
            const raw = localStorage.getItem('usuarioSesion');
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            localStorage.removeItem('usuarioSesion');
            return null;
        }
    }

    function paginaActualConQuery() {
        if (typeof window === 'undefined') return 'index.html';
        const nombre = (window.location.pathname.split('/').pop() || 'index.html');
        return `${nombre}${window.location.search || ''}`;
    }

    /**
     * Valida que exista una sesión activa con rol CLIENTE.
     * Si no hay sesión redirige a login.html?redirect=<pagina>; si es personal redirige a dashboard.html.
     */
    function requerirCliente() {
        const sesion = leerSesion();
        if (!sesion || !sesion.token) {
            if (typeof window !== 'undefined') {
                const retorno = encodeURIComponent(paginaActualConQuery());
                window.location.href = `login.html?redirect=${retorno}`;
            }
            return null;
        }
        if (ROLES_PERSONAL.includes(sesion.rol)) {
            if (typeof window !== 'undefined') {
                window.location.href = 'dashboard.html';
            }
            return null;
        }
        return sesion;
    }

    /**
     * Envoltura de fetchAPI que intercepta 401 (sesión expirada o token inválido),
     * limpia localStorage.usuarioSesion y redirige a login.html?redirect=<pagina>.
     */
    async function apiCliente(endpoint, options = {}) {
        try {
            return await fetchAPI(endpoint, options);
        } catch (error) {
            if (error && error.status === 401) {
                if (typeof localStorage !== 'undefined') {
                    localStorage.removeItem('usuarioSesion');
                }
                if (typeof window !== 'undefined') {
                    const retorno = encodeURIComponent(paginaActualConQuery());
                    window.location.href = `login.html?redirect=${retorno}`;
                }
            }
            throw error;
        }
    }

    /**
     * Formatea el folio visual a partir del ID numérico (RES-0015 o REN-0003).
     */
    function formatearFolio(prefijo, id) {
        const num = Number(id);
        const limpio = Number.isFinite(num) && num > 0 ? Math.trunc(num) : 0;
        return `${prefijo}-${String(limpio).padStart(4, '0')}`;
    }

    /**
     * Formatea una fecha ISO (YYYY-MM-DD o LocalDateTime) a formato corto en español: "25 mar 2026".
     */
    function formatearFechaCorta(fechaIso) {
        if (!fechaIso) return '—';
        const fecha = new Date(fechaIso);
        if (Number.isNaN(fecha.getTime())) return String(fechaIso);
        const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
        const dia = String(fecha.getDate()).padStart(2, '0');
        const mes = meses[fecha.getMonth()];
        const anio = fecha.getFullYear();
        return `${dia} ${mes} ${anio}`;
    }

    function formatearRangoFechas(inicioIso, finIso) {
        if (!inicioIso && !finIso) return '—';
        if (!finIso) return formatearFechaCorta(inicioIso);
        return `${formatearFechaCorta(inicioIso)} – ${formatearFechaCorta(finIso)}`;
    }

    /**
     * Calcula los días cobrados entre dos fechas siguiendo la regla del backend:
     * 1 + floor((segundos - 1) / 86400), mínimo 1 día.
     */
    function calcularDiasOperacion(inicioIso, finIso) {
        const ini = new Date(inicioIso).getTime();
        const fin = new Date(finIso).getTime();
        if (!Number.isFinite(ini) || !Number.isFinite(fin) || fin <= ini) return 1;
        const segundos = Math.floor((fin - ini) / 1000);
        return Math.max(1, 1 + Math.floor((segundos - 1) / 86400));
    }

    /**
     * Calcula el progreso de una renta activa ("Días transcurridos X de N").
     */
    function calcularProgresoRenta(fechaEntregaIso, fechaFinPrevistaIso, ahoraMs = Date.now()) {
        const diasTotales = calcularDiasOperacion(fechaEntregaIso, fechaFinPrevistaIso);
        const inicioMs = new Date(fechaEntregaIso).getTime();
        if (!Number.isFinite(inicioMs)) {
            return { diasTranscurridos: 1, diasTotales, porcentaje: 0 };
        }
        const diffSegundos = Math.max(0, Math.floor((ahoraMs - inicioMs) / 1000));
        const transcurridosBruto = diffSegundos === 0 ? 1 : Math.min(diasTotales, 1 + Math.floor(diffSegundos / 86400));
        const diasTranscurridos = Math.max(1, Math.min(diasTotales, transcurridosBruto));
        const porcentaje = Math.min(100, Math.max(5, Math.round((diasTranscurridos / diasTotales) * 100)));
        return { diasTranscurridos, diasTotales, porcentaje };
    }

    /**
     * Determina si una reservación puede cancelarse desde el portal:
     * estado PENDIENTE o CONFIRMADA y fechaInicio estrictamente en el futuro.
     */
    function esCancelableReserva(reserva, ahoraMs = Date.now()) {
        if (!reserva) return false;
        if (typeof reserva.puedeCancelar === 'boolean') {
            return reserva.puedeCancelar;
        }
        const estadoValido = reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA';
        const inicioMs = new Date(reserva.fechaInicio).getTime();
        return estadoValido && Number.isFinite(inicioMs) && inicioMs > ahoraMs;
    }

    function infoBadgeEstado(estado) {
        switch (estado) {
            case 'CONFIRMADA':
                return { texto: 'Confirmada', clase: 'badge-confirmada' };
            case 'PENDIENTE':
                return { texto: 'Pendiente', clase: 'badge-pendiente' };
            case 'CANCELADA':
                return { texto: 'Cancelada', clase: 'badge-cancelada' };
            case 'EN_CURSO':
                return { texto: 'Renta activa', clase: 'badge-activa' };
            case 'FINALIZADA':
                return { texto: 'Finalizada', clase: 'badge-finalizada' };
            case 'RESERVADA':
                return { texto: 'Reservada', clase: 'badge-pendiente' };
            default:
                return { texto: estado || '—', clase: '' };
        }
    }

    function sanitizarCeldaCsv(valor) {
        const str = String(valor ?? '').replace(/"/g, '""');
        const prefijoPeligroso = /^[=+\-@\t\r]/.test(str) ? "'" : '';
        return `"${prefijoPeligroso}${str}"`;
    }

    function generarContenidoCsv(encabezados, filas) {
        const cabecera = encabezados.map(sanitizarCeldaCsv).join(',');
        const cuerpo = filas.map(fila => fila.map(sanitizarCeldaCsv).join(',')).join('\r\n');
        return `\uFEFF${cabecera}\r\n${cuerpo}`;
    }

    function descargarCsv(nombreArchivo, encabezados, filas) {
        const csv = generarContenidoCsv(encabezados, filas);
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    function visualVehiculo(vehiculoId, altTexto) {
        const icono = (typeof Portal !== 'undefined' && Portal.ICONO_AUTO) ? Portal.ICONO_AUTO : '';
        if (!vehiculoId || typeof API_BASE_URL === 'undefined') {
            return `<div class="op-visual">${icono}</div>`;
        }
        const src = `${API_BASE_URL}/v1/catalogo/vehiculos/${encodeURIComponent(vehiculoId)}/imagen`;
        const alt = (typeof Portal !== 'undefined' && Portal.escapar) ? Portal.escapar(altTexto) : String(altTexto || 'Vehículo');
        return `<div class="op-visual"><img src="${src}" alt="${alt}" loading="lazy" data-foto-vehiculo></div>`;
    }

    return {
        leerSesion,
        requerirCliente,
        apiCliente,
        formatearFolio,
        formatearFechaCorta,
        formatearRangoFechas,
        calcularDiasOperacion,
        calcularProgresoRenta,
        esCancelableReserva,
        infoBadgeEstado,
        sanitizarCeldaCsv,
        generarContenidoCsv,
        descargarCsv,
        visualVehiculo
    };
})();

if (typeof module !== 'undefined' && module.exports) {
    module.exports = PortalCuenta;
}
