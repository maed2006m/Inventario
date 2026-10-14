package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** RN-09: el NIT es unico. Se verifica antes de insertar. */
    boolean existsByNit(String nit);

    /** Para validar el NIT al editar sin chocar con el propio registro. */
    boolean existsByNitAndIdNot(String nit, Long id);

    List<Proveedor> findByActivoTrueOrderByNombre();

    List<Proveedor> findAllByOrderByNombre();
}
