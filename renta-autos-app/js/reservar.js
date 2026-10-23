// reservar.js - Flujo de reservación en cuatro pasos (S4-09)
// Selección -> Datos -> Confirmación -> Resultado.
// Crea la reservación con POST /v1/cliente/reservaciones (S4-06).

document.addEventListener('DOMContentLoaded', () => {
    const HORA = 'T10:00:00';
    const SUCURSAL = 'Sucursal Saltillo';
    const NOMBRES_PASOS = ['Selección', 'Datos', 'Confirmación', 'Resultado'];
    const NOMBRE_ESTADO = {
        PENDIENTE: 'Pendiente de confirmación',
        CONFIRMADA: 'Confirmada',
        CANCELADA: 'Cancelada'
    };

    const e = Portal.escapar;
    const contenedor = document.getElementById('reservaContenedor');
    const barraPasos = document.getElementById('pasos');
    const volver = document.getElementById('volverCatalogo');
    const params = new URLSearchParams(window.location.search);

    const datos = {
        vehiculo: null,
        inicio: Portal.esFechaISO(params.get('inicio')) ? params.get('inicio') : '',
        fin: Portal.esFechaISO(params.get('fin')) ? params.get('fin') : '',
        observaciones: '',
        acepta: false
    };

    if (datos.inicio && datos.fin) {
        volver.href = `catalogo.html?${new URLSearchParams({ inicio: datos.inicio, fin: datos.fin }).toString()}`;
    }

    // ---------- Utilidades ----------

    function leerSesion() {
        try {
            return JSON.parse(localStorage.getItem('usuarioSesion'));
        } catch (_) {
            return null;
        }
    }

    function irALogin(destino) {
        window.location.href = `login.html?redirect=${encodeURIComponent(destino)}`;
    }

    function urlReservar() {
        const p = new URLSearchParams();
        if (datos.inicio) p.set('inicio', datos.inicio);
        if (datos.fin) p.set('fin', datos.fin);
        p.set('vehiculoId', datos.vehiculo.id);
        return `reservar.html?${p.toString()}`;
    }

    function fechaLarga(iso) {
        return new Date(`${iso}T00:00:00`).toLocaleDateString('es-MX', {
            day: 'numeric', month: 'long', year: 'numeric'
        });
    }

    function fechaHora(texto) {
        const d = new Date(texto);
        if (Number.isNaN(d.getTime())) return '—';
        return d.toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' });
    }

    function calcularResumen() {
        const dias = Portal.diasEntre(datos.inicio, datos.fin);
        const tarifa = Number(datos.vehiculo.tarifaDia);
        return { dias, tarifa, total: dias * tarifa };
    }

    // Devuelve un mensaje de error o cadena vacía si los datos son válidos
    function validarDatos() {
        const problema = Portal.validarFechas(datos.inicio, datos.fin);
        if (problema) return problema;
        if (new Date(`${datos.inicio}${HORA}`) <= new Date()) {
            return 'La recogida es a las 10:00 y para hoy ya pasó esa hora. Elige una fecha de inicio desde mañana.';
        }
        return '';
    }

    function pintarPasos(actual) {
        barraPasos.innerHTML = NOMBRES_PASOS.map((texto, i) => {
            const n = i + 1;
            const clase = n < actual ? 'hecho' : (n === actual ? 'activo' : '');
            const marca = n < actual ? '<i class="fa-solid fa-check" aria-hidden="true"></i>' : n;
            return `<li class="${clase}"${n === actual ? ' aria-current="step"' : ''}>
                <span class="paso-num">${marca}</span><span class="paso-texto">${texto}</span></li>`;
        }).join('');
    }

    function enfocarTitulo() {
        const titulo = contenedor.querySelector('h2');
        if (titulo) {
            titulo.tabIndex = -1;
            titulo.focus();
        }
    }

    function mostrarEstado(opciones) {
        barraPasos.innerHTML = '';
        contenedor.innerHTML = '<div></div>';
        Portal.mostrarEstado(contenedor.firstElementChild, opciones);
    }

    function noDisponible() {
        mostrarEstado({
            tipo: 'vacio',
            titulo: 'No encontramos ese vehículo',
            texto: 'Puede que ya no esté disponible o que el enlace no sea correcto. Revisa otras opciones en el catálogo.',
            accion: {
                texto: 'Ver catálogo',
                icono: 'fa-car',
                alHacerClic: () => { window.location.href = 'catalogo.html'; }
            }
        });
    }

    // ---------- Paso 1: Selección ----------

    function paso1() {
        pintarPasos(1);
        const v = datos.vehiculo;
        contenedor.innerHTML = `
            <section class="card paso">
                <h2 class="card-title">1. Vehículo seleccionado</h2>
                <div class="resumen-vehiculo">
                    <div class="resumen-foto">${Portal.visual(v)}</div>
                    <div>
                        <h3>${e(Portal.nombreVehiculo(v))}</h3>
                        <p class="vehiculo-categoria">${e(v.categoria)}</p>
                        <p class="precio">${e(Portal.moneda(v.tarifaDia))} / día</p>
                        <p class="nota"><i class="fa-solid fa-location-dot"></i> ${SUCURSAL}</p>
                    </div>
                </div>
                <div class="acciones-paso">
                    <a class="pt-btn pt-btn-outline" href="${e(volver.getAttribute('href'))}">Cambiar vehículo</a>
                    <button type="button" class="pt-btn pt-btn-primary" id="siguiente">Continuar</button>
                </div>
            </section>`;
        document.getElementById('siguiente').addEventListener('click', paso2);
    }

    // ---------- Paso 2: Datos ----------

    function paso2() {
        pintarPasos(2);
        contenedor.innerHTML = `
            <form class="card paso" id="formDatos" novalidate>
                <h2 class="card-title">2. Datos de la reservación</h2>
                <p class="nota"><i class="fa-solid fa-location-dot"></i> ${SUCURSAL} · Recogida y devolución a las 10:00</p>
                <div class="campos-fechas">
                    <div class="pt-field">
                        <label for="fechaInicio">Fecha de inicio</label>
                        <input type="date" id="fechaInicio" value="${e(datos.inicio)}">
                    </div>
                    <div class="pt-field">
                        <label for="fechaFin">Fecha de fin</label>
                        <input type="date" id="fechaFin" value="${e(datos.fin)}">
                    </div>
                </div>
                <div class="pt-field">
                    <label for="observaciones">Observaciones (opcional)</label>
                    <textarea id="observaciones" rows="3" maxlength="2000">${e(datos.observaciones)}</textarea>
                </div>
                <p class="estimado" id="estimado"></p>
                <p class="pt-error" id="errorDatos" role="alert"></p>
                <div class="acciones-paso">
                    <button type="button" class="pt-btn pt-btn-outline" id="atras">Atrás</button>
                    <button type="submit" class="pt-btn pt-btn-primary">Continuar</button>
                </div>
            </form>`;

        const inicio = document.getElementById('fechaInicio');
        const fin = document.getElementById('fechaFin');
        const notas = document.getElementById('observaciones');
        const estimado = document.getElementById('estimado');
        const error = document.getElementById('errorDatos');

        inicio.min = Portal.hoyISO();
        fin.min = inicio.value || Portal.hoyISO();

        function leer() {
            datos.inicio = inicio.value;
            datos.fin = fin.value;
            datos.observaciones = notas.value;
        }

        function actualizar() {
            leer();
            if (inicio.value) fin.min = inicio.value;
            if (Portal.validarFechas(datos.inicio, datos.fin)) {
                estimado.innerHTML = '';
                return;
            }
            const { dias, total } = calcularResumen();
            estimado.innerHTML = `<span>${dias} ${dias === 1 ? 'día' : 'días'} · Total estimado</span>
                <strong>${e(Portal.moneda(total))}</strong>`;
        }

        inicio.addEventListener('change', actualizar);
        fin.addEventListener('change', actualizar);
        notas.addEventListener('input', leer);
        actualizar();

        document.getElementById('atras').addEventListener('click', () => { leer(); paso1(); });
        document.getElementById('formDatos').addEventListener('submit', (ev) => {
            ev.preventDefault();
            leer();
            const problema = validarDatos();
            error.textContent = problema;
            if (!problema) paso3();
        });
        enfocarTitulo();
    }

    // ---------- Paso 3: Confirmación ----------

    function paso3() {
        pintarPasos(3);
        const v = datos.vehiculo;
        const { dias, tarifa, total } = calcularResumen();
        const notas = datos.observaciones.trim();
        contenedor.innerHTML = `
            <section class="card paso">
                <h2 class="card-title">3. Confirma tu reservación</h2>
                <dl class="resumen-lista">
                    <div><dt>Vehículo</dt><dd>${e(Portal.nombreVehiculo(v))} · ${e(v.categoria)}</dd></div>
                    <div><dt>Sucursal</dt><dd>${SUCURSAL}</dd></div>
                    <div><dt>Recogida</dt><dd>${e(fechaLarga(datos.inicio))}, 10:00</dd></div>
                    <div><dt>Devolución</dt><dd>${e(fechaLarga(datos.fin))}, 10:00</dd></div>
                    <div><dt>Duración</dt><dd>${dias} ${dias === 1 ? 'día' : 'días'}</dd></div>
                    <div><dt>Tarifa por día</dt><dd>${e(Portal.moneda(tarifa))}</dd></div>
                    ${notas ? `<div><dt>Observaciones</dt><dd>${e(notas)}</dd></div>` : ''}
                    <div class="total"><dt>Total estimado</dt><dd>${e(Portal.moneda(total))}</dd></div>
                </dl>
                <p class="nota">No se cobra nada en línea. El cobro se registra al devolver el vehículo y el costo final puede variar.</p>
                <label class="terminos">
                    <input type="checkbox" id="acepto"${datos.acepta ? ' checked' : ''}>
                    <span>Acepto los términos de la reservación: el total mostrado es un estimado y el cobro real se registra al devolver el vehículo.</span>
                </label>
                <p class="pt-error" id="errorConfirmar" role="alert"></p>
                <div class="acciones-paso">
                    <button type="button" class="pt-btn pt-btn-outline" id="atras">Atrás</button>
                    <button type="button" class="pt-btn pt-btn-primary" id="confirmar">
                        <i class="fa-solid fa-calendar-check"></i>Confirmar reservación
                    </button>
                </div>
            </section>`;

        const acepto = document.getElementById('acepto');
        const error = document.getElementById('errorConfirmar');
        const confirmar = document.getElementById('confirmar');
        const atras = document.getElementById('atras');

        acepto.addEventListener('change', () => { datos.acepta = acepto.checked; });
        atras.addEventListener('click', paso2);
        confirmar.addEventListener('click', async () => {
            if (!acepto.checked) {
                error.textContent = 'Debes aceptar los términos para confirmar la reservación.';
                acepto.focus();
                return;
            }
            const problema = validarDatos();
            if (problema) {
                error.textContent = problema;
                return;
            }
            error.textContent = '';
            confirmar.disabled = true;
            atras.disabled = true;
            confirmar.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>Enviando…';
            await enviarReservacion();
        });
        enfocarTitulo();
    }

    // ---------- Envío a la API ----------

    async function enviarReservacion() {
        const cuerpo = {
            vehiculoId: Number(datos.vehiculo.id),
            fechaInicio: `${datos.inicio}${HORA}`,
            fechaFin: `${datos.fin}${HORA}`
        };
        const notas = datos.observaciones.trim();
        if (notas) cuerpo.observaciones = notas;

        try {
            const respuesta = await fetchAPI('/v1/cliente/reservaciones', {
                method: 'POST',
                body: JSON.stringify(cuerpo)
            });
            paso4Exito(respuesta?.data || {});
        } catch (err) {
            paso4Error(err);
        }
    }

    // ---------- Paso 4: Resultado ----------

    function paso4Exito(r) {
        pintarPasos(5);
        const v = datos.vehiculo;
        contenedor.innerHTML = `
            <section class="card paso resultado ok" role="status">
                <i class="fa-solid fa-circle-check" aria-hidden="true"></i>
                <h2>¡Reservación registrada!</h2>
                <p>Quedó pendiente de confirmación por parte del personal.</p>
                <dl class="resumen-lista">
                    <div><dt>Folio</dt><dd>#${e(r.id ?? '—')}</dd></div>
                    <div><dt>Estado</dt><dd>${e(NOMBRE_ESTADO[r.estado] || r.estado || '—')}</dd></div>
                    <div><dt>Vehículo</dt><dd>${e(Portal.nombreVehiculo(v))}</dd></div>
                    <div><dt>Sucursal</dt><dd>${SUCURSAL}</dd></div>
                    <div><dt>Recogida</dt><dd>${e(fechaHora(r.fechaInicio))}</dd></div>
                    <div><dt>Devolución</dt><dd>${e(fechaHora(r.fechaFin))}</dd></div>
                    <div><dt>Tarifa por día</dt><dd>${e(Portal.moneda(r.tarifaDia))}</dd></div>
                    <div class="total"><dt>Total estimado</dt><dd>${e(Portal.moneda(r.totalEstimado))}</dd></div>
                </dl>
                <div class="acciones-paso">
                    <a class="pt-btn pt-btn-primary" href="catalogo.html">Seguir explorando</a>
                </div>
            </section>`;
        enfocarTitulo();
    }

    function paso4Error(err) {
        if (err && err.status === 401) {
            localStorage.removeItem('usuarioSesion');
            irALogin(urlReservar());
            return;
        }

        let titulo;
        let texto;
        let textoAccion;
        let accion;

        if (!err || err.status === undefined) {
            titulo = 'No pudimos conectar con el servidor';
            texto = 'Revisa tu conexión a internet e intenta de nuevo.';
            textoAccion = 'Reintentar';
            accion = paso3;
        } else if (err.status === 409) {
            titulo = 'Ese vehículo no está disponible en esas fechas';
            texto = err.message || 'Elige otras fechas u otro vehículo.';
            textoAccion = 'Cambiar fechas';
            accion = paso2;
        } else if (err.status === 400) {
            titulo = 'Revisa los datos de la reservación';
            texto = err.message || 'Alguno de los datos no es válido.';
            textoAccion = 'Corregir datos';
            accion = paso2;
        } else if (err.status === 403) {
            titulo = 'Esta cuenta no puede reservar';
            texto = 'Inicia sesión con una cuenta de cliente para reservar desde el portal.';
            textoAccion = 'Iniciar sesión';
            accion = () => irALogin(urlReservar());
        } else {
            titulo = 'No pudimos registrar tu reservación';
            texto = 'Ocurrió un problema inesperado. Intenta de nuevo en unos minutos.';
            textoAccion = 'Volver a confirmar';
            accion = paso3;
        }

        pintarPasos(4);
        contenedor.innerHTML = `
            <section class="card paso resultado error" role="alert">
                <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
                <h2>${e(titulo)}</h2>
                <p>${e(texto)}</p>
                <div class="acciones-paso">
                    <a class="pt-btn pt-btn-outline" href="catalogo.html">Ver catálogo</a>
                    <button type="button" class="pt-btn pt-btn-primary" id="accionError">${e(textoAccion)}</button>
                </div>
            </section>`;
        document.getElementById('accionError').addEventListener('click', accion);
        enfocarTitulo();
    }

    // ---------- Arranque ----------

    async function iniciar() {
        const sesion = leerSesion();
        if (!sesion || !sesion.token) {
            irALogin(`reservar.html${window.location.search}`);
            return;
        }
        if (sesion.rol && sesion.rol !== 'CLIENTE') {
            mostrarEstado({
                tipo: 'vacio',
                titulo: 'Las reservaciones del personal van en el panel',
                texto: 'Las reservaciones del personal se registran desde el panel de Reservaciones.',
                accion: {
                    texto: 'Ir al panel',
                    icono: 'fa-table-columns',
                    alHacerClic: () => { window.location.href = 'dashboard.html'; }
                }
            });
            return;
        }

        const id = params.get('vehiculoId');
        if (!id || !/^\d+$/.test(id)) {
            noDisponible();
            return;
        }

        contenedor.setAttribute('aria-busy', 'true');
        contenedor.innerHTML = '<div class="card paso skeleton"><div class="sk-line w60" style="height:2rem"></div><div class="sk-line" style="height:10rem;margin-top:1rem"></div><span class="sr-only">Cargando…</span></div>';
        try {
            const vehiculos = await Portal.obtenerCatalogo();
            const vehiculo = vehiculos.find(v => String(v.id) === id);
            if (!vehiculo) {
                noDisponible();
                return;
            }
            datos.vehiculo = vehiculo;
            contenedor.removeAttribute('aria-busy');
            paso1();
        } catch (err) {
            const { titulo, texto } = Portal.mensajeError(err);
            mostrarEstado({
                tipo: 'error',
                titulo,
                texto,
                accion: { texto: 'Reintentar', alHacerClic: iniciar }
            });
        }
    }

    iniciar();
});
