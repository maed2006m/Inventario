
package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinámicos para el historial de movimientos de inventario.
 *
 * Permite combinar filtros por producto, tipo, motivo, usuario
 * y rango de fechas.
 */
public final class MovimientoSpecs {

    private MovimientoSpecs() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Construye una Specification con los filtros recibidos.
     *
     * @param productoId identificador del producto; null ignora el filtro
     * @param tipo tipo de movimiento; null ignora el filtro
     * @param motivo motivo del movimiento; null ignora el filtro
     * @param username nombre de usuario que registró el movimiento;
     *                 null o vacío ignora el filtro
     * @param desde fecha inicial inclusiva; null ignora el límite inferior
     * @param hasta fecha final inclusiva; null ignora el límite superior
     * @return Specification que combina los filtros aplicables
     */
    public static Specification<Movimiento> filtrar(
            Long productoId,
            TipoMovimiento tipo,
            MotivoMovimiento motivo,
            String username,
            OffsetDateTime desde,
            OffsetDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (productoId != null) {
                condiciones.add(
                        cb.equal(root.get("producto").get("id"), productoId));
            }

            if (tipo != null) {
                condiciones.add(
                        cb.equal(root.get("tipo"), tipo));
            }

            if (motivo != null) {
                condiciones.add(
                        cb.equal(root.get("motivo"), motivo));
            }

            if (username != null && !username.isBlank()) {
                condiciones.add(
                        cb.equal(
                                cb.lower(root.get("usuario").get("username")),
                                username.trim().toLowerCase()));
            }

            if (desde != null) {
                condiciones.add(
                        cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }

            if (hasta != null) {
                condiciones.add(
                        cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
