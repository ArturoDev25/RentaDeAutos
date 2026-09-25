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
      document.getElementById('reservaciones-total').textContent = `Total de reservaciones: ${data.totalReservaciones}`;
      pintarConteos(document.getElementById('reservaciones-estado'), data.porEstado);
    } catch (error) {
      aviso(error.status === 401 ? 'Sesión expirada. Vuelve a iniciar sesión.' : error.message);
    }
  }

  document.getElementById('recargar-inventario').addEventListener('click', cargarInventario);
  document.getElementById('recargar-clientes').addEventListener('click', cargarClientes);
  document.getElementById('recargar-reservaciones').addEventListener('click', cargarReservaciones);

  Promise.all([cargarInventario(), cargarClientes(), cargarReservaciones()])
    .catch(error => aviso(error.message));
})();
