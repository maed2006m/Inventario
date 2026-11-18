package co.edu.javeriana.inventario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * TEMPORAL: prueba que falla a proposito para validar el pipeline de
 * Jenkins (guia, "Validacion del Pipeline"): si una prueba falla, el
 * pipeline debe detenerse en la etapa Pruebas y NO desplegar.
 * Este commit se revierte despues de la validacion.
 */
class ValidacionPipelineTest {

    @Test
    @DisplayName("Falla a proposito para comprobar que Jenkins no despliega")
    void fallaAProposito() {
        fail("Prueba fallida a proposito: el pipeline no debe llegar a Deploy.");
    }
}
