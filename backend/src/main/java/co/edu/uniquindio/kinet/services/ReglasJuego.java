package co.edu.uniquindio.kinet.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Reglas del juego que no dependen de la base de datos, en un solo lugar para poder probarlas y ajustarlas.
 * El XP por minuto y los umbrales de nivel y de rango sí están en la base (REGLA_PUNTUACION,
 * NIVEL_PROGRESION, RANGO) porque el administrador los cambia.
 */
public final class ReglasJuego {

    // ---------- cronómetro y anti-trampa ----------
    public static final int DURACION_MAXIMA = 240;
    /** Más de esto (y hasta DURACION_MAXIMA) queda PENDIENTE de revisión por un entrenador. */
    public static final int UMBRAL_REVISION = 180;
    public static final int MAX_SESIONES_DIA = 4;
    public static final int MAX_MINUTOS_DIA = 300;
    /** Un cronómetro que sigue corriendo después de esto se cierra solo como RECHAZADO. */
    public static final int MINUTOS_CIERRE_AUTOMATICO = 300;

    // ---------- evidencias ----------
    /** XP extra por subir una imagen o un enlace de respaldo del entrenamiento. */
    public static final int BONUS_EVIDENCIA = 10;
    /** Plazo para subir la evidencia (opcional), contado desde que terminó el entrenamiento. */
    public static final int HORAS_PARA_EVIDENCIA = 48;
    /**
     * Plazo para subir el enlace de una sesión de más de UMBRAL_REVISION minutos, que sin él no se puede aprobar.
     * Pasado el plazo sin enlace, la sesión se rechaza sola.
     */
    public static final int HORAS_EVIDENCIA_SESION_LARGA = 4;
    public static final int TAMANO_MAXIMO_EVIDENCIA = 5 * 1024 * 1024;

    // ---------- evaluación ----------
    public static final int DIAS_ENTRE_EVALUACIONES = 30;
    public static final String VERSION_EVALUACION = "VOTO_PONDERADO_V1";

    // ---------- retos ----------
    /** Plazo de un reto UNICA que no tiene fecha de fin. */
    public static final int DIAS_RETO_UNICO_SIN_FIN = 30;

    // ---------- racha ----------
    /** Desde esta racha se avisa al jugador cada vez que la aumenta. */
    public static final int RACHA_AVISO = 3;

    // ---------- enfrentamientos ----------
    public static final int HORAS_ENFRENTAMIENTO = 24;
    public static final int TOPE_XP_COLOCACION = 300;
    public static final int DIVISOR_COLOCACION = 3;
    public static final int PUNTOS_VICTORIA = 25;
    public static final int PUNTOS_EMPATE = 10;
    public static final int PUNTOS_DERROTA = -15;

    // competencias amistosas (por invitación)
    public static final int MAX_COMPETENCIAS_ACTIVAS = 3;
    public static final int DIAS_MAXIMOS_PARA_INICIAR = 7;
    public static final int HORAS_RESPUESTA_DUELO = 24;
    public static final int PREMIO_DUELO = 30;
    public static final int PREMIO_GRUPAL = 50;
    public static final int PREMIO_TORNEO = 100;

    private ReglasJuego() {
    }

    /** XP = minutos x XP por minuto de la regla, redondeado. */
    public static int calcularXp(int minutos, BigDecimal xpPorMinuto) {
        return BigDecimal.valueOf(minutos).multiply(xpPorMinuto).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /** Puntos de temporada de una colocación: min(XP en la ventana, 300) / 3. */
    public static int puntosColocacion(long xp) {
        return (int) (Math.min(xp, TOPE_XP_COLOCACION) / DIVISOR_COLOCACION);
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
