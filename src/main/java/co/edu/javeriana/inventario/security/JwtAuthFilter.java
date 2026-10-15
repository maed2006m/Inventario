package co.edu.javeriana.inventario.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que lee el token de cada peticion y establece quien esta
 * autenticado (RNF-09, RNF-10).
 *
 * Hereda de OncePerRequestFilter para garantizar que se ejecute una
 * sola vez por peticion, incluso si hay reenvios internos.
 *
 * Nota importante: cuando no hay token, o el token es invalido, este
 * filtro NO lanza un error. Deja el contexto vacio y continua. Es
 * Spring Security quien decide despues si esa ruta exigia autenticacion
 * y responde 401. Gracias a eso, las rutas publicas (/api/auth/login,
 * Swagger) funcionan con el mismo filtro activo.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.leer(cabecera.substring(7));
                String username = claims.getSubject();
                String rol = claims.get("rol", String.class);

                /*
                 * El prefijo "ROLE_" es obligatorio.
                 *
                 * En SecurityConfig escribimos .hasRole("ADMIN"), y
                 * hasRole() agrega "ROLE_" automaticamente antes de
                 * comparar. Si aqui pusieramos solo "ADMIN", ninguna
                 * regla de rol coincidiria y todo responderia 403.
                 */
                var autenticacion = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                );

                SecurityContextHolder.getContext().setAuthentication(autenticacion);

            } catch (JwtException | IllegalArgumentException e) {
                // Token alterado, vencido o mal formado: sigue como anonimo.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
