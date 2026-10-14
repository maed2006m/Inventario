package co.edu.javeriana.inventario.mapper;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProductoResponse;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el DTO de producto y la entidad Producto.
 * No contiene reglas de negocio ni consulta la base de datos:
 * el servicio le entrega el proveedor ya validado, y la existencia y el estado ya calculados.
 */
@Component
public class ProductoMapper {

    /** Crea una entidad nueva con el proveedor que el servicio ya validó (RN-06). Nace activa. */
    public Producto aEntidad(ProductoRequest request, Proveedor proveedor) {
        Producto producto = new Producto();
        copiarCampos(request, proveedor, producto);
        producto.setActivo(true);
        return producto;
    }

    /** Copia los datos editables sobre un producto que ya existe (RF-09). No toca id, activo ni fechas. */
    public void actualizar(Producto producto, ProductoRequest request, Proveedor proveedor) {
        copiarCampos(request, proveedor, producto);
    }

    /**
     * Convierte la entidad en la respuesta que ve el cliente (RF-12, RF-20, RF-23).
     * La existencia y el estado se reciben calculados: no existen en la entidad (RN-01, RN-10).
     */
    public ProductoResponse aRespuesta(Producto producto, int existencia, EstadoStock estado) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getCategoria(),
                producto.getProveedor().getId(),
                producto.getProveedor().getNombre(),
                producto.getStockMinimo(),
                producto.getCostoUnitario(),
                producto.getPrecioVenta(),
                producto.isActivo(),
                existencia,
                estado);
    }

    /** Los campos que el cliente puede enviar. Se escribe una sola vez para crear y editar. */
    private void copiarCampos(ProductoRequest request, Proveedor proveedor, Producto producto) {
        producto.setSku(request.sku());
        producto.setNombre(request.nombre());
        producto.setCategoria(request.categoria());
        producto.setProveedor(proveedor);
        producto.setStockMinimo(request.stockMinimo());
        producto.setCostoUnitario(request.costoUnitario());
        producto.setPrecioVenta(request.precioVenta());
    }
}