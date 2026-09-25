document.addEventListener('DOMContentLoaded', async () => {

    // SESIÓN
    

    const sesion = localStorage.getItem('usuarioSesion');

    // Si no hay sesión, regresar al login
    if (!sesion) {
        window.location.href = 'login.html';
        return;
    }

    let usuario;

    try {
        usuario = JSON.parse(sesion);
    } catch (error) {
        console.error('Error al leer la sesión:', error);
        localStorage.removeItem('usuarioSesion');
        window.location.href = 'login.html';
        return;
    }

    // Verificar que el usuario sea administrador
    if (usuario.rol !== 'ADMINISTRADOR') {
        alert('No tienes permisos para acceder al panel de administrador.');
        window.location.href = 'index.html';
        return;
    }

   
    // MOSTRAR USUARIO
    

    const nombreAdministrador =
        document.getElementById('nombreAdministrador');

    if (nombreAdministrador) {
        nombreAdministrador.textContent =
            usuario.nombre || 'Administrador';
    }

  
    // CERRAR SESIÓN
   

    const btnCerrarSesion =
        document.getElementById('btnCerrarSesion');

    if (btnCerrarSesion) {

        btnCerrarSesion.addEventListener('click', () => {

            localStorage.removeItem('usuarioSesion');

            window.location.href = 'login.html';
        });
    }

  
    // CARGAR INFORMACIÓN DEL DASHBOARD
   

    await cargarDashboard();

});


// DASHBOARD


async function cargarDashboard() {

    try {

        console.log('Cargando información del dashboard...');

        // Consultar los reportes del backend
        const [inventario, clientes, reservaciones] =
            await Promise.all([
                fetchAPI('/reportes/inventario'),
                fetchAPI('/reportes/clientes'),
                fetchAPI('/reportes/reservaciones')
            ]);

        console.log('Inventario:', inventario);
        console.log('Clientes:', clientes);
        console.log('Reservaciones:', reservaciones);

        // Actualizar indicadores
        actualizarIndicadores(
            inventario,
            clientes,
            reservaciones
        );

        // Actualizar gráfica de vehículos
        actualizarEstadoVehiculos(inventario);
        actualizarReservaciones(reservaciones);

        // Actualizar fecha
        actualizarFecha();

    } catch (error) {

        console.error(
            'Error al cargar el dashboard:',
            error
        );

        mostrarErrorDashboard(
            'No se pudieron cargar los datos del dashboard.'
        );
    }
}



// INDICADORES//


function actualizarIndicadores(
    inventario,
    clientes,
    reservaciones
) {

    const totalVehiculos =
        obtenerData(inventario)?.totalVehiculos ?? 0;

    const totalClientesActivos =
        obtenerData(clientes)?.Activos ?? 0;

    const totalReservaciones =
        obtenerData(reservaciones)?.totalReservaciones ?? 0;

    const porEstado =
        obtenerData(inventario)?.porEstado || [];

    // Buscar vehículos disponibles
    const disponibles =
        buscarEstado(porEstado, 'DISPONIBLE');

    // Los cuatro elementos strong de las tarjetas
    const tarjetas =
        document.querySelectorAll(
            '.dashboard-statistics .stat-information strong'
        );

    if (tarjetas.length >= 4) {

        // Vehículos
        tarjetas[0].textContent =
            totalVehiculos;

        // Clientes
        tarjetas[1].textContent =
            totalClientesActivos;

        // Reservaciones
        tarjetas[2].textContent =
            totalReservaciones;

        // Vehículos disponibles
        tarjetas[3].textContent =
            disponibles;
    }
}


// ESTADO DE VEHÍCULOS//

