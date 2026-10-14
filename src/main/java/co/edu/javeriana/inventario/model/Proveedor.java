package co.edu.javeriana.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Empresa que surte productos a la tienda.
 *
 * "Eliminar" un proveedor es inactivarlo (RN-07, RF-03): el campo
 * activo pasa a false y el historial de movimientos se conserva.
 * Nunca se borra la fila, porque los movimientos pasados apuntan a
 * sus productos.
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String direccion;

    /** Dias que tarda en entregar. Insumo del punto de reorden. */
    @Column(name = "tiempo_entrega_dias", nullable = false)
    private Integer tiempoEntregaDias = 2;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @PrePersist
    void alCrear() {
        OffsetDateTime ahora = OffsetDateTime.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = OffsetDateTime.now();
    }
}
