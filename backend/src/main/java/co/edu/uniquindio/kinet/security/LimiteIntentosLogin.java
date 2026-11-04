package co.edu.uniquindio.kinet.security;

import co.edu.uniquindio.kinet.exceptions.DemasiadosIntentosException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena la adivinación de contraseñas: después de 5 intentos fallidos con el mismo correo, ese correo no puede
 * iniciar sesión durante 15 minutos (429). Un inicio de sesión correcto borra el contador.
 * Se guarda en memoria: suficiente para una sola instancia del backend (despliegue actual).
 */
@Component
public class LimiteIntentosLogin {

    public static final int MAX_INTENTOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant desde) {
    }

    private final Map<String, Registro> fallos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteIntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    /** Lanza 429 si el correo agotó sus intentos y la ventana de 15 minutos sigue abierta. */
    public void verificar(String correo) {
        Registro r = fallos.get(correo);
        if (r == null) {
            return;
        }
        if (vencido(r)) {
            fallos.remove(correo);
            return;
        }
        if (r.fallos() >= MAX_INTENTOS) {
            long minutos = Math.max(1, Duration.between(Instant.now(reloj), r.desde().plus(BLOQUEO)).toMinutes() + 1);
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en "
                    + minutos + (minutos == 1 ? " minuto" : " minutos") + " o recupera tu contraseña.");
        }
    }

    public void registrarFallo(String correo) {
        fallos.compute(correo, (k, r) -> r == null || vencido(r)
                ? new Registro(1, Instant.now(reloj))
                : new Registro(r.fallos() + 1, r.desde()));
    }

    public void limpiar(String correo) {
        fallos.remove(correo);
    }

    /** Borra todos los contadores (lo usan las pruebas para empezar cada caso desde cero). */
    public void limpiarTodo() {
        fallos.clear();
    }

    private boolean vencido(Registro r) {
        return r.desde().plus(BLOQUEO).isBefore(Instant.now(reloj));
    }
}
