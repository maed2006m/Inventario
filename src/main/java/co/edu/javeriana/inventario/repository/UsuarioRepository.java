package co.edu.javeriana.inventario.repository;

import co.edu.javeriana.inventario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de usuarios.
 *
 * findByUsername es una consulta derivada: Spring Data JPA lee el
 * nombre del metodo y genera el SELECT ... WHERE username = ?
 * sin que escribamos SQL.
 *
 * Devuelve Optional porque el usuario puede no existir; asi se
 * obliga a quien lo llama a decidir que hacer en ese caso.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
