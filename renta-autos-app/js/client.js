// client.js - Módulo base para consumir la API REST de Spring Boot

const esEntornoLocal =
    window.location.protocol === 'file:' ||
    ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);

const API_BASE_URL = window.API_BASE_URL ||
    (esEntornoLocal
        ? 'http://localhost:8080/api'
        : `${window.location.origin}/api`);

async function fetchAPI(endpoint, options = {}) {
    const sesion = localStorage.getItem('usuarioSesion');
    let usuario = null;

    if (sesion) {
        try {
            usuario = JSON.parse(sesion);
        } catch (error) {
            console.error('Error al leer la sesión:', error);
            localStorage.removeItem('usuarioSesion');
        }
    }

    const defaultHeaders = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (usuario?.token) {
        defaultHeaders.Authorization = `${usuario.tokenType || 'Bearer'} ${usuario.token}`;
    }

    const config = {
        ...options,
        headers: defaultHeaders
    };

    let response;
    try {
        response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    } catch (error) {
        console.error(`No se pudo conectar con el backend en ${endpoint}:`, error);
        throw new Error('No se pudo conectar con el servidor. Verifica tu conexión.');
    }

    if (response.status === 204) return null;

    let body = null;
    try {
        body = await response.json();
    } catch (_) {
        // Algunas respuestas no contienen JSON.
    }

    if (!response.ok) {
        const error = new Error(
            body?.message || `Error HTTP: ${response.status}`
        );
        error.status = response.status;
        throw error;
    }

    return body;
}
