// reportes-cliente.js - Vista "Reportes" del cliente autenticado (S4-10)

document.addEventListener('DOMContentLoaded', () => {
    const sesion = PortalCuenta.requerirCliente();
    if (!sesion) return;

    const contenedor = document.getElementById('reportesContenedor');
    const selectorPeriodo = document.getElementById('selectorPeriodo');
    const avisoImportes = document.getElementById('avisoImportes');

    let periodoActual = '3M';
    let resumenActual = null;

    function badgeVariacion(pct) {
        const valor = Number(pct ?? 0);
        if (!Number.isFinite(valor) || valor === 0) {
            return `<span class="kpi-delta neu">0%</span>`;
        }
        const positivo = valor > 0;
        const signo = positivo ? '+' : '';
        const icono = positivo ? 'fa-arrow-up' : 'fa-arrow-down';
        const clase = positivo ? 'pos' : 'neg';
        return `<span class="kpi-delta ${clase}"><i class="fa-solid ${icono}"></i>${signo}${valor.toFixed(0)}%</span>`;
    }

    function renderizarBarras(serie) {
        const esc = Portal.escapar;
        if (!Array.isArray(serie) || !serie.length) {
            return '<p class="nota">Sin actividad registrada en este periodo.</p>';
        }
        const max = Math.max(1, ...serie.map(s => Number(s.cantidad) || 0));
        return `
            <div class="bar-chart" role="img" aria-label="Gráfica de rentas por periodo">
                ${serie.map((item, idx) => {
                    const cant = Number(item.cantidad) || 0;
                    const alturaPct = cant === 0 ? 6 : Math.max(14, Math.round((cant / max) * 100));
                    const esUltimo = idx === serie.length - 1;
                    return `
                        <div class="bar-col ${esUltimo ? 'highlight' : ''}">
                            <span class="bar-val">${esc(cant)}</span>
                            <div class="bar-fill" style="height:${alturaPct}%"></div>
                            <span class="bar-lbl">${esc(item.etiqueta)}</span>
                        </div>`;
                }).join('')}
            </div>`;
    }

    function renderizarDona(distribucion) {
        const esc = Portal.escapar;
        const colores = {
            FINALIZADA: { color: '#22c55e', nombre: 'Finalizadas' },
            EN_CURSO: { color: '#2563eb', nombre: 'Activas' },
            RESERVADA: { color: '#f59e0b', nombre: 'Reservadas' },
            CANCELADA: { color: '#ef4444', nombre: 'Canceladas' }
        };
        const items = Array.isArray(distribucion) ? distribucion : [];
        const total = items.reduce((acc, it) => acc + (Number(it.cantidad) || 0), 0);

        const radio = 15.9155;
        let acumulado = 0;
        const segmentos = total > 0
            ? items.filter(it => Number(it.cantidad) > 0).map(it => {
                const pct = (Number(it.cantidad) / total) * 100;
                const dash = `${pct} ${100 - pct}`;
                const offset = 25 - acumulado;
                acumulado += pct;
                const cfg = colores[it.estado] || { color: '#94a3b8' };
                return `<circle cx="21" cy="21" r="${radio}" fill="transparent"
                            stroke="${cfg.color}" stroke-width="5.5"
                            stroke-dasharray="${dash}" stroke-dashoffset="${offset}"></circle>`;
            }).join('')
            : `<circle cx="21" cy="21" r="${radio}" fill="transparent" stroke="#e2e8f0" stroke-width="5.5"></circle>`;

        return `
            <div class="donut-wrap">
                <svg class="donut-svg" viewBox="0 0 42 42" role="img" aria-label="Distribución por estado">
                    <circle cx="21" cy="21" r="${radio}" fill="transparent" stroke="#f1f5f9" stroke-width="5.5"></circle>
                    ${segmentos}
                    <text x="21" y="20" text-anchor="middle" font-size="6.5" font-weight="800" fill="#1e293b">${esc(total)}</text>
                    <text x="21" y="25.5" text-anchor="middle" font-size="3.2" fill="#64748b">operaciones</text>
                </svg>
                <ul class="donut-legend">
                    ${items.map(it => {
                        const cfg = colores[it.estado] || { color: '#94a3b8', nombre: it.estado };
                        return `
                            <li>
                                <span><span class="donut-dot" style="background-color:${cfg.color}"></span>${esc(cfg.nombre)}</span>
                                <strong>${esc(it.cantidad)} (${esc(Number(it.porcentaje ?? 0).toFixed(0))}%)</strong>
                            </li>`;
                    }).join('')}
                </ul>
            </div>`;
    }

    function renderizarReporte(data) {
        const esc = Portal.escapar;
        if (data.avisoImportes && avisoImportes) {
            avisoImportes.querySelector('span').textContent = data.avisoImportes;
        }
        const v = data.variacionPeriodoAnterior || {};
        const historial = Array.isArray(data.historial) ? data.historial : [];

        contenedor.innerHTML = `
            <section class="kpi-grid" aria-label="Indicadores principales">
                <article class="kpi-card">
                    <div class="kpi-top">
                        <span class="kpi-icon blue"><i class="fa-solid fa-car"></i></span>
                        ${badgeVariacion(v.rentasPct)}
                    </div>
                    <span class="kpi-label">Total de rentas</span>
                    <strong class="kpi-value">${esc(data.totalRentas)}</strong>
                </article>
                <article class="kpi-card">
                    <div class="kpi-top">
                        <span class="kpi-icon green"><i class="fa-solid fa-dollar-sign"></i></span>
                        ${badgeVariacion(v.gastoPct)}
                    </div>
                    <span class="kpi-label">Gasto total (estimado/final)</span>
                    <strong class="kpi-value">${esc(Portal.moneda(data.gastoTotalEstimado))}</strong>
                </article>
                <article class="kpi-card">
                    <div class="kpi-top">
                        <span class="kpi-icon amber"><i class="fa-regular fa-calendar-check"></i></span>
                        ${badgeVariacion(v.diasPct)}
                    </div>
                    <span class="kpi-label">Días rentados</span>
                    <strong class="kpi-value">${esc(data.diasRentados)}</strong>
                </article>
                <article class="kpi-card">
                    <div class="kpi-top">
                        <span class="kpi-icon purple"><i class="fa-solid fa-tag"></i></span>
                        ${badgeVariacion(v.costoPromedioDiaPct)}
                    </div>
                    <span class="kpi-label">Costo promedio / día</span>
                    <strong class="kpi-value">${esc(Portal.moneda(data.costoPromedioDia))}</strong>
                </article>
            </section>

            <section class="charts-grid">
                <div class="card">
                    <h2 class="card-title"><i class="fa-solid fa-chart-column"></i>Rentas por periodo</h2>
                    ${renderizarBarras(data.seriePorPeriodo)}
                </div>
                <div class="card">
                    <h2 class="card-title"><i class="fa-solid fa-chart-pie"></i>Distribución por estado</h2>
                    ${renderizarDona(data.distribucionEstado)}
                </div>
            </section>

            <section class="card">
                <h2 class="card-title"><i class="fa-solid fa-table-list"></i>Historial de operaciones en el periodo</h2>
                ${historial.length ? `
                <div class="tabla-responsive">
                    <table class="pt-tabla">
                        <thead>
                            <tr>
                                <th>Folio</th>
                                <th>Vehículo</th>
                                <th>Fechas</th>
                                <th>Días</th>
                                <th>Importe</th>
                                <th>Estado</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${historial.map(item => {
                                const badge = PortalCuenta.infoBadgeEstado(item.estado);
                                const importe = item.totalFinal != null ? item.totalFinal : item.totalEstimado;
                                const notaImp = item.totalFinal != null ? 'Final' : 'Estimado';
                                return `
                                    <tr>
                                        <td><strong>${esc(item.folio)}</strong></td>
                                        <td>${esc(item.vehiculo)}</td>
                                        <td>${esc(PortalCuenta.formatearRangoFechas(item.fechaInicio, item.fechaFin))}</td>
                                        <td>${esc(item.dias)}</td>
                                        <td><strong>${esc(Portal.moneda(importe))}</strong> <small class="nota">(${esc(notaImp)})</small></td>
                                        <td><span class="badge ${esc(badge.clase)}">${esc(badge.texto)}</span></td>
                                    </tr>`;
                            }).join('')}
                        </tbody>
                    </table>
                </div>` : `
                <p class="nota">No se registraron operaciones en el periodo seleccionado.</p>`}
            </section>

            <section class="card descarga-bar">
                <div>
                    <h2 class="card-title" style="margin-bottom:.25rem"><i class="fa-solid fa-file-arrow-down"></i>Descargar reporte</h2>
                    <p class="nota">Exporta el resumen de tus operaciones del periodo seleccionado en formato CSV compatible con hojas de cálculo.</p>
                </div>
                <button type="button" class="pt-btn pt-btn-primary" id="btnDescargarCsv" ${historial.length ? '' : 'disabled'}>
                    <i class="fa-solid fa-file-csv"></i>Resumen de mis rentas (CSV)
                </button>
            </section>`;

        const btnCsv = document.getElementById('btnDescargarCsv');
        if (btnCsv) {
            btnCsv.addEventListener('click', () => exportarHistorialCsv(historial));
        }
    }

    function exportarHistorialCsv(historial) {
        const encabezados = ['Folio', 'Vehiculo', 'Categoria', 'Fecha Inicio', 'Fecha Fin', 'Dias', 'Total Estimado', 'Total Final', 'Estado'];
        const filas = historial.map(h => [
            h.folio,
            h.vehiculo,
            h.categoria || '',
            PortalCuenta.formatearFechaCorta(h.fechaInicio),
            PortalCuenta.formatearFechaCorta(h.fechaFin),
            h.dias,
            h.totalEstimado ?? '',
            h.totalFinal ?? 'Estimado (pendiente)',
            PortalCuenta.infoBadgeEstado(h.estado).texto
        ]);
        PortalCuenta.descargarCsv(`resumen-mis-rentas-${periodoActual.toLowerCase()}.csv`, encabezados, filas);
    }

    async function cargarResumen() {
        contenedor.innerHTML = '<div class="card"><p class="nota">Cargando resumen de operaciones…</p></div>';
        try {
            const resp = await PortalCuenta.apiCliente(`/v1/cliente/reportes/resumen?periodo=${encodeURIComponent(periodoActual)}`);
            resumenActual = resp?.data;
            if (!resumenActual) throw new Error('Respuesta vacía del servidor.');
            renderizarReporte(resumenActual);
        } catch (error) {
            if (error && error.status === 401) return;
            Portal.mostrarEstado(contenedor, {
                tipo: 'error',
                titulo: 'No pudimos cargar tu resumen de reportes',
                texto: error?.message || 'Verifica tu conexión e inténtalo nuevamente.',
                accion: {
                    texto: 'Reintentar',
                    icono: 'fa-rotate-right',
                    alHacerClic: cargarResumen
                }
            });
        }
    }

    selectorPeriodo.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-periodo]');
        if (!btn) return;
        periodoActual = btn.getAttribute('data-periodo') || '3M';
        selectorPeriodo.querySelectorAll('.chip-btn').forEach(b => {
            b.classList.toggle('active', b === btn);
        });
        cargarResumen();
    });

    cargarResumen();
});
