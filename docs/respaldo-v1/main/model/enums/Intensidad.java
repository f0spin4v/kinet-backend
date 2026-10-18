package co.edu.uniquindio.kinet.model.enums;

/** Intensidad declarada de un entrenamiento y su multiplicador de XP. */
public enum Intensidad {
    BAJA(1.0),
    MEDIA(1.5),
    ALTA(2.0);

    private final double multiplicador;

    Intensidad(double multiplicador) {
        this.multiplicador = multiplicador;
    }

    public double getMultiplicador() {
        return multiplicador;
    }
}
