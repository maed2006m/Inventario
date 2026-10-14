package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long>,
                                            JpaSpecificationExecutor<Producto> {

    /**
     * RN-14: bloqueo pesimista de la fila del producto.
     *
     * Genera un SELECT ... FOR UPDATE. La transaccion que lo ejecuta
     * retiene la fila hasta terminar; cualquier otra que pida el mismo
     * producto espera.
     *
     * Sin esto, dos ventas simultaneas leerian la misma existencia,
     * ambas pasarian la validacion y el stock quedaria negativo.
     * Es la condicion de carrera que prueba la clase MovimientoApiIT.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    /**
     * @EntityGraph trae el proveedor en la MISMA consulta (un JOIN),
     * en lugar de una consulta extra por cada producto.
     *
     * Esta es la solucion al problema N+1 que el informe debe
     * documentar: sin ella, listar 50 productos genera 51 consultas.
     */
    @Override
    @EntityGraph(attributePaths = "proveedor")
    List<Producto> findAll(org.springframework.data.jpa.domain.Specification<Producto> spec,
                           org.springframework.data.domain.Sort sort);

    @EntityGraph(attributePaths = "proveedor")
    Optional<Producto> findWithProveedorById(Long id);

    /** RN-09: el SKU es unico. */
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** RN-07: cuantos productos activos tiene un proveedor. */
    long countByProveedorIdAndActivoTrue(Long proveedorId);
}
