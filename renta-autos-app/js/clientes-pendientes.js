// clientes-pendientes.js — S5-09: revisión de cuentas registradas desde el portal.
// Lista los clientes PENDIENTE y permite aprobarlos o rechazarlos.
// La seguridad real la impone el backend (ADMINISTRADOR, AGENTE y SUPERVISOR).

const ROLES_REVISION = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR'];

const ESTADOS_APROBACION = {
    PENDIENTE: { texto: 'Pendiente', clase: 'pendiente' },
    APROBADO: { texto: 'Aprobado', clase: 'aprobado' },
    RECHAZADO: { texto: 'Rechazado', clase: 'rechazado' }
};

/** Etiqueta y clase CSS del estado de aprobación (null = alta en mostrador). */
function describirEstadoAprobacion(estado) {
    return ESTADOS_APROBACION[estado] || { texto: 'Sin revisión', clase: 'inactivo' };
}

/** Ruta del endpoint para aprobar o rechazar (el id se valida como entero positivo). */
function rutaRevision(id, accion) {
    if (!['aprobar', 'rechazar'].includes(accion)) throw new Error('Acción no válida');
    const numero = Number(id);
    if (!Number.isInteger(numero) || numero <= 0) throw new Error('Cliente no válido');
    return `/v1/clientes/${numero}/${accion}`;
}

/** Aplica la respuesta del backend a la lista local, conservando la fila revisada. */
function aplicarRevision(lista, clienteActualizado) {
    return lista.map(cliente => cliente.id === clienteActualizado.id
        ? { ...cliente, ...clienteActualizado }
        : cliente);
}

