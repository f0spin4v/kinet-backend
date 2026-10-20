package co.edu.uniquindio.kinet.funcionales;

import co.edu.uniquindio.kinet.PruebaIntegracion;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Entorno de las pruebas funcionales (el equivalente de environment.py en behave).
 *
 * La configuración viene de PruebaIntegracion: aplicación completa con H2 en modo Oracle
 * (src/test/resources/application.properties y data.sql), así que no hace falta levantar Oracle ni el backend.
 */
@CucumberContextConfiguration
public class Entorno extends PruebaIntegracion {

    /** Antes de cada escenario: datos de juego en blanco y catálogos como en data.sql. */
    @Before
    public void antesDelEscenario() {
        limpiarDatos();
    }

    /** Después de cada escenario: el reloj del servidor vuelve a la hora real. */
    @After
    public void despuesDelEscenario() {
        reloj.liberar();
    }
}
