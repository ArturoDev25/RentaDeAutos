document.addEventListener('DOMContentLoaded', () => {
    // --- LÓGICA DE LOGIN ---
       // --- LÓGICA DE LOGIN ---
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');
            const errorBox = document.getElementById('loginError');
            const submitBtn = loginForm.querySelector('button[type="submit"]');

            if (!emailInput || !passwordInput) return;

            const correo = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (errorBox) {
                errorBox.style.display = 'none';
                errorBox.textContent = '';
            }

            if (!correo || !password) return;

            if (submitBtn) submitBtn.disabled = true;

            try {
                const respuesta = await fetchAPI('/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ correo, password })
                });

                const usuario = {
                    correo: respuesta.data.user.correo,
                    nombre: respuesta.data.user.nombre,
                    rol: respuesta.data.user.rol,
                    token: respuesta.data.accessToken
                };

                localStorage.setItem('usuarioSesion', JSON.stringify(usuario));
                window.location.href = 'index.html';
            } catch (error) {
                if (errorBox) {
                    errorBox.textContent = error.message || 'No se pudo iniciar sesión.';
                    errorBox.style.display = 'block';
                } else {
                    alert(error.message || 'No se pudo iniciar sesión.');
                }
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
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