package co.edu.javeriana.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion del catalogo de productos (HU04 a HU07, HU11).
 *
 * Los listados se filtran siempre por un proveedor o una categoria creados
 * en la propia prueba: la base es compartida y no se pueden suponer conteos
 * globales.
 */
class ProductoApiIT extends ApiIntegracionBase {

    private List<Long> ids(String ruta) throws Exception {
        JsonNode lista = leer(enviar(HttpMethod.GET, ruta, tokenTendero(), null).andExpect(status().isOk()));
        List<Long> ids = new ArrayList<>();
        lista.forEach(p -> ids.add(p.get("id").asLong()));
        return ids;
    }

    // ---------- consulta ----------

    @Test
    @DisplayName("HU05: consultar un producto existente devuelve sus datos, existencia y estado")
    void obtenerProductoExistente() throws Exception {
        long proveedorId = crearProveedor();
        String sku = "OBT-" + sufijo();
        long id = crearProducto(proveedorId, sku, "Granos", 3);

        enviar(HttpMethod.GET, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.categoria").value("Granos"))
                .andExpect(jsonPath("$.proveedorId").value(proveedorId))
                .andExpect(jsonPath("$.existencia").value(0))
                .andExpect(jsonPath("$.estado").value("AGOTADO"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("HU05: consultar un producto inexistente responde 404 con accion correctiva")
    void obtenerProductoInexistente() throws Exception {
        enviar(HttpMethod.GET, "/api/productos/" + ID_INEXISTENTE, tokenTendero(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    // ---------- listado y filtros (RF-24) ----------

    @Test
    @DisplayName("RF-24: el listado filtra por proveedor, texto, categoria y estado combinados")
    void listadoConFiltros() throws Exception {
        long proveedorId = crearProveedor();
        String marca = sufijo();
        String categoria = "Cat-" + marca;
        long arroz = crearProducto(proveedorId, "ARZ-" + marca, categoria, 3);
        long frijol = crearProducto(proveedorId, "FRJ-" + marca, "Otra-" + marca, 3);
        registrar(arroz, "ENTRADA", "COMPRA", 10).andExpect(status().isCreated());

        String base = "/api/productos?proveedorId=" + proveedorId;
        assertEquals(List.of(arroz, frijol), ids(base), "Ordenados por nombre");

        // Texto: busca en SKU sin distinguir mayusculas
        assertEquals(List.of(arroz), ids(base + "&q=arz-" + marca));
        // Texto en blanco se ignora
        assertEquals(2, ids(base + "&q= &categoria= ").size());

        assertEquals(List.of(arroz), ids("/api/productos?categoria=" + categoria));
        assertEquals(List.of(arroz), ids(base + "&estado=EN_STOCK"));
        assertEquals(List.of(frijol), ids(base + "&estado=AGOTADO"));
    }

    @Test
    @DisplayName("RF-10: un producto deshabilitado solo aparece en el listado con soloActivos=false")
    void listadoConDeshabilitados() throws Exception {
        long proveedorId = crearProveedor();
        long id = crearProducto(proveedorId, "DES-" + sufijo(), null, 1);

        enviar(HttpMethod.DELETE, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(status().isNoContent());

        assertTrue(ids("/api/productos?proveedorId=" + proveedorId).isEmpty());
        assertEquals(List.of(id), ids("/api/productos?soloActivos=false&proveedorId=" + proveedorId));
    }

    @Test
    @DisplayName("HU11: las alertas incluyen productos agotados y en bajo stock, no los que estan en stock")
    void alertasDeStock() throws Exception {
        long proveedorId = crearProveedor();
        String marca = sufijo();
        long agotado = crearProducto(proveedorId, "AG-" + marca, null, 3);
        long bajo = crearProducto(proveedorId, "BJ-" + marca, null, 3);
        long enStock = crearProducto(proveedorId, "OK-" + marca, null, 3);
        registrar(bajo, "ENTRADA", "COMPRA", 2).andExpect(status().isCreated());
        registrar(enStock, "ENTRADA", "COMPRA", 20).andExpect(status().isCreated());

        List<Long> alertas = ids("/api/productos/alertas");

        assertTrue(alertas.contains(agotado), "Agotado debe estar en alertas");
        assertTrue(alertas.contains(bajo), "Bajo stock debe estar en alertas");
        assertFalse(alertas.contains(enStock), "En stock no debe estar en alertas");
    }

    // ---------- creacion ----------

    @Test
    @DisplayName("RN-09: crear un producto con un SKU repetido responde 409")
    void crearConSkuRepetido() throws Exception {
        long proveedorId = crearProveedor();
        String sku = "DUP-" + sufijo();
        crearProducto(proveedorId, sku, null, 1);

        enviar(HttpMethod.POST, "/api/productos", tokenTendero(), producto(sku, "Otro", null, proveedorId, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RN-06: no se puede crear un producto con un proveedor inactivo")
    void crearConProveedorInactivo() throws Exception {
        long proveedorId = crearProveedor();
        inactivarProveedor(proveedorId);

        enviar(HttpMethod.POST, "/api/productos", tokenTendero(),
                producto("INA-" + sufijo(), "Producto", null, proveedorId, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    // ---------- edicion (HU06) ----------

    @Test
    @DisplayName("HU06: editar un producto actualiza sus datos y conserva la existencia")
    void editarProducto() throws Exception {
        long proveedorId = crearProveedor();
        String sku = "EDI-" + sufijo();
        long id = crearProducto(proveedorId, sku, null, 3);
        registrar(id, "ENTRADA", "COMPRA", 5).andExpect(status().isCreated());

        Map<String, Object> cambios = producto(sku, "Nombre editado", "Lacteos", proveedorId, 2);
        cambios.put("precioVenta", 2500);

        enviar(HttpMethod.PUT, "/api/productos/" + id, tokenTendero(), cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre editado"))
                .andExpect(jsonPath("$.categoria").value("Lacteos"))
                .andExpect(jsonPath("$.precioVenta").value(2500))
                .andExpect(jsonPath("$.existencia").value(5))
                .andExpect(jsonPath("$.estado").value("EN_STOCK"));
    }

    @Test
    @DisplayName("HU06: no se puede cambiar el SKU de un producto con movimientos")
    void editarSkuConMovimientos() throws Exception {
        long proveedorId = crearProveedor();
        long id = crearProducto(proveedorId, "MOV-" + sufijo(), null, 1);
        registrar(id, "ENTRADA", "COMPRA", 1).andExpect(status().isCreated());

        enviar(HttpMethod.PUT, "/api/productos/" + id, tokenTendero(),
                producto("NUEVO-" + sufijo(), "Producto", null, proveedorId, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("HU06: cambiar el SKU por el de otro producto responde 409")
    void editarConSkuDeOtroProducto() throws Exception {
        long proveedorId = crearProveedor();
        String skuOcupado = "OCU-" + sufijo();
        crearProducto(proveedorId, skuOcupado, null, 1);
        long id = crearProducto(proveedorId, "LIB-" + sufijo(), null, 1);

        enviar(HttpMethod.PUT, "/api/productos/" + id, tokenTendero(),
                producto(skuOcupado, "Producto", null, proveedorId, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(skuOcupado)));
    }

    @Test
    @DisplayName("HU06: reasignar un producto a un proveedor inexistente responde 404")
    void editarConProveedorInexistente() throws Exception {
        String sku = "PRV-" + sufijo();
        long id = crearProducto(crearProveedor(), sku, null, 1);

        enviar(HttpMethod.PUT, "/api/productos/" + id, tokenTendero(),
                producto(sku, "Producto", null, ID_INEXISTENTE, 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RN-11: no se puede reasignar un producto a un proveedor inactivo")
    void editarConProveedorInactivo() throws Exception {
        String sku = "RN11-" + sufijo();
        long id = crearProducto(crearProveedor(), sku, null, 1);
        long inactivo = crearProveedor();
        inactivarProveedor(inactivo);

        enviar(HttpMethod.PUT, "/api/productos/" + id, tokenTendero(),
                producto(sku, "Producto", null, inactivo, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    // ---------- deshabilitar y habilitar (RF-10, RF-11) ----------

    @Test
    @DisplayName("RF-11: deshabilitar y volver a habilitar un producto conserva su existencia")
    void deshabilitarYHabilitar() throws Exception {
        long id = crearProducto();
        registrar(id, "ENTRADA", "COMPRA", 4).andExpect(status().isCreated());

        enviar(HttpMethod.DELETE, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(status().isNoContent());
        enviar(HttpMethod.GET, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(jsonPath("$.activo").value(false));

        enviar(HttpMethod.PATCH, "/api/productos/" + id + "/habilitar", tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.existencia").value(4));
    }

    @Test
    @DisplayName("RN-11: no se puede habilitar un producto cuyo proveedor esta inactivo")
    void habilitarConProveedorInactivo() throws Exception {
        long proveedorId = crearProveedor();
        long id = crearProducto(proveedorId, "HAB-" + sufijo(), null, 1);

        enviar(HttpMethod.DELETE, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(status().isNoContent());
        inactivarProveedor(proveedorId);   // ya no tiene productos activos

        enviar(HttpMethod.PATCH, "/api/productos/" + id + "/habilitar", tokenTendero(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RF-10: deshabilitar o habilitar un producto inexistente responde 404")
    void deshabilitarInexistente() throws Exception {
        enviar(HttpMethod.DELETE, "/api/productos/" + ID_INEXISTENTE, tokenTendero(), null)
                .andExpect(status().isNotFound());
        enviar(HttpMethod.PATCH, "/api/productos/" + ID_INEXISTENTE + "/habilitar", tokenTendero(), null)
                .andExpect(status().isNotFound());
    }
}
