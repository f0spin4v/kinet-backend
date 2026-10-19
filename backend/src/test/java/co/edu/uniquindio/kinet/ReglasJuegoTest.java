package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.services.CriterioLogro;
import co.edu.uniquindio.kinet.services.ReglasJuego;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasJuegoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @Test
    void elXpEsMinutosPorXpPorMinutoRedondeado() {
        assertThat(ReglasJuego.calcularXp(40, new BigDecimal("3.00"))).isEqualTo(120);
        assertThat(ReglasJuego.calcularXp(45, new BigDecimal("3.75"))).isEqualTo(169);
        assertThat(ReglasJuego.calcularXp(10, new BigDecimal("1.25"))).isEqualTo(13);
    }

    @Test
    void laColocacionDaUnTercioDelXpConTope() {
        assertThat(ReglasJuego.puntosColocacion(120)).isEqualTo(40);
        assertThat(ReglasJuego.puntosColocacion(900)).isEqualTo(100);
        assertThat(ReglasJuego.puntosColocacion(0)).isZero();
    }

    @Test
    void laRachaSubeSoloConDiasSeguidos() {
        assertThat(ReglasJuego.nuevaRacha(0, null, HOY)).isEqualTo(1);
        assertThat(ReglasJuego.nuevaRacha(2, HOY.minusDays(1), HOY)).isEqualTo(3);
        assertThat(ReglasJuego.nuevaRacha(2, HOY, HOY)).isEqualTo(2);
        assertThat(ReglasJuego.nuevaRacha(5, HOY.minusDays(3), HOY)).isEqualTo(1);
        assertThat(ReglasJuego.nuevaRacha(5, HOY, HOY.minusDays(2))).isEqualTo(5);
    }

    @Test
    void laRachaVigenteSePierdeSiNoEntrenoNiHoyNiAyer() {
        assertThat(ReglasJuego.rachaVigente(4, HOY.minusDays(1), HOY)).isEqualTo(4);
        assertThat(ReglasJuego.rachaVigente(4, HOY.minusDays(2), HOY)).isZero();
    }

    @Test
    void elCriterioDeLogroSeLeeYSeEvalua() {
        CriterioLogro criterio = CriterioLogro.leer("racha>=3");
        assertThat(criterio.metrica()).isEqualTo(CriterioLogro.Metrica.RACHA);
        assertThat(criterio.seCumple(3)).isTrue();
        assertThat(criterio.seCumple(2)).isFalse();
        assertThatThrownBy(() -> CriterioLogro.leer("RACHA=3")).isInstanceOf(IllegalArgumentException.class);
    }
}
