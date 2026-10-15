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

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>,
                                              JpaSpecificationExecutor<Movimiento> {

    /**
     * RN-01, RF-20: la existencia de UN producto.
     *
     * Suma las entradas y resta las salidas. coalesce devuelve 0
     * cuando el producto todavia no tiene movimientos, en lugar de null.
     */
    @Query("""
           select coalesce(sum(case when m.tipo = co.edu.javeriana.inventario.model.TipoMovimiento.ENTRADA
                                    then m.cantidad else -m.cantidad end), 0)
           from Movimiento m
           where m.producto.id = :productoId
           """)
    long calcularExistencia(@Param("productoId") Long productoId);

    /**
     * Las existencias de TODO el catalogo en una sola consulta agrupada.
     *
     * Es lo que permite listar los productos con su existencia sin
     * hacer una consulta por producto (RNF-06).
     */
    @Query("""
           select m.producto.id as productoId,
                  sum(case when m.tipo = co.edu.javeriana.inventario.model.TipoMovimiento.ENTRADA
                           then m.cantidad else -m.cantidad end) as existencia
           from Movimiento m
           group by m.producto.id
           """)
    List<ExistenciaView> existenciasTodos();

    /**
     * Historial paginado y filtrable (RF-27, RF-28).
     *
     * El @EntityGraph trae producto, proveedor y usuario en la misma
     * consulta: sin el, cada fila del historial dispararia tres
     * consultas extra al construir la respuesta.
     */
    @Override
    @EntityGraph(attributePaths = {"producto", "producto.proveedor", "usuario"})
    Page<Movimiento> findAll(Specification<Movimiento> spec, Pageable pageable);

    /** RN-11: impide cambiar el SKU de un producto que ya tiene historial. */
    boolean existsByProductoId(Long productoId);
}
