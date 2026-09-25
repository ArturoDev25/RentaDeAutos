// vehiculos.js - Pantalla de vehículos del Panel de Administrador (S2-12)
// Consume la API de S2-06 / S2-07: /api/v1/vehiculos
document.addEventListener('DOMContentLoaded', async () => {
    const $ = id => document.getElementById(id);

    const POR_PAGINA = 5;
    const RUTA = '/v1/vehiculos';

    const ESTADOS = {
        DISPONIBLE: 'Disponible',
        RESERVADO: 'Reservado',
        RENTADO: 'Rentado',
        MANTENIMIENTO: 'En mantenimiento',
        BAJA: 'Baja'
    };

    // Fotos disponibles en assets; si no hay foto para un vehículo se muestra un ícono.
    const FOTOS = [
        { marca: 'audi', modelo: 'q7', archivo: 'Audiq7.jpg' },
        { marca: 'bmw', archivo: 'bmw.jpg' },
        { marca: 'tesla', archivo: 'tesla.jpg' },
        { marca: 'ferrari', archivo: 'Ferrari.jpg' },
        { marca: 'nissan', modelo: 'tsuru', archivo: 'Tsuru.jpg' }
    ];

    const tabla = $('tabla-vehiculos');
    const mensaje = $('mensaje');
    const buscar = $('buscar');
    const filtroCategoria = $('filtro-categoria');
    const filtroEstado = $('filtro-estado');
    const menuEstado = $('menu-estado');
    const dialogoVehiculo = $('dialogo-vehiculo');
    const formulario = $('formulario-vehiculo');
    const errorFormulario = $('error-formulario');
    const botonGuardar = $('guardar-vehiculo');
    const dialogoDetalle = $('dialogo-detalle');

    let vehiculos = [];
    let categorias = [];
    let pagina = 1;
    let idEdicion = null;
    let vehiculoMenu = null;
    let consultaActual = 0;
    let temporizadorBusqueda = null;

    // ---------------------------------------------------------------
    // Sesión
    // ---------------------------------------------------------------
    let sesion;
    try {
        sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null');
    } catch {
        sesion = null;
    }

    if (!sesion?.token) {
        window.location.replace('login.html');
        return;
    }

    // Según SecurityConfig, solo ADMINISTRADOR y SUPERVISOR pueden modificar vehículos.
    const puedeModificar = ['ADMINISTRADOR', 'SUPERVISOR'].includes(sesion.rol);

    $('nombre-usuario').textContent = sesion.nombre || 'Usuario';
    $('rol-usuario').textContent = nombreRol(sesion.rol);
    $('registrar-vehiculo').hidden = !puedeModificar;

    $('cerrar-sesion').addEventListener('click', () => {
        localStorage.removeItem('usuarioSesion');
        window.location.href = 'login.html';
    });

    function nombreRol(rol) {
        if (!rol) return '—';
        return rol.charAt(0) + rol.slice(1).toLowerCase();
    }

    async function solicitar(endpoint, opciones = {}) {
        try {
            return await fetchAPI(endpoint, {
                ...opciones,
                headers: { ...opciones.headers, Authorization: `Bearer ${sesion.token}` }
            });
        } catch (error) {
            if (error.status === 401) {
                localStorage.removeItem('usuarioSesion');
                window.location.replace('login.html');
            }
            if (error.status === 403) {
                error.message = 'No tienes permiso para realizar esta operación.';
            }
            throw error;
        }
    }

    // ---------------------------------------------------------------
    // Utilidades de interfaz
    // ---------------------------------------------------------------
    function mostrarMensaje(texto, esError = false) {
        mensaje.textContent = texto;
        mensaje.className = `veh-mensaje ${esError ? 'error' : 'exito'}`;
        mensaje.hidden = false;
        clearTimeout(mostrarMensaje.temporizador);
        if (!esError) {
            mostrarMensaje.temporizador = setTimeout(() => { mensaje.hidden = true; }, 4000);
        }
    }

    function crear(etiqueta, clase, texto) {
        const elemento = document.createElement(etiqueta);
        if (clase) elemento.className = clase;
        if (texto !== undefined && texto !== null) elemento.textContent = texto;
        return elemento;
    }

    function icono(clases) {
        const i = document.createElement('i');
        i.className = clases;
        i.setAttribute('aria-hidden', 'true');
        return i;
    }

    function filaAviso(texto) {
        const fila = crear('tr');
        const celda = crear('td', 'veh-celda-aviso', texto);
        celda.colSpan = 9;
        fila.append(celda);
        tabla.replaceChildren(fila);
    }

    function miniatura(vehiculo) {
        const caja = crear('div', 'veh-miniatura');
        const marca = (vehiculo.marca || '').toLowerCase();
        const modelo = (vehiculo.modelo || '').toLowerCase();
        const foto = FOTOS.find(f => marca.includes(f.marca) && (!f.modelo || modelo.includes(f.modelo)));

        if (foto) {
            const img = document.createElement('img');
            img.src = `assets/Imagenes/Carros/${foto.archivo}`;
            img.alt = `${vehiculo.marca} ${vehiculo.modelo}`;
            img.loading = 'lazy';
            img.addEventListener('error', () => img.replaceWith(icono('fa-solid fa-car-side')));
            caja.append(img);
        } else {
            caja.append(icono('fa-solid fa-car-side'));
        }
        return caja;
    }

    function etiquetaEstado(estado) {
        return crear('span', `veh-estado ${estado.toLowerCase()}`, ESTADOS[estado] || estado);
    }

    function botonAccion(clase, iconoClase, titulo, alHacerClic) {
        const boton = crear('button', `veh-accion ${clase}`);
        boton.type = 'button';
        boton.title = titulo;
        boton.setAttribute('aria-label', titulo);
        boton.append(icono(iconoClase));
        boton.addEventListener('click', alHacerClic);
        return boton;
    }

    // ---------------------------------------------------------------
    // Carga de datos
    // ---------------------------------------------------------------
    async function cargarCategorias() {
        try {
            const respuesta = await solicitar('/categorias');
            categorias = respuesta?.data || [];
        } catch (error) {
            categorias = [];
            mostrarMensaje(`No se pudieron cargar las categorías: ${error.message}`, true);
        }

        filtroCategoria.replaceChildren(new Option('Todas', ''));
        categorias.forEach(c => filtroCategoria.append(new Option(c.nombre, c.id)));
    }

    async function cargarResumen() {
        try {
            const respuesta = await solicitar(RUTA);
            const todos = respuesta?.data || [];
            const contar = estado => todos.filter(v => v.estado === estado).length;

            $('total-vehiculos').textContent = todos.length;
            $('total-disponibles').textContent = contar('DISPONIBLE');
            $('total-rentados').textContent = contar('RENTADO');
            $('total-mantenimiento').textContent = contar('MANTENIMIENTO');
        } catch {
            ['total-vehiculos', 'total-disponibles', 'total-rentados', 'total-mantenimiento']
                .forEach(id => { $(id).textContent = '—'; });
        }
    }

    function parametrosFiltro() {
        const parametros = new URLSearchParams();
        const texto = buscar.value.trim();
        if (texto) parametros.set('q', texto);
        if (filtroCategoria.value) parametros.set('categoriaId', filtroCategoria.value);
        if (filtroEstado.value) parametros.set('estado', filtroEstado.value);
        return parametros.toString();
    }

    async function cargarVehiculos({ conservarPagina = false } = {}) {
        const numero = ++consultaActual;
        const consulta = parametrosFiltro();

        try {
            const respuesta = await solicitar(consulta ? `${RUTA}?${consulta}` : RUTA);
            if (numero !== consultaActual) return; // llegó una búsqueda más nueva

            vehiculos = respuesta?.data || [];
            if (!conservarPagina) pagina = 1;
            dibujar();
        } catch (error) {
            if (numero !== consultaActual) return;
            vehiculos = [];
            filaAviso('No se pudieron cargar los vehículos.');
            dibujarPaginacion();
            mostrarMensaje(error.message, true);
        }
    }

    async function refrescarTodo(opciones) {
        await Promise.all([cargarResumen(), cargarVehiculos(opciones)]);
    }

    // ---------------------------------------------------------------
    // Tabla y paginación
    // ---------------------------------------------------------------
    function dibujar() {
        const totalPaginas = Math.max(1, Math.ceil(vehiculos.length / POR_PAGINA));
        pagina = Math.min(pagina, totalPaginas);

        if (vehiculos.length === 0) {
            const hayFiltros = parametrosFiltro() !== '';
            filaAviso(hayFiltros
                ? 'Ningún vehículo coincide con la búsqueda o los filtros.'
                : 'Todavía no hay vehículos registrados.');
            dibujarPaginacion();
            return;
        }

        const inicio = (pagina - 1) * POR_PAGINA;
        const filas = vehiculos.slice(inicio, inicio + POR_PAGINA).map(vehiculo => {
            const fila = crear('tr');
            const celda = contenido => {
                const td = crear('td');
                if (contenido instanceof Node) td.append(contenido);
                else td.textContent = contenido ?? '—';
                fila.append(td);
                return td;
            };

            celda(miniatura(vehiculo));
            celda(vehiculo.marca);
            celda(vehiculo.modelo);
            celda(vehiculo.anio);
            celda(vehiculo.placa);
            celda(vehiculo.vin).classList.add('veh-vin');
            celda(vehiculo.categoriaNombre);
            celda(etiquetaEstado(vehiculo.estado));

            const acciones = crear('div', 'veh-acciones');
            acciones.append(botonAccion('ver', 'fa-solid fa-eye', 'Ver detalle', () => abrirDetalle(vehiculo)));
            if (puedeModificar) {
                acciones.append(
                    botonAccion('editar', 'fa-solid fa-pen', 'Editar', () => abrirFormulario(vehiculo)),
                    botonAccion('mas', 'fa-solid fa-ellipsis-vertical', 'Cambiar estado',
                        evento => abrirMenuEstado(evento.currentTarget, vehiculo))
                );
            }
            celda(acciones);
            return fila;
        });

        tabla.replaceChildren(...filas);
        dibujarPaginacion();
    }

    function dibujarPaginacion() {
        const total = vehiculos.length;
        const totalPaginas = Math.max(1, Math.ceil(total / POR_PAGINA));
        const contenedor = $('botones-paginacion');

        const desde = total === 0 ? 0 : (pagina - 1) * POR_PAGINA + 1;
        const hasta = Math.min(pagina * POR_PAGINA, total);
        $('texto-paginacion').textContent =
            `Mostrando ${desde} a ${hasta} de ${total} ${total === 1 ? 'vehículo' : 'vehículos'}`;

        const boton = (contenido, destino, { activa = false, deshabilitado = false, etiqueta } = {}) => {
            const b = crear('button', activa ? 'activa' : '');
            b.type = 'button';
            if (contenido instanceof Node) b.append(contenido);
            else b.textContent = contenido;
            b.disabled = deshabilitado;
            if (etiqueta) b.setAttribute('aria-label', etiqueta);
            if (activa) b.setAttribute('aria-current', 'page');
            b.addEventListener('click', () => { pagina = destino; dibujar(); });
            return b;
        };

        // Se muestran como máximo 5 números alrededor de la página actual.
        let primera = Math.max(1, pagina - 2);
        const ultima = Math.min(totalPaginas, primera + 4);
        primera = Math.max(1, ultima - 4);

        const botones = [];
        const anterior = boton(icono('fa-solid fa-chevron-left'), pagina - 1,
            { deshabilitado: pagina === 1, etiqueta: 'Página anterior' });
        anterior.classList.add('flecha');
        botones.push(anterior);

        for (let n = primera; n <= ultima; n++) {
            botones.push(boton(String(n), n, { activa: n === pagina, etiqueta: `Página ${n}` }));
        }

        const siguiente = boton(icono('fa-solid fa-chevron-right'), pagina + 1,
            { deshabilitado: pagina === totalPaginas, etiqueta: 'Página siguiente' });
        siguiente.classList.add('flecha');
        botones.push(siguiente);

        contenedor.replaceChildren(...botones);
    }

    // ---------------------------------------------------------------
    // Filtros
    // ---------------------------------------------------------------
    buscar.addEventListener('input', () => {
        clearTimeout(temporizadorBusqueda);
        temporizadorBusqueda = setTimeout(() => cargarVehiculos(), 350);
    });
    filtroCategoria.addEventListener('change', () => cargarVehiculos());
    filtroEstado.addEventListener('change', () => cargarVehiculos());

    $('limpiar-filtros').addEventListener('click', () => {
        buscar.value = '';
        filtroCategoria.value = '';
        filtroEstado.value = '';
        cargarVehiculos();
    });

    // ---------------------------------------------------------------
    // Menú de cambio de estado
    // ---------------------------------------------------------------
    function abrirMenuEstado(boton, vehiculo) {
        if (!menuEstado.hidden && vehiculoMenu?.id === vehiculo.id) {
            cerrarMenuEstado();
            return;
        }
        vehiculoMenu = vehiculo;
        menuEstado.querySelectorAll('button').forEach(opcion => {
            opcion.disabled = opcion.dataset.estado === vehiculo.estado;
        });

        menuEstado.hidden = false;
        const caja = boton.getBoundingClientRect();
        const ancho = menuEstado.offsetWidth;
        const alto = menuEstado.offsetHeight;
        let arriba = caja.bottom + 6;
        if (arriba + alto > window.innerHeight - 8) arriba = caja.top - alto - 6;
        menuEstado.style.top = `${Math.max(8, arriba)}px`;
        menuEstado.style.left = `${Math.max(8, caja.right - ancho)}px`;
        menuEstado.querySelector('button:not(:disabled)')?.focus();
    }

    function cerrarMenuEstado() {
        menuEstado.hidden = true;
        vehiculoMenu = null;
    }

    menuEstado.addEventListener('click', async evento => {
        const opcion = evento.target.closest('button[data-estado]');
        if (!opcion || opcion.disabled || !vehiculoMenu) return;

        const vehiculo = vehiculoMenu;
        const estado = opcion.dataset.estado;
        cerrarMenuEstado();

        try {
            await solicitar(`${RUTA}/${vehiculo.id}/estado`, {
                method: 'PATCH',
                body: JSON.stringify({ estado })
            });
            mostrarMensaje(`${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.placa}) ahora está: ${ESTADOS[estado]}.`);
            await refrescarTodo({ conservarPagina: true });
        } catch (error) {
            mostrarMensaje(error.message, true);
        }
    });

    document.addEventListener('click', evento => {
        if (!menuEstado.hidden && !menuEstado.contains(evento.target)
            && !evento.target.closest('.veh-accion.mas')) {
            cerrarMenuEstado();
        }
    });
    document.addEventListener('keydown', evento => {
        if (evento.key === 'Escape' && !menuEstado.hidden) cerrarMenuEstado();
    });
    window.addEventListener('scroll', cerrarMenuEstado, true);
    window.addEventListener('resize', cerrarMenuEstado);

    // ---------------------------------------------------------------
    // Detalle
    // ---------------------------------------------------------------
    function abrirDetalle(vehiculo) {
        const kilometraje = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 1 })
            .format(vehiculo.kilometraje ?? 0);

        const datos = [
            ['Marca', vehiculo.marca],
            ['Modelo', vehiculo.modelo],
            ['Año', vehiculo.anio],
            ['Color', vehiculo.color || '—'],
            ['Placa', vehiculo.placa],
            ['Categoría', vehiculo.categoriaNombre],
            ['VIN', vehiculo.vin],
            ['Kilometraje', `${kilometraje} km`]
        ];

        const lista = document.createElement('dl');
        datos.forEach(([titulo, valor]) => {
            const grupo = crear('div');
            grupo.append(crear('dt', null, titulo), crear('dd', null, valor));
            lista.append(grupo);
        });

        const estado = crear('div');
        estado.append(crear('dt', null, 'Estado'));
        const dd = crear('dd');
        dd.append(etiquetaEstado(vehiculo.estado));
        estado.append(dd);
        lista.append(estado);

        $('titulo-detalle').textContent = `${vehiculo.marca} ${vehiculo.modelo}`;
        $('contenido-detalle').replaceChildren(miniatura(vehiculo), lista);
        dialogoDetalle.showModal();
    }

    // ---------------------------------------------------------------
    // Formulario registrar / editar
    // ---------------------------------------------------------------
    const campo = nombre => formulario.elements[nombre];

    function llenarCategoriasFormulario(seleccionada) {
        const select = campo('categoriaId');
        select.replaceChildren(new Option('Selecciona una categoría', ''));
        categorias
            .filter(c => c.activo || c.id === seleccionada)
            .forEach(c => {
                const texto = c.activo ? c.nombre : `${c.nombre} (inactiva)`;
                select.append(new Option(texto, c.id));
            });
        select.value = seleccionada ?? '';
    }

    function abrirFormulario(vehiculo = null) {
        idEdicion = vehiculo?.id ?? null;
        formulario.reset();
        errorFormulario.hidden = true;
        formulario.querySelectorAll('[aria-invalid]').forEach(e => e.removeAttribute('aria-invalid'));

        const anioMaximo = new Date().getFullYear() + 1;
        campo('anio').max = anioMaximo;

        $('titulo-formulario').textContent = vehiculo ? 'Editar vehículo' : 'Registrar vehículo';
        llenarCategoriasFormulario(vehiculo?.categoriaId ?? null);

        if (vehiculo) {
            campo('marca').value = vehiculo.marca;
            campo('modelo').value = vehiculo.modelo;
            campo('placa').value = vehiculo.placa;
            campo('anio').value = vehiculo.anio;
            campo('vin').value = vehiculo.vin;
            campo('color').value = vehiculo.color || '';
            campo('kilometraje').value = vehiculo.kilometraje;
        } else {
            campo('kilometraje').value = 0;
        }

        dialogoVehiculo.showModal();
        campo(vehiculo ? 'marca' : 'categoriaId').focus();
    }

    // Mismas reglas que valida la API; aquí solo sirven para avisar antes de enviar.
    function validar(datos) {
        const errores = [];
        const marcar = nombre => campo(nombre).setAttribute('aria-invalid', 'true');
        formulario.querySelectorAll('[aria-invalid]').forEach(e => e.removeAttribute('aria-invalid'));

        const anioMaximo = new Date().getFullYear() + 1;

        if (!datos.categoriaId) { errores.push('Selecciona una categoría.'); marcar('categoriaId'); }
        if (!datos.marca) { errores.push('La marca es obligatoria.'); marcar('marca'); }
        if (!datos.modelo) { errores.push('El modelo es obligatorio.'); marcar('modelo'); }
        if (!/^[A-Z0-9][A-Z0-9-]{3,13}[A-Z0-9]$/.test(datos.placa)) {
            errores.push('La placa debe tener de 5 a 15 letras, números o guiones.'); marcar('placa');
        }
        if (!/^[A-HJ-NPR-Z0-9]{17}$/.test(datos.vin)) {
            errores.push('El VIN debe tener 17 letras o números, sin I, O ni Q.'); marcar('vin');
        }
        if (!Number.isInteger(datos.anio) || datos.anio < 2000 || datos.anio > anioMaximo) {
            errores.push(`El año debe estar entre 2000 y ${anioMaximo}.`); marcar('anio');
        }
        if (!Number.isFinite(datos.kilometraje) || datos.kilometraje < 0) {
            errores.push('El kilometraje no puede ser negativo.'); marcar('kilometraje');
        }
        return errores;
    }

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();

        const texto = nombre => campo(nombre).value.trim();
        const datos = {
            categoriaId: Number(campo('categoriaId').value) || null,
            marca: texto('marca'),
            modelo: texto('modelo'),
            placa: texto('placa').toUpperCase(),
            vin: texto('vin').toUpperCase(),
            anio: Number(texto('anio')),
            color: texto('color') || null,
            kilometraje: texto('kilometraje') === '' ? NaN : Number(texto('kilometraje'))
        };

        const errores = validar(datos);
        if (errores.length > 0) {
            errorFormulario.textContent = errores.join(' ');
            errorFormulario.hidden = false;
            return;
        }

        errorFormulario.hidden = true;
        botonGuardar.disabled = true;
        botonGuardar.textContent = 'Guardando…';

        try {
            const esEdicion = idEdicion !== null;
            await solicitar(esEdicion ? `${RUTA}/${idEdicion}` : RUTA, {
                method: esEdicion ? 'PUT' : 'POST',
                body: JSON.stringify(datos)
            });
            dialogoVehiculo.close();
            mostrarMensaje(esEdicion
                ? `Se actualizó el vehículo ${datos.placa}.`
                : `Se registró el vehículo ${datos.placa} como Disponible.`);
            await refrescarTodo({ conservarPagina: esEdicion });
        } catch (error) {
            errorFormulario.textContent = error.message;
            errorFormulario.hidden = false;
        } finally {
            botonGuardar.disabled = false;
            botonGuardar.textContent = 'Guardar';
        }
    });

    $('registrar-vehiculo').addEventListener('click', () => abrirFormulario());

    document.querySelectorAll('[data-cerrar]').forEach(boton => {
        boton.addEventListener('click', () => boton.closest('dialog').close());
    });

    // ---------------------------------------------------------------
    // Inicio
    // ---------------------------------------------------------------
    await cargarCategorias();
    await refrescarTodo();
});
