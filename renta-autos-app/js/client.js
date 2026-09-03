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

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        // Si la respuesta no tiene contenido (status 204 No Content), retornamos null
        if (response.status === 204) {
            return null;
        }

        return await response.json();
    } catch (error) {
        console.error(`Error al conectar con el backend en ${endpoint}:`, error);
        throw error;
    }
}