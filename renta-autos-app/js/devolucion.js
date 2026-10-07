// S3-15 — Pantalla y flujo de devolución de vehículo.
// Consume: GET /reservaciones, GET /reservaciones/{id}, GET /reservaciones/opciones,
//          GET /v1/vehiculos/{id}, GET /v1/clientes/{id},
//          GET /v1/entregas/reservacion/{id} (S3-09), POST /v1/devoluciones (S3-10).
(() => {
  const $ = id => document.getElementById(id);
  const mensaje = $('mensaje');
  const panelSeleccion = $('panel-seleccion');
  const panelResumen = $('panel-resumen');
  const panelForm = $('panel-form');
  const panelResultado = $('panel-resultado');
  const formSeleccion = $('form-seleccion');
  const form = $('form-devolucion');
  const registrar = $('registrar');

  const MOTIVO_POR_ESTADO = {
    PENDIENTE: 'La reservación está PENDIENTE: el vehículo todavía no se entrega al cliente.',
    CONFIRMADA: 'La reservación está CONFIRMADA pero el vehículo aún no se entrega. Registra primero la entrega.',
    CANCELADA: 'La reservación fue CANCELADA.',
    FINALIZADA: 'Esta renta ya está FINALIZADA: el vehículo ya fue devuelto.'
  };
  // Campos del backend (DevolucionRequest) → campo del formulario donde se muestra el error.
  const CAMPO_FORMULARIO = {
    kilometrajeEntrada: 'kilometrajeEntrada',
    combustibleEntrada: 'combustibleEntrada',
    condicionEntrada: 'condicionDetalle',
    cargoDanos: 'cargoDanos',
    observaciones: 'observaciones'
  };

  let sesion;
  let reservacion = null;
  let entrega = null;

  // ---------- Sesión (mismo criterio que reservaciones.js y entrega.js) ----------
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
  const moneda = valor => new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(Number(valor) || 0);
  const plural = (n, palabra) => `${n} ${palabra}${n === 1 ? '' : 's'}`;
  const badge = (elemento, estado) => { elemento.textContent = estado || '—'; elemento.dataset.estado = estado || ''; };
  const textoError = error => {
    if (error.status === 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    if (error.status === 403) return 'Tu rol no tiene permiso para registrar devoluciones.';
    if (error.status >= 500) return 'El servidor tuvo un problema. Intenta de nuevo en unos momentos.';
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
  // Días: horas completas / 24 redondeado hacia arriba (igual que DevolucionService).
  const diasEntre = (desde, hasta) => Math.ceil(Math.floor((hasta - desde) / 3_600_000) / 24);

  // ---------- Paso 1: elegir renta en curso ----------
  async function mostrarSeleccion() {
    panelSeleccion.hidden = false;
    const select = formSeleccion.elements.reservacionId;
    try {
      const [{ data: lista }, { data: opciones }] = await Promise.all([
        peticion('/reservaciones'), peticion('/reservaciones/opciones')
      ]);
      const clientes = new Map(opciones.clientes.map(c => [c.id, c.nombre]));
      const vehiculos = new Map(opciones.vehiculos.map(v => [v.id, v.nombre]));
      const enCurso = lista.filter(r => r.estado === 'EN_CURSO');
      select.length = 0;
      select.add(new Option(enCurso.length ? 'Selecciona una renta' : 'No hay rentas en curso', ''));
      enCurso.forEach(r => select.add(new Option(
        `#${r.id} · ${clientes.get(r.clienteId) || `Cliente ${r.clienteId}`} · ${vehiculos.get(r.vehiculoId) || `Vehículo ${r.vehiculoId}`} · devuelve ${fecha(r.fechaFin)}`,
        r.id)));
      if (!enCurso.length) aviso('No hay rentas EN_CURSO pendientes de devolución.');
    } catch (error) { aviso(textoError(error)); }
  }

  formSeleccion.addEventListener('submit', event => {
    event.preventDefault();
    const id = formSeleccion.elements.reservacionId.value;
    if (!id) { aviso('Selecciona una renta.'); return; }
    window.location.search = `?reservacion=${encodeURIComponent(id)}`;
  });

  // ---------- Paso 2: cargar la renta y su entrega ----------
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
    const vehiculo = vehiculoRes.status === 'fulfilled' ? vehiculoRes.value.data : null;
    const cliente = clienteRes.status === 'fulfilled' ? clienteRes.value.data : null;
    entrega = entregaRes.status === 'fulfilled' ? entregaRes.value.data : null;

    $('r-cliente').textContent = cliente ? `${cliente.nombre} ${cliente.apellidos}` : `Cliente #${reservacion.clienteId}`;
    $('r-vehiculo').textContent = vehiculo
      ? `${vehiculo.marca} ${vehiculo.modelo} ${vehiculo.anio ?? ''} · ${vehiculo.placa}`
      : `Vehículo #${reservacion.vehiculoId}`;
    $('r-periodo').textContent = `${fecha(reservacion.fechaInicio)} → ${fecha(reservacion.fechaFin)}`;
    $('r-tarifa').textContent = moneda(reservacion.tarifaDia);
    badge($('r-estado'), reservacion.estado);
    badge($('r-estado-vehiculo'), vehiculo?.estado);
    if (entrega) {
      $('r-fecha-salida').textContent = fecha(entrega.fechaEntrega);
      $('r-km-salida').textContent = km(entrega.kilometrajeSalida);
      $('r-comb-salida').textContent = `${Number(entrega.combustibleSalida)} % · ${describirCombustible(Number(entrega.combustibleSalida))}`;
      $('r-cond-salida').textContent = entrega.condicionSalida || '—';
    }

    if (reservacion.estado !== 'EN_CURSO') {
      bloquear(MOTIVO_POR_ESTADO[reservacion.estado] || `La reservación está en estado ${reservacion.estado}.`);
      return;
    }
    if (!entrega) {
      const error = entregaRes.reason;
      bloquear(error?.status === 404 || !error
        ? 'La reservación no tiene una entrega registrada, así que no hay nada que devolver.'
        : `No se pudo consultar la entrega: ${textoError(error)}`);
      return;
    }

    prepararFormulario();
  }

  function bloquear(texto) {
    $('bloqueo-texto').textContent = texto;
    $('bloqueo').hidden = false;
    panelForm.hidden = true;
  }

  // ---------- Paso 3: formulario ----------
  const kmSalida = () => Number(entrega?.kilometrajeSalida) || 0;

  function prepararFormulario() {
    const campoKm = form.elements.kilometrajeEntrada;
    campoKm.min = kmSalida();
    campoKm.value = '';
    campoKm.placeholder = `${kmSalida()}`;
    $('km-ayuda').textContent = `Debe ser mayor o igual a ${km(kmSalida())} (kilometraje de salida).`;
    // Por defecto se propone el mismo nivel de combustible con el que salió.
    const combSalida = Number(entrega.combustibleSalida);
    if (Number.isFinite(combSalida)) {
      form.elements.combustibleEntrada.value = combSalida;
      form.elements.combustibleRango.value = combSalida;
      actualizarCombustible();
    }
    panelForm.hidden = false;
    actualizarDestino();
    actualizarPreview();
    campoKm.focus();
  }

  // Sincroniza el control deslizante con el campo numérico de combustible.
  const rango = form.elements.combustibleRango;
  const combustible = form.elements.combustibleEntrada;
  function actualizarCombustible() {
    const valor = Number(combustible.value);
    let texto = describirCombustible(valor);
    const salida = Number(entrega?.combustibleSalida);
    if (entrega && Number.isFinite(salida) && combustible.value !== '') {
      if (valor < salida) texto += ` · ${+(salida - valor).toFixed(2)} % menos que a la salida`;
      else if (valor === salida) texto += ' · igual que a la salida';
    }
    $('comb-ayuda').textContent = texto;
  }
  rango.addEventListener('input', () => { combustible.value = rango.value; actualizarCombustible(); validarCampo('combustibleEntrada'); });
  combustible.addEventListener('input', () => { rango.value = combustible.value; actualizarCombustible(); });

  const detalle = form.elements.condicionDetalle;
  detalle.addEventListener('input', () => { $('cond-contador').textContent = `${detalle.value.length} / 1900`; });

  const kmCampo = form.elements.kilometrajeEntrada;
  kmCampo.addEventListener('input', () => {
    const n = Number(kmCampo.value);
    $('km-ayuda').textContent = kmCampo.value !== '' && n >= kmSalida()
      ? `Recorrido: ${km(n - kmSalida())}`
      : `Debe ser mayor o igual a ${km(kmSalida())} (kilometraje de salida).`;
  });

  const condicionEntrada = () => `${form.elements.condicionGeneral.value}: ${detalle.value.trim()}`;

  // Anticipa a qué estado pasará el vehículo. Misma regla que el backend (PR #110):
  // MANTENIMIENTO si se marca la casilla o si hay cargo por daños mayor a 0.
  function actualizarDestino() {
    const destino = $('destino-vehiculo');
    const porMantenimiento = form.elements.requiereMantenimiento.checked;
    const porDanos = Number(form.elements.cargoDanos.value) > 0;
    if (porMantenimiento || porDanos) {
      destino.dataset.estado = 'MANTENIMIENTO';
      destino.innerHTML = '<i class="fa-solid fa-screwdriver-wrench" aria-hidden="true"></i> El vehículo pasará a <strong>MANTENIMIENTO</strong>'
        + (porMantenimiento ? ' (lo marcaste para revisión).' : ' porque se registró un cargo por daños.');
    } else {
      destino.dataset.estado = 'DISPONIBLE';
      destino.innerHTML = '<i class="fa-solid fa-circle-check" aria-hidden="true"></i> El vehículo quedará <strong>DISPONIBLE</strong> para nuevas rentas.';
    }
  }
  form.elements.requiereMantenimiento.addEventListener('change', actualizarDestino);
  form.elements.cargoDanos.addEventListener('input', actualizarDestino);

  // Vista previa del cobro con la misma fórmula que DevolucionService (PR #110):
  // subtotal = días PACTADOS × tarifa; el atraso se cobra aparte una sola vez.
  function actualizarPreview() {
    if (!entrega || !reservacion) return;
    const ahora = new Date();
    const tarifa = Number(reservacion.tarifaDia) || 0;
    const fin = new Date(reservacion.fechaFin);
    const dias = Math.max(1, diasEntre(new Date(reservacion.fechaInicio), fin));
    const diasAtraso = ahora > fin ? diasEntre(fin, ahora) : 0;
    const danosValor = Number(form.elements.cargoDanos.value);
    const danos = Number.isFinite(danosValor) && danosValor > 0 ? danosValor : 0;
    const subtotal = tarifa * dias;
    const atraso = tarifa * diasAtraso;
    $('p-dias-texto').textContent = `Renta pactada (${plural(dias, 'día')} × ${moneda(tarifa)})`;
    $('p-subtotal').textContent = moneda(subtotal);
    $('p-atraso-texto').textContent = diasAtraso ? `Atraso (${plural(diasAtraso, 'día')})` : 'Atraso';
    $('p-atraso').textContent = moneda(atraso);
    $('p-danos').textContent = moneda(danos);
    $('p-total').textContent = moneda(subtotal + atraso + danos);
  }
  form.elements.cargoDanos.addEventListener('input', actualizarPreview);

  const reglas = {
    kilometrajeEntrada(valor) {
      if (valor === '') return 'El kilometraje de entrada es obligatorio.';
      const n = Number(valor);
      if (!Number.isFinite(n) || n < 0) return 'Ingresa un kilometraje válido (0 o mayor).';
      if (!/^\d{1,9}(\.\d)?$/.test(valor)) return 'Usa hasta 9 enteros y 1 decimal.';
      if (n < kmSalida()) return `No puede ser menor al kilometraje de salida (${km(kmSalida())}).`;
      return '';
    },
    combustibleEntrada(valor) {
      if (valor === '') return 'El nivel de combustible es obligatorio.';
      const n = Number(valor);
      if (!Number.isFinite(n) || n < 0 || n > 100) return 'El combustible debe estar entre 0 y 100 %.';
      if (!/^\d{1,3}(\.\d{1,2})?$/.test(valor)) return 'Usa hasta 2 decimales.';
      return '';
    },
    condicionGeneral(valor) { return valor ? '' : 'Selecciona la condición general del vehículo.'; },
    condicionDetalle(valor) {
      if (!valor) return 'Describe la condición en que regresa el vehículo.';
      if (valor.length < 5) return 'Describe la condición con un poco más de detalle.';
      if (valor.length > 1900) return 'Máximo 1900 caracteres.';
      return '';
    },
    cargoDanos(valor) {
      if (valor === '') return form.elements.cargoDanos.validity.badInput ? 'Ingresa un monto válido.' : '';
      const n = Number(valor);
      if (!Number.isFinite(n) || n < 0) return 'El cargo no puede ser negativo.';
      if (!/^\d{1,8}(\.\d{1,2})?$/.test(valor)) return 'Usa hasta 8 enteros y 2 decimales.';
      return '';
    },
    observaciones(valor) { return valor.length > 2000 ? 'Máximo 2000 caracteres.' : ''; },
    revisado() { return form.elements.revisado.checked ? '' : 'Confirma que revisaste el vehículo con el cliente.'; }
  };

  function marcarError(nombre, error) {
    const campo = form.elements[nombre];
    const salida = form.querySelector(`[data-error-de="${nombre}"]`);
    if (salida) salida.textContent = error;
    campo.setAttribute('aria-invalid', error ? 'true' : 'false');
  }

  function validarCampo(nombre) {
    const campo = form.elements[nombre];
    const error = reglas[nombre](campo.type === 'checkbox' ? campo.checked : campo.value.trim());
    marcarError(nombre, error);
    return !error;
  }

  Object.keys(reglas).forEach(nombre => {
    const campo = form.elements[nombre];
    campo.addEventListener(campo.type === 'checkbox' || campo.tagName === 'SELECT' ? 'change' : 'blur', () => validarCampo(nombre));
    // Mientras se corrige un campo marcado, el error se actualiza al escribir.
    campo.addEventListener('input', () => { if (campo.getAttribute('aria-invalid') === 'true') validarCampo(nombre); });
  });

  // El backend responde 400 con "campo: mensaje; campo: mensaje" (GlobalExceptionHandler)
  // o con el texto de RN-08. Se intenta colocar cada error junto a su campo.
  function mostrarErroresServidor(texto) {
    let ubicados = 0;
    texto.split(';').forEach(parte => {
      const [campo, ...resto] = parte.split(':');
      const destino = CAMPO_FORMULARIO[campo.trim()];
      if (destino && resto.length) { marcarError(destino, `El servidor indica que ${resto.join(':').trim()}.`); ubicados++; }
    });
    if (!ubicados && /kilometraje/i.test(texto)) { marcarError('kilometrajeEntrada', texto); ubicados++; }
    return ubicados;
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    limpiarAviso();
    const invalidos = Object.keys(reglas).filter(nombre => !validarCampo(nombre));
    if (invalidos.length) {
      form.elements[invalidos[0]].focus();
      aviso('Revisa los campos marcados en rojo.');
      return;
    }

    const cargo = form.elements.cargoDanos.value.trim();
    const datos = {
      entregaId: entrega.id,
      kilometrajeEntrada: Number(form.elements.kilometrajeEntrada.value),
      combustibleEntrada: Number(form.elements.combustibleEntrada.value),
      condicionEntrada: condicionEntrada(),
      cargoDanos: cargo === '' ? null : Number(cargo),
      observaciones: form.elements.observaciones.value.trim() || null,
      requiereMantenimiento: form.elements.requiereMantenimiento.checked
    };

    registrar.disabled = true;
    registrar.lastChild.textContent = ' Registrando…';
    try {
      const { data } = await peticion('/v1/devoluciones', { method: 'POST', body: JSON.stringify(datos) });
      // Estado actualizado de la renta y del vehículo, tal como lo devolvió el backend.
      badge($('r-estado'), data.estadoReservacion);
      badge($('r-estado-vehiculo'), data.estadoVehiculo);
      reservacion.estado = data.estadoReservacion;
      panelForm.hidden = true;
      mostrarResultado(data);
      aviso(`Devolución #${data.id} registrada. Costo final: ${moneda(data.totalFinal)}. La renta quedó ${data.estadoReservacion}.`, 'ok');
    } catch (error) {
      if (error.status === 400) {
        const ubicados = mostrarErroresServidor(error.message);
        aviso(ubicados ? 'El servidor rechazó algunos datos. Revisa los campos marcados.' : `Datos inválidos: ${error.message}`);
      } else if (error.status === 404 || error.status === 409) {
        // La renta cambió mientras se capturaba (ya devuelta, cancelada o inexistente).
        bloquear(error.message);
        aviso(`No se pudo registrar la devolución: ${error.message}`);
        panelResumen.scrollIntoView({ behavior: 'smooth' });
      } else {
        aviso(textoError(error));
      }
    } finally {
      registrar.disabled = false;
      registrar.lastChild.textContent = ' Registrar devolución';
    }
  });

  // ---------- Paso 4: resultado con el costo final ----------
  function mostrarResultado(d) {
    const tarifa = Number(reservacion.tarifaDia) || 0;
    const subtotal = Number(d.subtotal);
    const dias = tarifa > 0 ? Math.round(subtotal / tarifa) : null;
    $('d-subtotal-texto').textContent = dias ? `Renta pactada (${plural(dias, 'día')} × ${moneda(tarifa)})` : 'Renta pactada';
    $('d-subtotal').textContent = moneda(subtotal);
    $('d-atraso').textContent = moneda(d.cargoAtraso);
    $('d-danos').textContent = moneda(d.cargoDanos);
    $('d-total').textContent = moneda(d.totalFinal);
    $('d-total-fila').textContent = moneda(d.totalFinal);

    const estimado = Number(reservacion.totalEstimado);
    if (Number.isFinite(estimado) && estimado > 0) {
      const diferencia = Number(d.totalFinal) - estimado;
      $('d-comparacion').textContent = Math.abs(diferencia) < 0.005
        ? `Igual al total estimado de la reservación (${moneda(estimado)}).`
        : `Total estimado de la reservación: ${moneda(estimado)} (${diferencia > 0 ? '+' : '−'}${moneda(Math.abs(diferencia))}).`;
    }

    $('d-id').textContent = `#${d.id}`;
    $('d-fecha').textContent = fecha(d.fechaDevolucion);
    $('d-placa').textContent = d.vehiculoPlaca || '—';
    $('d-km').textContent = km(d.kilometrajeEntrada);
    $('d-comb').textContent = `${Number(d.combustibleEntrada)} % · ${describirCombustible(Number(d.combustibleEntrada))}`;
    $('d-recorrido').textContent = km(Number(d.kilometrajeEntrada) - kmSalida());
    $('d-cond').textContent = d.condicionEntrada;
    badge($('d-estado'), d.estadoReservacion);
    badge($('d-estado-vehiculo'), d.estadoVehiculo);
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
