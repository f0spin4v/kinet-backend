package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.ProveedorWearable;

public record ProveedorDTO(Long id, String nombre) {
    public static ProveedorDTO de(ProveedorWearable proveedor) {
        return new ProveedorDTO(proveedor.getId(), proveedor.getNombre());
    }
}
