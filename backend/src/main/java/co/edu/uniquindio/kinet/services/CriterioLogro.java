package co.edu.uniquindio.kinet.services;

import java.util.Locale;

/**
 * Criterio evaluable de un logro, guardado como texto "METRICA>=VALOR" (por ejemplo "RACHA>=3").
 * Así el administrador crea logros nuevos sin tocar el código.
 */
public record CriterioLogro(Metrica metrica, long valor) {

    public enum Metrica {
        /** Entrenamientos validados. */
        ENTRENAMIENTOS,
        /** Minutos de entrenamiento validado. */
        MINUTOS,
        /** Racha máxima de días seguidos. */
        RACHA,
        /** XP total. */
        XP,
        /** Nivel del jugador. */
        NIVEL,
        /** Retos completados. */
        RETOS,
        /** Enfrentamientos ganados. */
        VICTORIAS,
        /** Clanes a los que se ha unido. */
        CLAN
    }

    public static CriterioLogro leer(String criterio) {
        String[] partes = criterio.trim().toUpperCase(Locale.ROOT).split(">=");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Criterio de logro inválido: " + criterio);
        }
        return new CriterioLogro(Metrica.valueOf(partes[0].trim()), Long.parseLong(partes[1].trim()));
    }

    public boolean seCumple(long valorActual) {
        return valorActual >= valor;
    }
}
