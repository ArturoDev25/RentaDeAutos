// mis-rentas.js - Vista "Mis rentas" (solo lectura) del cliente autenticado (S4-10)

document.addEventListener('DOMContentLoaded', () => {
    const sesion = PortalCuenta.requerirCliente();
    if (!sesion) return;

    const contenedor = document.getElementById('rentasLista');
    const filtrosContenedor = document.getElementById('filtrosRenta');
    const modal = document.getElementById('modalRenta');
    const modalCuerpo = document.getElementById('modalRentaCuerpo');
    const btnCerrarModal = document.getElementById('btnCerrarModalRenta');
    const btnAceptarRenta = document.getElementById('btnAceptarRenta');

    let rentasCache = [];
    let filtroActual = '';

    function cerrarModal() {
        modal.hidden = true;
    }

    btnCerrarModal.addEventListener('click', cerrarModal);
    btnAceptarRenta.addEventListener('click', cerrarModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) cerrarModal();
    });

    function mostrarSkeleton() {
        contenedor.setAttribute('aria-busy', 'true');
        contenedor.innerHTML = Array.from({ length: 3 }, () => `
            <div class="op-card skeleton" aria-hidden="true">
                <div class="op-visual vehiculo-visual"></div>
                <div class="op-info">
                    <div class="sk-line w60"></div>
                    <div class="sk-line w40"></div>
                    <div class="sk-line w60"></div>
                </div>
            </div>`).join('');
    }

    function renderizarTarjeta(r) {
        const esc = Portal.escapar;
        const tituloAuto = [r.marca, r.modelo, r.anio].filter(Boolean).join(' ') || `Vehículo #${r.vehiculoId}`;
        const subtitulo = [r.categoria || 'Automóvil', PortalCuenta.formatearFolio('REN', r.reservacionId)].join(' · ');
        const badge = PortalCuenta.infoBadgeEstado(r.estado);
        const esActiva = r.estado === 'EN_CURSO';
        const fechaFinRef = r.fechaDevolucionReal || r.fechaDevolucionPrevista;
        const dias = PortalCuenta.calcularDiasOperacion(r.fechaEntrega, fechaFinRef);
        const textoDias = `${dias} ${dias === 1 ? 'día' : 'días'}`;
        const montoMostrar = r.totalFinal != null ? r.totalFinal : r.totalEstimado;
        const etiquetaMonto = r.totalFinal != null ? 'total final' : 'total estimado';

        let barraProgreso = '';
        if (esActiva) {
            const prog = PortalCuenta.calcularProgresoRenta(r.fechaEntrega, r.fechaDevolucionPrevista);
            barraProgreso = `
                <div class="renta-progreso" aria-label="Progreso de la renta activa">
                    <div class="renta-progreso-head">
                        <span>Días transcurridos</span>
                        <strong>${esc(prog.diasTranscurridos)} de ${esc(prog.diasTotales)} días</strong>
                    </div>
                    <div class="renta-progreso-track">
                        <div class="renta-progreso-fill" style="width:${esc(prog.porcentaje)}%"></div>
                    </div>
                </div>`;
        }

        return `
            <article class="op-card ${esActiva ? 'op-card--activa' : ''}" data-id="${esc(r.reservacionId)}">
                ${PortalCuenta.visualVehiculo(r.vehiculoId, tituloAuto)}
                <div class="op-info">
                    <div class="op-titulo-fila">
                        <h2 class="op-titulo">${esc(tituloAuto)}</h2>
                        <span class="badge ${esc(badge.clase)}">${esc(badge.texto)}</span>
                    </div>
                    <p class="op-subtitulo">${esc(subtitulo)}</p>
                    <div class="op-meta">
                        <span><i class="fa-regular fa-calendar"></i>${esc(PortalCuenta.formatearRangoFechas(r.fechaEntrega, fechaFinRef))} (${esc(textoDias)})</span>
                        <span><i class="fa-solid fa-location-dot"></i>Sucursal Saltillo</span>
                    </div>
                    <p class="op-precio-fila">
                        <strong>${esc(Portal.moneda(montoMostrar))}</strong>
                        <small>(${esc(Portal.moneda(r.tarifaDia))}/día · ${esc(etiquetaMonto)})</small>
                    </p>
                    ${barraProgreso}
                </div>
                <div class="op-acciones">
                    <button type="button" class="pt-btn pt-btn-outline pt-btn-sm" data-ver-renta="${esc(r.reservacionId)}">
                        Ver detalles
                    </button>
                </div>
            </article>`;
    }

    function aplicarFiltroYRenderizar() {
        contenedor.removeAttribute('aria-busy');
        const filtradas = filtroActual
            ? rentasCache.filter(r => r.estado === filtroActual)
            : rentasCache;

        if (!filtradas.length) {
            const esFiltro = !!filtroActual && rentasCache.length > 0;
            Portal.mostrarEstado(contenedor, {
                tipo: 'vacio',
                titulo: esFiltro
                    ? 'No hay rentas en este estado'
                    : 'Aún no tienes rentas registradas',
                texto: esFiltro
                    ? 'Prueba seleccionando "Todas" para ver tu historial completo.'
                    : 'Cuando recojas un vehículo en sucursal, tu renta aparecerá aquí.',
                accion: esFiltro
                    ? {
                        texto: 'Ver todas',
                        icono: 'fa-list',
                        alHacerClic: () => seleccionarFiltro('')
                    }
                    : {
                        texto: 'Ver catálogo',
                        icono: 'fa-car',
                        alHacerClic: () => { window.location.href = 'catalogo.html'; }
                    }
            });
            return;
        }
        contenedor.innerHTML = filtradas.map(renderizarTarjeta).join('');
    }

    function seleccionarFiltro(nuevoEstado) {
        filtroActual = nuevoEstado;
        filtrosContenedor.querySelectorAll('.chip-btn').forEach(btn => {
            const activo = btn.getAttribute('data-estado') === nuevoEstado;
            btn.classList.toggle('active', activo);
            btn.setAttribute('aria-selected', String(activo));
        });
        aplicarFiltroYRenderizar();
    }

    filtrosContenedor.addEventListener('click', (e) => {
        const btn = e.target.closest('.chip-btn');
        if (!btn) return;
        seleccionarFiltro(btn.getAttribute('data-estado') || '');
    });

    function abrirDetalleRenta(reservacionId) {
        const r = rentasCache.find(item => String(item.reservacionId) === String(reservacionId));
        if (!r) return;
        const esc = Portal.escapar;
        const tituloAuto = [r.marca, r.modelo, r.anio].filter(Boolean).join(' ') || `Vehículo #${r.vehiculoId}`;
        const badge = PortalCuenta.infoBadgeEstado(r.estado);
        const fechaFinRef = r.fechaDevolucionReal || r.fechaDevolucionPrevista;
        const dias = PortalCuenta.calcularDiasOperacion(r.fechaEntrega, fechaFinRef);

        modalCuerpo.innerHTML = `
            <div style="display:flex;align-items:center;justify-content:space-between;gap:.75rem;flex-wrap:wrap">
                <strong>${esc(tituloAuto)}</strong>
                <span class="badge ${esc(badge.clase)}">${esc(badge.texto)}</span>
            </div>
            <div class="pt-modal-dl">
                <div><small>Folio</small><strong>${esc(PortalCuenta.formatearFolio('REN', r.reservacionId))}</strong></div>
                <div><small>Categoría</small><strong>${esc(r.categoria || 'General')}</strong></div>
                <div><small>Fecha de entrega</small><strong>${esc(PortalCuenta.formatearFechaCorta(r.fechaEntrega))}</strong></div>
                <div><small>Devolución prevista</small><strong>${esc(PortalCuenta.formatearFechaCorta(r.fechaDevolucionPrevista))}</strong></div>
                <div><small>Devolución real</small><strong>${esc(r.fechaDevolucionReal ? PortalCuenta.formatearFechaCorta(r.fechaDevolucionReal) : 'En curso')}</strong></div>
                <div><small>Duración</small><strong>${esc(dias)} ${dias === 1 ? 'día' : 'días'}</strong></div>
                <div><small>Tarifa por día</small><strong>${esc(Portal.moneda(r.tarifaDia))}</strong></div>
                <div><small>Total estimado</small><strong>${esc(Portal.moneda(r.totalEstimado))}</strong></div>
                <div><small>Total final</small><strong>${esc(r.totalFinal != null ? Portal.moneda(r.totalFinal) : 'Pendiente al cierre')}</strong></div>
                <div><small>Sucursal</small><strong>Sucursal Saltillo</strong></div>
            </div>
            <p class="nota" style="margin-top:.85rem">
                Información de solo lectura. El cierre y cobro definitivo de la renta se registra en sucursal al devolver el vehículo.
            </p>`;
        modal.hidden = false;
    }

    contenedor.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-ver-renta]');
        if (btn) {
            abrirDetalleRenta(btn.getAttribute('data-ver-renta'));
        }
    });

    async function cargarRentas() {
        mostrarSkeleton();
        try {
            const resp = await PortalCuenta.apiCliente('/v1/cliente/reservaciones/historial');
            rentasCache = Array.isArray(resp?.data) ? resp.data : [];
            aplicarFiltroYRenderizar();
        } catch (error) {
            if (error && error.status === 401) return;
            Portal.mostrarEstado(contenedor, {
                tipo: 'error',
                titulo: 'No pudimos cargar tu historial de rentas',
                texto: error?.message || 'Verifica tu conexión e inténtalo nuevamente.',
                accion: {
                    texto: 'Reintentar',
                    icono: 'fa-rotate-right',
                    alHacerClic: cargarRentas
                }
            });
        }
    }

    cargarRentas();
});
