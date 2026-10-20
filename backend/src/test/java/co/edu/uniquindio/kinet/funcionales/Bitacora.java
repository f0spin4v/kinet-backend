package co.edu.uniquindio.kinet.funcionales;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que pasa dentro de cada paso (peticiones HTTP, respuestas, consultas a la base de datos), para mostrarlo en
 * consola debajo del paso. Los pasos escriben aquí y ResumenLigero lo imprime y lo vacía al terminar cada paso.
 * Los escenarios corren uno a la vez, así que basta una lista compartida.
 */
public final class Bitacora {

    private static final int MAXIMO = 160;
    private static final List<String> LINEAS = new ArrayList<>();

    private Bitacora() {
    }

    /** Una petición a la API y su respuesta. Los cuerpos llegan ya como JSON. */
    public static synchronized void http(String metodo, String url, String enviado, int estado, String razon,
                                         String recibido) {
        LINEAS.add("→ " + metodo + " " + url);
        if (enviado != null && !enviado.isBlank()) {
            LINEAS.add("  datos:     " + limpiar(enviado));
        }
        LINEAS.add("← " + estado + " " + razon);
        if (recibido != null && !recibido.isBlank()) {
            LINEAS.add("  respuesta: " + limpiar(recibido));
        }
    }

    /** Una verificación o consulta a la base de datos, en texto libre. */
    public static synchronized void nota(String texto) {
        LINEAS.add("• " + texto);
    }

    public static synchronized List<String> vaciar() {
        List<String> copia = List.copyOf(LINEAS);
        LINEAS.clear();
        return copia;
    }

    /** Acorta los tokens JWT, oculta contraseñas y corta los textos largos. */
    private static String limpiar(String json) {
        String texto = json
                .replaceAll("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+", "eyJ…(JWT)")
                .replaceAll("\"(contrasena\\w*|nuevaContrasena)\":\"[^\"]*\"", "\"$1\":\"••••\"");
        return texto.length() > MAXIMO ? texto.substring(0, MAXIMO) + "…" : texto;
    }
}
