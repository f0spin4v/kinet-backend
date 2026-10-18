package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.temporada.ParticipacionTemporadaDTO;
import co.edu.uniquindio.kinet.dto.temporada.TemporadaDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.ParticipacionTemporada;
import co.edu.uniquindio.kinet.model.Temporada;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que cambia el estado de la temporada o de un participante y además toca el matchmaking: cerrar la temporada,
 * abandonarla o descalificar a un jugador. Está aparte porque MatchmakingService ya depende de TemporadaService.
 * Un enfrentamiento en curso de quien abandona o es descalificado termina normal, pero a esa persona no le suma.
 */
@Service
@RequiredArgsConstructor
public class CierreTemporadaService {

    private final TemporadaService temporadaService;
    private final MatchmakingService matchmakingService;

    @Transactional
    public TemporadaDTO cerrar(Long idTemporada) {
        Temporada temporada = temporadaService.buscar(idTemporada);
        if (temporada.getEstado() != EstadoTemporada.ACTIVA) {
            throw new ReglaNegocioException("Solo se puede cerrar la temporada activa.");
        }
        matchmakingService.finalizarVencidos();
        matchmakingService.cancelarDeTemporada(temporada);
        return temporadaService.cerrar(temporada);
    }

    /** El jugador abandona la temporada activa: sale de la cola y deja de sumar puntos. */
    @Transactional
    public ParticipacionTemporadaDTO abandonar(String correo) {
        ParticipacionTemporada participacion = temporadaService.miParticipacionEntidad(correo);
        temporadaService.abandonar(participacion);
        matchmakingService.salirDeCola(participacion.getUsuario(), participacion.getTemporada());
        return temporadaService.miParticipacion(correo);
    }

    /** Moderación descalifica a un jugador de la temporada activa. */
    @Transactional
    public void descalificar(Long idUsuario, String motivo) {
        Temporada temporada = temporadaService.activaObligatoria();
        ParticipacionTemporada participacion = temporadaService.participacion(idUsuario, temporada.getId())
                .orElseThrow(() -> new NoEncontradoException("El jugador no participa en la temporada activa."));
        temporadaService.descalificar(participacion, motivo);
        matchmakingService.salirDeCola(participacion.getUsuario(), temporada);
    }
}
