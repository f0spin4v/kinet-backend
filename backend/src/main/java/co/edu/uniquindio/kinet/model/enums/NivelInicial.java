package co.edu.uniquindio.kinet.model.enums;

/** Nivel de entrada que asigna la evaluación inicial (Placement Match). */
public enum NivelInicial {
    INICIACION,
    INTERMEDIO,
    AVANZADO;

    /** 0 a 1 puntos: Iniciación. 2 a 3: Intermedio. 4 a 5: Avanzado. */
    public static NivelInicial segunPuntaje(int puntaje) {
        if (puntaje <= 1) {
            return INICIACION;
        }
        if (puntaje <= 3) {
            return INTERMEDIO;
        }
        return AVANZADO;
    }
}
