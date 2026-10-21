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

  const nombreRol = (r) => r ? r.charAt(0) + r.slice(1).toLowerCase() : '-';
  const pct = (parte, total) => (total > 0 ? (parte / total * 100).toFixed(1) : '0.0') + '%';

  // Encabezado común tipo reporte (marca, título, fecha/hora y responsable).
  function encabezadoReporte(titulo) {
    const fechaHora = new Date().toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' });
    return [
      ['CAR RENTAL SERVICE — Sistema de Renta de Autos'],
      [titulo],
      ['Generado el:', fechaHora],
      ['Generado por:', `${sesion.nombre || 'Usuario'} (${nombreRol(sesion.rol)})`],
      []
    ];
  }

  const pieReporte = () => [[], ['— Reporte generado automáticamente por el Sistema de Renta de Autos —']];

  function exportarInventario() {
    if (!datosInventario) { aviso('Primero carga el inventario (Actualizar).'); return; }
    const d = datosInventario;
    const total = Number(d.totalVehiculos) || 0;
    const filas = encabezadoReporte('Reporte de inventario de vehículos');
    filas.push(['Total de vehículos:', total], []);
    filas.push(['Distribución por estado'], ['Estado', 'Cantidad', '% del total']);
    (d.porEstado || []).forEach(i => filas.push([i.etiqueta, i.cantidad, pct(Number(i.cantidad), total)]));
    filas.push([], ['Distribución por categoría'], ['Categoría', 'Cantidad', '% del total']);
    (d.porCategoria || []).forEach(i => filas.push([i.etiqueta, i.cantidad, pct(Number(i.cantidad), total)]));
    filas.push(...pieReporte());
    descargarCSV(`reporte-inventario-${fechaArchivo()}.csv`, filas);
  }

  function exportarClientes() {
    if (!datosClientes) { aviso('Primero carga los clientes (Actualizar).'); return; }
    const d = datosClientes;
    const total = Number(d.totalClientes) || 0;
    const filas = encabezadoReporte('Reporte de clientes');
    filas.push(['Métrica', 'Cantidad', '% del total']);
    filas.push(['Total de clientes', total, '100.0%']);
    filas.push(['Clientes activos', d.activos, pct(Number(d.activos), total)]);
    filas.push(['Clientes inactivos', d.inactivos, pct(Number(d.inactivos), total)]);
    filas.push(...pieReporte());
    descargarCSV(`reporte-clientes-${fechaArchivo()}.csv`, filas);
  }

  function exportarReservaciones() {
    if (!datosReservaciones) { aviso('Primero carga las reservaciones (Actualizar).'); return; }
    const d = datosReservaciones;
    const total = Number(d.totalReservaciones) || 0;
    const filas = encabezadoReporte('Reporte de reservaciones');
    filas.push(['Total de reservaciones:', total], []);
    filas.push(['Distribución por estado'], ['Estado', 'Cantidad', '% del total']);
    (d.porEstado || []).forEach(i => filas.push([i.etiqueta, i.cantidad, pct(Number(i.cantidad), total)]));
    filas.push(...pieReporte());
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
