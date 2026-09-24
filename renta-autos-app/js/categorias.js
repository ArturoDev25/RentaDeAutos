document.addEventListener('DOMContentLoaded', async () => {
    const elemento = id => document.getElementById(id);

    const tabla = elemento('tabla-categorias');
    const filtro = elemento('filtro-estado');
    const mensaje = elemento('mensaje');
    const dialogo = elemento('dialogo-categoria');
    const formulario = elemento('formulario-categoria');
    const errorFormulario = elemento('error-formulario');
    const nueva = elemento('nueva-categoria');
    const recargar = elemento('recargar');
    const guardar = elemento('guardar');
    const cancelar = elemento('cancelar');

    const moneda = new Intl.NumberFormat('es-MX', {
        style: 'currency',
        currency: 'MXN'
    });

    let sesion;
    let idEdicion = null;
    let ocupado = false;

    try {
        sesion = JSON.parse(localStorage.getItem('usuarioSesion') || 'null');
    } catch {
        sesion = null;
    }

    if (!sesion?.token) {
        window.location.replace('login.html');
        return;
    }

    function mostrarMensaje(texto, esError = false) {
        mensaje.textContent = texto;
        mensaje.className = esError ? 'mensaje-error' : 'mensaje-exito';
        mensaje.hidden = false;
    }

    function mostrarFila(texto) {
        tabla.replaceChildren();

        const fila = document.createElement('tr');
        const celda = document.createElement('td');

        celda.colSpan = 5;
        celda.textContent = texto;
        fila.append(celda);
        tabla.append(fila);
    }

    function bloquear(valor) {
        ocupado = valor;
        nueva.disabled = valor;
        recargar.disabled = valor;
        filtro.disabled = valor;
        guardar.disabled = valor;
        cancelar.disabled = valor;

        tabla.querySelectorAll('button').forEach(boton => {
            boton.disabled = valor;
        });
    }

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

            if (error.status === 403) {
                error.message = 'No tienes permiso para realizar esta operación.';
            }

            throw error;
        }
    }

    function agregarCelda(fila, texto) {
        const celda = document.createElement('td');
        celda.textContent = texto;
        fila.append(celda);
        return celda;
    }

    function dibujarCategorias(categorias) {
        tabla.replaceChildren();

        if (categorias.length === 0) {
            mostrarFila('No hay categorías para el estado seleccionado.');
        }

        categorias.forEach(categoria => {
            const fila = document.createElement('tr');

            agregarCelda(fila, categoria.nombre);
            agregarCelda(fila, categoria.descripcion || '—');
            agregarCelda(fila, moneda.format(categoria.depositoBase));

            const celdaEstado = agregarCelda(fila, '');
            const etiqueta = document.createElement('span');
            etiqueta.className =
                `estado ${categoria.activo ? 'activa' : 'inactiva'}`;
            etiqueta.textContent = categoria.activo ? 'Activa' : 'Inactiva';
            celdaEstado.append(etiqueta);

            const celdaAcciones = agregarCelda(fila, '');
            const acciones = document.createElement('div');
            acciones.className = 'acciones';

            const editar = document.createElement('button');
            editar.type = 'button';
            editar.className = 'secundario';
            editar.textContent = 'Editar';
            editar.setAttribute('aria-label', `Editar ${categoria.nombre}`);
            editar.addEventListener('click', () => abrirFormulario(categoria));

            const estado = document.createElement('button');
            estado.type = 'button';
            estado.className = 'secundario';
            estado.textContent = categoria.activo ? 'Desactivar' : 'Activar';
            estado.setAttribute(
                'aria-label',
                `${estado.textContent} ${categoria.nombre}`
            );
            estado.addEventListener('click', () => cambiarEstado(categoria));

            acciones.append(editar, estado);
            celdaAcciones.append(acciones);
            tabla.append(fila);
        });

        elemento('total-categorias').textContent =
            `Categorías mostradas: ${categorias.length}`;
    }

    async function cargarCategorias() {
        mostrarFila('Cargando categorías…');
        elemento('total-categorias').textContent = '';

        const consulta = filtro.value ? `?activo=${filtro.value}` : '';

        try {
            const resultado = await solicitar(`/categorias${consulta}`);

            if (!resultado?.success || !Array.isArray(resultado.data)) {
                throw new Error('El servidor devolvió una respuesta inesperada.');
            }

            dibujarCategorias(resultado.data);
        } catch (error) {
            mostrarFila('No se pudo cargar el catálogo. Pulsa Actualizar.');
            throw error;
        }
    }

    async function actualizarListado() {
        if (ocupado) return;

        mensaje.hidden = true;
        bloquear(true);

        try {
            await cargarCategorias();
        } catch (error) {
            mostrarMensaje(error.message, true);
        } finally {
            bloquear(false);
        }
    }

    function abrirFormulario(categoria = null) {
        if (ocupado) return;

        formulario.reset();
        errorFormulario.hidden = true;
        elemento('nombre').setCustomValidity('');

        idEdicion = categoria?.id ?? null;
        elemento('titulo-formulario').textContent =
            categoria ? 'Editar categoría' : 'Nueva categoría';

        elemento('nombre').value = categoria?.nombre ?? '';
        elemento('descripcion').value = categoria?.descripcion ?? '';
        elemento('deposito').value = categoria?.depositoBase ?? 0;

        dialogo.showModal();
        elemento('nombre').focus();
    }

    async function refrescarTrasCambio(texto) {
        try {
            await cargarCategorias();
            mostrarMensaje(texto);
        } catch (error) {
            mostrarMensaje(
                `${texto} No se pudo actualizar la tabla: ${error.message} ` +
                'Pulsa Actualizar; no necesitas repetir el cambio.',
                true
            );
        }
    }

    async function cambiarEstado(categoria) {
        if (ocupado) return;

        const accion = categoria.activo ? 'desactivar' : 'activar';

        if (!window.confirm(
            `¿Deseas ${accion} la categoría "${categoria.nombre}"?`
        )) {
            return;
        }

        bloquear(true);
        mensaje.hidden = true;

        try {
            await solicitar(`/categorias/${categoria.id}/estado`, {
                method: 'PATCH',
                body: JSON.stringify({ activo: !categoria.activo })
            });

            await refrescarTrasCambio('Estado actualizado correctamente.');
        } catch (error) {
            mostrarMensaje(error.message, true);
        } finally {
            bloquear(false);
        }
    }

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (ocupado) return;

        const nombre = elemento('nombre');
        nombre.value = nombre.value.trim();
        nombre.setCustomValidity(
            nombre.value ? '' : 'Escribe un nombre; no puede contener solo espacios.'
        );

        if (!formulario.reportValidity()) return;

        const datos = {
            nombre: nombre.value,
            descripcion: elemento('descripcion').value.trim() || null,
            depositoBase: Number(elemento('deposito').value)
        };

        const editando = idEdicion !== null;
        const ruta = editando ? `/categorias/${idEdicion}` : '/categorias';

        errorFormulario.hidden = true;
        mensaje.hidden = true;
        bloquear(true);
        guardar.textContent = 'Guardando…';

        try {
            await solicitar(ruta, {
                method: editando ? 'PUT' : 'POST',
                body: JSON.stringify(datos)
            });

            dialogo.close();

            await refrescarTrasCambio(
                editando
                    ? 'Categoría actualizada correctamente.'
                    : 'Categoría registrada correctamente.'
            );
        } catch (error) {
            errorFormulario.textContent = error.message;
            errorFormulario.hidden = false;
        } finally {
            guardar.textContent = 'Guardar';
            bloquear(false);
        }
    });

    elemento('nombre').addEventListener('input', () => {
        elemento('nombre').setCustomValidity('');
    });

    cancelar.addEventListener('click', () => dialogo.close());

    dialogo.addEventListener('cancel', evento => {
        if (ocupado) evento.preventDefault();
    });

    nueva.addEventListener('click', () => abrirFormulario());
    recargar.addEventListener('click', actualizarListado);
    filtro.addEventListener('change', actualizarListado);

    // Verificar la identidad con la API, no solo con localStorage.
    bloquear(true);

    try {
        const resultado = await solicitar('/auth/me');
        const usuario = resultado?.data;

        if (!resultado?.success || !usuario) {
            throw new Error('No se pudo verificar la sesión.');
        }

        if (usuario.rol !== 'ADMINISTRADOR') {
            mostrarFila('Este apartado está disponible para el administrador.');
            mostrarMensaje('Tu cuenta no tiene acceso a este panel.', true);
            return;
        }

        elemento('nombre-usuario').textContent = usuario.nombre;
    } catch (error) {
        mostrarFila('No se pudo verificar la sesión. Recarga la página.');
        mostrarMensaje(error.message, true);
        return;
    }

    bloquear(false);
    await actualizarListado();
});