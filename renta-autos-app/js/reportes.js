(() => {
  const mensaje = document.getElementById('mensaje');
  let sesion;

  try { sesion = JSON.parse(localStorage.getItem('usuarioSesion')); } catch (_) { /* sesión inválida */ }
  if (!sesion?.token) {
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
  const peticion = (ruta, opciones = {}) => fetchAPI(ruta, {
    ...opciones,
    headers: { Authorization: `Bearer ${sesion.token}`, ...opciones.headers }
  });

  // Últimos datos cargados de cada reporte (para exportarlos a CSV).
  let datosInventario = null, datosClientes = null, datosReservaciones = null;

  const fechaArchivo = () => new Date().toISOString().slice(0, 10);

  // Convierte una matriz de filas a texto CSV (con BOM para que Excel lea los acentos).
  function aCSV(filas) {
    const esc = (v) => {
      const s = String(v ?? '');
      return /[",\n\r;]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
    };
    return '﻿' + filas.map(f => f.map(esc).join(',')).join('\r\n');
  }

  function descargarCSV(nombre, filas) {
    const blob = new Blob([aCSV(filas)], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  function exportarInventario() {
    if (!datosInventario) { aviso('Primero carga el inventario (Actualizar).'); return; }
    const d = datosInventario;
    const filas = [['Tipo', 'Etiqueta', 'Cantidad'], ['Total', 'Vehículos', d.totalVehiculos]];
    (d.porEstado || []).forEach(i => filas.push(['Estado', i.etiqueta, i.cantidad]));
    (d.porCategoria || []).forEach(i => filas.push(['Categoría', i.etiqueta, i.cantidad]));
    descargarCSV(`reporte-inventario-${fechaArchivo()}.csv`, filas);
  }

  function exportarClientes() {
    if (!datosClientes) { aviso('Primero carga los clientes (Actualizar).'); return; }
    const d = datosClientes;
    const filas = [
      ['Métrica', 'Valor'],
      ['Total', d.totalClientes],
      ['Activos', d.activos],
      ['Inactivos', d.inactivos]
    ];
    descargarCSV(`reporte-clientes-${fechaArchivo()}.csv`, filas);
  }

  function exportarReservaciones() {
    if (!datosReservaciones) { aviso('Primero carga las reservaciones (Actualizar).'); return; }
    const d = datosReservaciones;
    const filas = [['Tipo', 'Etiqueta', 'Cantidad'], ['Total', 'Reservaciones', d.totalReservaciones]];
    (d.porEstado || []).forEach(i => filas.push(['Estado', i.etiqueta, i.cantidad]));
    descargarCSV(`reporte-reservaciones-${fechaArchivo()}.csv`, filas);
  }

  function pintarConteos(tbody, lista) {
    tbody.replaceChildren();
    if (!lista.length) {
      const tr = tbody.insertRow();
      const td = tr.insertCell(); td.colSpan = 2; td.textContent = 'Sin datos disponibles.';
      return;
    }
    lista.forEach(item => {
      const tr = tbody.insertRow();
      tr.insertCell().textContent = item.etiqueta;
      tr.insertCell().textContent = item.cantidad;
    });
  }

  async function cargarInventario() {
    try {
      const data = (await peticion('/reportes/inventario')).data;
      datosInventario = data;
      document.getElementById('inventario-total').textContent = `Total de vehículos: ${data.totalVehiculos}`;
      pintarConteos(document.getElementById('inventario-estado'), data.porEstado);
      pintarConteos(document.getElementById('inventario-categoria'), data.porCategoria);
    } catch (error) {
      aviso(error.status === 401 ? 'Sesión expirada. Vuelve a iniciar sesión.' : error.message);
    }
  }

  async function cargarClientes() {
    try {
      const data = (await peticion('/reportes/clientes')).data;
      datosClientes = data;
      const contenedor = document.getElementById('clientes-tarjetas');
      contenedor.replaceChildren();
      [
        ['Total', data.totalClientes],
        ['Activos', data.activos],
        ['Inactivos', data.inactivos]
      ].forEach(([etiqueta, valor]) => {
        const div = document.createElement('div');
        div.className = 'reporte-tarjeta';
        div.innerHTML = `<div class="valor">${valor}</div><div>${etiqueta}</div>`;
        contenedor.append(div);
      });
    } catch (error) {
      aviso(error.status === 401 ? 'Sesión expirada. Vuelve a iniciar sesión.' : error.message);
    }
  }

  async function cargarReservaciones() {
    try {
      const data = (await peticion('/reportes/reservaciones')).data;
      datosReservaciones = data;
      document.getElementById('reservaciones-total').textContent = `Total de reservaciones: ${data.totalReservaciones}`;
      pintarConteos(document.getElementById('reservaciones-estado'), data.porEstado);
    } catch (error) {
      aviso(error.status === 401 ? 'Sesión expirada. Vuelve a iniciar sesión.' : error.message);
    }
  }

  document.getElementById('recargar-inventario').addEventListener('click', cargarInventario);
  document.getElementById('recargar-clientes').addEventListener('click', cargarClientes);
  document.getElementById('recargar-reservaciones').addEventListener('click', cargarReservaciones);

  document.getElementById('csv-inventario').addEventListener('click', exportarInventario);
  document.getElementById('csv-clientes').addEventListener('click', exportarClientes);
  document.getElementById('csv-reservaciones').addEventListener('click', exportarReservaciones);

  Promise.all([cargarInventario(), cargarClientes(), cargarReservaciones()])
    .catch(error => aviso(error.message));
})();
