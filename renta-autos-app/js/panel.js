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
        AUDITOR:       ['categorias.html', 'usuarios.html']
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
