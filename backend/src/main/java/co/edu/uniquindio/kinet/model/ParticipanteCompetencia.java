package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoInvitacion;
import co.edu.uniquindio.kinet.model.enums.ResultadoCompetencia;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Invitado a una competencia. El creador queda ACEPTADO desde el inicio. */
@Entity
@Table(name = "PARTICIPANTE_COMPETENCIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteCompetencia {

    @EmbeddedId
    private Id id;

    @MapsId("idCompetencia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_competencia")
    private Competencia competencia;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_invitacion", nullable = false, length = 10)
    private EstadoInvitacion estadoInvitacion = EstadoInvitacion.INVITADO;

    @Builder.Default
    @Column(name = "fecha_invitacion", nullable = false)
    private LocalDateTime fechaInvitacion = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    /** XP acumulado en las rondas ya cerradas. */
    @Builder.Default
    @Column(name = "puntaje", nullable = false, precision = 10, scale = 2)
    private BigDecimal puntaje = BigDecimal.ZERO;

    /** Solo torneos: ronda en la que quedó fuera. NULL = sigue en competencia (o ganó). */
    @Column(name = "eliminado_en_ronda")
    private Integer eliminadoEnRonda;

    @Column(name = "posicion")
    private Integer posicion;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 10)
    private ResultadoCompetencia resultado;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "id_competencia")
        private Long idCompetencia;
        @Column(name = "id_usuario")
        private Long idUsuario;
    }
}
