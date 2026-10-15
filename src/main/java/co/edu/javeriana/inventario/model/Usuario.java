package co.edu.javeriana.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Usuario del sistema.
 *
 * Mapea la tabla "usuarios" creada en V1__esquema_inicial.sql.
 * Como application.yaml usa ddl-auto: validate, los nombres y tipos
 * de esta clase deben coincidir exactamente con la tabla o la
 * aplicacion no arranca.
 *
 * La contrasena nunca se guarda en claro: la columna password_hash
 * almacena el hash BCrypt (ver V2__usuarios_iniciales.sql).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    /**
     * EnumType.STRING guarda el texto "TENDERO" o "ADMIN".
     * Con EnumType.ORDINAL guardaria 0 o 1 y romperia el CHECK de la tabla.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }
}
