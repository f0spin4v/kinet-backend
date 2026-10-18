package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.NivelProgresion;

public record NivelProgresionDTO(Integer numeroNivel, Integer xpRequerido) {
    public static NivelProgresionDTO de(NivelProgresion nivel) {
        return new NivelProgresionDTO(nivel.getNumeroNivel(), nivel.getXpRequerido());
    }
}
