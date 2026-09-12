document.addEventListener('DOMContentLoaded', () => {
    console.log('Sistema RentaAutos inicializado');

    // --- GESTIÓN DE SESIÓN (Header Dinámico) ---
    const initSession = () => {
        const authButtons = document.getElementById('authButtons');
        const userGreeting = document.getElementById('userGreeting');
        const sessionData = localStorage.getItem('usuarioSesion');

        if (sessionData) {
            const usuario = JSON.parse(sessionData);

            // Ocultar botones de acceso
            if (authButtons) authButtons.style.display = 'none';

            // Mostrar saludo y botón de cierre de sesión
            if (userGreeting) {
                userGreeting.innerHTML = `
                    <span>Hola, <strong>${usuario.nombre}</strong></span>
                    <button id="btnLogout" class="logout-btn">Cerrar Sesión</button>
                `;

                document.getElementById('btnLogout').addEventListener('click', () => {
                    localStorage.removeItem('usuarioSesion');
                    window.location.reload();
                });
            }
        } else {
            // Asegurar que los botones sean visibles si no hay sesión
            if (authButtons) authButtons.style.display = 'flex';
            if (userGreeting) userGreeting.innerHTML = '';
        }
    };

    initSession();

    // Búsqueda de vehículos
    const searchBtn = document.querySelector('.search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', (e) => {
            e.preventDefault();
            const destinationInput = document.getElementById('destination');
            const destination = destinationInput ? destinationInput.value.trim() : '';

            if (destination) {
                console.log(`Buscando autos disponibles en: ${destination}`);
            }
        });
    }

    // Selección de tarjetas de autos
    const carCards = document.querySelectorAll('.car-card');
    if (carCards.length > 0) {
        carCards.forEach(card => {
            card.addEventListener('click', () => {
                const carTitle = card.querySelector('h3');
                if (carTitle) {
                    const carName = carTitle.innerText;
                    console.log(`Auto seleccionado: ${carName}`);
                }
            });
        });
    }
});