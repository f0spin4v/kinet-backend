package co.edu.uniquindio.kinet.dto.clan;

import jakarta.validation.constraints.*;

public record GuardarClanDTO(
        @NotBlank(message = "Escribe el nombre del clan.")
        @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres.")
        String nombre,
        @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
        @NotNull(message = "Indica la capacidad del clan.")
        @Min(value = 2, message = "La capacidad debe ser de al menos 2 miembros.")
        @Max(value = 200, message = "La capacidad no puede pasar de 200 miembros.")
        Integer capacidadMax
) {
}
