package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.config.AsyncConfig;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoEnvio;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.services.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las notificaciones se guardan y envían en un hilo aparte y solo si la operación que las originó se confirma. */
@TestPropertySource(properties = "kinet.notificaciones.asincronas=true")
class NotificacionAsincronaTest extends PruebaIntegracion {

    @Autowired private NotificacionService notificacionService;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired @Qualifier(AsyncConfig.EJECUTOR_NOTIFICACIONES) private TaskExecutor ejecutor;

    @Test
    void lasNotificacionesUsanUnPoolDeHilosPropio() {
        assertThat(ejecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) ejecutor).getThreadNamePrefix()).isEqualTo("notificacion-");
    }

    @Test
    void unEntrenamientoGeneraSusNotificacionesEnSegundoPlano() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        entrenar(token, "FUERZA", 50, "MEDIA", haceHoras(2), null).andExpect(status().isOk());

        esperarEnviadas(3); // validado + nivel 2 + logro
        assertThat(notificacionRepository.findAll()).extracting(n -> n.getTipo())
                .containsExactlyInAnyOrder(TipoNotificacion.VALIDACION, TipoNotificacion.NIVEL, TipoNotificacion.LOGRO);
    }

    @Test
    void siLaOperacionSeDeshaceNoSeCreaLaNotificacion() throws Exception {
        registrar("leo@kinet.com", "leo_run");
        Usuario usuario = usuarioRepository.findByCorreo("leo@kinet.com").orElseThrow();
        TransactionTemplate plantilla = new TransactionTemplate(transacciones);

        plantilla.executeWithoutResult(estado -> {
            notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD, "No", "Se deshace");
            estado.setRollbackOnly();
        });
        plantilla.executeWithoutResult(estado ->
                notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD, "Sí", "Se confirma"));

        esperarEnviadas(1);
        Thread.sleep(300); // margen para que una notificación indebida alcanzara a guardarse
        assertThat(notificacionRepository.findAll()).extracting(n -> n.getMensaje()).containsExactly("Se confirma");
    }

    private void esperarEnviadas(long cantidad) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && notificacionRepository.findAll().stream()
                .filter(n -> n.getEstadoEnvio() == EstadoEnvio.ENVIADA).count() < cantidad) {
            Thread.sleep(50);
        }
        assertThat(notificacionRepository.findAll()).filteredOn(n -> n.getEstadoEnvio() == EstadoEnvio.ENVIADA)
                .hasSize((int) cantidad);
    }
}
