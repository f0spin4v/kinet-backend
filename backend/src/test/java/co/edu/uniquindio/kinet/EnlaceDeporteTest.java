package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.PlataformaDeporte;
import co.edu.uniquindio.kinet.repositories.PlataformaDeporteRepository;
import co.edu.uniquindio.kinet.services.EnlaceDeporte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** El verificador solo mira la forma del enlace, con las apps que haya en la tabla PLATAFORMA_DEPORTE. */
class EnlaceDeporteTest {

    private EnlaceDeporte enlaces;

    @BeforeEach
    void preparar() {
        PlataformaDeporteRepository repositorio = mock(PlataformaDeporteRepository.class);
        when(repositorio.findByActivaOrderByIdAsc(true)).thenReturn(List.of(
                plataforma("STRAVA", "strava.com", "^/activities/[0-9]+/?$"),
                plataforma("STRAVA", "strava.app.link", "^/[A-Za-z0-9]{6,20}$"),
                plataforma("GARMIN CONNECT", "connect.garmin.com", "^/modern/activity/[0-9]+/?$")));
        enlaces = new EnlaceDeporte(repositorio);
    }

    @Test
    void reconoceLaActividadYLaDejaNormalizada() {
        var reconocido = enlaces.reconocer("  https://WWW.Strava.com/activities/123456/?utm_source=app#mapa ");
        assertThat(reconocido.plataforma().getNombre()).isEqualTo("STRAVA");
        assertThat(reconocido.url()).isEqualTo("https://www.strava.com/activities/123456/");

        assertThat(enlaces.reconocer("https://strava.app.link/aB3dE9xYz").plataforma().getDominio())
                .isEqualTo("strava.app.link");
        assertThat(enlaces.reconocer("https://connect.garmin.com/modern/activity/987").plataforma().getNombre())
                .isEqualTo("GARMIN CONNECT");
    }

    @Test
    void rechazaLoQueNoEsUnaActividadDeUnaAppReconocida() {
        for (String enlace : new String[]{
                "http://www.strava.com/activities/123456",             // sin https
                "https://strava.com.otro.com/activities/123456",       // dominio parecido
                "https://otrostrava.com/activities/123456",            // termina igual pero no es subdominio
                "https://www.strava.com/athletes/123456",              // no es una actividad
                "https://www.strava.com/activities/abc",               // sin id
                "https://usuario:clave@www.strava.com/activities/1",   // con credenciales
                "https://ejemplo.com/actividad/1",                     // app desconocida
                "no es un enlace", "", "   "}) {
            assertThatThrownBy(() -> enlaces.reconocer(enlace))
                    .as(enlace).isInstanceOf(ReglaNegocioException.class);
        }
        assertThatThrownBy(() -> enlaces.reconocer(null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> enlaces.reconocer("https://www.strava.com/activities/" + "1".repeat(500)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void sinAppsActivasNoSeAceptaNada() {
        PlataformaDeporteRepository vacio = mock(PlataformaDeporteRepository.class);
        when(vacio.findByActivaOrderByIdAsc(true)).thenReturn(List.of());
        assertThatThrownBy(() -> new EnlaceDeporte(vacio).reconocer("https://www.strava.com/activities/1"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    private static PlataformaDeporte plataforma(String nombre, String dominio, String patron) {
        return PlataformaDeporte.builder().nombre(nombre).dominio(dominio).patronRuta(patron).build();
    }
}
