document.addEventListener('DOMContentLoaded', () => {
    // 1. Verificar autenticación
    const usuarioSesion = JSON.parse(localStorage.getItem('usuarioSesion') || '{}');
    if (!usuarioSesion.token || !['ADMINISTRADOR', 'SUPERVISOR', 'AUDITOR'].includes(usuarioSesion.rol)) {
        window.location.href = 'index.html';
        return;
    }

    // Topbar — nombre y rol (igual que dashboard.js)
    document.getElementById('topbarUsername').textContent = usuarioSesion.nombre || 'Usuario';
    const rolEl = document.getElementById('topbarRol');
    if (rolEl) rolEl.textContent = usuarioSesion.rol || '';

    // 2. Fetch Wrapper con JWT
    async function fetchAudit(endpoint) {
        try {
            const response = await fetch(`${API_BASE_URL}/v1/audit${endpoint}`, {
                headers: {
                    'Authorization': `Bearer ${usuarioSesion.token}`,
                    'Content-Type': 'application/json'
                }
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Error en la petición');
            return data.data;
        } catch (error) {
            console.error('Error Audit API:', error);
            return null;
        }
    }

    // 3. Cargar Métricas KPI
    async function cargarMetricas() {
        const metrics = await fetchAudit('/metrics');
        if (metrics) {
            document.getElementById('kpi-total').textContent = metrics.totalRegistros.toLocaleString();
            document.getElementById('kpi-usuarios').textContent = metrics.usuariosActivos.toLocaleString();
            document.getElementById('kpi-hoy').textContent = metrics.accionesHoy.toLocaleString();
            document.getElementById('kpi-fallidos').textContent = metrics.intentosFallidos.toLocaleString();
        }
    }

    // 4. Cargar Filtros
    async function cargarFiltros() {
        const filters = await fetchAudit('/filters');
        if (filters) {
            const selectUsr = document.getElementById('filter-usuario');
            filters.usuarios.forEach(u => {
                const opt = document.createElement('option');
                opt.value = u.id; opt.textContent = u.nombre;
                selectUsr.appendChild(opt);
            });

            const selectMod = document.getElementById('filter-modulo');
            filters.modulos.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m; opt.textContent = m;
                selectMod.appendChild(opt);
            });

            const selectAcc = document.getElementById('filter-accion');
            filters.acciones.forEach(a => {
                const opt = document.createElement('option');
                opt.value = a; opt.textContent = a;
                selectAcc.appendChild(opt);
            });
        }
    }

    // 5. Cargar Tabla
    let currentPage = 0;
    
    function formatearFecha(isoString) {
        const d = new Date(isoString);
        return d.toLocaleDateString('es-MX') + ' ' + d.toLocaleTimeString('es-MX', {hour: '2-digit', minute:'2-digit'});
    }

    function getBadgeClass(accion) {
        const act = accion.toLowerCase();
        if (act.includes('edit')) return 'aud-badge-editar';
        if (act.includes('crear')) return 'aud-badge-crear';
        if (act.includes('elimin')) return 'aud-badge-eliminar';
        if (act.includes('inicio')) return 'aud-badge-login';
        if (act.includes('cancel')) return 'aud-badge-cancelar';
        if (act.includes('actualiz')) return 'aud-badge-actualizar';
        if (act.includes('error')) return 'aud-badge-error';
        return 'aud-badge-default';
    }

    async function cargarTabla(page = 0) {
        currentPage = page;
        const q = document.getElementById('filter-q').value;
        const u = document.getElementById('filter-usuario').value;
        const m = document.getElementById('filter-modulo').value;
        const a = document.getElementById('filter-accion').value;

        const params = new URLSearchParams({ page, size: 10 });
        if (q) params.append('q', q);
        if (u) params.append('usuarioId', u);
        if (m) params.append('modulo', m);
        if (a) params.append('accion', a);

        const pageData = await fetchAudit(`?${params.toString()}`);
        const tbody = document.getElementById('audit-tbody');
        
        if (!pageData || pageData.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">No se encontraron registros</td></tr>';
            document.getElementById('page-info').textContent = 'Mostrando 0 a 0 de 0 registros';
            document.getElementById('page-controls').innerHTML = '';
            return;
        }

        tbody.innerHTML = '';
        pageData.content.forEach(row => {
            const tr = document.createElement('tr');
            
            const badgeAction = getBadgeClass(row.accion);
            const badgeResult = row.resultado === 'Exitosa' ? 'aud-res-exitosa' : 'aud-res-fallida';
            
            tr.innerHTML = `
                <td><a href="#" class="aud-link open-modal" data-id="${row.id}">${row.idFormateado}</a></td>
                <td><a href="#" class="aud-link open-modal" data-id="${row.id}">${formatearFecha(row.fechaHora)}</a></td>
                <td>${row.usuarioNombre}</td>
                <td>${row.modulo}</td>
                <td><span class="aud-badge ${badgeAction}">${row.accion}</span></td>
                <td>${row.descripcion}</td>
                <td class="aud-ip">${row.direccionIp || '-'}</td>
                <td><span class="aud-badge ${badgeResult}">${row.resultado}</span></td>
            `;
            tbody.appendChild(tr);
        });

        // Event listeners para los enlaces del modal
        document.querySelectorAll('.open-modal').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                abrirModal(e.target.dataset.id);
            });
        });

        // Info de paginación
        const from = pageData.number * pageData.size + 1;
        const to = Math.min(from + pageData.size - 1, pageData.totalElements);
        document.getElementById('page-info').textContent = `Mostrando ${from} a ${to} de ${pageData.totalElements} registros`;

        // Controles de paginación
        renderizarPaginacion(pageData.totalPages, pageData.number);
    }

    function renderizarPaginacion(totalPages, current) {
        const controls = document.getElementById('page-controls');
        controls.innerHTML = '';
        
        if (totalPages <= 1) return;

        // Prev
        const prev = document.createElement('button');
        prev.className = `aud-page-btn ${current === 0 ? 'disabled' : ''}`;
        prev.innerHTML = '&lt;';
        prev.onclick = () => { if (current > 0) cargarTabla(current - 1); };
        controls.appendChild(prev);

        // Numeros
        for (let i = 0; i < totalPages; i++) {
            // Logica simple para mostrar todas si son pocas, o recortar. Aquí mostramos máx 5.
            if (totalPages > 7) {
                if (i !== 0 && i !== totalPages - 1 && Math.abs(i - current) > 1) {
                    if (Math.abs(i - current) === 2) {
                        const dots = document.createElement('span');
                        dots.innerHTML = '...';
                        controls.appendChild(dots);
                    }
                    continue;
                }
            }
            
            const btn = document.createElement('button');
            btn.className = `aud-page-btn ${i === current ? 'active' : ''}`;
            btn.textContent = i + 1;
            btn.onclick = () => { if (i !== current) cargarTabla(i); };
            controls.appendChild(btn);
        }

        // Next
        const next = document.createElement('button');
        next.className = `aud-page-btn ${current === totalPages - 1 ? 'disabled' : ''}`;
        next.innerHTML = '&gt;';
        next.onclick = () => { if (current < totalPages - 1) cargarTabla(current + 1); };
        controls.appendChild(next);
    }

    // 6. Modal Forense
    const modal = document.getElementById('auditModal');
    
    async function abrirModal(id) {
        const detail = await fetchAudit(`/${id}`);
        if (!detail) return;

        document.getElementById('modal-id').textContent = detail.idFormateado;
        document.getElementById('modal-usuario').textContent = detail.usuarioNombre;
        document.getElementById('modal-fecha').textContent = formatearFecha(detail.fechaHora);
        document.getElementById('modal-modulo').textContent = detail.modulo;
        
        const badgeAction = getBadgeClass(detail.accion);
        document.getElementById('modal-accion').innerHTML = `<span class="aud-badge ${badgeAction}">${detail.accion}</span>`;
        
        const badgeResult = detail.resultado === 'Exitosa' ? 'aud-res-exitosa' : 'aud-res-fallida';
        document.getElementById('modal-resultado').innerHTML = `<span class="aud-badge ${badgeResult}">${detail.resultado}</span>`;
        
        document.getElementById('modal-ip').textContent = detail.direccionIp || '-';
        document.getElementById('modal-descripcion').textContent = detail.descripcion;

        const formatJson = (str) => {
            if (!str) return 'N/A';
            try {
                return JSON.stringify(JSON.parse(str), null, 2);
            } catch (e) {
                return str; // Fallback
            }
        };

        document.getElementById('modal-antes').textContent = formatJson(detail.valoresAnteriores);
        document.getElementById('modal-despues').textContent = formatJson(detail.valoresNuevos);

        modal.classList.add('show');
    }

    document.getElementById('btn-cerrar-modal').addEventListener('click', () => {
        modal.classList.remove('show');
    });

    // Cerrar al clickear fuera
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.remove('show');
    });

    // 7. Listeners de Filtros
    let timeoutId;
    document.getElementById('filter-q').addEventListener('input', () => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => cargarTabla(0), 300);
    });

    ['filter-usuario', 'filter-modulo', 'filter-accion'].forEach(id => {
        document.getElementById(id).addEventListener('change', () => cargarTabla(0));
    });

    document.getElementById('btn-limpiar').addEventListener('click', () => {
        document.getElementById('filter-q').value = '';
        document.getElementById('filter-usuario').value = '';
        document.getElementById('filter-modulo').value = '';
        document.getElementById('filter-accion').value = '';
        cargarTabla(0);
    });

    // INICIALIZACIÓN
    cargarMetricas();
    cargarFiltros();
    cargarTabla(0);
});
