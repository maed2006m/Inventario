package co.edu.javeriana.inventario.repository;

/**
 * Proyeccion de solo lectura: producto -> existencia.
 *
 * Spring Data crea la implementacion automaticamente a partir de los
 * alias de la consulta ("as productoId", "as existencia"). Permite
 * traer solo esos dos valores en lugar de entidades completas.
 */
public interface ExistenciaView {

    Long getProductoId();

    Long getExistencia();
}
