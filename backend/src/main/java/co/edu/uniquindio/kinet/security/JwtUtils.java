package co.edu.uniquindio.kinet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Genera y valida los tokens JWT de sesión (RNF-22). */
@Component
public class JwtUtils {

    /** El secreto que traía el código antes de la auditoría: es público, así que nunca se acepta. */
    private static final String SECRETO_PUBLICADO = "clave-solo-para-desarrollo-local-kinet-2026-cambiar";

    private final SecretKey clave;
    private final Duration expiracion;

    /**
     * El secreto viene SIEMPRE de la variable KINET_JWT_SECRET (sin valor por defecto en el código). Si falta, es
     * corto o es el secreto que se publicó, la API no arranca: así nunca se firma con una clave conocida.
     */
    public JwtUtils(@Value("${kinet.jwt.secret:}") String secreto,
                    @Value("${kinet.jwt.expiracion-minutos}") long minutos) {
        if (secreto == null || secreto.isBlank() || secreto.length() < 32 || SECRETO_PUBLICADO.equals(secreto)) {
            throw new IllegalStateException("Define la variable KINET_JWT_SECRET con un secreto propio de al menos "
                    + "32 caracteres (por ejemplo: openssl rand -base64 48). La API no arranca sin ella.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracion = Duration.ofMinutes(minutos);
    }

    public String generarToken(String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los datos del token, o vacío si está vencido, alterado o mal formado. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
