package co.edu.javeriana.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entrada o salida de mercancia. Es el hecho contable del sistema:
 * la existencia de cada producto se deriva de estas filas.
 *
 * @Immutable (RN-05, RNF-04): Hibernate nunca genera UPDATE sobre
 * esta entidad. Un movimiento mal digitado no se edita; se corrige
 * registrando otro movimiento que lo compense.
 *
 * La inmutabilidad se defiende en tres niveles:
 *   1. No hay endpoints PUT ni DELETE en MovimientoController.
 *   2. Esta anotacion impide que Hibernate actualice la fila.
 *   3. El trigger trg_movimientos_inmutables la bloquea en la base.
 */
@Entity
@Table(name = "movimientos")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoMovimiento motivo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "valor_unitario", nullable = false)
    private BigDecimal valorUnitario;

    @Column(length = 250)
    private String observacion;

    /** Quien registro el movimiento. Se toma del token, no del cliente. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }
}
