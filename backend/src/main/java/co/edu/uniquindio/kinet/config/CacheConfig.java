package co.edu.uniquindio.kinet.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caché en memoria para catálogos que casi no cambian y se consultan en cada operación (la tabla de niveles de
 * progresión se lee al registrar cada movimiento de XP). Se invalida al guardar.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    public static final String NIVELES_PROGRESION = "nivelesProgresion";
    public static final String PLATAFORMAS_DEPORTE = "plataformasDeporte";
}
