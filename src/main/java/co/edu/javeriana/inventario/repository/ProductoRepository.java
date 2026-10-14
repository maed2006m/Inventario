package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de productos.
 * JpaSpecificationExecutor permite construir filtros dinámicos (texto, proveedor, categoría)
 * sin escribir una consulta por cada combinación.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /**
     * RN-14: busca el producto y bloquea su fila (SELECT ... FOR UPDATE) hasta que termine la transacción.
     * Así, dos salidas simultáneas del mismo producto se ejecutan una después de la otra
     * y la segunda se valida contra la existencia real.
     * Debe llamarse dentro de un método @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    /** Busca un producto trayendo su proveedor en la misma consulta (evita el problema N+1). */
    @Query("select p from Producto p join fetch p.proveedor where p.id = :id")
    Optional<Producto> findByIdConProveedor(@Param("id") Long id);

    /** Listado filtrable que carga el proveedor con un JOIN y no con una consulta por producto. */
    @Override
    @EntityGraph(attributePaths = "proveedor")
    List<Producto> findAll(Specification<Producto> spec, Sort sort);

    /** RN-09: el SKU es único. Se usa al crear. */
    boolean existsBySku(String sku);

    /** RN-09 al editar: busca el SKU en otros productos. */
    boolean existsBySkuAndIdNot(String sku, Long id);

    /** RN-07: cuántos productos activos tiene un proveedor, para impedir inactivarlo. */
    long countByProveedorIdAndActivoTrue(Long proveedorId);
}