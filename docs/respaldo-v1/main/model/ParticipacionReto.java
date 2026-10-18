package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Inscripción de un jugador en un reto y su avance hacia la meta. */
@Entity
@Table(name = "PARTICIPACION_RETO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipacionReto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participacion")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_reto", nullable = false)
    private Reto reto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @Builder.Default
    @Column(name = "progreso", nullable = false)
    private Integer progreso = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoParticipacion estado = EstadoParticipacion.EN_CURSO;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;
}
