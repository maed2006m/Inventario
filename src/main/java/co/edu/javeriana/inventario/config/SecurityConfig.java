package co.edu.javeriana.inventario.config;

import co.edu.javeriana.inventario.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;
import java.util.List;

/**
 * Configuracion de seguridad (RNF-09, RNF-10).
 *
 * Toda la matriz de permisos del informe vive en un solo lugar:
 * el bloque authorizeHttpRequests de abajo. Eso la hace facil de
 * leer, de probar y de explicar.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {

        http
            /*
             * CSRF protege contra peticiones que el navegador envia
             * solo, usando cookies de sesion. Esta API no usa cookies
             * sino un token que el cliente adjunta explicitamente,
             * asi que el ataque no aplica y se desactiva.
             */
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            /*
             * STATELESS: el servidor no guarda sesiones. Cada peticion
             * se autentica sola con su token. Por eso no existe un
             * "logout" del lado del servidor: cerrar sesion es borrar
             * el token en el cliente.
             */
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // --- Rutas publicas ---
                .requestMatchers("/api/auth/login").permitAll()
		.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                 .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // --- Solo ADMIN ---
                // El orden importa: estas reglas especificas van ANTES
                // de anyRequest(), que atrapa todo lo demas.
                .requestMatchers(HttpMethod.PUT,    "/api/proveedores/**").hasRole("ADMIN")  // HU2
                .requestMatchers(HttpMethod.DELETE, "/api/proveedores/**").hasRole("ADMIN")  // HU3
                .requestMatchers("/api/movimientos/export").hasRole("ADMIN")                 // HU15
                .requestMatchers("/api/reportes/**").hasRole("ADMIN")                        // HU12, HU13
                .requestMatchers("/api/analitica/**").hasRole("ADMIN")

                // --- Todo lo demas: autenticado, cualquier rol ---
                .anyRequest().authenticated()
            )

            /*
             * Sin estos dos manejadores, Spring devolveria una pagina
             * de login HTML en lugar de un codigo limpio. Una API REST
             * debe responder 401 (no autenticado) y 403 (sin permiso).
             */
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) ->
                        res.sendError(HttpServletResponse.SC_FORBIDDEN))
            )

            /*
             * Nuestro filtro va ANTES del de login por formulario de
             * Spring. Si fuera despues, Spring ya habria concluido que
             * la peticion es anonima.
             */
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt para verificar contrasenas.
     *
     * matches(textoPlano, hash) no descifra nada: vuelve a hashear el
     * texto plano con la misma sal (que viene dentro del propio hash)
     * y compara. Por eso BCrypt es de una sola via.
     *
     * Es compatible con los hashes que genera PostgreSQL en
     * V2__usuarios_iniciales.sql mediante crypt(..., gen_salt('bf', 10)).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS: permite que Angular (localhost:4200) llame a la API
     * (localhost:8080). Sin esto el navegador bloquea las peticiones.
     *
     * exposedHeaders con Content-Disposition es necesario para que la
     * descarga del CSV tome el nombre del archivo (RF-29).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
