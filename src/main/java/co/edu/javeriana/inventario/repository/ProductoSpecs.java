package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinámicos para el listado de productos (RF-24).
 * Cada filtro es opcional: solo se agrega a la consulta si el usuario lo envió.
 */
public final class ProductoSpecs {

    private ProductoSpecs() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Construye el filtro combinando las condiciones recibidas.
     *
     * @param q           texto a buscar dentro del nombre o del SKU (sin distinguir mayúsculas); null o vacío lo ignora
     * @param proveedorId solo productos de ese proveedor; null lo ignora
     * @param categoria   solo productos de esa categoría; null o vacío lo ignora
     * @param soloActivos si es true, oculta los productos deshabilitados
     */
    public static Specification<Producto> filtrar(String q, Long proveedorId, String categoria, boolean soloActivos) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("sku")), patron)));
            }
            if (proveedorId != null) {
                condiciones.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (categoria != null && !categoria.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("categoria")), categoria.trim().toLowerCase()));
            }
            if (soloActivos) {
                condiciones.add(cb.isTrue(root.get("activo")));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}