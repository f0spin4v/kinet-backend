package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Clanes: un clan por jugador, capacidad, líder único, salida, expulsión y cierre. */
class ClanIntegracionTest extends PruebaIntegracion {

    @Test
    void crearUnClanDejaAlCreadorComoLiderYDaElLogro() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/clanes", Map.of("nombre", "Los Rápidos", "descripcion", "Corremos", "capacidadMax", 5), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.clan.lider").value("leo_run"))
                .andExpect(jsonPath("$.respuesta.miembros[0].rolClan").value("LIDER"));
        post("/api/clanes", Map.of("nombre", "Otro", "capacidadMax", 5), token).andExpect(status().isConflict());
        get("/api/logros", token).andExpect(jsonPath("$.respuesta[?(@.nombre=='Espíritu de equipo')].obtenido").value(true));
    }

    @Test
    void unClanLlenoNoAceptaMasMiembros() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Dúo", "capacidadMax", 2), lider)).at("/clan/id").asLong();
        post("/api/clanes/" + clan + "/miembros", null, registrar("ana@kinet.com", "ana_gg")).andExpect(status().isCreated());
        post("/api/clanes/" + clan + "/miembros", null, registrar("eva@kinet.com", "eva_fit")).andExpect(status().isBadRequest());
    }

    @Test
    void siSaleElLiderElMasAntiguoTomaElLiderazgo() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        String eva = registrar("eva@kinet.com", "eva_fit");
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Los Rápidos", "capacidadMax", 5), lider)).at("/clan/id").asLong();
        post("/api/clanes/" + clan + "/miembros", null, ana);
        post("/api/clanes/" + clan + "/miembros", null, eva);

        post("/api/clanes/mio/salida", null, lider).andExpect(status().isOk());
        get("/api/clanes/" + clan, ana)
                .andExpect(jsonPath("$.respuesta.clan.lider").value("ana_gg"))
                .andExpect(jsonPath("$.respuesta.clan.miembros").value(2));
    }

    @Test
    void soloElLiderExpulsaYTransfiereElLiderazgo() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Los Rápidos", "capacidadMax", 5), lider)).at("/clan/id").asLong();
        post("/api/clanes/" + clan + "/miembros", null, ana);
        Long idAna = idDe("ana@kinet.com");
        Long idLeo = idDe("leo@kinet.com");

        delete("/api/clanes/" + clan + "/miembros/" + idLeo, ana).andExpect(status().isForbidden());
        put("/api/clanes/" + clan + "/lider", Map.of("idUsuario", idAna), lider)
                .andExpect(jsonPath("$.respuesta.clan.lider").value("ana_gg"));
        delete("/api/clanes/" + clan + "/miembros/" + idLeo, ana).andExpect(status().isOk());
        get("/api/clanes/mio", lider).andExpect(status().isNotFound());
    }

    @Test
    void cuandoSaleElUltimoMiembroElClanSeCierra() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Solo yo", "capacidadMax", 5), lider)).at("/clan/id").asLong();
        post("/api/clanes/mio/salida", null, lider).andExpect(status().isOk());
        get("/api/clanes/" + clan, lider).andExpect(jsonPath("$.respuesta.clan.estado").value("CERRADO"));
        post("/api/clanes/" + clan + "/miembros", null, registrar("ana@kinet.com", "ana_gg")).andExpect(status().isBadRequest());
    }

    @Test
    void moderacionPuedeCerrarCualquierClan() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Tóxicos", "capacidadMax", 5), lider)).at("/clan/id").asLong();
        post("/api/moderacion/clanes/" + clan + "/cierre", null, entrenador).andExpect(status().isOk());
        get("/api/clanes/mio", lider).andExpect(status().isNotFound());
    }
}
