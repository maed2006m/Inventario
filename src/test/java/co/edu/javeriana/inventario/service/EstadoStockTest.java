package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.model.EstadoStock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RN-10, RF-23: estado de stock en sus casos limite.
 *
 * Una prueba parametrizada corre el mismo metodo con varios juegos de
 * datos. Los casos elegidos son justo las fronteras entre estados,
 * que es donde se esconden los errores de "menor que" contra
 * "menor o igual que".
 */
class EstadoStockTest {

    @ParameterizedTest(name = "existencia {0}, minimo {1} -> {2}")
    @CsvSource({
            "0,  10, AGOTADO",      // sin unidades
            "1,  10, BAJO_STOCK",   // apenas una unidad
            "9,  10, BAJO_STOCK",   // justo debajo del minimo
            "10, 10, BAJO_STOCK",   // exactamente en el minimo: sigue siendo bajo
            "11, 10, EN_STOCK",     // una unidad por encima
            "0,  0,  AGOTADO"       // producto sin minimo definido
    })
    void estadoSegunExistencia(int existencia, int minimo, EstadoStock esperado) {
        assertEquals(esperado, ProductoService.estadoDe(existencia, minimo));
    }
}
