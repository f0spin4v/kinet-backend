package co.edu.uniquindio.kinet.funcionales.steps;

import co.edu.uniquindio.kinet.PruebaIntegracion;
import co.edu.uniquindio.kinet.funcionales.Bitacora;
import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pasos de Gherkin de las pruebas funcionales (el equivalente de steps/steps.py en behave).
 * Cada escenario crea una instancia nueva, así que los campos guardan el estado entre pasos.
 *
 * Toda petición a la API pasa por get/post/put/patch/delete, que aquí se anotan en la Bitacora: así la consola
 * muestra el endpoint, los datos enviados y la respuesta debajo de cada paso.
 */
public class Pasos extends PruebaIntegracion {

    private static final String CONTRASENA = "Segura123";

    private String token;
    private String correo;
    private ResultActions ultimaRespuesta;

    // ---------- comunes ----------

    @Dado("que el sistema está operativo")
    public void queElSistemaEstaOperativo() throws Exception {
        // la documentación de la API es pública: si responde, la aplicación arrancó con su seguridad
        get("/v3/api-docs", null).andExpect(status().isOk());
    }

    @Entonces("el sistema responde con un código de estado {int}")
    public void elSistemaRespondeConUnCodigoDeEstado(int estado) throws Exception {
        MockHttpServletResponse respuesta = ultimaRespuesta.andReturn().getResponse();
        Bitacora.nota("código esperado: " + estado + " · código recibido: " + respuesta.getStatus());
        assertThat(respuesta.getStatus())
                .as("Esperaba %d. Respuesta: %s", estado, respuesta.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(estado);
    }

    @Entonces("la respuesta debe contener un token de acceso")
    public void laRespuestaDebeContenerUnTokenDeAcceso() throws Exception {
        String jwt = respuesta(ultimaRespuesta).at("/token").asText();
        Bitacora.nota("respuesta.token: JWT de " + jwt.length() + " caracteres");
        assertThat(jwt).isNotBlank();
    }

    // ---------- humo y seguridad ----------

    @Dado("que estoy autenticado como {string}")
    public void queEstoyAutenticadoComo(String rol) throws Exception {
        token = rol.equals("JUGADOR")
                ? registrar("jugador@test.com", "jugador_test")
                : cuentaConRol(rol.toLowerCase() + "@test.com", rol.toLowerCase() + "_test", rol);
    }

    @Cuando("intento consultar mi perfil")
    public void intentoConsultarMiPerfil() throws Exception {
        ultimaRespuesta = get("/api/usuarios/me", token);
    }

    @Cuando("intento consultar {string}")
    public void intentoConsultar(String ruta) throws Exception {
        ultimaRespuesta = get(ruta, token);
    }

    // ---------- onboarding ----------

    @Cuando("un jugador se registra con gamertag {string} y correo {string}")
    public void unJugadorSeRegistra(String gamertag, String correo) throws Exception {
        this.correo = correo;
        ultimaRespuesta = post("/api/auth/registro", datosRegistro(correo, gamertag), null);
    }

    @Dado("que existe un jugador registrado con gamertag {string} y correo {string}")
    public void queExisteUnJugadorRegistrado(String gamertag, String correo) throws Exception {
        this.correo = correo;
        this.token = registrar(correo, gamertag);
    }

    @Entonces("el jugador aún no tiene nivel de habilidad")
    public void elJugadorAunNoTieneNivel() throws Exception {
        JsonNode nivel = respuesta(ultimaRespuesta).at("/usuario/nivelHabilidad");
        Bitacora.nota("respuesta.usuario.nivelHabilidad: " + (nivel.isMissingNode() ? "(no viene)" : nivel));
        assertThat(nivel.isMissingNode() || nivel.isNull()).isTrue();
    }

    @Entonces("el jugador queda guardado en la base de datos")
    public void elJugadorQuedaGuardadoEnLaBaseDeDatos() {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        assertThat(usuario).as("No existe en la base de datos el usuario %s", correo).isNotNull();
        Bitacora.nota("BD: tabla USUARIO → id " + usuario.getId() + " · " + usuario.getCorreo()
                + " · rol " + usuario.getRol().getNombre() + " · estado " + usuario.getEstado());
        Bitacora.nota("BD: la contraseña se guardó cifrada con BCrypt (" + usuario.getPasswordHash().substring(0, 7) + "…)");
        assertThat(usuario.getPasswordHash()).doesNotContain(CONTRASENA);
    }

    @Cuando("el jugador responde la evaluación eligiendo el nivel {string}")
    public void elJugadorRespondeLaEvaluacion(String nivel) throws Exception {
        // en cada pregunta se elige la opción que vota por el nivel pedido
        List<Long> opciones = preguntaRepository.findByActivaOrderByOrdenAsc(true).stream()
                .map(p -> p.getOpciones().stream()
                        .filter(o -> o.getNivelHabilidad().getNombre().equals(nivel))
                        .findFirst().orElseThrow().getId())
                .toList();
        Bitacora.nota("BD: " + opciones.size() + " preguntas activas → opciones que votan por " + nivel + ": " + opciones);
        ultimaRespuesta = post("/api/evaluacion", Map.of("idsOpciones", opciones), token);
    }

    @Entonces("su nivel de habilidad es {string}")
    public void suNivelDeHabilidadEs(String nivel) throws Exception {
        String actual = respuesta(get("/api/usuarios/me", token)).at("/nivelHabilidad").asText();
        Bitacora.nota("nivel esperado: " + nivel + " · nivel en el perfil: " + actual);
        assertThat(actual).isEqualTo(nivel);
    }

    // ---------- offboarding ----------

    @Cuando("el administrador cambia el estado de la cuenta del jugador a {string}")
    public void elAdministradorCambiaElEstado(String estado) throws Exception {
        Bitacora.nota("se prepara un administrador: se registra, se le cambia el rol a ADMINISTRADOR en la BD "
                + "e inicia sesión");
        String admin = cuentaConRol("admin@test.com", "admin_test", "ADMINISTRADOR");
        ultimaRespuesta = patch("/api/soporte/usuarios/" + idDe(correo) + "/estado", Map.of("estado", estado), admin);
    }

    @Cuando("el jugador intenta iniciar sesión")
    public void elJugadorIntentaIniciarSesion() throws Exception {
        ultimaRespuesta = post("/api/auth/login", Map.of("correo", correo, "contrasena", CONTRASENA), null);
    }

    @Entonces("la cuenta queda en estado {string} en la base de datos")
    public void laCuentaQuedaEnEstado(String estado) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseThrow();
        Bitacora.nota("BD: tabla USUARIO → " + correo + " · estado " + usuario.getEstado());
        assertThat(usuario.getEstado().name()).isEqualTo(estado);
    }

