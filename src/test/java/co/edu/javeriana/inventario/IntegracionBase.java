package co.edu.javeriana.inventario;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de integracion.
 *
 * REQUISITO: Docker Desktop debe estar corriendo. Testcontainers
 * levanta un PostgreSQL 16 real, aplica las migraciones de Flyway y
 * ejecuta las pruebas contra el. Si Docker esta apagado, todas estas
 * clases fallan al arrancar.
 *
 * Por que una base real y no H2 en memoria: el proyecto depende de
 * triggers, de una vista y de SELECT ... FOR UPDATE. Nada de eso se
 * comporta igual en una base distinta, asi que probar contra H2
 * daria una falsa sensacion de seguridad.
 *
 * @ServiceConnection inyecta automaticamente la url, el usuario y la
 * contrasena del contenedor; no hay que configurar el datasource.
 *
 * El contenedor es static: se levanta una vez y lo comparten todas
 * las pruebas de la clase, en lugar de arrancar uno por cada metodo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegracionBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
}
