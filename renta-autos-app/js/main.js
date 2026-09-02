document.addEventListener('DOMContentLoaded', () => {
    console.log('RentaAutos Home loaded');

    const searchBtn = document.querySelector('.search-btn');
    const carCards = document.querySelectorAll('.car-card');

    searchBtn.addEventListener('click', () => {
        const destination = document.getElementById('destination').value;
        if (destination) {
            alert(`Buscando los mejores autos en ${destination}... 🚗`);
        } else {
            alert('Por favor, ingresa un destino para buscar.');
        }
    });

    carCards.forEach(card => {
        card.addEventListener('click', () => {
            const carName = card.querySelector('h3').innerText;
            alert(`Has seleccionado el ${carName}. ¡Redirigiendo a los detalles de reserva!`);
        });
    });
});