function actualizarEstadoVehiculos(inventario) {

    const data = obtenerData(inventario);

    if (!data) {
        return;
    }

    const total =
        data.totalVehiculos || 0;

    const porEstado =
        data.porEstado || [];

    // Total en el centro del gráfico
    const donutTotal =
        document.querySelector(
            '.donut-center strong'
        );

    if (donutTotal) {
        donutTotal.textContent = total;
    }

    // Filas de la leyenda
    const filas =
        document.querySelectorAll(
            '.vehicle-legend .legend-row'
        );

    const estados = [
        'DISPONIBLE',
        'RENTADO',
        'MANTENIMIENTO',
        'RESERVADO',
        'BAJA'
    ];

    filas.forEach((fila, index) => {

        if (index >= estados.length) {
            return;
        }

        const estado =
            estados[index];

        const cantidad =
            buscarEstado(
                porEstado,
                estado
            );

        const porcentaje =
            total > 0
                ? ((cantidad / total) * 100).toFixed(1)
                : '0.0';

        const strong =
            fila.querySelector('strong');

        const small =
            fila.querySelector('small');

        if (strong) {
            strong.textContent = cantidad;
        }

        if (small) {
            small.textContent =
                `${porcentaje}%`;
        }

    });
}



// BUSCAR ESTADO//


function buscarEstado(
    lista,
    estadoBuscado
) {

    if (!Array.isArray(lista)) {
        return 0;
    }

    const resultado =
        lista.find(item => {

            const etiqueta =
                String(
                    item.etiqueta || ''
                ).toUpperCase();

            return etiqueta === estadoBuscado;
        });

    return resultado
        ? Number(resultado.cantidad || 0)
        : 0;
}



// OBTENER DATA DE LA RESPUESTA//


function obtenerData(respuesta) {

    if (!respuesta) {
        return null;
    }

    // Formato ApiResponse:
    // { success: true, data: {...} }

    if (
        respuesta.data !== undefined
    ) {
        return respuesta.data;
    }

    // Por si el backend devuelve directamente
    // el objeto esperado
    return respuesta;
}


// FECHA ACTUAL//


function actualizarFecha() {

    const elemento =
        document.querySelector(
            '.dashboard-date strong'
        );

    if (!elemento) {
        return;
    }

    const fecha = new Date();

    const opciones = {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    };

    elemento.textContent =
        fecha.toLocaleDateString(
            'es-MX',
            opciones
        );
}



// MENSAJE DE ERROR// 

function mostrarErrorDashboard(mensaje) {

    console.error(mensaje);

    const contenido =
        document.querySelector(
            '.dashboard-content'
        );

    if (!contenido) {
        return;
    }

    const alerta =
        document.createElement('div');

    alerta.style.padding = '12px 16px';
    alerta.style.marginBottom = '20px';
    alerta.style.borderRadius = '8px';
    alerta.style.backgroundColor = '#ffe5e5';
    alerta.style.color = '#9b1c1c';
    alerta.textContent = mensaje;

    contenido.prepend(alerta);
}

// =========================================================
// RESERVACIONES DEL DASHBOARD
// =========================================================

function actualizarReservaciones(reservaciones) {

    const tbody =
        document.getElementById(
            'reservacionesDashboardBody'
        );

    if (!tbody) {
        return;
    }

    tbody.innerHTML = '';

    const data = obtenerData(reservaciones);

    if (!data) {
        return;
    }

    // El reporte actualmente contiene el total y
    // las cantidades agrupadas por estado.
    const total =
        Number(data.totalReservaciones || 0);

    // Si no existen reservaciones, mostrar mensaje
    if (total === 0) {

        const fila =
            document.createElement('tr');

        fila.innerHTML = `
            <td colspan="5" style="text-align: center;">
                No hay reservaciones próximas.
            </td>
        `;

        tbody.appendChild(fila);

        return;
    }

    // Por ahora el endpoint de reportes solamente
    // proporciona el resumen por estado.
    const porEstado =
        Array.isArray(data.porEstado)
            ? data.porEstado
            : [];

    porEstado.forEach(item => {

        const fila =
            document.createElement('tr');

        fila.innerHTML = `
            <td colspan="4">
                Reservaciones
            </td>

            <td>
                <span class="reservation-status confirmed">
                    ${item.etiqueta}: ${item.cantidad}
                </span>
            </td>
        `;

        tbody.appendChild(fila);
    });
}