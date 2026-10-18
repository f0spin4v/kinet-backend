package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoParticipacionTemporada;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Inscripción de un jugador en una temporada. puntosFinales y posicionFinal son la foto al cierre. */
@Entity
@Table(name = "PARTICIPACION_TEMPORADA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipacionTemporada {

    @EmbeddedId
    private Id id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @MapsId("idTemporada")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_temporada")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rango_inicial", nullable = false)
    private Rango rangoInicial;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rango_actual", nullable = false)
    private Rango rangoActual;

    @Builder.Default
    @Column(name = "puntos_iniciales", nullable = false)
    private Integer puntosIniciales = 0;

    @Builder.Default
    @Column(name = "puntos_actuales", nullable = false)
    private Integer puntosActuales = 0;

    @Column(name = "puntos_finales")
    private Integer puntosFinales;

    @Column(name = "posicion_final")
    private Integer posicionFinal;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 13)
    private EstadoParticipacionTemporada estado = EstadoParticipacionTemporada.ACTIVA;

    @Builder.Default
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion = LocalDateTime.now();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "id_usuario")
        private Long idUsuario;
        @Column(name = "id_temporada")
        private Long idTemporada;
    }
}
