package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Acceso a datos de movimientos de inventario.
 * La existencia de un producto no se guarda: se calcula aquí sumando entradas y restando salidas (RN-01).
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    /** Resultado de la consulta agrupada: una fila por producto con su existencia. */
    interface ExistenciaView {
        Long getProductoId();
        Long getExistencia();
    }

    /**
     * Existencia de UN producto = suma de entradas menos suma de salidas.
     * coalesce devuelve 0 cuando el producto aún no tiene movimientos.
     */
    @Query("""
        select coalesce(sum(case when m.tipo = co.edu.javeriana.inventario.model.TipoMovimiento.ENTRADA
                                 then m.cantidad else -m.cantidad end), 0L)
        from Movimiento m
        where m.producto.id = :productoId
        """)
    long calcularExistencia(@Param("productoId") Long productoId);

    /**
     * Existencia de TODOS los productos en una sola consulta agrupada (RNF-06).
     * Evita hacer una consulta por producto al listar el catálogo.
     * Los productos sin movimientos no aparecen: el servicio les asigna 0.
     */
    @Query("""
        select m.producto.id as productoId,
               sum(case when m.tipo = co.edu.javeriana.inventario.model.TipoMovimiento.ENTRADA
                        then m.cantidad else -m.cantidad end) as existencia
        from Movimiento m
        group by m.producto.id
        """)
    List<ExistenciaView> existenciasTodos();

    /** Historial filtrable (Specification) y paginado, trayendo producto, proveedor y usuario en un solo JOIN. */
    @Override
    @EntityGraph(attributePaths = {"producto", "producto.proveedor", "usuario"})
    Page<Movimiento> findAll(Specification<Movimiento> spec, Pageable pageable);

    /** Movimientos de un producto, del más reciente al más antiguo según el Pageable (RF-11). */
    @EntityGraph(attributePaths = {"producto", "producto.proveedor", "usuario"})
    Page<Movimiento> findByProductoId(Long productoId, Pageable pageable);

    /** RN-11: saber si un producto ya tiene historial, para impedir cambiar su SKU. */
    boolean existsByProductoId(Long productoId);
}