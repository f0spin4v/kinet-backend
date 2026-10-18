package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.ConexionWearable;
import co.edu.uniquindio.kinet.repositories.ProveedorWearableRepository;
import co.edu.uniquindio.kinet.security.CifradoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Wearables: token cifrado, una conexión activa por proveedor, importación sin duplicados y revocación. */
class WearableIntegracionTest extends PruebaIntegracion {

    @Autowired private CifradoService cifradoService;
    @Autowired private ProveedorWearableRepository proveedorRepository;

    @Test
    void elTokenSeGuardaCifradoYNoSeDevuelve() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/wearables/conexiones", conexion("Strava", "strava-1"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.respuesta.tokenAccesoCifrado").doesNotExist());
        ConexionWearable guardada = conexionRepository.findAll().get(0);
        assertThat(guardada.getTokenAccesoCifrado()).doesNotContain("secreto-oauth");
        assertThat(cifradoService.descifrar(guardada.getTokenAccesoCifrado())).isEqualTo("secreto-oauth");

        post("/api/wearables/conexiones", conexion("Strava", "strava-2"), token).andExpect(status().isConflict());
        post("/api/wearables/conexiones", conexion("Strava", "strava-1"), registrar("ana@kinet.com", "ana_gg"))
                .andExpect(status().isConflict());
    }

    @Test
    void laSincronizacionImportaUnaSolaVezCadaActividad() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(post("/api/wearables/conexiones", conexion("Strava", "strava-1"), token)).at("/id").asLong();
        Map<String, Object> carrera = actividad("act-1", "RUNNING", 50, 30, 5.0);
        Map<String, Object> larga = actividad("act-2", "CICLISMO", 200, 200, 70.0);
        post("/api/wearables/conexiones/" + id + "/sincronizaciones", Map.of("actividades", List.of(carrera, larga)), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.importadas").value(2))
                .andExpect(jsonPath("$.respuesta.validadas").value(1))
                .andExpect(jsonPath("$.respuesta.pendientes").value(1))
                .andExpect(jsonPath("$.respuesta.entrenamientos[0].fuente").value("Strava"));
        post("/api/wearables/conexiones/" + id + "/sincronizaciones", Map.of("actividades", List.of(carrera)), token)
                .andExpect(jsonPath("$.respuesta.importadas").value(0))
                .andExpect(jsonPath("$.respuesta.duplicadas").value(1));
    }

    @Test
    void revocarBorraElTokenYDetieneLaSincronizacion() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(post("/api/wearables/conexiones", conexion("Google Fit", "gfit-1"), token)).at("/id").asLong();
        delete("/api/wearables/conexiones/" + id, token).andExpect(jsonPath("$.respuesta.estado").value("REVOCADA"));
        assertThat(conexionRepository.findAll().get(0).getTokenAccesoCifrado()).isNull();
        post("/api/wearables/conexiones/" + id + "/sincronizaciones",
                Map.of("actividades", List.of(actividad("x", "RUNNING", 30, 20, 3.0))), token)
                .andExpect(status().isBadRequest());
        // ya se puede volver a conectar
        post("/api/wearables/conexiones", conexion("Google Fit", "gfit-1"), token).andExpect(status().isCreated());
    }

    private Map<String, Object> conexion(String proveedor, String cuenta) {
        Long idProveedor = proveedorRepository.findAll().stream().filter(p -> p.getNombre().equals(proveedor))
                .findFirst().orElseThrow().getId();
        return Map.of("idProveedor", idProveedor, "idCuentaExterna", cuenta, "tokenAcceso", "secreto-oauth");
    }

    private Map<String, Object> actividad(String externa, String tipo, int haceMinutos, int minutos, Double km) {
        Map<String, Object> a = new HashMap<>();
        a.put("idActividadExterna", externa);
        a.put("idTipoActividad", idActividad(tipo));
        a.put("fechaHoraInicio", LocalDateTime.now().minusMinutes(haceMinutos + minutos).truncatedTo(ChronoUnit.SECONDS));
        a.put("duracionMin", minutos);
        a.put("intensidad", "MEDIA");
        a.put("distanciaKm", km);
        return a;
    }
}
