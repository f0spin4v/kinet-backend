package co.edu.uniquindio.kinet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

/** La autenticación es propia (JWT), por eso se excluye el usuario en memoria que Spring crea por defecto. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class KinetApplication {

    public static void main(String[] args) {
        // Las rachas y los topes diarios se cuentan por día de Colombia, aunque el servidor esté en otra zona.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        SpringApplication.run(KinetApplication.class, args);
    }
}
