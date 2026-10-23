// mis-reservaciones.js - Vista "Mis reservaciones" del cliente autenticado (S4-10)

document.addEventListener('DOMContentLoaded', () => {
    const sesion = PortalCuenta.requerirCliente();
    if (!sesion) return;

    const contenedor = document.getElementById('reservacionesLista');
    const modal = document.getElementById('modalReservacion');
    const modalTitulo = document.getElementById('modalTitulo');
    const modalCuerpo = document.getElementById('modalCuerpo');
    const modalPie = document.getElementById('modalPie');
    const btnCerrarModal = document.getElementById('btnCerrarModal');

    let reservacionesCache = [];

    function cerrarModal() {
        modal.hidden = true;
    }

    btnCerrarModal.addEventListener('click', cerrarModal);
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
        const subtitulo = [r.categoria || 'Automóvil', PortalCuenta.formatearFolio('RES', r.id)].join(' · ');
        const badge = PortalCuenta.infoBadgeEstado(r.estado);
        const dias = PortalCuenta.calcularDiasOperacion(r.fechaInicio, r.fechaFin);
        const textoDias = `${dias} ${dias === 1 ? 'día' : 'días'}`;
        const cancelable = PortalCuenta.esCancelableReserva(r);

        return `
            <article class="op-card" data-id="${esc(r.id)}">
                ${PortalCuenta.visualVehiculo(r.vehiculoId, tituloAuto)}
                <div class="op-info">
                    <div class="op-titulo-fila">
                        <h2 class="op-titulo">${esc(tituloAuto)}</h2>
                        <span class="badge ${esc(badge.clase)}">${esc(badge.texto)}</span>
                    </div>
                    <p class="op-subtitulo">${esc(subtitulo)}</p>
                    <div class="op-meta">
                        <span><i class="fa-regular fa-calendar"></i>${esc(PortalCuenta.formatearRangoFechas(r.fechaInicio, r.fechaFin))} (${esc(textoDias)})</span>
                        <span><i class="fa-solid fa-location-dot"></i>Sucursal Saltillo</span>
                    </div>
                    <p class="op-precio-fila">
                        <strong>${esc(Portal.moneda(r.totalEstimado))}</strong>
                        <small>(${esc(Portal.moneda(r.tarifaDia))}/día · total estimado)</small>
                    </p>
                </div>
                <div class="op-acciones">
                    <button type="button" class="pt-btn pt-btn-outline pt-btn-sm" data-ver-detalle="${esc(r.id)}">
                        Ver detalles
                    </button>
                    ${cancelable ? `
                    <button type="button" class="pt-btn pt-btn-danger-outline pt-btn-sm" data-cancelar="${esc(r.id)}">
                        Cancelar
                    </button>` : ''}
                </div>
            </article>`;
    }

    function renderizarLista(lista) {
        contenedor.removeAttribute('aria-busy');
        if (!lista.length) {
            Portal.mostrarEstado(contenedor, {
                tipo: 'vacio',
                titulo: 'Aún no tienes reservaciones registradas',
                texto: 'Explora nuestro catálogo y agenda tu próximo vehículo en minutos.',
                accion: {
                    texto: 'Ir al catálogo',
                    icono: 'fa-car',
                    alHacerClic: () => { window.location.href = 'catalogo.html'; }
                }
            });
            return;
        }
        contenedor.innerHTML = lista.map(renderizarTarjeta).join('');
    }

    async function abrirDetalle(id) {
        const esc = Portal.escapar;
        modalTitulo.textContent = 'Detalle de la reservación';
        modalCuerpo.innerHTML = '<p>Cargando información…</p>';
        modalPie.innerHTML = '';
        modal.hidden = false;

        try {
            const resp = await PortalCuenta.apiCliente(`/v1/cliente/reservaciones/${encodeURIComponent(id)}`);
            const r = resp?.data;
            if (!r) throw new Error('No se encontró la reservación.');
            const tituloAuto = [r.marca, r.modelo, r.anio].filter(Boolean).join(' ') || `Vehículo #${r.vehiculoId}`;
            const badge = PortalCuenta.infoBadgeEstado(r.estado);
            const dias = PortalCuenta.calcularDiasOperacion(r.fechaInicio, r.fechaFin);

            modalCuerpo.innerHTML = `
                <div style="display:flex;align-items:center;justify-content:space-between;gap:.75rem;flex-wrap:wrap">
                    <strong>${esc(tituloAuto)}</strong>
                    <span class="badge ${esc(badge.clase)}">${esc(badge.texto)}</span>
                </div>
                <div class="pt-modal-dl">
                    <div><small>Folio</small><strong>${esc(PortalCuenta.formatearFolio('RES', r.id))}</strong></div>
                    <div><small>Categoría</small><strong>${esc(r.categoria || 'General')}</strong></div>
                    <div><small>Fecha de recogida</small><strong>${esc(PortalCuenta.formatearFechaCorta(r.fechaInicio))}</strong></div>
                    <div><small>Fecha de devolución</small><strong>${esc(PortalCuenta.formatearFechaCorta(r.fechaFin))}</strong></div>
                    <div><small>Duración</small><strong>${esc(dias)} ${dias === 1 ? 'día' : 'días'}</strong></div>
                    <div><small>Sucursal</small><strong>Sucursal Saltillo</strong></div>
                    <div><small>Tarifa por día</small><strong>${esc(Portal.moneda(r.tarifaDia))}</strong></div>
                    <div><small>Total estimado</small><strong>${esc(Portal.moneda(r.totalEstimado))}</strong></div>
                </div>
                <p class="nota" style="margin-top:.85rem">
                    Los montos son estimados; el cobro final se registra al devolver el vehículo.
                </p>`;
            modalPie.innerHTML = `
                <button type="button" class="pt-btn pt-btn-primary pt-btn-sm" id="btnAceptarDetalle">Cerrar</button>`;
            document.getElementById('btnAceptarDetalle').addEventListener('click', cerrarModal);
        } catch (error) {
            modalCuerpo.innerHTML = `<p class="pt-error">${esc(error.message || 'No se pudo cargar el detalle.')}</p>`;
            modalPie.innerHTML = `<button type="button" class="pt-btn pt-btn-outline pt-btn-sm" id="btnCerrarErr">Cerrar</button>`;
            document.getElementById('btnCerrarErr').addEventListener('click', cerrarModal);
        }
    }

    function confirmarCancelacion(id) {
        const r = reservacionesCache.find(item => String(item.id) === String(id));
        if (!r) return;
        const esc = Portal.escapar;
        const tituloAuto = [r.marca, r.modelo, r.anio].filter(Boolean).join(' ') || `Vehículo #${r.vehiculoId}`;
        const folio = PortalCuenta.formatearFolio('RES', r.id);

        modalTitulo.textContent = 'Cancelar reservación';
        modalCuerpo.innerHTML = `
            <p>¿Estás seguro de que deseas cancelar la reservación <strong>${esc(folio)}</strong> para el vehículo <strong>${esc(tituloAuto)}</strong>?</p>
            <p class="nota" style="margin-top:.65rem">Esta acción liberará la unidad apartada y no se puede deshacer.</p>
            <p class="pt-error" id="errorCancelarModal" style="margin-top:.65rem"></p>`;
        modalPie.innerHTML = `
            <button type="button" class="pt-btn pt-btn-outline pt-btn-sm" id="btnNoCancelar">Volver</button>
            <button type="button" class="pt-btn pt-btn-danger-outline pt-btn-sm" id="btnConfirmarCancelar">
                Sí, cancelar reservación
            </button>`;
        modal.hidden = false;

        document.getElementById('btnNoCancelar').addEventListener('click', cerrarModal);
        const btnConfirmar = document.getElementById('btnConfirmarCancelar');
        btnConfirmar.addEventListener('click', async () => {
            btnConfirmar.disabled = true;
            btnConfirmar.textContent = 'Cancelando…';
            const errorP = document.getElementById('errorCancelarModal');
            errorP.textContent = '';
            try {
                await PortalCuenta.apiCliente(`/v1/cliente/reservaciones/${encodeURIComponent(id)}/cancelar`, {
                    method: 'PATCH'
                });
                cerrarModal();
                await cargarReservaciones();
            } catch (error) {
                errorP.textContent = error.message || 'No fue posible cancelar la reservación.';
                btnConfirmar.disabled = false;
                btnConfirmar.textContent = 'Sí, cancelar reservación';
            }
        });
    }

    contenedor.addEventListener('click', (e) => {
        const btnDetalle = e.target.closest('[data-ver-detalle]');
        if (btnDetalle) {
            abrirDetalle(btnDetalle.getAttribute('data-ver-detalle'));
            return;
        }
        const btnCancelar = e.target.closest('[data-cancelar]');
        if (btnCancelar) {
            confirmarCancelacion(btnCancelar.getAttribute('data-cancelar'));
        }
    });

    async function cargarReservaciones() {
        mostrarSkeleton();
        try {
            const resp = await PortalCuenta.apiCliente('/v1/cliente/reservaciones');
            reservacionesCache = Array.isArray(resp?.data) ? resp.data : [];
            renderizarLista(reservacionesCache);
        } catch (error) {
            if (error && error.status === 401) return;
            Portal.mostrarEstado(contenedor, {
                tipo: 'error',
                titulo: 'No pudimos cargar tus reservaciones',
                texto: error?.message || 'Verifica tu conexión e inténtalo nuevamente.',
                accion: {
                    texto: 'Reintentar',
                    icono: 'fa-rotate-right',
                    alHacerClic: cargarReservaciones
                }
            });
        }
    }

    cargarReservaciones();
});
