package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.NivelHabilidad;

public record NivelHabilidadDTO(Long id, String nombre, Integer orden, String descripcion) {
    public static NivelHabilidadDTO de(NivelHabilidad nivel) {
        return new NivelHabilidadDTO(nivel.getId(), nivel.getNombre(), nivel.getOrden(), nivel.getDescripcion());
    }
}
