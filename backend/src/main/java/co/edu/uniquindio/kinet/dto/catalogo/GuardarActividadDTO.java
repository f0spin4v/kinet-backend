package co.edu.uniquindio.kinet.dto.catalogo;

import jakarta.validation.constraints.*;

public record GuardarActividadDTO(
        @NotBlank(message = "Escribe el nombre de la actividad.")
        @Size(max = 40, message = "El nombre no puede tener más de 40 caracteres.")
        @Pattern(regexp = "^[A-Za-z_ÁÉÍÓÚÑáéíóúñ ]+$", message = "El nombre solo puede tener letras, espacios y guion bajo.")
        String nombre
) {
}
