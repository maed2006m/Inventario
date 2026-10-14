package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de proveedores.
 * Spring Data JPA genera la implementación en tiempo de ejecución a partir de los nombres de los métodos.
 */
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** RN-09: el NIT es único. Se usa al crear un proveedor. */
    boolean existsByNit(String nit);

    /** RN-09 al editar: busca el NIT en otros proveedores, para que no choque consigo mismo. */
    boolean existsByNitAndIdNot(String nit, Long id);

    /** Proveedores activos ordenados por nombre. Alimenta el selector del formulario de productos (RF-07). */
    List<Proveedor> findByActivoTrueOrderByNombreAsc();
}