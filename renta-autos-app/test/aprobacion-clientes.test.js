// aprobacion-clientes.test.js — S5-09: aprobación y rechazo de clientes (frontend).
// Ejecutar desde renta-autos-app:  node --test test/
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const { autenticarEnCascada, mensajeDeError, botonRegistro, MENSAJE_REGISTRO_PENDIENTE } = require('../js/auth.js');
const {
    describirEstadoAprobacion, rutaRevision, aplicarRevision, alertaLicencia, ROLES_REVISION
} = require('../js/clientes-pendientes.js');

const raiz = path.join(__dirname, '..');
const leer = archivo => fs.readFileSync(path.join(raiz, archivo), 'utf8');

test('S5-09: el registro del portal muestra "Tu cuenta quedó pendiente de aprobación"', () => {
    assert.strictEqual(MENSAJE_REGISTRO_PENDIENTE, 'Tu cuenta quedó pendiente de aprobación');
    const html = leer('registro.html');
    assert.ok(html.includes('id="registerSuccess"'), 'registro.html debe tener la confirmación');
    assert.ok(html.includes(MENSAJE_REGISTRO_PENDIENTE), 'registro.html debe mostrar el mensaje de pendiente');
});

test('S5-09: el formulario de registro rediseñado encuentra su botón (.registro-boton)', () => {
    const boton = { clase: 'registro-boton' };
    const formulario = {
        querySelector(selector) {
            assert.ok(selector.includes('.registro-boton'), 'Debe buscar el botón del rediseño S5-02');
            return boton;
        }
    };
    assert.strictEqual(botonRegistro(formulario), boton);
    assert.strictEqual(botonRegistro(null), null);
    assert.ok(leer('registro.html').includes('class="registro-boton"'));
});

test('S5-09: cuenta pendiente (403) no reintenta como personal y muestra el motivo', async () => {
    const llamadas = [];
    const mockFetch = async endpoint => {
        llamadas.push(endpoint);
        const error = new Error('Tu cuenta está pendiente de aprobación. Podrás iniciar sesión cuando el personal la apruebe.');
        error.status = 403;
        throw error;
    };
    await assert.rejects(
        () => autenticarEnCascada('pendiente@correo.com', 'Clave123!', mockFetch),
        error => {
            assert.strictEqual(error.status, 403);
            assert.match(mensajeDeError(error, 'x'), /pendiente de aprobación/);
            return true;
        });
    assert.deepStrictEqual(llamadas, ['/auth/cliente/login'], 'No debe intentar /auth/login');
});

test('S5-09: cuenta rechazada (403) muestra el mensaje del backend', () => {
    const error = new Error('Tu solicitud de cuenta fue rechazada. Comunícate con la sucursal para más información.');
    error.status = 403;
    assert.match(mensajeDeError(error, 'x'), /rechazada/);
});

test('S5-09: 401 sigue mostrando el mensaje genérico (no revela el estado de la cuenta)', () => {
    const error = new Error('lo que sea'); error.status = 401;
    assert.strictEqual(mensajeDeError(error, 'x'), 'Correo o contraseña incorrectos');
});

test('S5-09: etiquetas que distinguen pendiente, aprobado y rechazado', () => {
    assert.deepStrictEqual(describirEstadoAprobacion('PENDIENTE'), { texto: 'Pendiente', clase: 'pendiente' });
    assert.deepStrictEqual(describirEstadoAprobacion('APROBADO'), { texto: 'Aprobado', clase: 'aprobado' });
    assert.deepStrictEqual(describirEstadoAprobacion('RECHAZADO'), { texto: 'Rechazado', clase: 'rechazado' });
    assert.strictEqual(describirEstadoAprobacion(null).texto, 'Sin revisión');
});

test('S5-09: rutas de aprobar y rechazar, con id validado', () => {
    assert.strictEqual(rutaRevision(7, 'aprobar'), '/v1/clientes/7/aprobar');
    assert.strictEqual(rutaRevision('12', 'rechazar'), '/v1/clientes/12/rechazar');
    assert.throws(() => rutaRevision(7, 'borrar'));
    assert.throws(() => rutaRevision('1/../2', 'aprobar'));
    assert.throws(() => rutaRevision(0, 'aprobar'));
});

test('S5-09: tras una acción la lista se actualiza con el estado nuevo y conserva la fila', () => {
    const lista = [
        { id: 1, nombre: 'Ana', estadoAprobacion: 'PENDIENTE', activo: false },
        { id: 2, nombre: 'Luis', estadoAprobacion: 'PENDIENTE', activo: false }
    ];
    const nueva = aplicarRevision(lista, { id: 2, estadoAprobacion: 'APROBADO', activo: true });
    assert.strictEqual(nueva.length, 2);
    assert.strictEqual(nueva[0].estadoAprobacion, 'PENDIENTE');
    assert.strictEqual(nueva[1].estadoAprobacion, 'APROBADO');
    assert.strictEqual(nueva[1].activo, true);
    assert.strictEqual(nueva[1].nombre, 'Luis');
    assert.strictEqual(lista[1].estadoAprobacion, 'PENDIENTE', 'No muta la lista original');
});

test('S5-09: alerta de licencia vencida o próxima a vencer', () => {
    const hoy = new Date(2026, 10, 2);
    assert.strictEqual(alertaLicencia('2026-11-01', hoy), 'Licencia vencida');
    assert.strictEqual(alertaLicencia('2026-11-03', hoy), 'Vence en 1 día');
    assert.strictEqual(alertaLicencia('2026-11-20', hoy), 'Vence en 18 días');
    assert.strictEqual(alertaLicencia('2027-11-02', hoy), null);
});

test('S5-09: solo Admin, Agente y Supervisor revisan; el menú lo oculta al Auditor', () => {
    assert.deepStrictEqual(ROLES_REVISION, ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR']);
    const panel = leer('js/panel.js');
    assert.match(panel, /AUDITOR:\s*\[[^\]]*'clientes-pendientes\.html'/);
});

test('S5-09: la vista tiene botones de Aprobar y Rechazar y está enlazada en el menú', () => {
    const vista = leer('clientes-pendientes.html') + leer('js/clientes-pendientes.js');
    assert.ok(vista.includes("'Aprobar'") && vista.includes("'Rechazar'"));
    for (const pagina of ['dashboard.html', 'clientes.html', 'reservaciones.html']) {
        assert.ok(leer(pagina).includes('href="clientes-pendientes.html"'), `${pagina} debe enlazar la vista`);
    }
});
