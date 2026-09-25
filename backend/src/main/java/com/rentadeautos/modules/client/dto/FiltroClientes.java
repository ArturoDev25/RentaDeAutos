package com.rentadeautos.modules.client.dto;

/** Criterios opcionales para buscar clientes. */
public record FiltroClientes(String texto, Boolean activo) {

    public static FiltroClientes sinFiltros() {
        return new FiltroClientes(null, null);
    }
}