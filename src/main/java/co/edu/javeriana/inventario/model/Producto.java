package co.edu.javeriana.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Articulo del catalogo de la tienda.
 *
 * NO tiene campo "existencia" (RN-01, RF-20). La existencia se
 * calcula sumando entradas y restando salidas de sus movimientos.
 * Que la columna no exista en la clase ni en la tabla es lo que
 * garantiza por diseno que nadie pueda digitarla.
 *
 * Los precios son BigDecimal y no double: con double, 0.1 + 0.2
 * no da exactamente 0.3, y en dinero eso es inaceptable.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 60)
    private String categoria;

    @Column(nullable = false, length = 20)
    private String unidad = "UNIDAD";

    /**
     * LAZY: al traer un producto NO se trae su proveedor, salvo que
     * alguien lo pida. Evita el problema N+1 al listar el catalogo.
     * Cuando si se necesita, se usa @EntityGraph en el repositorio.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

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
