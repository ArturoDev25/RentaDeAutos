document.addEventListener('DOMContentLoaded', () => {
    function mostrarError(elemento, mensaje) {
        if (!elemento) return;
        elemento.innerText = mensaje;
        elemento.style.display = 'block';
    }

    function mensajeDeError(error, mensajePredeterminado) {
        if (error?.status === 400) return error.message || 'Revisa los datos ingresados.';
        if (error?.status === 401) return 'Correo o contraseña incorrectos';
        if (error?.status === 409) {
            return 'El correo electrónico o número de licencia ya están registrados.';
        }
        return error?.message || mensajePredeterminado;
    }

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');
            const errorDiv = document.getElementById('authError');
            const submitBtn = loginForm.querySelector('.login-btn');
            if (!emailInput || !passwordInput || !submitBtn) return;

            const email = emailInput.value.trim();
            const password = passwordInput.value;
            if (!email || !password) {
                mostrarError(errorDiv, 'Ingresa tu correo electrónico y contraseña.');
                return;
            }

            if (errorDiv) errorDiv.style.display = 'none';
            submitBtn.disabled = true;
            submitBtn.classList.add('btn-loading');
            const originalBtnText = submitBtn.innerText;
            submitBtn.innerText = 'CARGANDO...';

            try {
                const result = await fetchAPI('/auth/cliente/login', {
                    method: 'POST',
                    body: JSON.stringify({ correo: email, password })
                });

                if (result?.success && result.data?.accessToken && result.data?.user) {
                    const usuario = {
                        email: result.data.user.correo,
                        nombre: result.data.user.nombre,
                        rol: result.data.user.rol,
                        token: result.data.accessToken,
                        tokenType: result.data.tokenType || 'Bearer',
                        expiresIn: result.data.expiresIn
                    };
                    localStorage.setItem('usuarioSesion', JSON.stringify(usuario));

                    const rolesStaff = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR', 'AUDITOR'];
                    if (rolesStaff.includes(usuario.rol)) {
                        window.location.href = 'dashboard.html';
                    } else {
                        const redirect = new URLSearchParams(window.location.search).get('redirect');
                        const destinoSeguro = redirect &&
                            /^[a-z0-9_-]+\.html(\?[\w=&%.-]*)?$/i.test(redirect);
                        window.location.href = destinoSeguro ? redirect : 'index.html';
                    }
                } else {
                    mostrarError(errorDiv, result?.message || 'No se pudo iniciar sesión.');
                }
            } catch (error) {
                console.error('Error de inicio de sesión:', error);
                mostrarError(errorDiv, mensajeDeError(
                    error,
                    'No se pudo conectar con el servidor. Intenta más tarde.'
                ));
            } finally {
                submitBtn.disabled = false;
                submitBtn.classList.remove('btn-loading');
                submitBtn.innerText = originalBtnText;
            }
        });
    }
    
    // Mostrar y ocultar contraseña en el login
    const passwordInput = document.getElementById('password');
    const togglePassword = document.getElementById('togglePassword');

    if (passwordInput && togglePassword) {
        togglePassword.addEventListener('click', () => {
            const mostrar = passwordInput.type === 'password';

            passwordInput.type = mostrar ? 'text' : 'password';

            togglePassword.innerHTML = mostrar
                ? '<i class="fa-solid fa-eye-slash"></i>'
                : '<i class="fa-solid fa-eye"></i>';

            togglePassword.setAttribute(
                'aria-label',
                mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña'
            );
        });
    }


    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        const vencimientoInput = document.getElementById('licenciaVencimiento');
        if (vencimientoInput) {
            const manana = new Date();
            manana.setDate(manana.getDate() + 1);
            manana.setMinutes(manana.getMinutes() - manana.getTimezoneOffset());
            vencimientoInput.min = manana.toISOString().slice(0, 10);
        }

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const ids = [
                'nombre', 'apellidos', 'reg-email', 'reg-password', 'telefono',
                'numeroLicencia', 'licenciaVencimiento', 'direccion'
            ];
            const campos = Object.fromEntries(ids.map(id => [id, document.getElementById(id)]));
            const errorDiv = document.getElementById('authError');
            const submitBtn = registerForm.querySelector('.login-btn');
            if (Object.values(campos).some(campo => !campo) || !submitBtn) return;

            const nombre = campos.nombre.value.trim();
            const apellidos = campos.apellidos.value.trim();
            const email = campos['reg-email'].value.trim();
            const password = campos['reg-password'].value;
            const telefono = campos.telefono.value.trim();
            const numeroLicencia = campos.numeroLicencia.value.trim();
            const licenciaVencimiento = campos.licenciaVencimiento.value;
            const direccion = campos.direccion.value.trim();

            if (!nombre || !apellidos || !email || !password || !telefono ||
                !numeroLicencia || !licenciaVencimiento) {
                mostrarError(errorDiv, 'Completa todos los campos obligatorios.');
                return;
            }

            if (errorDiv) errorDiv.style.display = 'none';
            submitBtn.disabled = true;
            submitBtn.classList.add('btn-loading');
            const originalBtnText = submitBtn.innerText;
            submitBtn.innerText = 'REGISTRANDO...';

            try {
                const result = await fetchAPI('/auth/cliente/registro', {
                    method: 'POST',
                    body: JSON.stringify({
                        nombre,
                        apellidos,
                        correo: email,
                        password,
                        telefono,
                        numeroLicencia,
                        licenciaVencimiento,
                        direccion: direccion || null
                    })
                });

                if (result?.success) {
                    window.location.href = 'login.html';
                } else {
                    mostrarError(errorDiv, result?.message || 'Hubo un problema al registrarse.');
                }
            } catch (error) {
                console.error('Error de registro:', error);
                mostrarError(errorDiv, mensajeDeError(
                    error,
                    'No se pudo conectar con el servidor. Intenta de nuevo.'
                ));
            } finally {
                submitBtn.disabled = false;
                submitBtn.classList.remove('btn-loading');
                submitBtn.innerText = originalBtnText;
            }
        });
    }

    window.logout = () => {
        localStorage.removeItem('usuarioSesion');
        window.location.href = 'login.html';
    };

    window.validarAcceso = (rolRequerido = null) => {
        const sesion = localStorage.getItem('usuarioSesion');
        if (!sesion) {
            window.location.href = 'login.html';
            return;
        }

        try {
            const usuario = JSON.parse(sesion);
            if (!usuario.token || (rolRequerido && usuario.rol !== rolRequerido)) {
                window.location.href = 'index.html';
            }
        } catch (error) {
            console.error('Sesión inválida:', error);
            localStorage.removeItem('usuarioSesion');
            window.location.href = 'login.html';
        }
    };
});
