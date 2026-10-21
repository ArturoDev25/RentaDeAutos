// catalogo.js - Catálogo del portal con filtros por categoría y precio diario

document.addEventListener('DOMContentLoaded', () => {
    const grid = document.getElementById('catalogoGrid');
    const conteo = document.getElementById('conteo');
    const formulario = document.getElementById('formFiltros');
    const selectCategoria = document.getElementById('filtroCategoria');
    const selectPrecio = document.getElementById('filtroPrecio');

    const params = new URLSearchParams(window.location.search);
    const estado = {
        categoriaId: params.get('categoriaId') || '',
        precio: params.get('precio') || '',
        q: (params.get('q') || '').trim(),
        inicio: Portal.esFechaISO(params.get('inicio')) ? params.get('inicio') : '',
        fin: Portal.esFechaISO(params.get('fin')) ? params.get('fin') : ''
    };

    let consultaActual = 0;

    selectPrecio.innerHTML = Portal.RANGOS_PRECIO
        .map(r => `<option value="${r.id}">${Portal.escapar(r.texto)}</option>`)
        .join('');
    if (!Portal.RANGOS_PRECIO.some(r => r.id === estado.precio)) estado.precio = '';
    selectPrecio.value = estado.precio;

    function normalizar(texto) {
        return String(texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    }

    function coincideTexto(v) {
        if (!estado.q) return true;
        const buscado = normalizar(estado.q);
        return normalizar(`${v.marca} ${v.modelo} ${v.anio} ${v.categoria}`).includes(buscado);
    }

    function hayFiltros() {
        return Boolean(estado.categoriaId || estado.precio || estado.q);
    }

    function filtrosAPI() {
        const rango = Portal.RANGOS_PRECIO.find(r => r.id === estado.precio) || {};
        return { categoriaId: estado.categoriaId, precioMin: rango.min, precioMax: rango.max };
    }

    function actualizarURL() {
        const nuevos = new URLSearchParams();
        ['categoriaId', 'precio', 'q', 'inicio', 'fin'].forEach(k => { if (estado[k]) nuevos.set(k, estado[k]); });
        const query = nuevos.toString();
        history.replaceState(null, '', `catalogo.html${query ? `?${query}` : ''}`);
    }

    function llenarCategorias(categorias) {
        selectCategoria.innerHTML = '<option value="">Todas</option>' + categorias
            .map(c => `<option value="${Portal.escapar(c.id)}">${Portal.escapar(c.nombre)}</option>`)
            .join('');
        if (!categorias.some(c => String(c.id) === String(estado.categoriaId))) estado.categoriaId = '';
        selectCategoria.value = estado.categoriaId;
    }

    function limpiar() {
        estado.categoriaId = '';
        estado.precio = '';
        estado.q = '';
        selectCategoria.value = '';
        selectPrecio.value = '';
        const buscador = document.getElementById('buscarVehiculos');
        if (buscador) buscador.value = '';
        buscar();
    }

    function pintar(vehiculos) {
        const lista = vehiculos.filter(coincideTexto);
        const total = lista.length;
        conteo.textContent = `${total} ${total === 1 ? 'vehículo encontrado' : 'vehículos encontrados'}`;

        if (total === 0) {
            if (hayFiltros()) {
                Portal.mostrarEstado(grid, {
                    tipo: 'vacio',
                    titulo: 'No hay autos con estos filtros',
                    texto: 'Prueba con otra categoría, otro rango de precio u otra búsqueda.',
                    accion: { texto: 'Limpiar filtros', icono: 'fa-filter-circle-xmark', alHacerClic: limpiar }
                });
            } else {
                Portal.mostrarEstado(grid, {
                    tipo: 'vacio',
                    titulo: 'Sin vehículos disponibles',
                    texto: 'Todos nuestros autos están ocupados en este momento. Vuelve a consultar más tarde.'
                });
            }
            return;
        }
        Portal.mostrarVehiculos(grid, lista, { inicio: estado.inicio, fin: estado.fin });
    }

    function mostrarError(err, reintentar) {
        const { titulo, texto } = Portal.mensajeError(err);
        conteo.textContent = '';
        Portal.mostrarEstado(grid, {
            tipo: 'error',
            titulo,
            texto,
            accion: { texto: 'Reintentar', alHacerClic: reintentar }
        });
    }

    async function buscar() {
        const id = ++consultaActual;
        Portal.mostrarCarga(grid);
        conteo.textContent = 'Buscando…';
        actualizarURL();
        try {
            const datos = await Portal.obtenerCatalogo(filtrosAPI());
            if (id !== consultaActual) return;
            pintar(datos);
        } catch (err) {
            if (id !== consultaActual) return;
            mostrarError(err, buscar);
        }
    }

    async function iniciar() {
        Portal.mostrarCarga(grid);
        conteo.textContent = 'Cargando…';
        try {
            // La lista sin filtros da las categorías con autos disponibles
            const todos = await Portal.obtenerCatalogo();
            llenarCategorias(Portal.extraerCategorias(todos));
            if (estado.categoriaId || estado.precio) {
                await buscar();
            } else {
                actualizarURL();
                pintar(todos);
            }
        } catch (err) {
            mostrarError(err, iniciar);
        }
    }

    formulario.addEventListener('submit', (e) => {
        e.preventDefault();
        estado.categoriaId = selectCategoria.value;
        estado.precio = selectPrecio.value;
        buscar();
    });

    iniciar();
});
