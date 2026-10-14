package co.edu.uniquindio.kinet.dto;

/**
 * Envoltura de todas las respuestas de la API, como en las guías del curso.
 * error = false: "respuesta" trae el resultado. error = true: "respuesta" trae el mensaje o la lista de errores.
 */
public record ResponseDTO<T>(boolean error, T respuesta) {
}
