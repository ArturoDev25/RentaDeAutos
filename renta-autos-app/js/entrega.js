// S3-14 — Pantalla y flujo de entrega de vehículo.
// Consume: GET /reservaciones, GET /reservaciones/{id}, GET /reservaciones/opciones,
//          GET /v1/vehiculos/{id}, GET /v1/clientes/{id},
//          GET /v1/entregas/reservacion/{id}, POST /v1/entregas (S3-09).
(() => {
  const $ = id => document.getElementById(id);
  const mensaje = $('mensaje');
  const panelSeleccion = $('panel-seleccion');
  const panelResumen = $('panel-resumen');
  const panelForm = $('panel-form');
  const panelResultado = $('panel-resultado');
  const formSeleccion = $('form-seleccion');
  const form = $('form-entrega');
  const registrar = $('registrar');

  const ESTADOS_VEHICULO_BLOQUEADOS = ['RENTADO', 'MANTENIMIENTO', 'BAJA'];
  const MOTIVO_POR_ESTADO = {
    PENDIENTE: 'La reservación aún está PENDIENTE. Primero debe confirmarse.',
    CANCELADA: 'La reservación fue CANCELADA.',
    EN_CURSO: 'La reservación ya está EN_CURSO: el vehículo ya fue entregado.',
    FINALIZADA: 'La reservación ya está FINALIZADA.'
  };

  let sesion;
  let reservacion = null;
  let vehiculo = null;

  // ---------- Sesión (mismo criterio que reservaciones.js) ----------
  try { sesion = JSON.parse(localStorage.getItem('usuarioSesion')); } catch (_) { /* sesión inválida */ }
  if (!sesion?.token || sesion.rol !== 'ADMINISTRADOR') {
    window.location.replace('login.html');
    return;
  }
  $('operador').textContent = sesion.nombre;
  $('salir').addEventListener('click', () => {
    localStorage.removeItem('usuarioSesion');
    window.location.replace('login.html');
  });

  // ---------- Utilidades ----------
  const peticion = (ruta, opciones = {}) => fetchAPI(ruta, {
    ...opciones,
    headers: { Authorization: `Bearer ${sesion.token}`, ...opciones.headers }
  });
  const aviso = (texto, tipo = 'error') => { mensaje.textContent = texto; mensaje.className = tipo; };
  const limpiarAviso = () => { mensaje.textContent = ''; mensaje.className = ''; };
  const fecha = valor => valor ? valor.replace('T', ' ').slice(0, 16) : '—';
  const km = valor => `${new Intl.NumberFormat('es-MX', { maximumFractionDigits: 1 }).format(valor)} km`;
  const hoyISO = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  const badge = (elemento, estado) => { elemento.textContent = estado || '—'; elemento.dataset.estado = estado || ''; };
  const textoError = error => {
    if (error.status === 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    if (error.status === 403) return 'Tu rol no tiene permiso para registrar entregas.';
    return error.message;
  };
  const describirCombustible = valor => {
    if (valor >= 100) return 'Tanque lleno';
    if (valor >= 75) return 'Aprox. 3/4 de tanque';
    if (valor >= 50) return 'Aprox. 1/2 tanque';
    if (valor >= 25) return 'Aprox. 1/4 de tanque';
    if (valor > 0) return 'Reserva';
    return 'Vacío';
  };

  // ---------- Paso 1: elegir reservación ----------
  async function mostrarSeleccion() {
    panelSeleccion.hidden = false;
    const select = formSeleccion.elements.reservacionId;
    try {
      const [{ data: lista }, { data: opciones }] = await Promise.all([
        peticion('/reservaciones'), peticion('/reservaciones/opciones')
      ]);
      const clientes = new Map(opciones.clientes.map(c => [c.id, c.nombre]));
      const vehiculos = new Map(opciones.vehiculos.map(v => [v.id, v.nombre]));
      const confirmadas = lista.filter(r => r.estado === 'CONFIRMADA');
      select.length = 0;
      select.add(new Option(confirmadas.length ? 'Selecciona una reservación' : 'No hay reservaciones confirmadas', ''));
      confirmadas.forEach(r => select.add(new Option(
        `#${r.id} · ${clientes.get(r.clienteId) || `Cliente ${r.clienteId}`} · ${vehiculos.get(r.vehiculoId) || `Vehículo ${r.vehiculoId}`} · ${fecha(r.fechaInicio)}`,
        r.id)));
      if (!confirmadas.length) aviso('No hay reservaciones CONFIRMADAS para entregar.');
    } catch (error) { aviso(textoError(error)); }
  }

  formSeleccion.addEventListener('submit', event => {
    event.preventDefault();
    const id = formSeleccion.elements.reservacionId.value;
    if (!id) { aviso('Selecciona una reservación.'); return; }
    window.location.search = `?reservacion=${encodeURIComponent(id)}`;
  });

  // ---------- Paso 2: cargar y validar la reservación ----------
  async function cargarReservacion(id) {
    panelResumen.hidden = false;
    $('r-id').textContent = `#${id}`;
    try {
      reservacion = (await peticion(`/reservaciones/${id}`)).data;
    } catch (error) {
      bloquear(error.status === 404 ? `La reservación #${id} no existe.` : textoError(error));
      return;
    }

    const [vehiculoRes, clienteRes, entregaRes] = await Promise.allSettled([
      peticion(`/v1/vehiculos/${reservacion.vehiculoId}`),
      peticion(`/v1/clientes/${reservacion.clienteId}`),
      peticion(`/v1/entregas/reservacion/${reservacion.id}`)
    ]);
    vehiculo = vehiculoRes.status === 'fulfilled' ? vehiculoRes.value.data : null;
    const cliente = clienteRes.status === 'fulfilled' ? clienteRes.value.data : null;
    const entrega = entregaRes.status === 'fulfilled' ? entregaRes.value.data : null;

    $('r-cliente').textContent = cliente ? `${cliente.nombre} ${cliente.apellidos}` : `Cliente #${reservacion.clienteId}`;
    $('r-vehiculo').textContent = vehiculo
      ? `${vehiculo.marca} ${vehiculo.modelo} ${vehiculo.anio ?? ''} · ${vehiculo.placa}`
      : `Vehículo #${reservacion.vehiculoId}`;
    $('r-periodo').textContent = `${fecha(reservacion.fechaInicio)} → ${fecha(reservacion.fechaFin)}`;
    badge($('r-estado'), reservacion.estado);
    badge($('r-estado-vehiculo'), vehiculo?.estado);
    $('r-km').textContent = vehiculo ? km(vehiculo.kilometraje) : '—';

    // Si ya existe una entrega, se muestra en modo consulta.
    if (entrega) {
      bloquear('Esta reservación ya tiene una entrega registrada.');
      mostrarResultado(entrega, 'Entrega registrada');
      return;
    }
    if (entregaRes.status === 'rejected' && entregaRes.reason.status !== 404) {
      bloquear(textoError(entregaRes.reason));
      return;
    }

    const motivo = motivoBloqueo(reservacion, vehiculo, cliente);
    if (motivo) { bloquear(motivo); return; }

    prepararFormulario();
  }

  // Validaciones previas: reflejan las reglas del backend (S3-09) para no abrir
  // el formulario si la entrega va a ser rechazada. El backend sigue validando.
  function motivoBloqueo(r, v, c) {
    if (r.estado !== 'CONFIRMADA') return MOTIVO_POR_ESTADO[r.estado] || `La reservación está en estado ${r.estado}.`;
    if (new Date(r.fechaFin) < new Date()) return 'La reservación ya venció (su fecha de devolución ya pasó).';
    if (hoyISO() < r.fechaInicio.slice(0, 10)) return `La entrega se puede registrar a partir del ${r.fechaInicio.slice(0, 10)}.`;
    if (!v) return 'No se pudo consultar el vehículo de la reservación.';
    if (ESTADOS_VEHICULO_BLOQUEADOS.includes(v.estado)) return `El vehículo está en estado ${v.estado} y no puede entregarse.`;
    if (c && c.activo === false) return 'El cliente de la reservación está inactivo.';
    if (c?.licenciaVencimiento && c.licenciaVencimiento < hoyISO()) return 'La licencia del cliente está vencida.';
    return null;
  }

  function bloquear(texto) {
    $('bloqueo-texto').textContent = texto;
    $('bloqueo').hidden = false;
    panelForm.hidden = true;
  }

  // ---------- Paso 3: formulario ----------
  function prepararFormulario() {
    const kmActual = Number(vehiculo.kilometraje) || 0;
    const campoKm = form.elements.kilometrajeSalida;
    campoKm.min = kmActual;
    campoKm.value = kmActual;
    $('km-ayuda').textContent = `Debe ser mayor o igual a ${km(kmActual)} (último registrado).`;
    panelForm.hidden = false;
    campoKm.focus();
  }

  // Sincroniza el control deslizante con el campo numérico de combustible.
  const rango = form.elements.combustibleRango;
  const combustible = form.elements.combustibleSalida;
  const actualizarCombustible = () => { $('comb-ayuda').textContent = describirCombustible(Number(combustible.value)); };
  rango.addEventListener('input', () => { combustible.value = rango.value; actualizarCombustible(); validarCampo('combustibleSalida'); });
  combustible.addEventListener('input', () => { rango.value = combustible.value; actualizarCombustible(); });

  const detalle = form.elements.condicionDetalle;
  detalle.addEventListener('input', () => { $('cond-contador').textContent = `${detalle.value.length} / 1900`; });

  const reglas = {
    kilometrajeSalida(valor) {
      if (valor === '') return 'El kilometraje de salida es obligatorio.';
      const n = Number(valor);
      if (!Number.isFinite(n) || n < 0) return 'Ingresa un kilometraje válido (0 o mayor).';
      if (!/^\d{1,9}(\.\d)?$/.test(valor)) return 'Usa hasta 9 enteros y 1 decimal.';
      const minimo = Number(vehiculo?.kilometraje) || 0;
      if (n < minimo) return `No puede ser menor al kilometraje actual del vehículo (${km(minimo)}).`;
      return '';
    },
    combustibleSalida(valor) {
      if (valor === '') return 'El nivel de combustible es obligatorio.';
      const n = Number(valor);
      if (!Number.isFinite(n) || n < 0 || n > 100) return 'El combustible debe estar entre 0 y 100 %.';
      if (!/^\d{1,3}(\.\d{1,2})?$/.test(valor)) return 'Usa hasta 2 decimales.';
      return '';
    },
    condicionGeneral(valor) { return valor ? '' : 'Selecciona la condición general del vehículo.'; },
    condicionDetalle(valor) {
      const texto = valor.trim();
      if (!texto) return 'Describe la condición del vehículo.';
      if (texto.length < 5) return 'Describe la condición con un poco más de detalle.';
      if (texto.length > 1900) return 'Máximo 1900 caracteres.';
      return '';
    },
    observaciones(valor) { return valor.length > 2000 ? 'Máximo 2000 caracteres.' : ''; },
    revisado() { return form.elements.revisado.checked ? '' : 'Confirma que revisaste el vehículo con el cliente.'; }
  };

  function validarCampo(nombre) {
    const campo = form.elements[nombre];
    const error = reglas[nombre](campo.type === 'checkbox' ? campo.checked : campo.value.trim());
    const salida = form.querySelector(`[data-error-de="${nombre}"]`);
    if (salida) salida.textContent = error;
    campo.setAttribute('aria-invalid', error ? 'true' : 'false');
    return !error;
  }

  Object.keys(reglas).forEach(nombre => {
    const campo = form.elements[nombre];
    campo.addEventListener(campo.type === 'checkbox' || campo.tagName === 'SELECT' ? 'change' : 'blur', () => validarCampo(nombre));
    // Mientras se corrige un campo marcado, el error se actualiza al escribir.
    campo.addEventListener('input', () => { if (campo.getAttribute('aria-invalid') === 'true') validarCampo(nombre); });
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    limpiarAviso();
    const invalidos = Object.keys(reglas).filter(nombre => !validarCampo(nombre));
    if (invalidos.length) {
      form.elements[invalidos[0]].focus();
      aviso('Revisa los campos marcados en rojo.');
      return;
    }

    const datos = {
      reservacionId: reservacion.id,
      kilometrajeSalida: Number(form.elements.kilometrajeSalida.value),
      combustibleSalida: Number(form.elements.combustibleSalida.value),
      condicionSalida: `${form.elements.condicionGeneral.value}: ${detalle.value.trim()}`,
      observaciones: form.elements.observaciones.value.trim() || null
    };

    registrar.disabled = true;
    try {
      const { data } = await peticion('/v1/entregas', { method: 'POST', body: JSON.stringify(datos) });
      // Estado actualizado: refleja lo que devolvió el backend.
      badge($('r-estado'), data.estadoReservacion);
      badge($('r-estado-vehiculo'), data.estadoVehiculo);
      $('r-km').textContent = km(data.kilometrajeSalida);
      panelForm.hidden = true;
      mostrarResultado(data, 'Entrega registrada correctamente');
      aviso(`Entrega #${data.id} registrada. La reservación está EN_CURSO y el vehículo RENTADO.`, 'ok');
    } catch (error) {
      if (error.status === 409) {
        aviso(`No se pudo registrar: ${error.message}`);
      } else if (error.status === 400) {
        aviso(`Datos inválidos: ${error.message}`);
      } else {
        aviso(textoError(error));
      }
    } finally {
      registrar.disabled = false;
    }
  });

  // ---------- Paso 4: resultado ----------
  function mostrarResultado(e, titulo) {
    $('resultado-titulo').lastChild.textContent = titulo;
    $('e-id').textContent = `#${e.id}`;
    $('e-fecha').textContent = fecha(e.fechaEntrega);
    $('e-km').textContent = km(e.kilometrajeSalida);
    $('e-comb').textContent = `${Number(e.combustibleSalida)} % · ${describirCombustible(Number(e.combustibleSalida))}`;
    $('e-cond').textContent = e.condicionSalida;
    $('e-obs').textContent = e.observaciones || '—';
    badge($('e-estado'), e.estadoReservacion);
    badge($('e-estado-vehiculo'), e.estadoVehiculo);
    panelResultado.hidden = false;
    panelResultado.scrollIntoView({ behavior: 'smooth' });
  }

  // ---------- Inicio ----------
  const id = new URLSearchParams(window.location.search).get('reservacion');
  if (id && /^\d+$/.test(id)) {
    cargarReservacion(Number(id));
  } else {
    if (id) aviso('El identificador de reservación no es válido.');
    mostrarSeleccion();
  }
})();
