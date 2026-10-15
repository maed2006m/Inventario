package co.edu.javeriana.inventario.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Genera y valida los tokens JWT (RNF-09).
 *
 * Un JWT tiene tres partes separadas por puntos:
 *   cabecera.contenido.firma
 *
 * Las dos primeras son texto codificado en Base64, NO cifrado:
 * cualquiera puede pegar el token en jwt.io y leer el usuario y el rol.
 * Lo que nadie puede hacer sin la clave secreta es producir una firma
 * valida, asi que el token no se puede modificar ni fabricar.
 *
 * Por eso dentro del token nunca va informacion sensible.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutos;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiracion-minutos}") long minutos) {

        // HMAC-SHA256 exige una clave de al menos 256 bits = 32 caracteres.
        // Si el secreto es mas corto, JJWT lanza WeakKeyException al arrancar.
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutos = minutos;
    }

    /**
     * Crea un token firmado para un usuario.
     *
     * subject = username  -> quien es
     * claim "rol"         -> que permisos tiene
     * expiration          -> hasta cuando vale
     */
    public String generar(String username, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /**
     * Verifica la firma y la expiracion, y devuelve el contenido.
     * Lanza JwtException si el token fue alterado, esta vencido
     * o no es un JWT valido.
     */
    public Claims leer(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getMinutos() {
        return minutos;
    }
}
