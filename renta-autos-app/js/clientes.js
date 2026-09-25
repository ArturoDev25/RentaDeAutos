document.addEventListener('DOMContentLoaded', async () => {
    const $ = id => document.getElementById(id);
    const RUTA = '/v1/clientes';
    const POR_PAGINA = 5;
    const tabla = $('tabla-clientes');
    const mensaje = $('mensaje');
    const buscar = $('buscar');
    const filtroActivo = $('filtro-activo');
    const dialogo = $('dialogo-cliente');
    const dialogoDetalle = $('dialogo-detalle');
    const formulario = $('formulario-cliente');
    const errorFormulario = $('error-formulario');
    const guardar = $('guardar-cliente');
    let sesion;
    try { sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null'); } catch { sesion = null; }
    if (!sesion?.token) { window.location.replace('login.html'); return; }

    const puedeModificar = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR'].includes(sesion.rol);
    const puedeDesactivar = sesion.rol === 'ADMINISTRADOR';
    $('nombre-usuario').textContent = sesion.nombre || 'Usuario';
    $('rol-usuario').textContent = sesion.rol ? sesion.rol.charAt(0) + sesion.rol.slice(1).toLowerCase() : '-';
    $('registrar-cliente').hidden = !puedeModificar;
    $('cerrar-sesion').addEventListener('click', () => { localStorage.removeItem('usuarioSesion'); window.location.replace('login.html'); });

    async function solicitar(endpoint, opciones = {}) {
        try { return await fetchAPI(endpoint, opciones); }
        catch (error) {
            if (error.status === 401) { localStorage.removeItem('usuarioSesion'); window.location.replace('login.html'); }
            if (error.status === 403) error.message = 'No tienes permiso para realizar esta operación.';
            throw error;
        }
    }

    function mostrarMensaje(texto, esError = false) {
        mensaje.textContent = texto;
        mensaje.className = `veh-mensaje ${esError ? 'error' : 'exito'}`;
        mensaje.hidden = false;
        clearTimeout(mostrarMensaje.temporizador);
        if (!esError) mostrarMensaje.temporizador = setTimeout(() => { mensaje.hidden = true; }, 4000);
    }

    function crear(etiqueta, clase, texto) {
        const elemento = document.createElement(etiqueta);
        if (clase) elemento.className = clase;
        if (texto !== undefined) elemento.textContent = texto;
        return elemento;
    }

    function icono(clases) { const elemento = crear('i'); elemento.className = clases; elemento.setAttribute('aria-hidden', 'true'); return elemento; }
    function nombreCompleto(cliente) { return `${cliente.nombre} ${cliente.apellidos}`.trim(); }
    function fecha(valor) { return valor ? new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium' }).format(new Date(`${valor}T00:00:00`)) : '-'; }
    function iniciales(cliente) { return `${cliente.nombre?.[0] || ''}${cliente.apellidos?.[0] || ''}`.toUpperCase(); }
    function etiquetaEstado(cliente) { return crear('span', `veh-estado cliente-estado ${cliente.activo ? 'activo' : 'inactivo'}`, cliente.activo ? 'Activo' : 'Inactivo'); }
    function botonAccion(clase, iconoClase, titulo, accion) {
        const boton = crear('button', `veh-accion ${clase}`); boton.type = 'button'; boton.title = titulo; boton.setAttribute('aria-label', titulo); boton.append(icono(iconoClase)); boton.addEventListener('click', accion); return boton;
    }
    function parametros() {
        const query = new URLSearchParams();
        if (buscar.value.trim()) query.set('q', buscar.value.trim());
        if (filtroActivo.value) query.set('activo', filtroActivo.value);
        return query.toString();
    }
    function filaAviso(texto) { const fila = crear('tr'); const celda = crear('td', 'veh-celda-aviso', texto); celda.colSpan = 7; fila.append(celda); tabla.replaceChildren(fila); }

    let clientes = [];
    let pagina = 1;
    let idEdicion = null;
    let temporizadorBusqueda;
    let consultaActual = 0;

    async function cargarResumen() {
        try {
            const todos = (await solicitar(RUTA))?.data || [];
            $('total-clientes').textContent = todos.length;
            $('total-activos').textContent = todos.filter(cliente => cliente.activo).length;
            $('total-inactivos').textContent = todos.filter(cliente => !cliente.activo).length;
        } catch { ['total-clientes', 'total-activos', 'total-inactivos'].forEach(id => { $(id).textContent = '-'; }); }
    }

    async function cargarClientes({ conservarPagina = false } = {}) {
        const numero = ++consultaActual;
        const query = parametros();
        try {
            const respuesta = await solicitar(query ? `${RUTA}?${query}` : RUTA);
            if (numero !== consultaActual) return;
            clientes = respuesta?.data || [];
            if (!conservarPagina) pagina = 1;
            dibujar();
        } catch (error) {
            if (numero !== consultaActual) return;
            clientes = []; filaAviso('No se pudieron cargar los clientes.'); dibujarPaginacion(); mostrarMensaje(error.message, true);
        }
    }

    function dibujar() {
        const totalPaginas = Math.max(1, Math.ceil(clientes.length / POR_PAGINA));
        pagina = Math.min(pagina, totalPaginas);
        if (!clientes.length) { filaAviso(parametros() ? 'Ningún cliente coincide con la búsqueda o los filtros.' : 'Todavía no hay clientes registrados.'); dibujarPaginacion(); return; }
        const inicio = (pagina - 1) * POR_PAGINA;
        const filas = clientes.slice(inicio, inicio + POR_PAGINA).map(cliente => {
            const fila = crear('tr');
            const celda = contenido => { const td = crear('td'); if (contenido instanceof Node) td.append(contenido); else td.textContent = contenido ?? '-'; fila.append(td); return td; };
            const identidad = crear('div', 'cliente-nombre'); identidad.append(crear('span', 'cliente-avatar', iniciales(cliente)));
            const texto = crear('div'); texto.append(crear('strong', null, nombreCompleto(cliente)), crear('small', null, `ID #${cliente.id}`)); identidad.append(texto);
            celda(identidad); celda(cliente.correo || 'Sin correo'); celda(cliente.telefono); celda(cliente.numeroLicencia); celda(fecha(cliente.licenciaVencimiento)); celda(etiquetaEstado(cliente));
            const acciones = crear('div', 'veh-acciones'); acciones.append(botonAccion('ver', 'fa-solid fa-eye', 'Ver detalle', () => abrirDetalle(cliente)));
            if (puedeModificar) acciones.append(botonAccion('editar', 'fa-solid fa-pen', 'Editar', () => abrirFormulario(cliente)));
            if (puedeDesactivar && cliente.activo) acciones.append(botonAccion('eliminar', 'fa-solid fa-user-slash', 'Desactivar', () => desactivar(cliente)));
            if (puedeDesactivar && !cliente.activo) acciones.append(botonAccion('activar', 'fa-solid fa-user-check', 'Activar', () => reactivar(cliente)));
            celda(acciones); return fila;
        });
        tabla.replaceChildren(...filas); dibujarPaginacion();
    }

    function dibujarPaginacion() {
        const total = clientes.length; const totalPaginas = Math.max(1, Math.ceil(total / POR_PAGINA));
        const desde = total ? (pagina - 1) * POR_PAGINA + 1 : 0; const hasta = Math.min(pagina * POR_PAGINA, total);
        $('texto-paginacion').textContent = `Mostrando ${desde} a ${hasta} de ${total} ${total === 1 ? 'cliente' : 'clientes'}`;
        const contenedor = $('botones-paginacion');
        const boton = (contenido, destino, opciones = {}) => { const elemento = crear('button', opciones.activa ? 'activa' : ''); elemento.type = 'button'; elemento.disabled = opciones.deshabilitado; if (contenido instanceof Node) elemento.append(contenido); else elemento.textContent = contenido; if (opciones.etiqueta) elemento.setAttribute('aria-label', opciones.etiqueta); if (opciones.activa) elemento.setAttribute('aria-current', 'page'); elemento.addEventListener('click', () => { pagina = destino; dibujar(); }); return elemento; };
        const botones = [boton(icono('fa-solid fa-chevron-left'), pagina - 1, { deshabilitado: pagina === 1, etiqueta: 'Página anterior' })];
        for (let numero = 1; numero <= Math.min(totalPaginas, 5); numero++) botones.push(boton(String(numero), numero, { activa: numero === pagina, etiqueta: `Página ${numero}` }));
        botones.push(boton(icono('fa-solid fa-chevron-right'), pagina + 1, { deshabilitado: pagina === totalPaginas, etiqueta: 'Página siguiente' }));
        botones[0].classList.add('flecha'); botones.at(-1).classList.add('flecha'); contenedor.replaceChildren(...botones);
    }

    function abrirDetalle(cliente) {
        const datos = [['Nombre', nombreCompleto(cliente)], ['Correo', cliente.correo || 'Sin correo'], ['Teléfono', cliente.telefono], ['Licencia', cliente.numeroLicencia], ['Vencimiento', fecha(cliente.licenciaVencimiento)], ['Dirección', cliente.direccion || 'Sin dirección']];
        const lista = crear('dl'); datos.forEach(([titulo, valor]) => { const grupo = crear('div'); grupo.append(crear('dt', null, titulo), crear('dd', null, valor)); lista.append(grupo); });
        const estado = crear('div'); estado.append(crear('dt', null, 'Estado'), crear('dd')); estado.lastChild.append(etiquetaEstado(cliente)); lista.append(estado);
        $('titulo-detalle').textContent = nombreCompleto(cliente); $('contenido-detalle').replaceChildren(lista); dialogoDetalle.showModal();
    }

    function abrirFormulario(cliente = null) {
        idEdicion = cliente?.id ?? null; formulario.reset(); errorFormulario.hidden = true; formulario.querySelectorAll('[aria-invalid]').forEach(campo => campo.removeAttribute('aria-invalid'));
        $('titulo-formulario').textContent = cliente ? 'Editar cliente' : 'Registrar cliente';
        const minimo = new Date(); minimo.setDate(minimo.getDate() + 1); formulario.elements.licenciaVencimiento.min = minimo.toISOString().slice(0, 10);
        if (cliente) Object.entries(cliente).forEach(([nombre, valor]) => { if (formulario.elements[nombre]) formulario.elements[nombre].value = valor ?? ''; });
        dialogo.showModal(); formulario.elements.nombre.focus();
    }

    function datosFormulario() { const datos = Object.fromEntries(new FormData(formulario).entries()); return Object.fromEntries(Object.entries(datos).map(([clave, valor]) => [clave, valor.trim() || null])); }
    function validar(datos) {
        const errores = []; const marcar = nombre => formulario.elements[nombre].setAttribute('aria-invalid', 'true'); formulario.querySelectorAll('[aria-invalid]').forEach(campo => campo.removeAttribute('aria-invalid'));
        ['nombre', 'apellidos', 'telefono', 'numeroLicencia', 'licenciaVencimiento'].forEach(campo => { if (!datos[campo]) { errores.push(`El campo ${campo} es obligatorio.`); marcar(campo); } });
        if (datos.correo && !/^\S+@\S+\.\S+$/.test(datos.correo)) { errores.push('El correo no tiene un formato válido.'); marcar('correo'); }
        if (datos.licenciaVencimiento && datos.licenciaVencimiento <= new Date().toISOString().slice(0, 10)) { errores.push('La licencia debe tener una fecha de vencimiento futura.'); marcar('licenciaVencimiento'); }
        return errores;
    }
    async function desactivar(cliente) {
        if (!window.confirm(`¿Desactivar a ${nombreCompleto(cliente)}? Su historial se conservará.`)) return;
        try { await solicitar(`${RUTA}/${cliente.id}`, { method: 'DELETE' }); mostrarMensaje(`Se desactivó a ${nombreCompleto(cliente)}.`); await Promise.all([cargarResumen(), cargarClientes({ conservarPagina: true })]); }
        catch (error) { mostrarMensaje(error.message, true); }
    }

    async function reactivar(cliente) {
        if (!window.confirm(`¿Reactivar a ${nombreCompleto(cliente)}?`)) return;
        try {
            await solicitar(`${RUTA}/${cliente.id}/activar`, { method: 'PATCH' });
            mostrarMensaje(`Se reactivó a ${nombreCompleto(cliente)}.`);
            await Promise.all([cargarResumen(), cargarClientes({ conservarPagina: true })]);
        } catch (error) { mostrarMensaje(error.message, true); }
    }

    buscar.addEventListener('input', () => { clearTimeout(temporizadorBusqueda); temporizadorBusqueda = setTimeout(() => cargarClientes(), 350); });
    filtroActivo.addEventListener('change', () => cargarClientes());
    $('limpiar-filtros').addEventListener('click', () => { buscar.value = ''; filtroActivo.value = ''; cargarClientes(); });
    $('registrar-cliente').addEventListener('click', () => abrirFormulario());
    document.querySelectorAll('[data-cerrar]').forEach(boton => boton.addEventListener('click', () => boton.closest('dialog').close()));
    formulario.addEventListener('submit', async evento => {
        evento.preventDefault(); const datos = datosFormulario(); const errores = validar(datos);
        if (errores.length) { errorFormulario.textContent = errores.join(' '); errorFormulario.hidden = false; return; }
        guardar.disabled = true; guardar.textContent = 'Guardando...'; errorFormulario.hidden = true;
        try { const edicion = idEdicion !== null; await solicitar(edicion ? `${RUTA}/${idEdicion}` : RUTA, { method: edicion ? 'PUT' : 'POST', body: JSON.stringify(datos) }); dialogo.close(); mostrarMensaje(edicion ? 'Cliente actualizado correctamente.' : 'Cliente registrado correctamente.'); await Promise.all([cargarResumen(), cargarClientes({ conservarPagina: edicion })]); }
        catch (error) { errorFormulario.textContent = error.message; errorFormulario.hidden = false; }
        finally { guardar.disabled = false; guardar.textContent = 'Guardar'; }
    });
    await Promise.all([cargarResumen(), cargarClientes()]);
});