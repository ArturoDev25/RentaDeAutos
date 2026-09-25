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

    // Solo las pantallas que aún no tenían un manejador de cierre de sesión.
    document.querySelectorAll('[data-panel-logout]').forEach(boton => {
        boton.addEventListener('click', () => {
            localStorage.removeItem('usuarioSesion');
            window.location.replace('login.html');
        });
    });
});
