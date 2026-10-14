package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.model.enums.Intensidad;

import java.time.LocalDate;

/**
 * Reglas de gamificación del Proceso 2, en un solo lugar para poder probarlas y ajustarlas.
 * Son funciones puras: no dependen de la base de datos.
 */
public final class ReglasJuego {

    /** Límites del anti-trampa (política interna de validación de entrenamientos). */
    public static final int DURACION_MINIMA = 10;
    public static final int DURACION_MAXIMA = 240;
    public static final int MAX_SESIONES_DIA = 4;
    public static final int MAX_MINUTOS_DIA = 300;
    public static final int HORAS_MAXIMAS_ATRAS = 48;
    public static final int MINUTOS_TOLERANCIA_FUTURO = 5;

    private ReglasJuego() {
    }

    /** XP = minutos × XP por minuto de la actividad × multiplicador de intensidad (1, 1.5 o 2). */
    public static int calcularXp(int minutos, int xpPorMinuto, Intensidad intensidad) {
        return (int) Math.round(minutos * xpPorMinuto * intensidad.getMultiplicador());
    }

    /**
     * XP acumulado necesario para llegar a un nivel.
     * Nivel 1: 0, nivel 2: 100, nivel 3: 250, nivel 4: 450, nivel 5: 700... cada nivel pide 50 XP más que el anterior.
     */
    public static int xpParaNivel(int nivel) {
        if (nivel <= 1) {
            return 0;
        }
        return 25 * (nivel - 1) * (nivel + 2);
    }

    /** Nivel que corresponde a un XP acumulado. */
    public static int nivelParaXp(int xp) {
        int nivel = 1;
        while (xpParaNivel(nivel + 1) <= xp) {
            nivel++;
        }
        return nivel;
    }

    /**
     * Nueva racha al validar un entrenamiento del día "fecha".
     * Primer entrenamiento: 1. Mismo día: se mantiene. Día siguiente: +1. Hueco de uno o más días: vuelve a 1.
     * Un entrenamiento anterior al último registrado (cargado tarde) no cambia la racha.
     */
    public static int nuevaRacha(int rachaActual, LocalDate ultimoDia, LocalDate fecha) {
        if (ultimoDia == null) {
            return 1;
        }
        if (fecha.isBefore(ultimoDia)) {
            return rachaActual;
        }
        if (fecha.isEqual(ultimoDia)) {
            return Math.max(rachaActual, 1);
        }
        if (fecha.isEqual(ultimoDia.plusDays(1))) {
            return rachaActual + 1;
        }
        return 1;
    }

    /** Racha que se muestra hoy: si no entrenó ni hoy ni ayer, la racha ya se perdió. */
    public static int rachaVigente(int racha, LocalDate ultimoDia, LocalDate hoy) {
        if (ultimoDia == null || ultimoDia.isBefore(hoy.minusDays(1))) {
            return 0;
        }
        return racha;
    }
}
