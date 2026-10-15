package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros combinables del historial de movimientos (RF-27, RF-28).
 *
 * Cada filtro que llega agrega una condicion al WHERE; los que llegan
 * en null simplemente no participan. Asi una sola consulta cubre todas
 * las combinaciones posibles.
 */
public final class MovimientoSpecs {

    private MovimientoSpecs() {
    }

    public static Specification<Movimiento> filtrar(Long productoId, Long proveedorId,
                                                    TipoMovimiento tipo, MotivoMovimiento motivo,
                                                    OffsetDateTime desde, OffsetDateTime hasta) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (productoId != null) {
                condiciones.add(cb.equal(root.get("producto").get("id"), productoId));
            }

            // Navega dos niveles: movimiento -> producto -> proveedor
            if (proveedorId != null) {
                condiciones.add(cb.equal(root.get("producto").get("proveedor").get("id"), proveedorId));
            }

            if (tipo != null) {
                condiciones.add(cb.equal(root.get("tipo"), tipo));
            }

            if (motivo != null) {
                condiciones.add(cb.equal(root.get("motivo"), motivo));
            }

            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }

            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