    // ---------- perfiles ----------

    @Cuando("actualizo mi perfil con nombres {string} y gamertag {string}")
    public void actualizoMiPerfil(String nombres, String gamertag) throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", nombres);
        datos.put("apellidos", "De Prueba");
        datos.put("gamertag", gamertag);
        ultimaRespuesta = put("/api/usuarios/me", datos, token);
    }

    @Entonces("al consultar mi perfil muestra los nombres {string} y el gamertag {string}")
    public void alConsultarMiPerfilMuestra(String nombres, String gamertag) throws Exception {
        JsonNode perfil = respuesta(get("/api/usuarios/me", token));
        Bitacora.nota("esperado: " + nombres + " / " + gamertag
                + " · en el perfil: " + perfil.at("/nombres").asText() + " / " + perfil.at("/gamertag").asText());
        assertThat(perfil.at("/nombres").asText()).isEqualTo(nombres);
        assertThat(perfil.at("/gamertag").asText()).isEqualTo(gamertag);
    }

    // ---------- entrenamientos ----------

    @Cuando("registro un entrenamiento de {string} de {int} minutos con intensidad {string}")
    public void registroUnEntrenamiento(String actividad, int minutos, String intensidad) throws Exception {
        Bitacora.nota("el reloj del servidor se adelanta " + minutos + " minutos entre iniciar y finalizar");
        ultimaRespuesta = entrenar(token, actividad, minutos, intensidad, haceHoras(3), null);
    }

    @Entonces("el entrenamiento queda en estado {string}")
    public void elEntrenamientoQuedaEnEstado(String estado) throws Exception {
        String actual = respuesta(ultimaRespuesta).at("/entrenamiento/estado").asText();
        Bitacora.nota("estado esperado: " + estado + " · estado recibido: " + actual);
        assertThat(actual).isEqualTo(estado);
    }

    @Entonces("el entrenamiento otorga {int} XP")
    public void elEntrenamientoOtorga(int xp) throws Exception {
        int actual = respuesta(ultimaRespuesta).at("/entrenamiento/xpOtorgado").asInt();
        Bitacora.nota("XP esperado: " + xp + " · XP otorgado: " + actual);
        assertThat(actual).isEqualTo(xp);
    }

    @Entonces("el entrenamiento queda guardado en la base de datos con {int} minutos")
    public void elEntrenamientoQuedaGuardado(int minutos) throws Exception {
        long id = respuesta(ultimaRespuesta).at("/entrenamiento/id").asLong();
        Entrenamiento entrenamiento = entrenamientoRepository.findById(id).orElse(null);
        assertThat(entrenamiento).as("No existe en la base de datos el entrenamiento %d", id).isNotNull();
        Bitacora.nota("BD: tabla ENTRENAMIENTO → id " + id + " · " + entrenamiento.getTipoActividad().getNombre()
                + " · " + entrenamiento.getDuracionMin() + " min · " + entrenamiento.getEstadoValidacion());
        Bitacora.nota("BD: movimientos de XP del jugador → saldo " + movimientoRepository.saldo(idDe(correo)) + " XP");
        assertThat(entrenamiento.getDuracionMin()).isEqualTo(minutos);
    }

    // ---------- peticiones: se anotan en la bitácora ----------

    @Override
    protected ResultActions get(String url, String token) throws Exception {
        return anotar("GET", url, null, super.get(url, token));
    }

    @Override
    protected ResultActions post(String url, Object cuerpo, String token) throws Exception {
        return anotar("POST", url, cuerpo, super.post(url, cuerpo, token));
    }

    @Override
    protected ResultActions put(String url, Object cuerpo, String token) throws Exception {
        return anotar("PUT", url, cuerpo, super.put(url, cuerpo, token));
    }

    @Override
    protected ResultActions patch(String url, Object cuerpo, String token) throws Exception {
        return anotar("PATCH", url, cuerpo, super.patch(url, cuerpo, token));
    }

    @Override
    protected ResultActions delete(String url, String token) throws Exception {
        return anotar("DELETE", url, null, super.delete(url, token));
    }

    private ResultActions anotar(String metodo, String url, Object cuerpo, ResultActions resultado) throws Exception {
        MockHttpServletResponse respuesta = resultado.andReturn().getResponse();
        HttpStatus estado = HttpStatus.resolve(respuesta.getStatus());
        String enviado = cuerpo == null ? null : json.writeValueAsString(cuerpo);
        // de la respuesta { error, respuesta } se muestra solo "respuesta"; la documentación de la API es muy larga
        String recibido = null;
        if (!url.startsWith("/v3/")) {
            String texto = respuesta.getContentAsString(StandardCharsets.UTF_8);
            recibido = texto.isBlank() ? null : leer(texto).at("/respuesta").toString();
        }
        Bitacora.http(metodo, url, enviado, respuesta.getStatus(),
                estado == null ? "" : estado.getReasonPhrase(), recibido);
        return resultado;
    }
}
