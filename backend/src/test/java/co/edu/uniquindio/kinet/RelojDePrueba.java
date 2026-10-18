package co.edu.uniquindio.kinet;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj del servidor que las pruebas pueden fijar: así se simula "iniciar a las 8:00 y finalizar a las 8:40"
 * sin esperar. Liberado, marca la hora real.
 */
public class RelojDePrueba extends Clock {

    private final ZoneId zona = ZoneId.systemDefault();
    private volatile Instant fijado;

    public void fijar(LocalDateTime momento) {
        this.fijado = momento.atZone(zona).toInstant();
    }

    public void liberar() {
        this.fijado = null;
    }

    @Override
    public Instant instant() {
        Instant momento = fijado;
        return momento != null ? momento : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.system(zone);
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }
}
