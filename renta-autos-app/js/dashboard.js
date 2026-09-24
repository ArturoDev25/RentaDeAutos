document.addEventListener('DOMContentLoaded', () => {

    // Obtener la sesión guardada
    const sesion = localStorage.getItem('usuarioSesion');

    // Si no hay sesión, regresar al login
    if (!sesion) {
        window.location.href = 'login.html';
        return;
    }

    let usuario;

    try {
        usuario = JSON.parse(sesion);
    } catch (error) {
        console.error('Error al leer la sesión:', error);
        localStorage.removeItem('usuarioSesion');
        window.location.href = 'login.html';
        return;
    }

    // Verificar que el usuario sea administrador
    if (usuario.rol !== 'ADMINISTRADOR') {
        alert('No tienes permisos para acceder al panel de administrador.');
        window.location.href = 'index.html';
        return;
    }

    // Mostrar nombre del administrador
    const nombreAdministrador =
        document.getElementById('nombreAdministrador');

    if (nombreAdministrador) {
        nombreAdministrador.textContent =
            usuario.nombre || 'Administrador';
    }

    // Botón cerrar sesión
    const btnCerrarSesion =
        document.getElementById('btnCerrarSesion');

    if (btnCerrarSesion) {

        btnCerrarSesion.addEventListener('click', () => {

            localStorage.removeItem('usuarioSesion');

            window.location.href = 'login.html';
        });
    }

});