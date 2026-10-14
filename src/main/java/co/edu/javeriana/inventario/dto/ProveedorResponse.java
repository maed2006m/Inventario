package co.edu.javeriana.inventario.dto;

import java.time.OffsetDateTime;

/**
 * Datos de un proveedor que la API devuelve al cliente (RF-05).
 * Incluye los campos que controla el sistema: id, estado activo y fechas.
 * Nunca se devuelve la entidad Proveedor directamente.
 */
public record ProveedorResponse(
        Long id,
        String nombre,
        String nit,
        String telefono,
        String email,
        String direccion,
        Integer tiempoEntregaDias,
        boolean activo,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {
}