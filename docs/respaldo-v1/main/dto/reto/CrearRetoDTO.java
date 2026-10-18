package co.edu.uniquindio.kinet.dto.reto;

import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import co.edu.uniquindio.kinet.model.enums.TipoMeta;
import jakarta.validation.constraints.*;

/** Reto nuevo creado por el administrador. codigoActividad y nivelRequerido son opcionales. */
public record CrearRetoDTO(
        @NotBlank(message = "Escribe el título del reto.")
        @Size(max = 100, message = "El título no puede tener más de 100 caracteres.")
        String titulo,

        @NotBlank(message = "Escribe la descripción del reto.")
        @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,

        @NotNull(message = "Elige si la meta es en minutos o en sesiones.")
        TipoMeta tipoMeta,

        @NotNull(message = "Indica el valor de la meta.")
        @Min(value = 1, message = "La meta debe ser al menos 1.")
        @Max(value = 10000, message = "La meta es demasiado alta.")
        Integer metaValor,

        String codigoActividad,

        NivelInicial nivelRequerido,

        @NotNull(message = "Indica cuántos días dura el reto.")
        @Min(value = 1, message = "El reto debe durar al menos 1 día.")
        @Max(value = 90, message = "El reto no puede durar más de 90 días.")
        Integer duracionDias,

        @NotNull(message = "Indica el XP de recompensa.")
        @Min(value = 1, message = "La recompensa debe ser al menos 1 XP.")
        @Max(value = 5000, message = "La recompensa es demasiado alta.")
        Integer xpRecompensa
) {
}
