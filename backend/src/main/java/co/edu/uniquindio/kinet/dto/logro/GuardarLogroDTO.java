package co.edu.uniquindio.kinet.dto.logro;

import jakarta.validation.constraints.*;

/** criterio: METRICA>=VALOR con ENTRENAMIENTOS, MINUTOS, RACHA, XP, NIVEL, RETOS, VICTORIAS o CLAN. */
public record GuardarLogroDTO(
        @NotBlank(message = "Escribe el nombre del logro.")
        @Size(max = 80, message = "El nombre no puede tener más de 80 caracteres.")
        String nombre,
        @NotBlank(message = "Escribe la descripción del logro.")
        @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
        @NotBlank(message = "Escribe el criterio.")
        @Pattern(regexp = "^(ENTRENAMIENTOS|MINUTOS|RACHA|XP|NIVEL|RETOS|VICTORIAS|CLAN)>=\\d{1,9}$",
                message = "El criterio debe tener la forma METRICA>=VALOR, por ejemplo RACHA>=7.")
        String criterio,
        @NotNull(message = "Indica el XP de bonificación.")
        @Min(value = 0, message = "El XP de bonificación no puede ser negativo.")
        @Max(value = 5000, message = "El XP de bonificación es demasiado alto.")
        Integer xpBonus,
        @Size(max = 300, message = "La dirección del ícono es demasiado larga.")
        String iconoUrl
) {
}
