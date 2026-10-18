package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.enums.Intensidad;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import co.edu.uniquindio.kinet.services.Cuestionario;
import co.edu.uniquindio.kinet.services.ReglasJuego;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias de las reglas de gamificación (RNF-27, RNF-28, RNF-29). */
class ReglasJuegoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

    @Test
    void elXpMultiplicaMinutosPorXpDeLaActividadPorIntensidad() {
        assertThat(ReglasJuego.calcularXp(30, 2, Intensidad.BAJA)).isEqualTo(60);
        assertThat(ReglasJuego.calcularXp(30, 2, Intensidad.MEDIA)).isEqualTo(90);
        assertThat(ReglasJuego.calcularXp(30, 2, Intensidad.ALTA)).isEqualTo(120);
        assertThat(ReglasJuego.calcularXp(25, 1, Intensidad.MEDIA)).isEqualTo(38); // 37.5 se redondea
    }

    @Test
    void cadaNivelPideMasXpQueElAnterior() {
        assertThat(ReglasJuego.xpParaNivel(1)).isZero();
        assertThat(ReglasJuego.xpParaNivel(2)).isEqualTo(100);
        assertThat(ReglasJuego.xpParaNivel(3)).isEqualTo(250);
        assertThat(ReglasJuego.xpParaNivel(4)).isEqualTo(450);
        assertThat(ReglasJuego.xpParaNivel(5)).isEqualTo(700);
    }

    @Test
    void elNivelCambiaJustoEnElLimite() {
        assertThat(ReglasJuego.nivelParaXp(0)).isEqualTo(1);
        assertThat(ReglasJuego.nivelParaXp(99)).isEqualTo(1);
        assertThat(ReglasJuego.nivelParaXp(100)).isEqualTo(2);
        assertThat(ReglasJuego.nivelParaXp(249)).isEqualTo(2);
        assertThat(ReglasJuego.nivelParaXp(250)).isEqualTo(3);
        assertThat(ReglasJuego.nivelParaXp(999)).isEqualTo(5);
        assertThat(ReglasJuego.nivelParaXp(1000)).isEqualTo(6);
    }

    @Test
    void laRachaSubeSeMantieneOSeReinicia() {
        assertThat(ReglasJuego.nuevaRacha(0, null, HOY)).isEqualTo(1);             // primer entrenamiento
        assertThat(ReglasJuego.nuevaRacha(3, HOY, HOY)).isEqualTo(3);              // mismo día
        assertThat(ReglasJuego.nuevaRacha(3, HOY.minusDays(1), HOY)).isEqualTo(4); // día siguiente
        assertThat(ReglasJuego.nuevaRacha(5, HOY.minusDays(2), HOY)).isEqualTo(1); // hueco de un día
        assertThat(ReglasJuego.nuevaRacha(4, HOY, HOY.minusDays(1))).isEqualTo(4); // cargado tarde
    }

    @Test
    void laRachaSePierdeSiNoEntrenoNiHoyNiAyer() {
        assertThat(ReglasJuego.rachaVigente(4, HOY, HOY)).isEqualTo(4);
        assertThat(ReglasJuego.rachaVigente(4, HOY.minusDays(1), HOY)).isEqualTo(4);
        assertThat(ReglasJuego.rachaVigente(4, HOY.minusDays(2), HOY)).isZero();
        assertThat(ReglasJuego.rachaVigente(0, null, HOY)).isZero();
    }

    @Test
    void elPuntajeDeLaEvaluacionDefineElNivelInicial() {
        assertThat(Cuestionario.calcularPuntaje(List.of("A", "A", "A", "A", "A"))).isZero();
        assertThat(Cuestionario.calcularPuntaje(List.of("B", "A", "B", "A", "B"))).isEqualTo(3);
        assertThat(NivelInicial.segunPuntaje(1)).isEqualTo(NivelInicial.INICIACION);
        assertThat(NivelInicial.segunPuntaje(2)).isEqualTo(NivelInicial.INTERMEDIO);
        assertThat(NivelInicial.segunPuntaje(3)).isEqualTo(NivelInicial.INTERMEDIO);
        assertThat(NivelInicial.segunPuntaje(4)).isEqualTo(NivelInicial.AVANZADO);
        assertThat(Cuestionario.PREGUNTAS).hasSize(5);
    }
}
