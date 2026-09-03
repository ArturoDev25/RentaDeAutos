document.addEventListener('DOMContentLoaded', () => {
    console.log('Sistema RentaAutos inicializado');

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