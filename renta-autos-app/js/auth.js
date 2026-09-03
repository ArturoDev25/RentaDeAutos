document.addEventListener('DOMContentLoaded', () => {
    // --- LÓGICA DE LOGIN ---
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();

            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');

            if (!emailInput || !passwordInput) return;

            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!email || !password) return;

            const usuario = {
                email: email,
                token: 'token-simulado-123456'
            };

            localStorage.setItem('usuarioSesion', JSON.stringify(usuario));
            window.location.href = 'index.html';
        });
    }

    // --- LÓGICA DE REGISTRO ---
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', (e) => {
            e.preventDefault();

            const nombreInput = document.getElementById('nombre');
            const emailInput = document.getElementById('reg-email');
            const passwordInput = document.getElementById('reg-password');

            if (!nombreInput || !emailInput || !passwordInput) return;

            const nombre = nombreInput.value.trim();
            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!nombre || !email || !password) return;

            const nuevoUsuario = {
                nombre: nombre,
                email: email
            };

            localStorage.setItem('usuarioRegistrado', JSON.stringify(nuevoUsuario));
            window.location.href = 'login.html';
        });
    }
});