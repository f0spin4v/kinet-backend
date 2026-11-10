package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.AutorizacionMenor;
import co.edu.uniquindio.kinet.model.AuditoriaAdmin;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.CanalAutorizacion;
import co.edu.uniquindio.kinet.model.enums.EstadoAutorizacion;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.security.TokensSeguros;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-33: titulares de 14 a 17 años con autorización del representante legal
 * (Ley 1581 de 2012, art. 7; Decreto 1377 de 2013, art. 12).
 */
class MenoresIntegracionTest extends PruebaIntegracion {

    private static final String ENLACE = "enlace-de-prueba";

    private Map<String, Object> datosMenor(String correo, String gamertag, int anios) {
        Map<String, Object> datos = new HashMap<>(datosRegistro(correo, gamertag));
        datos.put("fechaNacimiento", LocalDate.now().minusYears(anios).minusDays(10).toString());
        datos.put("nombreRepresentante", "Andrés Pérez");
        datos.put("correoRepresentante", "acudiente@correo.com");
        datos.put("parentesco", "TUTOR_LEGAL");
        return datos;
    }

    /** Registra a un menor y fija un enlace conocido (el real solo viaja por correo). */
    private Long registrarMenor(String correo, String gamertag, int anios) throws Exception {
        String cuerpo = post("/api/auth/registro", datosMenor(correo, gamertag, anios), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.usuario.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.respuesta.usuario.menorDeEdad").value(true))
                .andExpect(jsonPath("$.respuesta.usuario.aceptaPolitica").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode token = leer(cuerpo).at("/respuesta/token");
        assertThat(token.isNull() || token.isMissingNode()).as("un menor pendiente no recibe sesión").isTrue();
        Long id = idDe(correo);
        AutorizacionMenor autorizacion = autorizacionRepository.findByUsuarioId(id).orElseThrow();
        autorizacion.setTokenHash(TokensSeguros.hash(ENLACE));
        autorizacionRepository.save(autorizacion);
        return id;
    }

    @Test
    void unMenorQuedaPendienteHastaQueSuRepresentanteAutoriza() throws Exception {
        Long id = registrarMenor("sofi@kinet.com", "sofi_run", 16);
        AutorizacionMenor solicitud = autorizacionRepository.findByUsuarioId(id).orElseThrow();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoAutorizacion.PENDIENTE);
        assertThat(solicitud.getCorreoRepresentante()).isEqualTo("acudiente@correo.com");
        assertThat(solicitud.getFechaExpiracion()).isAfter(LocalDateTime.now().plusDays(6));

        // no puede entrar mientras tanto
        post("/api/auth/login", Map.of("correo", "sofi@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.respuesta", containsString("representante legal")));

        // el representante abre el enlace: ve a quién autoriza, sin correos ni contraseñas
        post("/api/autorizaciones/consulta", Map.of("token", ENLACE), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombreMenor").value("Jugador"))
                .andExpect(jsonPath("$.respuesta.gamertag").value("sofi_run"))
                .andExpect(jsonPath("$.respuesta.edad").value(16))
                .andExpect(jsonPath("$.respuesta.correo").doesNotExist());

        post("/api/autorizaciones/respuesta", Map.of("token", ENLACE, "autoriza", true), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.mensaje", containsString("autorizada")));

        Usuario menor = usuarioRepository.findById(id).orElseThrow();
        assertThat(menor.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(menor.getAceptaPoliticaDatos()).isTrue();
        AutorizacionMenor respondida = autorizacionRepository.findByUsuarioId(id).orElseThrow();
        assertThat(respondida.getEstado()).isEqualTo(EstadoAutorizacion.AUTORIZADA);
        assertThat(respondida.getCanal()).isEqualTo(CanalAutorizacion.CORREO);
        assertThat(respondida.getTokenHash()).isNull();
        iniciarSesion("sofi@kinet.com", "Segura123");

        // el enlace sirve una sola vez
        post("/api/autorizaciones/respuesta", Map.of("token", ENLACE, "autoriza", false), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void siElRepresentanteNoAutorizaLaCuentaQuedaInactiva() throws Exception {
        Long id = registrarMenor("tom@kinet.com", "tom_gg", 15);
        post("/api/autorizaciones/respuesta", Map.of("token", ENLACE, "autoriza", false), null)
                .andExpect(status().isOk());
        assertThat(usuarioRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(autorizacionRepository.findByUsuarioId(id).orElseThrow().getEstado())
                .isEqualTo(EstadoAutorizacion.RECHAZADA);
        post("/api/auth/login", Map.of("correo", "tom@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void unEnlaceVencidoNoSirvePeroSePuedePedirOtro() throws Exception {
        Long id = registrarMenor("lu@kinet.com", "lu_fit", 14);
        AutorizacionMenor autorizacion = autorizacionRepository.findByUsuarioId(id).orElseThrow();
        autorizacion.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        autorizacionRepository.save(autorizacion);
        post("/api/autorizaciones/consulta", Map.of("token", ENLACE), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("venció")));

        // la respuesta es la misma exista o no la cuenta
        post("/api/auth/autorizacion/reenvio", Map.of("correo", "lu@kinet.com"), null).andExpect(status().isOk());
        post("/api/auth/autorizacion/reenvio", Map.of("correo", "nadie@kinet.com"), null).andExpect(status().isOk());
        AutorizacionMenor nueva = autorizacionRepository.findByUsuarioId(id).orElseThrow();
        assertThat(nueva.getTokenHash()).isNotEqualTo(TokensSeguros.hash(ENLACE));
        assertThat(nueva.enlaceVigente()).isTrue();
    }

    @Test
    void unMenorNecesitaDatosCompletosDeSuRepresentante() throws Exception {
        Map<String, Object> mismoCorreo = datosMenor("ana@kinet.com", "ana_gg", 16);
        mismoCorreo.put("correoRepresentante", "ana@kinet.com");
        post("/api/auth/registro", mismoCorreo, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("distinto")));

        Map<String, Object> sinParentesco = datosMenor("ana@kinet.com", "ana_gg", 16);
        sinParentesco.remove("parentesco");
        post("/api/auth/registro", sinParentesco, null).andExpect(status().isBadRequest());

        Map<String, Object> parentescoInvalido = datosMenor("ana@kinet.com", "ana_gg", 16);
        parentescoInvalido.put("parentesco", "VECINO");
        post("/api/auth/registro", parentescoInvalido, null).andExpect(status().isBadRequest());
        assertThat(usuarioRepository.existsByCorreo("ana@kinet.com")).isFalse();
    }

    @Test
    void elAdministradorRegistraUnaAutorizacionFirmadaYPuedeRevocarla() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        Long id = registrarMenor("isa@kinet.com", "isa_go", 17);
        Map<String, Object> firmada = Map.of("nombreRepresentante", "Andrés Ospina", "correoRepresentante",
                "tio@correo.com", "parentesco", "TUTOR_LEGAL",
                "soporte", "Autorización firmada el 10/11/2026, escaneada en Drive/Kinet/Autorizaciones");

        post("/api/admin/usuarios/" + id + "/autorizacion-menor", firmada, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("AUTORIZADA"))
                .andExpect(jsonPath("$.respuesta.canal").value("ADMINISTRADOR"));
        assertThat(usuarioRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        iniciarSesion("isa@kinet.com", "Segura123");
        assertThat(auditoriaRepository.findAll()).anySatisfy(a -> {
            assertThat(a.getAccion()).isEqualTo(AuditoriaAdmin.AUTORIZACION_MENOR);
            assertThat(a.getValorNuevo()).startsWith("AUTORIZADA (TUTOR_LEGAL)");
        });
        get("/api/admin/usuarios/" + id + "/autorizacion-menor", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.correoRepresentante").value("tio@correo.com"));

        // no se registra dos veces
        post("/api/admin/usuarios/" + id + "/autorizacion-menor", firmada, admin).andExpect(status().isConflict());

        post("/api/admin/usuarios/" + id + "/autorizacion-menor/revocacion", Map.of("motivo", "El tutor la retiró"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("REVOCADA"));
        assertThat(usuarioRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(auditoriaRepository.findAll()).filteredOn(a -> AuditoriaAdmin.AUTORIZACION_MENOR.equals(a.getAccion()))
                .hasSize(2);
    }

    @Test
    void sinAutorizacionNadieActivaAUnMenorYSoloAplicaDe14A17() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        Long menor = registrarMenor("mia@kinet.com", "mia_pro", 16);
        String jugador = registrar("leo@kinet.com", "leo_run");
        Long adulto = idDe("leo@kinet.com");

        patch("/api/soporte/usuarios/" + menor + "/estado", Map.of("estado", "ACTIVO"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("representante legal")));
        patch("/api/soporte/usuarios/" + adulto + "/estado", Map.of("estado", "PENDIENTE"), admin)
                .andExpect(status().isBadRequest());

        Map<String, Object> firmada = Map.of("nombreRepresentante", "Rosa Díaz", "correoRepresentante", "rosa@correo.com",
                "parentesco", "MADRE", "soporte", "Autorización firmada en papel, archivo 2026-11");
        post("/api/admin/usuarios/" + adulto + "/autorizacion-menor", firmada, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("14 a 17")));
        // solo el administrador
        post("/api/admin/usuarios/" + menor + "/autorizacion-menor", firmada, jugador).andExpect(status().isForbidden());
        // y un adulto no se puede volver menor en su perfil sin autorización
        put("/api/usuarios/me", Map.of("nombres", "Leo", "apellidos", "Run", "gamertag", "leo_run",
                "fechaNacimiento", LocalDate.now().minusYears(15).toString()), jugador)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("representante legal")));
    }
}
