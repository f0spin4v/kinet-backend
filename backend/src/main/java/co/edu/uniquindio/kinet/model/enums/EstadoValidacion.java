package co.edu.uniquindio.kinet.model.enums;

/**
 * Estado de un entrenamiento. EN_CURSO: el cronómetro está corriendo. Al finalizar, el anti-trampa lo deja
 * VALIDADO, RECHAZADO o PENDIENTE (espera la revisión de un entrenador).
 */
public enum EstadoValidacion {
    EN_CURSO, PENDIENTE, VALIDADO, RECHAZADO
}
