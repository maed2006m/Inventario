package co.edu.javeriana.inventario;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

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
 * El contenedor es un SINGLETON: se arranca una sola vez en el bloque
 * static y se comparte entre TODAS las clases de integracion. Spring
 * guarda el contexto en cache y lo reutiliza entre clases; si el
 * contenedor se apagara al terminar la primera clase, las siguientes
 * encontrarian la base apagada (Connection refused). Testcontainers lo
 * elimina solo al cerrar la JVM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracionBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static {
        postgres.start();
    }
}