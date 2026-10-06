(() => {
  const form = document.getElementById('form-reservacion');
  const filas = document.getElementById('filas');
  const mensaje = document.getElementById('mensaje');
  const guardar = document.getElementById('guardar');
  const limpiar = document.getElementById('limpiar');
  let edicion = null;
  let registros = [];
  const nombresClientes = new Map();
  const nombresVehiculos = new Map();
  let sesion;

  try { sesion = JSON.parse(localStorage.getItem('usuarioSesion')); } catch (_) { /* sesión inválida */ }
  if (!sesion?.token || sesion.rol !== 'ADMINISTRADOR') {
    window.location.replace('login.html');
    return;
  }
  document.getElementById('operador').textContent = sesion.nombre;
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

  async function cargarOpciones() {
    const { clientes, vehiculos } = (await peticion('/reservaciones/opciones')).data;
    for (const [campo, registros, nombres] of [
      ['clienteId', clientes, nombresClientes], ['vehiculoId', vehiculos, nombresVehiculos]
    ]) {
      const select = form.elements[campo];
      select.length = 1;
      registros.forEach(item => {
        nombres.set(item.id, item.nombre);
        select.add(new Option(item.nombre, item.id));
      });
    }
    // S3-14: los vehículos RENTADOS no vienen en /opciones; se toman del catálogo
    // para que la tabla muestre su nombre en las rentas EN_CURSO.
    try {
      (await peticion('/v1/vehiculos')).data.forEach(v => {
        if (!nombresVehiculos.has(v.id)) nombresVehiculos.set(v.id, `${v.marca} ${v.modelo} · ${v.placa}`);
      });
    } catch (_) { /* si falla, se muestra el ID como antes */ }
    pintar();
    if (!clientes.length || !vehiculos.length) aviso('Registra primero un cliente activo y un vehículo disponible.');
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
      if (['PENDIENTE', 'CONFIRMADA'].includes(r.estado)) {
        const editar = document.createElement('button'); editar.textContent = 'Editar'; editar.className = 'secondary';
        editar.addEventListener('click', () => comenzarEdicion(r)); celda.append(editar);
        const cancelar = document.createElement('button'); cancelar.textContent = 'Cancelar';
        cancelar.addEventListener('click', () => cancelarReservacion(r)); celda.append(cancelar);
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
    });
  }

  async function cargar() {
    try {
      registros = (await peticion('/reservaciones')).data;
      pintar();
    } catch (error) { aviso(error.status === 401 ? 'Sesión expirada. Vuelve a iniciar sesión.' : error.message); }
  }

  function resetear() {
    edicion = null; form.reset();
    guardar.textContent = 'Crear reservación'; limpiar.hidden = true;
    document.getElementById('form-titulo').textContent = 'Nueva reservación';
  }
  function comenzarEdicion(r) {
    edicion = r.id;
    ['clienteId', 'vehiculoId', 'observaciones'].forEach(campo => { form.elements[campo].value = r[campo] ?? ''; });
    ['fechaInicio', 'fechaFin'].forEach(campo => { form.elements[campo].value = r[campo].slice(0, 16); });
    guardar.textContent = 'Guardar cambios'; limpiar.hidden = false;
    document.getElementById('form-titulo').textContent = `Editar reservación #${r.id}`;
    form.scrollIntoView({ behavior: 'smooth' });
  }
  async function cancelarReservacion(r) {
    if (!window.confirm(`¿Cancelar la reservación #${r.id}?`)) return;
    try {
      await peticion(`/reservaciones/${r.id}/cancelar`, { method: 'PATCH' });
      if (edicion === r.id) resetear();
      await cargar(); aviso(`Reservación #${r.id} cancelada.`, 'ok');
    } catch (error) { aviso(error.message); }
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const datos = Object.fromEntries(new FormData(form));
    if (datos.fechaFin <= datos.fechaInicio) { aviso('La devolución debe ser posterior al inicio (RN-02).'); return; }
    datos.clienteId = Number(datos.clienteId);
    datos.vehiculoId = Number(datos.vehiculoId);
    guardar.disabled = true;
    try {
      const id = edicion;
      await peticion(id ? `/reservaciones/${id}` : '/reservaciones', {
        method: id ? 'PUT' : 'POST', body: JSON.stringify(datos)
      });
      resetear(); await cargar(); aviso(id ? 'Reservación actualizada.' : 'Reservación creada.', 'ok');
    } catch (error) { aviso(error.message); }
    finally { guardar.disabled = false; }
  });
  limpiar.addEventListener('click', resetear);
  document.getElementById('recargar').addEventListener('click', cargar);
  Promise.all([cargarOpciones(), cargar()]).catch(error => aviso(error.message));
})();
