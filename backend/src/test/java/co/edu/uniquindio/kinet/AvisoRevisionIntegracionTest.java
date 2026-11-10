package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.services.AvisoRevisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** KAN-85: aviso al entrenador cuando una revisión lleva más de 48 horas esperando. */
class AvisoRevisionIntegracionTest extends PruebaIntegracion {

    @Autowired AvisoRevisionService avisoRevisionService;

    @Test
    void elEntrenadorRecibeUnAvisoSiUnaRevisionLlevaMasDe48Horas() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        // 200 minutos queda PENDIENTE de revisión (más de 180 sin enlace); terminó hace unas 56 horas
        entrenar(jugador, "FUERZA", 200, "MEDIA", LocalDateTime.now().minusHours(60), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("PENDIENTE"));

        assertThat(avisoRevisionService.avisarAtrasadas()).isEqualTo(1);
        Long idEntrenador = idDe("entrenador@kinet.com");
        assertThat(bandeja(idEntrenador)).singleElement()
                .satisfies(n -> {
                    assertThat(n.getTipo()).isEqualTo(TipoNotificacion.VALIDACION);
                    assertThat(n.getTitulo()).isEqualTo(AvisoRevisionService.TITULO);
                    assertThat(n.getMensaje()).contains("1 sesión").contains("48 horas");
                });
        // el jugador no recibe este aviso
        Long idJugador = idDe("leo@kinet.com");
        assertThat(bandeja(idJugador)).extracting(Notificacion::getTitulo).doesNotContain(AvisoRevisionService.TITULO);

        // como mucho un aviso cada 24 horas
        assertThat(avisoRevisionService.avisarAtrasadas()).isZero();
    }

    private java.util.List<Notificacion> bandeja(Long idUsuario) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDescIdDesc(idUsuario,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    @Test
    void sinRevisionesAtrasadasNoHayAviso() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        // pendiente, pero de hace 5 horas
        entrenar(jugador, "FUERZA", 200, "MEDIA", LocalDateTime.now().minusHours(9), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("PENDIENTE"));
        assertThat(avisoRevisionService.avisarAtrasadas()).isZero();
    }
}
