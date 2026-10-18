package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.PlataformaDeporte;
import co.edu.uniquindio.kinet.repositories.PlataformaDeporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reconoce los enlaces a una actividad en una app deportiva. Las apps aceptadas salen de la tabla
 * PLATAFORMA_DEPORTE (dominio y patrón de la ruta), así que agregar una no requiere tocar el código.
 * Solo revisa la forma del enlace; no consulta a la app, así que no sabe si la actividad existe.
 */
@Component
@RequiredArgsConstructor
public class EnlaceDeporte {

    static final int LONGITUD_MAXIMA = 500;

    private final PlataformaDeporteRepository plataformaRepository;

    /** Un enlace aceptado: la app que lo reconoció y su forma normalizada (sin parámetros ni mayúsculas en el host). */
    public record Reconocido(PlataformaDeporte plataforma, String url) {
    }

    public Reconocido reconocer(String texto) {
        URI uri = leer(texto == null ? "" : texto.trim());
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String ruta = uri.getPath() == null ? "" : uri.getPath();
        List<PlataformaDeporte> plataformas = plataformaRepository.findByActivaOrderByIdAsc(true);
        for (PlataformaDeporte p : plataformas) {
            if (esDelDominio(host, p.getDominio()) && Pattern.compile(p.getPatronRuta()).matcher(ruta).matches()) {
                return new Reconocido(p, "https://" + host + ruta);
            }
        }
        boolean appConocida = plataformas.stream().anyMatch(p -> esDelDominio(host, p.getDominio()));
        throw new ReglaNegocioException(appConocida
                ? "El enlace no es de una actividad. Copia el enlace de la actividad terminada en la app."
                : "El enlace no es de una app deportiva reconocida (" + nombres(plataformas) + ").");
    }

    private static URI leer(String texto) {
        if (texto.isEmpty() || texto.length() > LONGITUD_MAXIMA) {
            throw new ReglaNegocioException("Pega el enlace de la actividad (hasta " + LONGITUD_MAXIMA + " caracteres).");
        }
        URI uri;
        try {
            uri = new URI(texto);
        } catch (URISyntaxException e) {
            throw new ReglaNegocioException("El enlace no es válido.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new ReglaNegocioException("El enlace debe empezar con https://.");
        }
        if (uri.getUserInfo() != null) {
            throw new ReglaNegocioException("El enlace no puede llevar usuario ni contraseña.");
        }
        return uri;
    }

    /** El host es el dominio o un subdominio suyo (www.strava.com sí; strava.com.otro.com no). */
    private static boolean esDelDominio(String host, String dominio) {
        return host.equals(dominio) || host.endsWith("." + dominio);
    }

    private static String nombres(List<PlataformaDeporte> plataformas) {
        return plataformas.stream().map(PlataformaDeporte::getNombre).distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
