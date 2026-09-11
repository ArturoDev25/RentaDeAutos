// client.js - Módulo base para consumir la API REST de Spring Boot
const API_BASE_URL = 'http://localhost:8080/api';

async function fetchAPI(endpoint, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json',
        ...options.headers
    };

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

    if (response.status === 204) {
        return null;
    }

    let body = null;
    try {
        body = await response.json();
    } catch (_) {
        // Respuesta sin cuerpo JSON válido
    }

    if (!response.ok) {
        const mensaje = body && body.message ? body.message : `Error HTTP: ${response.status}`;
        const error = new Error(mensaje);
        error.status = response.status;
        throw error;
    }

    return body;
}