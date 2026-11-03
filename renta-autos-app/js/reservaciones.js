(() => {
  const form = document.getElementById('form-reservacion');
  const filas = document.getElementById('filas');
  const mensaje = document.getElementById('mensaje');
  const guardar = document.getElementById('guardar');
  const limpiar = document.getElementById('limpiar');
  const recargar = document.getElementById('recargar');
  const buscarInput = document.getElementById('buscarReserva');
  const filtroEstado = document.getElementById('filtroEstado');
  const conteoReservas = document.getElementById('conteoReservas');
  const normalizar = s => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
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
  let vehiculosTodos = [];
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

  // ¿El rango [aIni,aFin) se traslapa con [bIni,bFin)?
  function seTraslapa(aIni, aFin, bIni, bFin) {
    return aIni < bFin && bIni < aFin;
  }

  // Un vehículo está libre en [ini,fin] si ninguna reserva activa se traslapa
  // (se ignora la reservación que se está editando).
  function vehiculoLibreEn(vehiculoId, ini, fin) {
    return registros.every(r =>
      r.vehiculoId !== vehiculoId ||
      r.id === edicion ||
      !['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'].includes(r.estado) ||
      !seTraslapa(ini, fin, new Date(r.fechaInicio), new Date(r.fechaFin))
    );
  }

  // Arma el dropdown de vehículo según las fechas: solo los libres para ese rango
  // (ignora mantenimiento/baja). Sin fechas válidas, muestra una pista.
  function actualizarVehiculosDisponibles() {
    const select = form.elements.vehiculoId;
    if (!select) return;
    const previo = select.value;
    const iniStr = form.elements.fechaInicio.value;
    const finStr = form.elements.fechaFin.value;
    select.replaceChildren();
    const ini = iniStr ? new Date(iniStr) : null;
    const fin = finStr ? new Date(finStr) : null;
    if (!ini || !fin || Number.isNaN(ini.getTime()) || Number.isNaN(fin.getTime()) || fin <= ini) {
      select.add(new Option('Primero elige inicio y devolución', ''));
      return;
    }
    const libres = vehiculosTodos
      .filter(v => v.estado !== 'MANTENIMIENTO' && v.estado !== 'BAJA')
      .filter(v => vehiculoLibreEn(v.id, ini, fin))
      .map(v => ({ id: v.id, nombre: nombresVehiculos.get(v.id) || ('#' + v.id) }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    if (!libres.length) {
      select.add(new Option('No hay vehículos disponibles para esas fechas', ''));
      return;
    }
    select.add(new Option('Selecciona un vehículo', ''));
    libres.forEach(v => select.add(new Option(v.nombre, v.id)));
    if (previo && [...select.options].some(o => o.value === previo)) select.value = previo;
  }

  async function cargarOpciones() {
    const { clientes, vehiculos } = (await peticion('/reservaciones/opciones')).data;
    // Clientes: se cargan en su select como siempre.
    const selectCliente = form.elements.clienteId;
    selectCliente.length = 1;
    nombresClientes.clear();
    clientes.forEach(item => {
      nombresClientes.set(item.id, item.nombre);
      selectCliente.add(new Option(item.nombre, item.id));
    });
    // Nombres de vehículos (para la tabla) desde /opciones.
    nombresVehiculos.clear();
    vehiculos.forEach(item => nombresVehiculos.set(item.id, item.nombre));
    // Lista COMPLETA de vehículos (con estado) para filtrar el dropdown por fechas.
    try {
      const todos = (await peticion('/v1/vehiculos')).data;
      vehiculosTodos = todos.map(v => ({ id: v.id, estado: v.estado }));
      todos.forEach(v => {
        if (!nombresVehiculos.has(v.id)) nombresVehiculos.set(v.id, `${v.marca} ${v.modelo} · ${v.placa}`);
      });
    } catch (_) {
      vehiculosTodos = vehiculos.map(v => ({ id: v.id, estado: 'DISPONIBLE' }));
    }
    // Nombres de TODOS los clientes (incluye inactivos). El select de /opciones solo
    // trae clientes activos; esto evita que al editar una reserva de un cliente
    // inactivo el formulario muestre a otro cliente por error.
    try {
      (await peticion('/v1/clientes')).data.forEach(c => {
        if (!nombresClientes.has(c.id)) nombresClientes.set(c.id, `${c.nombre} ${c.apellidos}`);
      });
    } catch (_) { /* los nombres extra son opcionales */ }
    // El dropdown de vehículo se arma según las fechas elegidas (S5: disponibilidad por fecha).
    actualizarVehiculosDisponibles();
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
    const texto = normalizar(buscarInput ? buscarInput.value.trim() : '');
    const estado = filtroEstado ? filtroEstado.value : '';
    const lista = registros.filter(r => {
      if (estado && r.estado !== estado) return false;
      if (!texto) return true;
      const cliente = normalizar(nombresClientes.get(r.clienteId) || '');
      const vehiculo = normalizar(nombresVehiculos.get(r.vehiculoId) || '');
      return String(r.id).includes(texto) || cliente.includes(texto) || vehiculo.includes(texto);
    });
    if (conteoReservas) {
      conteoReservas.textContent = registros.length ? `${lista.length} de ${registros.length}` : '';
    }
    if (!lista.length) {
      const tr = filas.insertRow();
      const td = tr.insertCell(); td.colSpan = 9;
      td.textContent = registros.length
        ? 'No hay reservaciones que coincidan con la búsqueda.'
        : 'No hay reservaciones registradas.';
      return;
    }
    lista.forEach(r => {
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
      // S3-14: la entrega solo se inicia desde una reservación CONFIRMADA.
      if (r.estado === 'CONFIRMADA') {
        const entregar = document.createElement('a'); entregar.textContent = 'Entregar'; entregar.className = 'accion-entregar';
        entregar.href = `entrega.html?reservacion=${r.id}`; celda.append(entregar);
      }
      if (['EN_CURSO', 'FINALIZADA'].includes(r.estado)) {
        const ver = document.createElement('a'); ver.textContent = 'Ver entrega'; ver.className = 'accion-entregar secondary';
        ver.href = `entrega.html?reservacion=${r.id}`; celda.append(ver);
      }
      // S3-15: la devolución solo se registra sobre una renta EN_CURSO (el Auditor solo consulta).
      if (puedeOperar && r.estado === 'EN_CURSO') {
        const devolver = document.createElement('a'); devolver.textContent = 'Devolver'; devolver.className = 'accion-entregar accion-devolver';
        devolver.href = `devolucion.html?reservacion=${r.id}`; celda.append(devolver);
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
    actualizarVehiculosDisponibles();
    guardar.textContent = 'Crear reservación'; limpiar.hidden = true;
    document.getElementById('form-titulo').textContent = 'Nueva reservación';
  }

  // Garantiza que un valor exista como opción del select aunque no esté en la lista
  // vigente (cliente inactivo, vehículo en mantenimiento, etc.).
  function asegurarOpcion(select, id, nombre) {
    if (!select || id == null || id === '') return;
    const existe = [...select.options].some(o => String(o.value) === String(id));
    if (!existe) select.add(new Option(nombre || ('#' + id), id));
  }

  function comenzarEdicion(r) {
    if (ocupado || !puedeOperar) return;
    edicion = r.id;
    // El cliente real puede estar inactivo y no venir en el dropdown: se asegura su
    // opción para mostrar al cliente correcto y no cambiarlo sin querer al guardar.
    asegurarOpcion(form.elements.clienteId, r.clienteId, nombresClientes.get(r.clienteId));
    form.elements.clienteId.value = r.clienteId ?? '';
    form.elements.observaciones.value = r.observaciones ?? '';
    ['fechaInicio', 'fechaFin'].forEach(campo => { form.elements[campo].value = r[campo].slice(0, 16); });
    // Arma el dropdown con las fechas de la reserva (su propio auto queda libre) y lo selecciona.
    actualizarVehiculosDisponibles();
    asegurarOpcion(form.elements.vehiculoId, r.vehiculoId, nombresVehiculos.get(r.vehiculoId));
    form.elements.vehiculoId.value = r.vehiculoId ?? '';
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
  if (buscarInput) buscarInput.addEventListener('input', pintar);
  if (filtroEstado) filtroEstado.addEventListener('change', pintar);
  ['fechaInicio', 'fechaFin'].forEach(campo => {
    const input = form.elements[campo];
    if (input) input.addEventListener('change', actualizarVehiculosDisponibles);
  });
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
