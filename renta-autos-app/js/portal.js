// portal.js - Funciones compartidas del portal del cliente (inicio, catálogo y detalle)

const Portal = (() => {

    const ROLES_PERSONAL = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR', 'AUDITOR'];

    const NOMBRE_ROL = {
        CLIENTE: 'Cliente',
        ADMINISTRADOR: 'Administrador',
        AGENTE: 'Agente',
        SUPERVISOR: 'Supervisor',
        AUDITOR: 'Auditor'
    };

    const ICONO_AUTO = `
        <svg viewBox="0 0 64 48" aria-hidden="true">
            <path fill="currentColor" d="M15.2 8h24.6a8 8 0 0 1 6.4 3.2L52.5 20H56a6 6 0 0 1 6 6v8a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4v-8a6 6 0 0 1 6-6h.6l3.3-9.2A4 4 0 0 1 15.2 8z"/>
            <path fill="#fff" opacity=".6" d="M16.5 13H27v7H13.9zM31 13h8.3a3 3 0 0 1 2.4 1.2L46.3 20H31z"/>
            <circle cx="17" cy="38" r="7.5" fill="currentColor" stroke="#fff" stroke-width="3"/>
            <circle cx="47" cy="38" r="7.5" fill="currentColor" stroke="#fff" stroke-width="3"/>
        </svg>`;

    // Rangos del filtro de precio; los límites de la API son inclusivos
    const RANGOS_PRECIO = [
        { id: '', texto: 'Todos' },
        { id: 'hasta-600', texto: 'Hasta $600', max: 600 },
        { id: '600-900', texto: '$601 a $900', min: 600.01, max: 900 },
        { id: '900-1200', texto: '$901 a $1,200', min: 900.01, max: 1200 },
        { id: 'mas-1200', texto: 'Más de $1,200', min: 1200.01 }
    ];

    const formatoEntero = new Intl.NumberFormat('es-MX', {
        style: 'currency', currency: 'MXN', minimumFractionDigits: 0, maximumFractionDigits: 0
    });
    const formatoDecimal = new Intl.NumberFormat('es-MX', {
        style: 'currency', currency: 'MXN', minimumFractionDigits: 2, maximumFractionDigits: 2
    });

    function escapar(valor) {
        return String(valor ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function moneda(valor) {
        const numero = Number(valor);
        if (!Number.isFinite(numero)) return '—';
        return Number.isInteger(numero) ? formatoEntero.format(numero) : formatoDecimal.format(numero);
    }

    function nombreVehiculo(v) {
        return [v.marca, v.modelo, v.anio].filter(Boolean).join(' ');
    }

    function leerSesion() {
        try {
            const data = localStorage.getItem('usuarioSesion');
            return data ? JSON.parse(data) : null;
        } catch (_) {
            return null;
        }
    }

    function esPersonal(sesion) {
        return !!sesion && ROLES_PERSONAL.includes(sesion.rol);
    }

    // ---------- API ----------

    async function obtenerCatalogo(filtros = {}) {
        const params = new URLSearchParams();
        Object.entries(filtros).forEach(([clave, valor]) => {
            if (valor !== undefined && valor !== null && valor !== '') {
                params.set(clave, valor);
            }
        });
        const query = params.toString();
        const respuesta = await fetchAPI(`/v1/catalogo/vehiculos${query ? `?${query}` : ''}`);
        return Array.isArray(respuesta?.data) ? respuesta.data : [];
    }

    function extraerCategorias(vehiculos) {
        const mapa = new Map();
        vehiculos.forEach(v => {
            if (v.categoriaId != null && !mapa.has(v.categoriaId)) {
                mapa.set(v.categoriaId, v.categoria);
            }
        });
        return [...mapa.entries()]
            .map(([id, nombre]) => ({ id, nombre }))
            .sort((a, b) => String(a.nombre).localeCompare(String(b.nombre), 'es'));
    }

    function mensajeError(error) {
        if (!error || error.status === undefined) {
            return {
                titulo: 'No pudimos conectar con el servidor',
                texto: 'Revisa tu conexión a internet o intenta de nuevo en unos minutos.'
            };
        }
        if (error.status === 400) {
            return {
                titulo: 'Revisa los filtros',
                texto: error.message || 'Alguno de los filtros no es válido.'
            };
        }
        return {
            titulo: 'Algo salió mal',
            texto: 'El catálogo no está disponible en este momento. Intenta de nuevo más tarde.'
        };
    }

    // ---------- Fechas ----------

    function hoyISO() {
        const hoy = new Date();
        hoy.setMinutes(hoy.getMinutes() - hoy.getTimezoneOffset());
        return hoy.toISOString().slice(0, 10);
    }

    function esFechaISO(valor) {
        return /^\d{4}-\d{2}-\d{2}$/.test(valor || '') && !Number.isNaN(Date.parse(valor));
    }

    function diasEntre(inicio, fin) {
        return Math.round((Date.parse(fin) - Date.parse(inicio)) / 86400000);
    }

    // Devuelve un mensaje de error o cadena vacía si las fechas son válidas
    function validarFechas(inicio, fin) {
        if (!inicio || !fin) return 'Selecciona la fecha de inicio y la de fin.';
        if (!esFechaISO(inicio) || !esFechaISO(fin)) return 'Las fechas no son válidas.';
        if (inicio < hoyISO()) return 'La fecha de inicio no puede ser anterior a hoy.';
        if (fin <= inicio) return 'La fecha de fin debe ser posterior a la de inicio.';
        return '';
    }

    function paramsFechas(inicio, fin) {
        const params = new URLSearchParams();
        if (esFechaISO(inicio)) params.set('inicio', inicio);
        if (esFechaISO(fin)) params.set('fin', fin);
        return params;
    }

    // ---------- Render ----------

    function urlImagen(v) {
        return `${API_BASE_URL}/v1/catalogo/vehiculos/${encodeURIComponent(v.id)}/imagen`;
    }

    function visual(v) {
        const contenido = v.tieneImagen
            ? `<img src="${escapar(urlImagen(v))}" alt="${escapar(nombreVehiculo(v))}" loading="lazy" data-foto-vehiculo>`
            : ICONO_AUTO;
        return `<div class="vehiculo-visual">${contenido}</div>`;
    }

    function tarjeta(v, fechas = {}) {
        const params = paramsFechas(fechas.inicio, fechas.fin);
        params.set('id', v.id);
        params.sort();
        return `
            <article class="vehiculo-card">
                ${visual(v)}
                <h3>${escapar(nombreVehiculo(v))}</h3>
                <p class="vehiculo-categoria">${escapar(v.categoria)}</p>
                <p class="precio">${escapar(moneda(v.tarifaDia))} / día</p>
                <a class="pt-btn pt-btn-primary pt-btn-block" href="vehiculo.html?${params.toString()}">Ver detalles</a>
            </article>`;
    }

    function mostrarCarga(contenedor, cantidad = 8) {
        contenedor.setAttribute('aria-busy', 'true');
        contenedor.innerHTML = Array.from({ length: cantidad }, () => `
            <div class="vehiculo-card skeleton" aria-hidden="true">
                <div class="vehiculo-visual"></div>
                <div class="sk-line w60"></div>
                <div class="sk-line w40"></div>
                <div class="sk-line alto"></div>
            </div>`).join('') + '<span class="sr-only">Cargando vehículos…</span>';
    }

    function mostrarVehiculos(contenedor, vehiculos, fechas) {
        contenedor.removeAttribute('aria-busy');
        contenedor.innerHTML = vehiculos.map(v => tarjeta(v, fechas)).join('');
    }

    // tipo: 'vacio' | 'error'. accion: { texto, icono, alHacerClic }
    function mostrarEstado(contenedor, { tipo, titulo, texto, accion }) {
        contenedor.removeAttribute('aria-busy');
        const icono = tipo === 'error' ? 'fa-plug-circle-xmark' : 'fa-car';
        contenedor.innerHTML = `
            <div class="estado ${tipo}" role="${tipo === 'error' ? 'alert' : 'status'}">
                <i class="fa-solid ${icono}"></i>
                <h3>${escapar(titulo)}</h3>
                <p>${escapar(texto)}</p>
                ${accion ? `<button type="button" class="pt-btn pt-btn-primary" data-accion-estado>
                    <i class="fa-solid ${accion.icono || 'fa-rotate-right'}"></i>${escapar(accion.texto)}
                </button>` : ''}
            </div>`;
        if (accion) {
            contenedor.querySelector('[data-accion-estado]').addEventListener('click', accion.alHacerClic);
        }
    }

    // ---------- Estructura común ----------

    function iniciarSesionUI() {
        const destino = document.getElementById('portalSession');
        if (!destino) return;

        const sesion = leerSesion();
        if (!sesion) {
            destino.innerHTML = `
                <a href="login.html" class="pt-btn pt-btn-outline">Iniciar sesión</a>
                <a href="registro.html" class="pt-btn pt-btn-primary">Registrarse</a>`;
            return;
        }

        const nombre = sesion.nombre || 'Usuario';
        const panel = esPersonal(sesion)
            ? '<a href="dashboard.html" class="pt-btn pt-btn-outline">Ir al panel</a>'
            : '';
        destino.innerHTML = `
            ${panel}
            <div class="usuario">
                <span class="avatar" aria-hidden="true">${escapar(nombre.trim().charAt(0).toUpperCase())}</span>
                <span class="usuario-datos">
                    <strong>${escapar(nombre)}</strong>
                    <small>${escapar(NOMBRE_ROL[sesion.rol] || 'Cliente')}</small>
                </span>
            </div>
            <button type="button" class="btn-salir" id="btnLogout" title="Cerrar sesión" aria-label="Cerrar sesión">
                <i class="fa-solid fa-arrow-right-from-bracket"></i>
            </button>`;
        document.getElementById('btnLogout').addEventListener('click', () => {
            localStorage.removeItem('usuarioSesion');
            window.location.reload();
        });
    }

    function iniciarEstructura() {
        iniciarSesionUI();

        const sidebar = document.getElementById('portalSidebar');
        const btnMenu = document.getElementById('btnMenu');
        if (sidebar && btnMenu) {
            btnMenu.addEventListener('click', () => {
                const abierto = sidebar.classList.toggle('abierto');
                btnMenu.setAttribute('aria-expanded', String(abierto));
            });
            document.addEventListener('click', (e) => {
                if (sidebar.classList.contains('abierto') && !sidebar.contains(e.target) && !btnMenu.contains(e.target)) {
                    sidebar.classList.remove('abierto');
                    btnMenu.setAttribute('aria-expanded', 'false');
                }
            });
        }

        const buscador = document.getElementById('buscadorGlobal');
        if (buscador) {
            const input = buscador.querySelector('input');
            const actual = new URLSearchParams(window.location.search).get('q');
            if (actual && window.location.pathname.endsWith('catalogo.html')) input.value = actual;
            buscador.addEventListener('submit', (e) => {
                e.preventDefault();
                const texto = input.value.trim();
                window.location.href = texto ? `catalogo.html?q=${encodeURIComponent(texto)}` : 'catalogo.html';
            });
        }

        // Los apartados que aún no existen no navegan
        document.querySelectorAll('[data-pendiente]').forEach(enlace => {
            enlace.addEventListener('click', (e) => e.preventDefault());
        });
    }

    // Si una foto no carga se reemplaza por el dibujo genérico
    document.addEventListener('error', (e) => {
        const img = e.target;
        if (img instanceof HTMLImageElement && img.hasAttribute('data-foto-vehiculo')) {
            img.insertAdjacentHTML('afterend', ICONO_AUTO);
            img.remove();
        }
    }, true);

    // Sin sesión manda a login y regresa al vehículo; con sesión de cliente abre la reservación.
    function iniciarReservacion(vehiculo, inicio, fin) {
        const sesion = leerSesion();
        const fechas = paramsFechas(inicio, fin);
        if (!sesion) {
            fechas.set('id', vehiculo.id);
            fechas.sort();
            const regreso = `vehiculo.html?${fechas.toString()}`;
            window.location.href = `login.html?redirect=${encodeURIComponent(regreso)}`;
            return { ok: true };
        }
        if (esPersonal(sesion)) {
            return {
                ok: false,
                mensaje: 'Las reservaciones del personal se registran desde el panel de Reservaciones.'
            };
        }
        try {
            sessionStorage.setItem('vehiculoSeleccionado', JSON.stringify({ ...vehiculo, inicio, fin }));
        } catch (_) {
            // Si no hay almacenamiento, la reservación toma los datos de la URL
        }
        fechas.set('vehiculoId', vehiculo.id);
        window.location.href = `reservar.html?${fechas.toString()}`;
        return { ok: true };
    }

    document.addEventListener('DOMContentLoaded', iniciarEstructura);

    return {
        ICONO_AUTO,
        RANGOS_PRECIO,
        escapar,
        moneda,
        nombreVehiculo,
        obtenerCatalogo,
        extraerCategorias,
        mensajeError,
        hoyISO,
        esFechaISO,
        diasEntre,
        validarFechas,
        visual,
        mostrarCarga,
        mostrarVehiculos,
        mostrarEstado,
        iniciarReservacion
    };
})();
