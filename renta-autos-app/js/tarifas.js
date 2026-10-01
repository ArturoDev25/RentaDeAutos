/**
 * js/tarifas.js  —  S3-06 Pantalla de Tarifas
 *
 * Endpoints consumidos:
 *   GET    /v1/tarifas               → ApiResponse<List<TarifaResponse>>
 *   POST   /v1/tarifas               → ApiResponse<TarifaResponse>  (ADMIN / SUPERVISOR)
 *   PUT    /v1/tarifas/{id}          → ApiResponse<TarifaResponse>  (ADMIN / SUPERVISOR)
 *   PATCH  /v1/tarifas/{id}/desactivar → ApiResponse<TarifaResponse>  (ADMIN / SUPERVISOR)
 *   GET    /categorias?activo=true   → ApiResponse<List<CategoriaResponse>>
 *
 * TarifaResponse: { id, categoriaId, categoriaNombre, precioDia,
 *                   cargoAtrasoDia, fechaInicio, fechaFin, activo }
 * TarifaRequest:  { categoriaId, precioDia, cargoAtrasoDia, fechaInicio, fechaFin }
 */

document.addEventListener('DOMContentLoaded', async () => {
    /* ── Refs del DOM ─────────────────────────────────────────────── */
    const $ = id => document.getElementById(id);

    const tabla           = $('tabla-tarifas');
    const filtroCat       = $('filtro-categoria');
    const filtroEstado    = $('filtro-estado');
    const filtroVigencia  = $('filtro-vigencia');
    const mensaje         = $('mensaje');
    const dialogo         = $('dialogo-tarifa');
    const formulario      = $('formulario-tarifa');
    const errorFormulario = $('error-formulario');
    const btnNueva        = $('nueva-tarifa');
    const btnRecargar     = $('recargar');
    const btnGuardar      = $('guardar-tarifa');
    const btnCancelar     = $('cancelar-tarifa');
    const campoCat        = $('campo-categoria');
    const campoPrecio     = $('campo-precio');
    const campoCargo      = $('campo-cargo');
    const campoInicio     = $('campo-inicio');
    const campoFin        = $('campo-fin');

    /* ── Estado interno ───────────────────────────────────────────── */
    let sesion      = null;
    let todasTarifas = [];
    let idEdicion   = null;
    let ocupado     = false;
    let puedeEditar = false;     // true si ADMINISTRADOR o SUPERVISOR

    const moneda = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
    const ROLES_ESCRITURA = new Set(['ADMINISTRADOR', 'SUPERVISOR']);

    /* ── Sesión ───────────────────────────────────────────────────── */
    try {
        sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null');
    } catch {
        sesion = null;
    }

    if (!sesion?.token) {
        window.location.replace('login.html');
        return;
    }

    /* ── Helpers de mensajes ──────────────────────────────────────── */
    function mostrarMensaje(texto, tipo = 'exito') {
        mensaje.textContent   = texto;
        mensaje.className     = `mensaje-${tipo}`;
        mensaje.hidden        = false;
    }

    function ocultarMensaje() { mensaje.hidden = true; }

    function mostrarFila(texto) {
        tabla.replaceChildren();
        const fila  = document.createElement('tr');
        const celda = document.createElement('td');
        celda.colSpan   = 8;
        celda.textContent = texto;
        fila.append(celda);
        tabla.append(fila);
    }

    /** Extrae un mensaje legible del error de la API. */
    function mensajeError(error) {
        const mapa = {
            400: `Datos inválidos: ${error.message}`,
            403: 'No tienes permiso para realizar esta operación.',
            409: `Conflicto de vigencias: ${error.message}`,
            401: 'Tu sesión ha expirado. Vuelve a iniciar sesión.'
        };
        return mapa[error.status] || error.message || 'Error inesperado del servidor.';
    }

    /* ── Bloqueo de UI ────────────────────────────────────────────── */
    function bloquear(valor) {
        ocupado = valor;
        btnNueva.disabled    = valor || !puedeEditar;
        btnRecargar.disabled = valor;
        [filtroCat, filtroEstado, filtroVigencia].forEach(s => { s.disabled = valor; });
        btnGuardar.disabled  = valor;
        btnCancelar.disabled = valor;
        tabla.querySelectorAll('button').forEach(b => { b.disabled = valor; });
    }

    /* ── Capa de red ──────────────────────────────────────────────── */
    async function solicitar(endpoint, opciones = {}) {
        try {
            return await fetchAPI(endpoint, {
                ...opciones,
                headers: {
                    ...opciones.headers,
                    Authorization: `Bearer ${sesion.token}`
                }
            });
        } catch (error) {
            if (error.status === 401) {
                localStorage.removeItem('usuarioSesion');
                window.location.replace('login.html');
            }
            throw error;
        }
    }

    /* ── Cálculo de vigencia ──────────────────────────────────────── */
    function calcularVigencia(fechaInicio, fechaFin) {
        // Las fechas llegan como "YYYY-MM-DD" desde la API
        const hoy   = new Date(); hoy.setHours(0, 0, 0, 0);
        const ini   = fechaInicio ? new Date(fechaInicio + 'T00:00:00') : null;
        const fin   = fechaFin    ? new Date(fechaFin    + 'T00:00:00') : null;

        if (!ini) return { tipo: 'sin-fin', etiqueta: 'Sin datos', icono: 'fa-question' };

        if (ini > hoy) return { tipo: 'futura',  etiqueta: 'Próxima',  icono: 'fa-clock' };
        if (!fin)      return { tipo: 'vigente', etiqueta: 'Vigente',  icono: 'fa-circle-check' };
        if (fin < hoy) return { tipo: 'vencida', etiqueta: 'Vencida',  icono: 'fa-circle-xmark' };
        return           { tipo: 'vigente', etiqueta: 'Vigente',  icono: 'fa-circle-check' };
    }

    /* ── Formateo de fechas ───────────────────────────────────────── */
    function formatearFecha(iso) {
        if (!iso) return '—';
        const [y, m, d] = iso.split('-');
        return `${d}/${m}/${y}`;
    }

    /* ── Renderizado de la tabla ──────────────────────────────────── */
    function dibujarTabla(tarifas) {
        tabla.replaceChildren();

        if (tarifas.length === 0) {
            mostrarFila('No hay tarifas para el filtro seleccionado.');
            $('total-tarifas').textContent = '';
            return;
        }

        tarifas.forEach(tar => {
            const vig  = calcularVigencia(tar.fechaInicio, tar.fechaFin);
            const fila = document.createElement('tr');

            // Categoría
            const tdCat = document.createElement('td');
            tdCat.className   = 'col-categoria';
            tdCat.textContent = tar.categoriaNombre || '—';
            fila.append(tdCat);

            // Precio / día
            const tdPrecio = document.createElement('td');
            tdPrecio.className   = 'col-monto';
            tdPrecio.textContent = moneda.format(tar.precioDia);
            fila.append(tdPrecio);

            // Cargo atraso / día
            const tdCargo = document.createElement('td');
            tdCargo.className   = 'col-monto';
            tdCargo.textContent = moneda.format(tar.cargoAtrasoDia);
            fila.append(tdCargo);

            // Fecha inicio
            const tdIni = document.createElement('td');
            tdIni.className   = 'col-fecha';
            tdIni.textContent = formatearFecha(tar.fechaInicio);
            fila.append(tdIni);

            // Fecha fin
            const tdFin = document.createElement('td');
            tdFin.className   = 'col-fecha';
            tdFin.textContent = formatearFecha(tar.fechaFin);
            fila.append(tdFin);

            // Badge de vigencia
            const tdVig = document.createElement('td');
            const badgeVig = document.createElement('span');
            badgeVig.className = `vigencia ${vig.tipo}`;
            badgeVig.innerHTML =
                `<i class="fa-solid ${vig.icono}" aria-hidden="true"></i>${vig.etiqueta}`;
            tdVig.append(badgeVig);
            fila.append(tdVig);

            // Badge de estado
            const tdEst = document.createElement('td');
            const badgeEst = document.createElement('span');
            badgeEst.className   = `estado ${tar.activo ? 'activa' : 'inactiva'}`;
            badgeEst.textContent = tar.activo ? 'Activa' : 'Inactiva';
            tdEst.append(badgeEst);
            fila.append(tdEst);

            // Acciones (solo si el rol lo permite)
            const tdAcc = document.createElement('td');
            const divAcc = document.createElement('div');
            divAcc.className = 'tar-acciones';

            if (puedeEditar) {
                // Botón Editar
                const btnEditar = document.createElement('button');
                btnEditar.type      = 'button';
                btnEditar.className = 'tar-btn secundario';
                btnEditar.setAttribute('aria-label', `Editar tarifa de ${tar.categoriaNombre}`);
                btnEditar.innerHTML = '<i class="fa-solid fa-pencil" aria-hidden="true"></i> Editar';
                btnEditar.addEventListener('click', () => abrirFormulario(tar));
                divAcc.append(btnEditar);

                // Botón Desactivar (solo si está activa)
                if (tar.activo) {
                    const btnDesactivar = document.createElement('button');
                    btnDesactivar.type      = 'button';
                    btnDesactivar.className = 'tar-btn peligro';
                    btnDesactivar.setAttribute('aria-label', `Desactivar tarifa de ${tar.categoriaNombre}`);
                    btnDesactivar.innerHTML = '<i class="fa-solid fa-ban" aria-hidden="true"></i> Desactivar';
                    btnDesactivar.addEventListener('click', () => desactivarTarifa(tar));
                    divAcc.append(btnDesactivar);
                }
            } else {
                // Solo lectura
                const span = document.createElement('span');
                span.textContent = '—';
                span.style.color = '#999';
                divAcc.append(span);
            }

            tdAcc.append(divAcc);
            fila.append(tdAcc);
            tabla.append(fila);
        });

        $('total-tarifas').textContent = `Tarifas mostradas: ${tarifas.length}`;
    }

    /* ── Aplicar filtros locales ──────────────────────────────────── */
    function aplicarFiltros() {
        const catVal = filtroCat.value;
        const estVal = filtroEstado.value;
        const vigVal = filtroVigencia.value;

        const filtradas = todasTarifas.filter(tar => {
            if (catVal && String(tar.categoriaId) !== catVal) return false;
            if (estVal === 'activo'   && !tar.activo)  return false;
            if (estVal === 'inactivo' && tar.activo)   return false;
            if (vigVal) {
                const vig = calcularVigencia(tar.fechaInicio, tar.fechaFin);
                if (vig.tipo !== vigVal) return false;
            }
            return true;
        });

        dibujarTabla(filtradas);
    }

    /* ── Cargar tarifas desde la API ──────────────────────────────── */
    async function cargarTarifas() {
        mostrarFila('Cargando tarifas…');
        $('total-tarifas').textContent = '';

        const resultado = await solicitar('/v1/tarifas');

        if (!resultado?.success || !Array.isArray(resultado.data)) {
            throw new Error('El servidor devolvió una respuesta inesperada.');
        }

        todasTarifas = resultado.data;
        aplicarFiltros();
    }

    /* ── Cargar categorías para selector del modal ────────────────── */
    async function cargarCategorias(selectDestino) {
        selectDestino.innerHTML = '<option value="">Cargando…</option>';
        selectDestino.disabled  = true;

        try {
            const res = await solicitar('/categorias?activo=true');
            const cats = res?.data || [];

            selectDestino.innerHTML = '<option value="">— Selecciona una categoría —</option>';
            cats.forEach(cat => {
                const opt  = document.createElement('option');
                opt.value  = cat.id;
                opt.textContent = cat.nombre;
                selectDestino.append(opt);
            });

            // También rellenar el filtro de categoría (si no está poblado aún)
            if (filtroCat.options.length <= 1) {
                cats.forEach(cat => {
                    const opt  = document.createElement('option');
                    opt.value  = cat.id;
                    opt.textContent = cat.nombre;
                    filtroCat.append(opt);
                });
            }
        } catch (error) {
            selectDestino.innerHTML =
                '<option value="">— No se pudieron cargar las categorías —</option>';
        } finally {
            selectDestino.disabled = false;
        }
    }

    /* ── Flujo completo de actualización ──────────────────────────── */
    async function actualizarListado() {
        if (ocupado) return;

        ocultarMensaje();
        bloquear(true);

        try {
            await cargarTarifas();
        } catch (error) {
            mostrarFila('No se pudo cargar el catálogo. Pulsa Actualizar.');
            mostrarMensaje(mensajeError(error), 'error');
        } finally {
            bloquear(false);
        }
    }

    async function refrescarTrasCambio(texto) {
        try {
            await cargarTarifas();
            mostrarMensaje(texto, 'exito');
        } catch (error) {
            mostrarMensaje(
                `${texto} No se pudo actualizar la tabla: ${mensajeError(error)}. ` +
                'Pulsa Actualizar; no necesitas repetir el cambio.',
                'advertencia'
            );
        }
    }

    /* ── Abrir modal (crear o editar) ─────────────────────────────── */
    async function abrirFormulario(tarifa = null) {
        if (ocupado) return;

        formulario.reset();
        errorFormulario.hidden = true;
        idEdicion = tarifa?.id ?? null;

        $('titulo-formulario').textContent =
            tarifa ? 'Editar tarifa' : 'Nueva tarifa';

        // Cargar categorías en el selector del formulario
        await cargarCategorias(campoCat);

        if (tarifa) {
            campoCat.value    = tarifa.categoriaId ?? '';
            campoPrecio.value = tarifa.precioDia   ?? '';
            campoCargo.value  = tarifa.cargoAtrasoDia ?? '';
            campoInicio.value = tarifa.fechaInicio ?? '';
            campoFin.value    = tarifa.fechaFin    ?? '';
        }

        dialogo.showModal();
        campoCat.focus();
    }

    /* ── Validaciones del formulario ──────────────────────────────── */
    function validarFormulario() {
        const precio = parseFloat(campoPrecio.value);
        const cargo  = parseFloat(campoCargo.value);
        const inicio = campoInicio.value;
        const fin    = campoFin.value;

        if (!campoCat.value) {
            return 'Debes seleccionar una categoría.';
        }
        if (isNaN(precio) || precio <= 0) {
            return 'El precio por día debe ser un valor mayor que cero.';
        }
        if (isNaN(cargo) || cargo < 0) {
            return 'El cargo por atraso no puede ser negativo.';
        }
        if (!inicio) {
            return 'La fecha de inicio es obligatoria.';
        }
        if (fin && fin < inicio) {
            return 'La fecha de fin no puede ser anterior a la fecha de inicio.';
        }
        return null;      // Sin errores
    }

    /* ── Envío del formulario ─────────────────────────────────────── */
    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (ocupado) return;

        const errValidacion = validarFormulario();
        if (errValidacion) {
            errorFormulario.textContent = errValidacion;
            errorFormulario.hidden      = false;
            return;
        }

        const datos = {
            categoriaId:    Number(campoCat.value),
            precioDia:      parseFloat(campoPrecio.value),
            cargoAtrasoDia: parseFloat(campoCargo.value),
            fechaInicio:    campoInicio.value,
            fechaFin:       campoFin.value || null
        };

        const editando = idEdicion !== null;
        const ruta     = editando ? `/v1/tarifas/${idEdicion}` : '/v1/tarifas';
        const metodo   = editando ? 'PUT' : 'POST';

        errorFormulario.hidden   = true;
        ocultarMensaje();
        bloquear(true);
        btnGuardar.textContent = 'Guardando…';

        try {
            await solicitar(ruta, {
                method: metodo,
                body:   JSON.stringify(datos)
            });

            dialogo.close();

            await refrescarTrasCambio(
                editando
                    ? 'Tarifa actualizada correctamente.'
                    : 'Tarifa registrada correctamente.'
            );
        } catch (error) {
            errorFormulario.textContent = mensajeError(error);
            errorFormulario.hidden      = false;
        } finally {
            btnGuardar.innerHTML = '<i class="fa-solid fa-floppy-disk" aria-hidden="true"></i> Guardar';
            bloquear(false);
        }
    });

    /* ── Desactivar tarifa (baja lógica) ──────────────────────────── */
    async function desactivarTarifa(tar) {
        if (ocupado) return;

        const confirmar = window.confirm(
            `¿Deseas desactivar la tarifa de "${tar.categoriaNombre}"?\n` +
            'Esta acción marcará la tarifa como inactiva (baja lógica).'
        );
        if (!confirmar) return;

        ocultarMensaje();
        bloquear(true);

        try {
            await solicitar(`/v1/tarifas/${tar.id}/desactivar`, { method: 'PATCH' });
            await refrescarTrasCambio('Tarifa desactivada correctamente.');
        } catch (error) {
            mostrarMensaje(mensajeError(error), 'error');
        } finally {
            bloquear(false);
        }
    }

    /* ── Listeners de controles ───────────────────────────────────── */
    btnNueva.addEventListener('click', () => abrirFormulario());
    btnRecargar.addEventListener('click', actualizarListado);

    [filtroCat, filtroEstado, filtroVigencia].forEach(sel => {
        sel.addEventListener('change', aplicarFiltros);
    });

    btnCancelar.addEventListener('click', () => dialogo.close());

    dialogo.addEventListener('cancel', evento => {
        if (ocupado) evento.preventDefault();
    });

    /* ── Verificar sesión con la API ──────────────────────────────── */
    bloquear(true);

    try {
        const resultado = await solicitar('/auth/me');
        const usuario   = resultado?.data;

        if (!resultado?.success || !usuario) {
            throw new Error('No se pudo verificar la sesión.');
        }

        // Mostrar nombre en la topbar
        $('nombre-usuario').textContent = usuario.nombre || usuario.username || 'Usuario';

        // Determinar permisos de escritura
        puedeEditar = ROLES_ESCRITURA.has(usuario.rol);

        if (!puedeEditar) {
            // Ocultar el botón "Nueva tarifa" para roles de solo lectura
            btnNueva.hidden = true;
        }
    } catch (error) {
        mostrarFila('No se pudo verificar la sesión. Recarga la página.');
        mostrarMensaje(mensajeError(error), 'error');
        return;
    }

    bloquear(false);

    // Cargar categorías en el filtro
    try {
        const res  = await solicitar('/categorias?activo=true');
        const cats = res?.data || [];
        cats.forEach(cat => {
            const opt  = document.createElement('option');
            opt.value  = cat.id;
            opt.textContent = cat.nombre;
            filtroCat.append(opt);
        });
    } catch (_) { /* silencioso; el filtro permanece con "Todas" */ }

    await actualizarListado();
});
