document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');

    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        // Simulación simple de validación de login
        if (email && password) {
            alert('¡Inicio de sesión exitoso! Bienvenido a RentaAutos.');
            window.location.href = 'index.html';
        } else {
            alert('Por favor, completa todos los campos.');
        }
    });
});
