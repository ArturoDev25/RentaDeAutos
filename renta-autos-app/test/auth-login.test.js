// auth-login.test.js — Pruebas unitarias para la lógica de autenticación en cascada (S5-03)
const test = require('node:test');
const assert = require('node:assert');
const { autenticarEnCascada, determinarDestinoLogin, mensajeDeError } = require('../js/auth.js');

test('S5-03: Caso 1 - Cliente válido autentica en /auth/cliente/login sin llamar a personal', async () => {
    const llamadas = [];
    const mockFetch = async (endpoint, options) => {
        llamadas.push({ endpoint, body: JSON.parse(options.body) });
        if (endpoint === '/auth/cliente/login') {
            return {
                success: true,
                data: {
                    accessToken: 'jwt-cliente-token',
                    tokenType: 'Bearer',
                    expiresIn: 1800,
                    user: { correo: 'cliente@correo.com', nombre: 'Juan Perez', rol: 'CLIENTE' }
                }
            };
        }
        throw new Error('No debio llamar a este endpoint');
    };

    const resultado = await autenticarEnCascada('cliente@correo.com', 'password123', mockFetch);

    assert.strictEqual(llamadas.length, 1, 'Solo debe llamar a 1 endpoint');
    assert.strictEqual(llamadas[0].endpoint, '/auth/cliente/login');
    assert.strictEqual(llamadas[0].body.correo, 'cliente@correo.com');
    assert.strictEqual(resultado.success, true);
    assert.strictEqual(resultado.data.user.rol, 'CLIENTE');
});

test('S5-03: Caso 2a - Personal ADMINISTRADOR recibe 401 en /auth/cliente/login y autentica en /auth/login', async () => {
    const llamadas = [];
    const mockFetch = async (endpoint, options) => {
        llamadas.push({ endpoint, body: JSON.parse(options.body) });
        if (endpoint === '/auth/cliente/login') {
            const err = new Error('Correo o contraseña incorrectos');
            err.status = 401;
            throw err;
        }
        if (endpoint === '/auth/login') {
            return {
                success: true,
                data: {
                    accessToken: 'jwt-admin-token',
                    tokenType: 'Bearer',
                    expiresIn: 1800,
                    user: { correo: 'admin@rentadeautos.com', nombre: 'Admin General', rol: 'ADMINISTRADOR' }
                }
            };
        }
        throw new Error('Endpoint no esperado');
    };

    const resultado = await autenticarEnCascada('admin@rentadeautos.com', 'Admin123!', mockFetch);

    assert.strictEqual(llamadas.length, 2, 'Debe intentar primero cliente y luego personal');
    assert.strictEqual(llamadas[0].endpoint, '/auth/cliente/login');
    assert.strictEqual(llamadas[1].endpoint, '/auth/login');
    assert.strictEqual(resultado.success, true);
    assert.strictEqual(resultado.data.user.rol, 'ADMINISTRADOR');
});

test('S5-03: Caso 2b - Personal AGENTE y SUPERVISOR autentican correctamente via /auth/login tras 401', async () => {
    for (const rol of ['AGENTE', 'SUPERVISOR', 'AUDITOR']) {
        const mockFetch = async (endpoint) => {
            if (endpoint === '/auth/cliente/login') {
                const err = new Error('Correo o contraseña incorrectos');
                err.status = 401;
                throw err;
            }
            if (endpoint === '/auth/login') {
                return {
                    success: true,
                    data: {
                        accessToken: `jwt-${rol}-token`,
                        tokenType: 'Bearer',
                        expiresIn: 1800,
                        user: { correo: `${rol.toLowerCase()}@rentadeautos.com`, nombre: `Usuario ${rol}`, rol }
                    }
                };
            }
            throw new Error('Inesperado');
        };

        const res = await autenticarEnCascada(`${rol.toLowerCase()}@rentadeautos.com`, 'Pass123!', mockFetch);
        assert.strictEqual(res.success, true);
        assert.strictEqual(res.data.user.rol, rol);
    }
});

test('S5-03: Caso 3 - Credenciales inválidas para cliente y personal lanzan error 401', async () => {
    const llamadas = [];
    const mockFetch = async (endpoint) => {
        llamadas.push(endpoint);
        const err = new Error('Correo o contraseña incorrectos');
        err.status = 401;
        throw err;
    };

    await assert.rejects(
        async () => {
            await autenticarEnCascada('noexiste@correo.com', 'clavemalo', mockFetch);
        },
        (err) => {
            assert.strictEqual(err.status, 401);
            return true;
        }
    );

    assert.strictEqual(llamadas.length, 2, 'Debe haber intentado ambos endpoints antes de fallar');
    assert.strictEqual(llamadas[0], '/auth/cliente/login');
    assert.strictEqual(llamadas[1], '/auth/login');
});

test('S5-03: Caso 4 - Error de red o servidor en cliente no intenta endpoint de personal', async () => {
    const llamadas = [];
    const mockFetch = async (endpoint) => {
        llamadas.push(endpoint);
        const err = new Error('Error 500 en el servidor');
        err.status = 500;
        throw err;
    };

    await assert.rejects(
        async () => {
            await autenticarEnCascada('cliente@correo.com', 'pass', mockFetch);
        },
        (err) => {
            assert.strictEqual(err.status, 500);
            return true;
        }
    );

    assert.strictEqual(llamadas.length, 1, 'No debe llamar al personal si no fue un 401');
});

test('S5-03: Caso 5 - Determinar destino correcto por rol', () => {
    assert.strictEqual(determinarDestinoLogin('ADMINISTRADOR'), 'dashboard.html');
    assert.strictEqual(determinarDestinoLogin('AGENTE'), 'dashboard.html');
    assert.strictEqual(determinarDestinoLogin('SUPERVISOR'), 'dashboard.html');
    assert.strictEqual(determinarDestinoLogin('AUDITOR'), 'dashboard.html');
    assert.strictEqual(determinarDestinoLogin('CLIENTE'), 'index.html');
    assert.strictEqual(determinarDestinoLogin('CLIENTE', '?redirect=reservar.html'), 'reservar.html');
    assert.strictEqual(determinarDestinoLogin('CLIENTE', '?redirect=https://malicioso.com'), 'index.html');
});

test('S5-03: Caso 6 - Mensaje de error genérico para 401 y otros códigos', () => {
    assert.strictEqual(mensajeDeError({ status: 401 }, 'Default'), 'Correo o contraseña incorrectos');
    assert.strictEqual(mensajeDeError({ status: 400, message: 'Falta campo' }, 'Default'), 'Falta campo');
    assert.strictEqual(mensajeDeError({ status: 409 }, 'Default'), 'El correo electrónico o número de licencia ya están registrados.');
    assert.strictEqual(mensajeDeError({ status: 500 }, 'Default'), 'Default');
});
