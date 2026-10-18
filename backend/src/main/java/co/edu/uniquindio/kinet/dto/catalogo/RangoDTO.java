package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.Rango;

public record RangoDTO(Long id, String nombre, String liga, String division, Integer orden, Integer puntosMinimo) {
    public static RangoDTO de(Rango rango) {
        return rango == null ? null : new RangoDTO(rango.getId(), rango.nombre(), rango.getLiga().getNombre(),
                rango.getDivision(), rango.getOrden(), rango.getPuntosMinimo());
    }
}
