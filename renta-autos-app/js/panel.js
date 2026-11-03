/* Presentación común; los permisos y la carga de datos siguen en cada módulo. */
document.addEventListener('DOMContentLoaded', () => {
    let sesion;
    try {
        sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null');
    } catch {
        sesion = null;
    }

    const roles = {
        ADMINISTRADOR: 'Administrador',
        AGENTE: 'Agente',
        SUPERVISOR: 'Supervisor',
        AUDITOR: 'Auditor',
        CLIENTE: 'Cliente'
    };
    document.querySelectorAll('[data-panel-rol]').forEach(elemento => {
        elemento.textContent = roles[sesion?.rol] || 'Usuario';
    });

    // Menú según el rol: se ocultan los accesos que el backend bloquea con 403
    // (Categorías es solo del Administrador; Auditoría no aplica al Agente).
    // Es únicamente presentación; la seguridad real la impone Spring Security.
    const paginasSinAcceso = {
        ADMINISTRADOR: [],
        AGENTE:        ['categorias.html', 'auditoria.html', 'usuarios.html'],
        SUPERVISOR:    ['categorias.html', 'usuarios.html'],
        // S5-09: el Auditor no aprueba ni rechaza cuentas de clientes.
        AUDITOR:       ['categorias.html', 'usuarios.html', 'clientes-pendientes.html']
    };
    const ocultar = paginasSinAcceso[sesion?.rol] || [];
    if (ocultar.length) {
        const selector = ocultar
            .map(p => `a.sidebar-link[href="${p}"], a.management-card[href="${p}"]`)
            .join(', ');
        document.querySelectorAll(selector).forEach(enlace => {
            enlace.style.display = 'none';
        });
    }

    // Solo las pantallas que aún no tenían un manejador de cierre de sesión.
    document.querySelectorAll('[data-panel-logout]').forEach(boton => {
        boton.addEventListener('click', () => {
            localStorage.removeItem('usuarioSesion');
            window.location.replace('login.html');
        });
    });
});

/* ============================================================
 * Paginación automática: 10 filas por página en TODAS las tablas.
 * Respeta filtros y recargas (vuelve a la página 1 al cambiar el
 * contenido). Es solo presentación; no toca los datos ni el backend.
 * ============================================================ */
(function () {
    const POR_PAGINA = 10;

    // Una sola fila con celda que ocupa toda la tabla = mensaje "no hay datos".
    function esPlaceholder(tbody) {
        return tbody.rows.length === 1
            && tbody.rows[0].cells.length === 1
            && tbody.rows[0].cells[0].colSpan > 1;
    }

    // Si la tabla está sola dentro de un contenedor, el paginador va después del contenedor.
    function ancla(tabla) {
        const padre = tabla.parentElement;
        return (padre && padre.children.length === 1) ? padre : tabla;
    }

    function obtenerPager(tabla) {
        const base = ancla(tabla);
        let pager = base.nextElementSibling;
        if (pager && pager.classList && pager.classList.contains('tabla-paginador')) {
            return pager;
        }
        pager = document.createElement('div');
        pager.className = 'tabla-paginador';
        pager.style.cssText = 'gap:0.6rem;align-items:center;justify-content:flex-end;margin:0.75rem 0;flex-wrap:wrap;';
        base.insertAdjacentElement('afterend', pager);
        return pager;
    }

    function boton(texto, deshabilitado, alHacerClic) {
        const b = document.createElement('button');
        b.type = 'button';
        b.textContent = texto;
        b.disabled = deshabilitado;
        b.style.cssText = 'padding:0.45rem 0.85rem;border:1px solid #d1d5db;border-radius:8px;'
            + 'background:#fff;color:#111827;font:inherit;cursor:' + (deshabilitado ? 'default;opacity:0.5;' : 'pointer;');
        if (!deshabilitado) b.addEventListener('click', alHacerClic);
        return b;
    }

    function paginar(tabla) {
        const tbody = tabla.tBodies[0];
        if (!tbody) return;
        const filas = Array.from(tbody.rows);
        const pager = obtenerPager(tabla);

        if (filas.length <= POR_PAGINA || esPlaceholder(tbody)) {
            filas.forEach(tr => { tr.style.display = ''; });
            pager.style.display = 'none';
            pager.replaceChildren();
            return;
        }

        const totalPaginas = Math.ceil(filas.length / POR_PAGINA);
        const pagina = Math.min(Math.max(tabla.__pagina || 1, 1), totalPaginas);
        tabla.__pagina = pagina;

        const inicio = (pagina - 1) * POR_PAGINA;
        filas.forEach((tr, i) => {
            tr.style.display = (i >= inicio && i < inicio + POR_PAGINA) ? '' : 'none';
        });

        const info = document.createElement('span');
        info.style.cssText = 'color:#6b7280;font-size:0.9rem;';
        info.textContent = 'Página ' + pagina + ' de ' + totalPaginas + ' (' + filas.length + ' en total)';

        pager.replaceChildren(
            boton('‹ Anterior', pagina === 1, () => { tabla.__pagina = pagina - 1; paginar(tabla); }),
            info,
            boton('Siguiente ›', pagina === totalPaginas, () => { tabla.__pagina = pagina + 1; paginar(tabla); })
        );
        pager.style.display = 'flex';
    }

    function activar() {
        document.querySelectorAll('table').forEach(tabla => {
            const tbody = tabla.tBodies[0];
            if (!tbody) return;
            if (!tabla.__paginado) {
                tabla.__paginado = true;
                // Al recargar o filtrar (cambia el contenido del tbody), volver a la página 1.
                new MutationObserver(() => {
                    tabla.__pagina = 1;
                    paginar(tabla);
                }).observe(tbody, { childList: true });
            }
            paginar(tabla);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', activar);
    } else {
        activar();
    }
})();
