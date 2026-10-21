// vehiculo.js - Detalle de un vehículo del catálogo e inicio de la reservación

document.addEventListener('DOMContentLoaded', () => {
    const contenedor = document.getElementById('detalleContenedor');
    const volver = document.getElementById('volverCatalogo');
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');
    const inicioURL = Portal.esFechaISO(params.get('inicio')) ? params.get('inicio') : '';
    const finURL = Portal.esFechaISO(params.get('fin')) ? params.get('fin') : '';

    if (inicioURL && finURL) {
        volver.href = `catalogo.html?${new URLSearchParams({ inicio: inicioURL, fin: finURL }).toString()}`;
    }

    function mostrarCarga() {
        contenedor.innerHTML = `
            <div class="detalle skeleton" aria-busy="true">
                <div class="card"><div class="vehiculo-visual" style="aspect-ratio:16/9"></div></div>
                <div>
                    <div class="sk-line w60" style="height:2rem"></div>
                    <div class="sk-line w40"></div>
                    <div class="sk-line" style="height:15rem; margin-top:1.5rem; border-radius:16px"></div>
                </div>
                <span class="sr-only">Cargando vehículo…</span>
            </div>`;
    }

    function mostrarEstado(opciones) {
        contenedor.innerHTML = '<div></div>';
        Portal.mostrarEstado(contenedor.firstElementChild, opciones);
    }

    function noDisponible() {
        mostrarEstado({
            tipo: 'vacio',
            titulo: 'Este vehículo ya no está disponible',
            texto: 'Puede que se haya rentado o que el enlace no sea correcto. Revisa otras opciones en el catálogo.',
            accion: {
                texto: 'Ver catálogo',
                icono: 'fa-car',
                alHacerClic: () => { window.location.href = 'catalogo.html'; }
            }
        });
    }

    function descripcion(v) {
        const color = v.color ? ` en color ${String(v.color).toLowerCase()}` : '';
        return `El ${Portal.nombreVehiculo(v)} pertenece a nuestra categoría ${v.categoria}${color}. `
            + `Está disponible para rentar en Sucursal Saltillo con una tarifa de ${Portal.moneda(v.tarifaDia)} por día.`;
    }

    function pintar(v) {
        const e = Portal.escapar;
        const nombre = Portal.nombreVehiculo(v);
        document.title = `${nombre} - RentaDeAutos`;

        contenedor.innerHTML = `
            <div class="detalle">
                <section class="card detalle-foto">${Portal.visual(v)}</section>

                <section>
                    <div class="detalle-titulo">
                        <h1>${e(nombre)}</h1>
                        <span class="badge badge-success">Disponible</span>
                    </div>
                    <p class="detalle-categoria">${e(v.categoria)}</p>

                    <form class="reserva-box" id="formReserva" novalidate>
                        <p class="precio">${e(Portal.moneda(v.tarifaDia))} / día</p>
                        <div class="fechas">
                            <div class="pt-field">
                                <label for="fechaInicio">Fecha de inicio</label>
                                <input type="date" id="fechaInicio" value="${e(inicioURL)}">
                            </div>
                            <div class="pt-field">
                                <label for="fechaFin">Fecha de fin</label>
                                <input type="date" id="fechaFin" value="${e(finURL)}">
                            </div>
                        </div>
                        <p class="estimado" id="estimado"></p>
                        <p class="pt-error" id="errorReserva" role="alert"></p>
                        <button type="submit" class="pt-btn pt-btn-primary pt-btn-block">
                            <i class="fa-solid fa-calendar-check"></i>Reservar ahora
                        </button>
                        <p class="nota">El precio corresponde a la tarifa vigente hoy. La disponibilidad para tus fechas y el costo final se confirman al reservar.</p>
                    </form>
                </section>
            </div>

            <section class="card descripcion">
                <h2>Descripción</h2>
                <p>${e(descripcion(v))}</p>
            </section>`;

        const inputInicio = document.getElementById('fechaInicio');
        const inputFin = document.getElementById('fechaFin');
        const estimado = document.getElementById('estimado');
        const error = document.getElementById('errorReserva');

        inputInicio.min = Portal.hoyISO();
        inputFin.min = inicioURL || Portal.hoyISO();

        function actualizarEstimado() {
            if (inputInicio.value) inputFin.min = inputInicio.value;
            if (Portal.validarFechas(inputInicio.value, inputFin.value)) {
                estimado.innerHTML = '';
                return;
            }
            const dias = Portal.diasEntre(inputInicio.value, inputFin.value);
            estimado.innerHTML = `<span>${dias} ${dias === 1 ? 'día' : 'días'} · Total estimado</span>
                <strong>${e(Portal.moneda(dias * Number(v.tarifaDia)))}</strong>`;
        }
        inputInicio.addEventListener('change', actualizarEstimado);
        inputFin.addEventListener('change', actualizarEstimado);
        actualizarEstimado();

        document.getElementById('formReserva').addEventListener('submit', (ev) => {
            ev.preventDefault();
            const problema = Portal.validarFechas(inputInicio.value, inputFin.value);
            error.textContent = problema;
            if (problema) return;
            const resultado = Portal.iniciarReservacion(v, inputInicio.value, inputFin.value);
            if (!resultado.ok) error.textContent = resultado.mensaje;
        });
    }

    async function cargar() {
        if (!id || !/^\d+$/.test(id)) {
            noDisponible();
            return;
        }
        mostrarCarga();
        try {
            // El catálogo público solo publica la lista; el detalle se toma de ella
            const vehiculos = await Portal.obtenerCatalogo();
            const vehiculo = vehiculos.find(v => String(v.id) === id);
            if (vehiculo) {
                pintar(vehiculo);
            } else {
                noDisponible();
            }
        } catch (err) {
            const { titulo, texto } = Portal.mensajeError(err);
            mostrarEstado({
                tipo: 'error',
                titulo,
                texto,
                accion: { texto: 'Reintentar', alHacerClic: cargar }
            });
        }
    }

    cargar();
});
