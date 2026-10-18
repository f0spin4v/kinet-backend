package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.ResultadoEnfrentamiento;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/** Jugador dentro de un enfrentamiento. puntaje y resultado se fijan al finalizar. */
@Entity
@Table(name = "PARTICIPANTE_ENFRENTAMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteEnfrentamiento {

    @EmbeddedId
    private Id id;

    @MapsId("idEnfrentamiento")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_enfrentamiento")
    private Enfrentamiento enfrentamiento;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "puntaje", precision = 10, scale = 2)
    private BigDecimal puntaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 12)
    private ResultadoEnfrentamiento resultado;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "id_enfrentamiento")
        private Long idEnfrentamiento;
        @Column(name = "id_usuario")
        private Long idUsuario;
    }
}
