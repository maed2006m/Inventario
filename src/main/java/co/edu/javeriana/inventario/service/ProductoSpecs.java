package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.model.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinamicos del catalogo (RF-24).
 *
 * Una Specification construye el WHERE en tiempo de ejecucion segun
 * los filtros que llegaron. La alternativa seria escribir una consulta
 * distinta por cada combinacion posible de filtros, que con cuatro
 * filtros serian dieciseis consultas.
 */
public final class ProductoSpecs {

    private ProductoSpecs() {
    }

    public static Specification<Producto> filtrar(String texto, Long proveedorId,
                                                  String categoria, boolean soloActivos) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            // Busqueda por nombre o SKU, sin distinguir mayusculas.
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.toLowerCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("sku")), patron)
                ));
            }

            if (proveedorId != null) {
                condiciones.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }

            if (categoria != null && !categoria.isBlank()) {
                condiciones.add(cb.equal(root.get("categoria"), categoria));
            }

            if (soloActivos) {
                condiciones.add(cb.isTrue(root.get("activo")));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
