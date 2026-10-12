/* Administración de cuentas del personal (solo ADMINISTRADOR). */
document.addEventListener('DOMContentLoaded', () => {
    const $ = (id) => document.getElementById(id);

    // --- Sesión y gate de rol ---
    let sesion;
    try {
        sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null');
    } catch {
        sesion = null;
    }
    if (!sesion?.token) {
        window.location.replace('login.html');
        return;
    }
    if (sesion.rol !== 'ADMINISTRADOR') {
        alert('Solo el administrador puede gestionar usuarios.');
        window.location.replace('dashboard.html');
        return;
    }

    const nombreEl = $('nombre-usuario');
    if (nombreEl) nombreEl.textContent = sesion.nombre || 'Administrador';
    const btnSalir = $('cerrar-sesion');
    if (btnSalir) {
        btnSalir.addEventListener('click', () => {
            localStorage.removeItem('usuarioSesion');
            window.location.replace('login.html');
        });
    }

    const correoActual = (sesion.email || '').toLowerCase();

    // --- Utilidades ---
    const escapar = (txt) => String(txt ?? '').replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));

    const nombreRol = (rol) => {
        if (!rol) return '-';
        return rol.charAt(0) + rol.slice(1).toLowerCase();
    };

    const mostrarMsg = (texto, ok) => {
        const msg = $('form-msg');
        msg.textContent = texto;
        msg.className = 'form-msg ' + (ok ? 'ok' : 'err');
    };

    // --- Cargar lista ---
    async function cargarUsuarios() {
        const tbody = $('tabla-body');
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#6b7a73">Cargando…</td></tr>';
        try {
            const res = await fetchAPI('/admin/usuarios');
            const usuarios = res?.data || [];

            if (usuarios.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#6b7a73">No hay cuentas registradas.</td></tr>';
                return;
            }

            tbody.innerHTML = usuarios.map((u) => {
                const activo = u.activo === true;
                const esYo = (u.correo || '').toLowerCase() === correoActual;
                const estado = activo
                    ? '<span class="badge activo">Activo</span>'
                    : '<span class="badge inactivo">Inactivo</span>';
                let accion;
                if (esYo) {
                    accion = '<span style="color:#9aa8a1;font-size:.85rem">Tu cuenta</span>';
                } else if (activo) {
                    accion = `<button class="btn-estado desactivar" data-id="${u.id}" data-activo="false">Desactivar</button>`;
                } else {
                    accion = `<button class="btn-estado activar" data-id="${u.id}" data-activo="true">Activar</button>`;
                }
                return `<tr>
                    <td>${escapar(u.nombre)}</td>
                    <td>${escapar(u.correo)}</td>
                    <td><span class="rol-chip">${escapar(nombreRol(u.rol))}</span></td>
                    <td>${estado}</td>
                    <td>${accion}</td>
                </tr>`;
            }).join('');

            // Wire botones de estado
            tbody.querySelectorAll('.btn-estado[data-id]').forEach((btn) => {
                btn.addEventListener('click', () =>
                    cambiarEstado(btn.dataset.id, btn.dataset.activo === 'true', btn));
            });
        } catch (error) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:#b3261e">${escapar(error.message || 'No se pudieron cargar los usuarios.')}</td></tr>`;
        }
    }

    // --- Crear cuenta ---
    async function crear(evento) {
        evento.preventDefault();
        const btn = $('btn-crear');
        const datos = {
            nombre: $('nombre').value.trim(),
            correo: $('correo').value.trim(),
            password: $('password').value,
            rol: $('rol').value
        };
        if (!datos.nombre || !datos.correo || !datos.password || !datos.rol) {
            mostrarMsg('Completa todos los campos obligatorios.', false);
            return;
        }

        btn.disabled = true;
        try {
            await fetchAPI('/admin/usuarios', {
                method: 'POST',
                body: JSON.stringify(datos)
            });
            mostrarMsg(`Cuenta de ${datos.nombre} creada correctamente.`, true);
            $('form-crear').reset();
            await cargarUsuarios();
        } catch (error) {
            mostrarMsg(error.message || 'No se pudo crear la cuenta.', false);
        } finally {
            btn.disabled = false;
        }
    }

    // --- Activar / desactivar ---
    async function cambiarEstado(id, nuevoActivo, btn) {
        btn.disabled = true;
        try {
            await fetchAPI(`/admin/usuarios/${id}/estado`, {
                method: 'PATCH',
                body: JSON.stringify({ activo: nuevoActivo })
            });
            await cargarUsuarios();
        } catch (error) {
            alert(error.message || 'No se pudo cambiar el estado.');
            btn.disabled = false;
        }
    }

    // --- Eventos ---
    $('form-crear').addEventListener('submit', crear);
    $('btn-recargar').addEventListener('click', cargarUsuarios);

    cargarUsuarios();
});
