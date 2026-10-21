// inicio.js - Inicio del portal: búsqueda por fechas y vehículos destacados

document.addEventListener('DOMContentLoaded', () => {
    const MAX_DESTACADOS = 4;

    const grid = document.getElementById('destacadosGrid');
    const formulario = document.getElementById('formBusqueda');
    const inputInicio = document.getElementById('fechaInicio');
    const inputFin = document.getElementById('fechaFin');
    const error = document.getElementById('errorBusqueda');

    inputInicio.min = Portal.hoyISO();
    inputFin.min = Portal.hoyISO();
    inputInicio.addEventListener('change', () => {
        if (inputInicio.value) inputFin.min = inputInicio.value;
    });

    async function cargar() {
        Portal.mostrarCarga(grid, MAX_DESTACADOS);
        try {
            const vehiculos = await Portal.obtenerCatalogo();
            if (vehiculos.length === 0) {
                Portal.mostrarEstado(grid, {
                    tipo: 'vacio',
                    titulo: 'Sin vehículos disponibles',
                    texto: 'Todos nuestros autos están ocupados en este momento. Vuelve a consultar más tarde.'
                });
                return;
            }
            Portal.mostrarVehiculos(grid, vehiculos.slice(0, MAX_DESTACADOS));
        } catch (err) {
            const { titulo, texto } = Portal.mensajeError(err);
            Portal.mostrarEstado(grid, {
                tipo: 'error',
                titulo,
                texto,
                accion: { texto: 'Reintentar', alHacerClic: cargar }
            });
        }
    }

    formulario.addEventListener('submit', (e) => {
        e.preventDefault();
        const inicio = inputInicio.value;
        const fin = inputFin.value;
        error.textContent = '';

        if (!inicio && !fin) {
            window.location.href = 'catalogo.html';
            return;
        }
        const problema = Portal.validarFechas(inicio, fin);
        if (problema) {
            error.textContent = problema;
            return;
        }
        window.location.href = `catalogo.html?${new URLSearchParams({ inicio, fin }).toString()}`;
    });

    cargar();
});
