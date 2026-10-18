package co.edu.uniquindio.kinet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj del servidor para el cronómetro de entrenamientos. Inyectarlo (en lugar de llamar a now() directo)
 * permite que las pruebas simulen una sesión de 40 minutos sin esperar 40 minutos.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
