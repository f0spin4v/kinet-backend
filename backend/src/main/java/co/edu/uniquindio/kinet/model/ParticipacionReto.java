package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Participación de un jugador en un reto durante un periodo (única por reto, jugador y periodo).
 * progreso es la suma de sus APORTE_RETO; en retos de CLAN la meta se compara con la suma del clan.
 */
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
    @Column(name = "id_participacion_reto")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_reto", nullable = false)
    private Reto reto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clan")
    private Clan clan;

    /** Día en que empieza el periodo: el día (DIARIA), el lunes (SEMANAL) o el inicio del reto o temporada. */
    @Column(name = "periodo", nullable = false)
    private LocalDate periodo;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @Builder.Default
    @Column(name = "progreso", nullable = false, precision = 10, scale = 2)
    private BigDecimal progreso = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoParticipacion estado = EstadoParticipacion.EN_CURSO;

    @Builder.Default
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion = LocalDateTime.now();

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;
}
