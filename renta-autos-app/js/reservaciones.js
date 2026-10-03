(() => {
  const form = document.getElementById('form-reservacion');
  const filas = document.getElementById('filas');
  const mensaje = document.getElementById('mensaje');
  const guardar = document.getElementById('guardar');
  const limpiar = document.getElementById('limpiar');
  const recargar = document.getElementById('recargar');
  const panelFormulario = form.closest('section');
  const rolesOperacion = ['ADMINISTRADOR', 'AGENTE', 'SUPERVISOR'];
  const rolesConsulta = [...rolesOperacion, 'AUDITOR'];
  let edicion = null;
  let registros = [];
  let ocupado = true;
  let puedeOperar = false;
  let autorizado = false;
  const nombresClientes = new Map();
  const nombresVehiculos = new Map();
  let sesion;

  // No habilitar operaciones hasta verificar la identidad con el servidor.
  panelFormulario.hidden = true;
  actualizarControles();
  try { sesion = JSON.parse(localStorage.getItem('usuarioSesion')); } catch (_) { /* Sesión inválida. */ }
  if (!sesion?.token) {
    window.location.replace('login.html');
    return;
  }
  document.getElementById('salir').addEventListener('click', () => {
    localStorage.removeItem('usuarioSesion');
    window.location.replace('login.html');
  });

  const aviso = (texto, tipo = 'error') => {
    mensaje.textContent = texto;
    mensaje.className = tipo;
  };
  const fecha = valor => valor ? valor.replace('T', ' ').slice(0, 16) : '';
  const moneda = valor => new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(valor);
  const peticion = (ruta, opciones = {}) => fetchAPI(ruta, {
    ...opciones,
    headers: { Authorization: `Bearer ${sesion.token}`, ...opciones.headers }
  });

  function textoError(error) {
    if (error.status === 401) {
      autorizado = false;
      puedeOperar = false;
      panelFormulario.hidden = true;
      localStorage.removeItem('usuarioSesion');
      window.location.replace('login.html');
      return 'Sesión expirada. Vuelve a iniciar sesión.';
    }
    if (error.status === 403) return 'No tienes permiso para realizar esta operación.';
    // Conservar el mensaje del backend: un 409 puede ser traslape o estado inválido.
    return error.message || 'No se pudo completar la operación. Intenta actualizar la lista.';
  }

  function actualizarControles() {
    form.querySelectorAll('input, select, textarea, button').forEach(control => {
      control.disabled = ocupado || !puedeOperar;
    });
    filas.querySelectorAll('button').forEach(boton => {
      boton.disabled = ocupado || !puedeOperar;
    });
    recargar.disabled = ocupado || !autorizado;
    filas.setAttribute('aria-busy', String(ocupado));
  }

  function bloquear(valor) {
    ocupado = valor;
    actualizarControles();
  }

  async function cargarOpciones() {
    const { clientes, vehiculos } = (await peticion('/reservaciones/opciones')).data;
    for (const [campo, opciones, nombres] of [
      ['clienteId', clientes, nombresClientes], ['vehiculoId', vehiculos, nombresVehiculos]
    ]) {
      const select = form.elements[campo];
      select.length = 1;
      nombres.clear();
      opciones.forEach(item => {
        nombres.set(item.id, item.nombre);
        select.add(new Option(item.nombre, item.id));
      });
    }
    pintar();
  }

  function botonAccion(celda, texto, accion, clase = '') {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.textContent = texto;
    boton.className = clase;
    boton.disabled = ocupado || !puedeOperar;
    boton.addEventListener('click', accion);
    celda.append(boton);
    return boton;
  }

  function pintar() {
    filas.replaceChildren();
    if (!registros.length) {
      const tr = filas.insertRow();
      const td = tr.insertCell(); td.colSpan = 9; td.textContent = 'No hay reservaciones registradas.';
      return;
    }
    registros.forEach(r => {
      const tr = filas.insertRow();
      [r.id, nombresClientes.get(r.clienteId) || r.clienteId,
        nombresVehiculos.get(r.vehiculoId) || r.vehiculoId, fecha(r.fechaInicio), fecha(r.fechaFin),
        r.estado, moneda(r.tarifaDia), moneda(r.totalEstimado)].forEach(valor => {
        tr.insertCell().textContent = valor;
      });
      const celda = tr.insertCell();
      if (puedeOperar && r.estado === 'PENDIENTE') {
        const confirmar = botonAccion(celda, 'Confirmar', () => cambiarEstado(r, 'confirmar'));
        confirmar.setAttribute('aria-label', `Confirmar reservación #${r.id}`);
      }
      if (puedeOperar && ['PENDIENTE', 'CONFIRMADA'].includes(r.estado)) {
        botonAccion(celda, 'Editar', () => comenzarEdicion(r), 'secondary');
        botonAccion(celda, 'Cancelar', () => cambiarEstado(r, 'cancelar'), 'secondary');
      }
      if (!celda.hasChildNodes()) celda.textContent = puedeOperar ? 'Sin acciones disponibles' : 'Solo consulta';
    });
  }

  async function cargar() {
    registros = (await peticion('/reservaciones')).data;
    pintar();
  }

  function resetear() {
    edicion = null; form.reset();
    guardar.textContent = 'Crear reservación'; limpiar.hidden = true;
    document.getElementById('form-titulo').textContent = 'Nueva reservación';
  }

  function comenzarEdicion(r) {
    if (ocupado || !puedeOperar) return;
    edicion = r.id;
    ['clienteId', 'vehiculoId', 'observaciones'].forEach(campo => { form.elements[campo].value = r[campo] ?? ''; });
    ['fechaInicio', 'fechaFin'].forEach(campo => { form.elements[campo].value = r[campo].slice(0, 16); });
    guardar.textContent = 'Guardar cambios'; limpiar.hidden = false;
    document.getElementById('form-titulo').textContent = `Editar reservación #${r.id}`;
    form.scrollIntoView({ behavior: 'smooth' });
  }

  async function cambiarEstado(r, accion) {
    if (ocupado || !puedeOperar) return;
    const esConfirmacion = accion === 'confirmar';
    if (esConfirmacion && r.estado !== 'PENDIENTE') {
      aviso('Solo se pueden confirmar reservaciones pendientes. Actualiza la lista.');
      return;
    }
    const detalle = esConfirmacion
      ? `\nVehículo: ${nombresVehiculos.get(r.vehiculoId) || r.vehiculoId}\nInicio: ${fecha(r.fechaInicio)}\nDevolución: ${fecha(r.fechaFin)}`
      : '';
    const edicionPendiente = edicion === r.id
      ? '\nSe utilizarán los datos guardados. Los cambios del formulario sin guardar no se aplicarán.'
      : '';
    if (!window.confirm(`¿${esConfirmacion ? 'Confirmar' : 'Cancelar'} la reservación #${r.id}?${detalle}${edicionPendiente}`)) return;

    bloquear(true);
    aviso(`${esConfirmacion ? 'Confirmando' : 'Cancelando'} reservación #${r.id}…`, 'ok');
    try {
      // El POST revalida estado, vehículo y traslapes dentro de la transacción del backend.
      const respuesta = await peticion(`/reservaciones/${r.id}/${accion}`, {
        method: esConfirmacion ? 'POST' : 'PATCH'
      });
      if (!respuesta?.data || respuesta.data.id !== r.id) {
        throw new Error('La respuesta del servidor no permitió verificar el resultado. Actualiza la lista antes de reintentar.');
      }
      // Aplicar la respuesta real, incluso si después falla la recarga del listado.
      registros = registros.map(item => item.id === r.id ? respuesta.data : item);
      if (edicion === r.id) resetear();
      pintar();
      const exito = `Reservación #${r.id} ${esConfirmacion ? 'confirmada' : 'cancelada'} correctamente.`;
      try {
        await cargar();
        aviso(exito, 'ok');
      } catch (error) {
        aviso(`${exito} No se pudo actualizar la lista completa. ${textoError(error)}`);
      }
    } catch (error) {
      let texto = textoError(error);
      // Recuperar un estado cambiado por otro usuario o por una respuesta perdida.
      if (error.status !== 401 && error.status !== 403) {
        try { await cargar(); }
        catch (errorRecarga) {
          texto += ` No se pudo actualizar la lista. ${textoError(errorRecarga)}`;
        }
      }
      aviso(texto);
    } finally {
      bloquear(false);
    }
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (ocupado || !puedeOperar || !form.reportValidity()) return;
    const datos = Object.fromEntries(new FormData(form));
    if (datos.fechaFin <= datos.fechaInicio) { aviso('La devolución debe ser posterior al inicio (RN-02).'); return; }
    datos.clienteId = Number(datos.clienteId);
    datos.vehiculoId = Number(datos.vehiculoId);
    bloquear(true);
    try {
      const id = edicion;
      await peticion(id ? `/reservaciones/${id}` : '/reservaciones', {
        method: id ? 'PUT' : 'POST', body: JSON.stringify(datos)
      });
      resetear();
      const exito = id ? 'Reservación actualizada.' : 'Reservación creada.';
      try { await cargar(); aviso(exito, 'ok'); }
      catch (error) { aviso(`${exito} No se pudo actualizar la lista. ${textoError(error)}`); }
    } catch (error) { aviso(textoError(error)); }
    finally { bloquear(false); }
  });
  limpiar.addEventListener('click', () => { if (!ocupado && puedeOperar) resetear(); });
  recargar.addEventListener('click', async () => {
    if (ocupado || !autorizado) return;
    bloquear(true);
    try { await cargar(); aviso('Lista actualizada.', 'ok'); }
    catch (error) { aviso(textoError(error)); }
    finally { bloquear(false); }
  });

  async function iniciar() {
    try {
      const usuario = (await peticion('/auth/me')).data;
      if (!usuario || !rolesConsulta.includes(usuario.rol)) {
        aviso('No tienes permiso para consultar reservaciones.');
        return;
      }
      autorizado = true;
      puedeOperar = rolesOperacion.includes(usuario.rol);
      document.getElementById('operador').textContent = usuario.nombre || 'Usuario';
      const rol = document.querySelector('[data-panel-rol]');
      if (rol) rol.textContent = usuario.rol.charAt(0) + usuario.rol.slice(1).toLowerCase();
      panelFormulario.hidden = !puedeOperar;
      const descripcion = document.querySelector('.dashboard-title p');
      if (descripcion) descripcion.textContent = puedeOperar
        ? 'Gestiona y confirma las reservaciones del sistema'
        : 'Consulta las reservaciones del sistema';
      // Secuencial para no repintar datos antiguos mientras se realiza una operación.
      await cargarOpciones();
      await cargar();
      if (puedeOperar && (!nombresClientes.size || !nombresVehiculos.size)) {
        aviso('Para crear una reservación necesitas un cliente activo y un vehículo apto para reservarse.');
      }
    } catch (error) { aviso(textoError(error)); }
    finally { bloquear(false); }
  }
  iniciar();
})();
