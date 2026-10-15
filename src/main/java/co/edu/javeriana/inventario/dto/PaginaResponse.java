package co.edu.javeriana.inventario.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada genérica (RF-27, RF-28).
 * Sirve para cualquier tipo de elemento: movimientos, productos, etc.
 *
 * @param contenido      elementos de la página actual
 * @param pagina         número de la página actual; la primera es la 0
 * @param tamanio        cuántos elementos caben por página
 * @param totalElementos cuántos elementos hay en total, sumando todas las páginas
 * @param totalPaginas   cuántas páginas hay en total
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    /**
     * Convierte un Page de Spring en esta respuesta, transformando cada elemento
     * (por ejemplo, de entidad Movimiento a MovimientoResponse).
     */
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> conversor) {
        return new PaginaResponse<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}