package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Enfrentamiento;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cuenta las sentencias SQL que ejecuta cada endpoint con 8 jugadores, un clan de 6, una competencia de 7 y una
 * temporada en curso. Sirve para detectar consultas dentro de bucles (N+1): si un endpoint pasa su tope, alguien
 * volvió a consultar fila por fila.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class ConsultasPorPeticionTest extends PruebaIntegracion {

    @Autowired
    private EntityManagerFactory emf;

    @Test
    void cadaEndpointUsaPocasConsultas() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        List<String> jugadores = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            jugadores.add(registrar("j" + i + "@kinet.com", "jugador_" + i));
        }
        String j0 = jugadores.get(0);
        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Los Rápidos", "capacidadMax", 10), j0))
                .at("/clan/id").asLong();
        for (int i = 1; i <= 5; i++) {
            post("/api/clanes/" + clan + "/miembros", null, jugadores.get(i)).andExpect(status().isCreated());
        }
        for (String j : jugadores) {
            post("/api/matchmaking/solicitudes", null, j).andExpect(status().isCreated());
        }
        for (Enfrentamiento e : enfrentamientoRepository.findAll()) {
            e.setFechaInicio(LocalDateTime.now().minusHours(25));
            e.setFechaFin(LocalDateTime.now().minusMinutes(1));
            enfrentamientoRepository.save(e);
        }
        for (String j : jugadores) {
            entrenarConEvidencia(j, "FUERZA", 30, "MEDIA", haceHoras(30), null);
        }
        get("/api/matchmaking/enfrentamientos", j0);
        for (int i = 1; i <= 4; i++) {
            post("/api/matchmaking/solicitudes", null, jugadores.get(i)).andExpect(status().isCreated());
        }
        for (var reto : retoRepository.findAll()) {
            post("/api/retos/" + reto.getId() + "/participaciones", null, j0);
        }
        Map<String, Object> competencia = new LinkedHashMap<>();
        competencia.put("tipo", "GRUPAL");
        competencia.put("nombre", "Grupal de medición");
        competencia.put("idsInvitados", List.of(idDe("j1@kinet.com"), idDe("j2@kinet.com"), idDe("j3@kinet.com"),
                idDe("j4@kinet.com"), idDe("j5@kinet.com"), idDe("j6@kinet.com")));
        competencia.put("duracionRondaHoras", 24);
        competencia.put("fechaInicio", LocalDateTime.now().plusDays(1).withNano(0).toString());
        long idCompetencia = respuesta(post("/api/competencias", competencia, j0)).at("/id").asLong();
        post("/api/competencias/" + idCompetencia + "/aceptacion", null, jugadores.get(1));

        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        Map<String, Long> consultas = new LinkedHashMap<>();
        Medicion medir = (nombre, peticion) -> {
            estadisticas.clear();
            peticion.ejecutar();
            consultas.put(nombre, estadisticas.getPrepareStatementCount());
        };
        medir.de("GET /api/clanes", () -> get("/api/clanes", j0));
        medir.de("GET /api/clanes/{id}", () -> get("/api/clanes/" + clan, j0));
        medir.de("GET /api/temporadas/activa/ranking", () -> get("/api/temporadas/activa/ranking", j0));
        medir.de("GET /api/temporadas/activa/mia", () -> get("/api/temporadas/activa/mia", j0));
        medir.de("GET /api/logros", () -> get("/api/logros", j0));
        medir.de("GET /api/retos", () -> get("/api/retos", j0));
        medir.de("GET /api/retos/mios", () -> get("/api/retos/mios", j0));
        medir.de("GET /api/progreso/movimientos", () -> get("/api/progreso/movimientos", j0));
        medir.de("GET /api/catalogos/actividades", () -> get("/api/catalogos/actividades", j0));
        medir.de("GET /api/matchmaking/enfrentamientos", () -> get("/api/matchmaking/enfrentamientos", j0));
        medir.de("GET /api/competencias", () -> get("/api/competencias", j0));
        medir.de("GET /api/competencias/invitaciones", () -> get("/api/competencias/invitaciones", jugadores.get(2)));
        medir.de("GET /api/competencias/{id}", () -> get("/api/competencias/" + idCompetencia, j0));
        medir.de("GET /api/soporte/usuarios", () -> get("/api/soporte/usuarios", admin));
        medir.de("POST /api/matchmaking/solicitudes", () -> post("/api/matchmaking/solicitudes", null, jugadores.get(5)));
        reloj.fijar(LocalDateTime.now().minusMinutes(40));
        iniciar(j0, "FUERZA");
        reloj.fijar(LocalDateTime.now());
        medir.de("POST /api/entrenamientos/en-curso/fin", () -> finalizar(j0, "MEDIA", null));
        reloj.liberar();
        medir.de("POST /api/clanes/mio/salida", () -> post("/api/clanes/mio/salida", null, jugadores.get(5)));

        consultas.forEach((endpoint, n) -> System.out.printf("CONSULTAS|%-42s|%3d%n", endpoint, n));
        // Las listas paginadas suman un COUNT cuando la página viene llena; aquí no, porque son pocas filas.
        // Topes medidos tras pasar los N+1 a consultas. No dependen de cuántos jugadores, miembros o
        // participantes haya: si alguno sube, se volvió a consultar fila por fila.
        Map<String, Integer> topes = new LinkedHashMap<>();
        topes.put("GET /api/clanes", 1);
        topes.put("GET /api/clanes/{id}", 3);
        topes.put("GET /api/temporadas/activa/ranking", 2);
        topes.put("GET /api/temporadas/activa/mia", 4);
        topes.put("GET /api/logros", 2);
        topes.put("GET /api/retos", 7);
        topes.put("GET /api/retos/mios", 4);
        topes.put("GET /api/progreso/movimientos", 2);
        topes.put("GET /api/catalogos/actividades", 2);
        topes.put("GET /api/matchmaking/enfrentamientos", 5);
        topes.put("GET /api/competencias", 7);
        topes.put("GET /api/competencias/invitaciones", 7);
        topes.put("GET /api/competencias/{id}", 6);
        topes.put("GET /api/soporte/usuarios", 2);
        topes.put("POST /api/matchmaking/solicitudes", 9);
        topes.put("POST /api/entrenamientos/en-curso/fin", 34);
        topes.put("POST /api/clanes/mio/salida", 5);
        topes.forEach((endpoint, tope) -> assertThat(consultas.get(endpoint)).as(endpoint).isLessThanOrEqualTo(tope));
    }

    @FunctionalInterface
    private interface Peticion {
        void ejecutar() throws Exception;
    }

    @FunctionalInterface
    private interface Medicion {
        void de(String nombre, Peticion peticion) throws Exception;
    }
}
