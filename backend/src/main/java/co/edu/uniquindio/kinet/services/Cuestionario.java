package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO.OpcionDTO;

import java.util.List;

/** Las 5 preguntas de la evaluación de nivel (UI-001). En todas, la opción A vale 0 puntos y la B vale 1. */
public final class Cuestionario {

    public static final List<PreguntaDTO> PREGUNTAS = List.of(
            new PreguntaDTO(1, "¿Hace cuánto entrenas de forma constante?", List.of(
                    new OpcionDTO("A", "Menos de 6 meses", "Estoy empezando o retomando el hábito"),
                    new OpcionDTO("B", "6 meses o más", "Ya tengo una rutina establecida"))),
            new PreguntaDTO(2, "¿Cuántos días a la semana haces actividad física?", List.of(
                    new OpcionDTO("A", "2 días o menos", "Entreno cuando el tiempo me lo permite"),
                    new OpcionDTO("B", "3 días o más", "El ejercicio hace parte de mi semana"))),
            new PreguntaDTO(3, "¿Cuál es tu volumen semanal habitual de entrenamiento de alta intensidad?", List.of(
                    new OpcionDTO("A", "Menos de 3 sesiones semanales", "Cargas moderadas, base aeróbica y desarrollo de resistencia"),
                    new OpcionDTO("B", "3 o más sesiones semanales", "Alta demanda metabólica, optimización de picos de potencia"))),
            new PreguntaDTO(4, "¿Cuánto dura normalmente una sesión tuya?", List.of(
                    new OpcionDTO("A", "Menos de 45 minutos", "Sesiones cortas y puntuales"),
                    new OpcionDTO("B", "45 minutos o más", "Sesiones largas y completas"))),
            new PreguntaDTO(5, "¿Cómo terminas una sesión exigente?", List.of(
                    new OpcionDTO("A", "Muy agotado", "Necesito parar antes de terminarla"),
                    new OpcionDTO("B", "Con energía", "La completo y me recupero rápido")))
    );

    private Cuestionario() {
    }

    /** Suma un punto por cada respuesta "B". */
    public static int calcularPuntaje(List<String> respuestas) {
        return (int) respuestas.stream().filter("B"::equals).count();
    }
}
