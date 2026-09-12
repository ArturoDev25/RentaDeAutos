document.addEventListener('DOMContentLoaded', () => {
    // --- LÓGICA DE LOGIN ---
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');
            const errorDiv = document.getElementById('authError');
            const submitBtn = loginForm.querySelector('.login-btn');

            if (!emailInput || !passwordInput) return;

            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!email || !password) return;

            // Estado de carga
            if (errorDiv) errorDiv.style.display = 'none';
            submitBtn.disabled = true;
            submitBtn.classList.add('btn-loading');
            const originalBtnText = submitBtn.innerText;
            submitBtn.innerText = 'CARGANDO...';

            try {
                const result = await fetchAPI('/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({
                        correo: email,
                        password: password
                    })
                });

                if (result.success) {
                    const usuario = {
                        email: result.data.user.correo,
                        nombre: result.data.user.nombre,
                        rol: result.data.user.rol,
                        token: result.data.accessToken
                    };

                    localStorage.setItem('usuarioSesion', JSON.stringify(usuario));
                    window.location.href = 'index.html';
                } else {
                    if (errorDiv) {
                        errorDiv.innerText = result.message || 'Credenciales inválidas';
                        errorDiv.style.display = 'block';
                    }
                }
            } catch (error) {
                console.error('Error de conexión:', error);
                if (errorDiv) {
                    errorDiv.innerText = 'No se pudo conectar con el servidor. Intenta más tarde.';
                    errorDiv.style.display = 'block';
                }
            } finally {
                submitBtn.disabled = false;
                submitBtn.classList.remove('btn-loading');
                submitBtn.innerText = originalBtnText;
            }
        });
    }

    // --- LÓGICA DE REGISTRO ---
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const nombreInput = document.getElementById('nombre');
            const emailInput = document.getElementById('reg-email');
            const passwordInput = document.getElementById('reg-password');
            const errorDiv = document.getElementById('authError');
            const submitBtn = registerForm.querySelector('.login-btn');

            if (!nombreInput || !emailInput || !passwordInput) return;

            const nombre = nombreInput.value.trim();
            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!nombre || !email || !password) return;

            // Estado de carga
            if (errorDiv) errorDiv.style.display = 'none';
            submitBtn.disabled = true;
            submitBtn.classList.add('btn-loading');
            const originalBtnText = submitBtn.innerText;
            submitBtn.innerText = 'REGISTRANDO...';

            try {
                const result = await fetchAPI('/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({
                        nombre: nombre,
                        correo: email,
                        password: password
                    })
                });

                if (result.success) {
                    window.location.href = 'login.html';
                } else {
                    if (errorDiv) {
                        errorDiv.innerText = result.message || 'Hubo un problema al registrarse';
                        errorDiv.style.display = 'block';
                    }
                }
            } catch (error) {
                if (errorDiv) {
                    errorDiv.innerText = 'No se pudo conectar con el servidor. Intenta de nuevo.';
                    errorDiv.style.display = 'block';
                }
            } finally {
                submitBtn.disabled = false;
                submitBtn.classList.remove('btn-loading');
                submitBtn.innerText = originalBtnText;
            }
        });
    }
});