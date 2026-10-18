package co.edu.uniquindio.kinet.dto.catalogo;

import jakarta.validation.constraints.*;

public record CrearProveedorDTO(
        @NotBlank(message = "Escribe el nombre del proveedor.")
        @Size(max = 40, message = "El nombre no puede tener más de 40 caracteres.")
        String nombre
) {
}
