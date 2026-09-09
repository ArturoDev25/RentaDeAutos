package com.rentadeautos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO de respuesta estándar para todos los endpoints de la API.
 *
 * @param <T> tipo del payload de datos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {

    /**
     * Crea una respuesta exitosa con datos.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Crea una respuesta de error con mensaje descriptivo.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
