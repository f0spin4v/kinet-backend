package co.edu.uniquindio.kinet.model.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda un booleano como 'S' o 'N' en una columna CHAR(1) de Oracle. */
@Converter
public class SiNoConverter implements AttributeConverter<Boolean, Character> {

    @Override
    public Character convertToDatabaseColumn(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? 'S' : 'N';
    }

    @Override
    public Boolean convertToEntityAttribute(Character valor) {
        return valor != null && valor == 'S';
    }
}
