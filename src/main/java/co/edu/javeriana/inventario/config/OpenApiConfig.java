package co.edu.javeriana.inventario.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de Swagger UI (documentacion tecnica de la rubrica).
 *
 * El @SecurityScheme agrega el boton "Authorize" en /swagger-ui.html:
 * se pega el token una vez y todas las peticiones de prueba lo llevan.
 * Sin esto, Swagger solo sirve para ver la lista de endpoints, no
 * para probarlos.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Sistema de Inventario para Tienda de Barrio",
                version = "0.2.0",
                description = "Entrega 2 - Principios de Desarrollo de Software"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