/** Licencia vencida o que vence en los próximos 30 días (ayuda a revisar). */
function alertaLicencia(vencimiento, hoy = new Date()) {
    if (!vencimiento) return null;
    const fecha = new Date(`${vencimiento}T00:00:00`);
    const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    const dias = Math.round((fecha - inicioHoy) / 86400000);
    if (dias <= 0) return 'Licencia vencida';
    if (dias <= 30) return `Vence en ${dias} ${dias === 1 ? 'día' : 'días'}`;
    return null;
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', async () => {
        const $ = id => document.getElementById(id);
        const RUTA = '/v1/clientes/pendientes';
        const tabla = $('tabla-pendientes');
        const mensaje = $('mensaje');
        const dialogoAprobar = $('dialogo-aprobar');
        const dialogoRechazar = $('dialogo-rechazar');
        const motivo = $('motivo-rechazo');

        let sesion;
        try { sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null'); } catch { sesion = null; }
        if (!sesion?.token) { window.location.replace('login.html'); return; }
        if (sesion.rol === 'CLIENTE') { window.location.replace('index.html'); return; }

        $('nombre-usuario').textContent = sesion.nombre || 'Usuario';
        $('cerrar-sesion').addEventListener('click', () => {
            localStorage.removeItem('usuarioSesion');
            window.location.replace('login.html');
        });

        let clientes = [];
        let seleccionado = null;
        const revisados = { APROBADO: 0, RECHAZADO: 0 };

        function crear(etiqueta, clase, texto) {
            const elemento = document.createElement(etiqueta);
            if (clase) elemento.className = clase;
            if (texto !== undefined) elemento.textContent = texto;
            return elemento;
        }
        function icono(clases) {
            const elemento = crear('i'); elemento.className = clases; elemento.setAttribute('aria-hidden', 'true'); return elemento;
        }
        const nombreCompleto = cliente => `${cliente.nombre} ${cliente.apellidos}`.trim();
        const iniciales = cliente => `${cliente.nombre?.[0] || ''}${cliente.apellidos?.[0] || ''}`.toUpperCase();
        const fecha = valor => valor
            ? new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium' }).format(new Date(`${valor}T00:00:00`))
            : '-';
        const fechaHora = valor => valor
            ? new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor))
            : '-';

        function mostrarMensaje(texto, esError = false) {
            mensaje.textContent = texto;
            mensaje.className = `veh-mensaje ${esError ? 'error' : 'exito'}`;
            mensaje.hidden = false;
            clearTimeout(mostrarMensaje.temporizador);
            if (!esError) mostrarMensaje.temporizador = setTimeout(() => { mensaje.hidden = true; }, 5000);
        }

        function filaAviso(texto) {
            const fila = crear('tr');
            const celda = crear('td', 'veh-celda-aviso', texto);
            celda.colSpan = 7;
            fila.append(celda);
            tabla.replaceChildren(fila);
        }

        async function solicitar(endpoint, opciones = {}) {
            try {
                return await fetchAPI(endpoint, opciones);
            } catch (error) {
                if (error.status === 401) {
                    localStorage.removeItem('usuarioSesion');
                    window.location.replace('login.html');
                }
                if (error.status === 403) error.message = 'No tienes permiso para revisar cuentas de clientes.';
                throw error;
            }
        }

        function actualizarResumen() {
            $('total-pendientes').textContent = clientes.filter(c => c.estadoAprobacion === 'PENDIENTE').length;
            $('total-aprobados').textContent = revisados.APROBADO;
            $('total-rechazados').textContent = revisados.RECHAZADO;
        }

        function etiquetaEstado(cliente) {
            const estado = describirEstadoAprobacion(cliente.estadoAprobacion);
            return crear('span', `veh-estado cliente-estado ${estado.clase}`, estado.texto);
        }

        function botonRevision(clase, iconoClase, texto, accion) {
            const boton = crear('button', `pendientes-accion ${clase}`);
            boton.type = 'button';
            boton.append(icono(iconoClase), crear('span', null, texto));
            boton.addEventListener('click', accion);
            return boton;
        }

        function dibujar() {
            actualizarResumen();
            if (!clientes.length) {
                filaAviso('No hay cuentas pendientes de aprobación.');
                return;
            }
            const filas = clientes.map(cliente => {
                const fila = crear('tr');
                fila.dataset.id = cliente.id;
                if (cliente.estadoAprobacion !== 'PENDIENTE') fila.className = 'pendientes-revisado';
                const celda = contenido => {
                    const td = crear('td');
                    if (contenido instanceof Node) td.append(contenido); else td.textContent = contenido ?? '-';
                    fila.append(td);
                    return td;
                };

                const identidad = crear('div', 'cliente-nombre');
                identidad.append(crear('span', 'cliente-avatar', iniciales(cliente)));
                const texto = crear('div');
                texto.append(crear('strong', null, nombreCompleto(cliente)), crear('small', null, `ID #${cliente.id}`));
                identidad.append(texto);
                celda(identidad);

                const contacto = crear('div', 'pendientes-dato');
                contacto.append(crear('span', null, cliente.correo || 'Sin correo'), crear('small', null, cliente.telefono || '-'));
                celda(contacto);

                const licencia = crear('div', 'pendientes-dato');
                licencia.append(crear('span', null, cliente.numeroLicencia || '-'),
                    crear('small', null, `Vence: ${fecha(cliente.licenciaVencimiento)}`));
                const alerta = alertaLicencia(cliente.licenciaVencimiento);
                if (alerta) licencia.append(crear('small', 'pendientes-alerta', alerta));
                celda(licencia);

                celda(cliente.direccion || 'Sin dirección');
                celda(fechaHora(cliente.fechaRegistro));
                celda(etiquetaEstado(cliente));

                const acciones = crear('div', 'pendientes-acciones');
                if (cliente.estadoAprobacion === 'PENDIENTE') {
                    acciones.append(
                        botonRevision('aprobar', 'fa-solid fa-user-check', 'Aprobar', () => abrirAprobar(cliente)),
                        botonRevision('rechazar', 'fa-solid fa-user-xmark', 'Rechazar', () => abrirRechazar(cliente)));
                } else {
                    acciones.append(crear('small', 'pendientes-revisado-texto',
                        `Revisado ${fechaHora(cliente.fechaRevision)}`));
                }
                celda(acciones);
                return fila;
            });
            tabla.replaceChildren(...filas);
        }

        async function cargar() {
            filaAviso('Cargando solicitudes...');
            try {
                clientes = (await solicitar(RUTA))?.data || [];
                dibujar();
            } catch (error) {
                clientes = [];
                actualizarResumen();
                filaAviso('No se pudieron cargar las solicitudes.');
                mostrarMensaje(error.message, true);
            }
        }

        function abrirAprobar(cliente) {
            seleccionado = cliente;
            $('texto-aprobar').textContent =
                `La cuenta de ${nombreCompleto(cliente)} (${cliente.correo}) quedará activa y podrá iniciar sesión en el portal.`;
            dialogoAprobar.showModal();
        }

        function abrirRechazar(cliente) {
            seleccionado = cliente;
            motivo.value = '';
            $('texto-rechazar').textContent =
                `La cuenta de ${nombreCompleto(cliente)} (${cliente.correo}) permanecerá inhabilitada y no podrá iniciar sesión.`;
            dialogoRechazar.showModal();
            motivo.focus();
        }

        async function revisar(accion, cuerpo, boton, dialogo) {
            if (!seleccionado) return;
            const cliente = seleccionado;
            boton.disabled = true;
            try {
                const opciones = { method: 'PATCH' };
                if (cuerpo) opciones.body = JSON.stringify(cuerpo);
                const respuesta = await solicitar(rutaRevision(cliente.id, accion), opciones);
                const actualizado = respuesta?.data;
                if (actualizado) {
                    clientes = aplicarRevision(clientes, actualizado);
                    revisados[actualizado.estadoAprobacion] = (revisados[actualizado.estadoAprobacion] || 0) + 1;
                }
                dialogo.close();
                dibujar();
                mostrarMensaje(accion === 'aprobar'
                    ? `Cuenta de ${nombreCompleto(cliente)} aprobada. Ya puede iniciar sesión.`
                    : `Cuenta de ${nombreCompleto(cliente)} rechazada.`);
            } catch (error) {
                dialogo.close();
                // 409: otra persona ya la revisó; 404: ya no existe. Se recarga la lista real.
                if (error.status === 409 || error.status === 404) await cargar();
                mostrarMensaje(error.message || 'No se pudo completar la acción.', true);
            } finally {
                boton.disabled = false;
                seleccionado = null;
            }
        }

        $('formulario-aprobar').addEventListener('submit', evento => {
            evento.preventDefault();
            revisar('aprobar', null, $('confirmar-aprobar'), dialogoAprobar);
        });
        $('formulario-rechazar').addEventListener('submit', evento => {
            evento.preventDefault();
            const texto = motivo.value.trim();
            revisar('rechazar', { motivo: texto || null }, $('confirmar-rechazar'), dialogoRechazar);
        });
        document.querySelectorAll('[data-cerrar]').forEach(boton => boton.addEventListener('click', () => {
            boton.closest('dialog').close();
            seleccionado = null;
        }));
        $('actualizar-lista').addEventListener('click', cargar);

        if (!ROLES_REVISION.includes(sesion.rol)) {
            $('actualizar-lista').hidden = true;
            actualizarResumen();
            filaAviso('Tu rol no tiene permiso para revisar cuentas de clientes.');
            return;
        }

        await cargar();
    });
}

// Exportación para pruebas unitarias en Node.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { describirEstadoAprobacion, rutaRevision, aplicarRevision, alertaLicencia, ROLES_REVISION };
}
