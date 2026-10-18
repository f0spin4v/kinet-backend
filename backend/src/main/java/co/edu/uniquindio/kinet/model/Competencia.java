package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoCompetencia;
import co.edu.uniquindio.kinet.model.enums.TipoCompetencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Competencia amistosa por invitación: DUELO, GRUPAL o TORNEO por rondas. El puntaje es el XP de los
 * entrenamientos que empezaron dentro de la ronda. fechaFin es el fin de la ronda en curso o de la competencia.
 */
@Entity
@Table(name = "COMPETENCIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_competencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_creador", nullable = false)
    private Usuario creador;

    /** NULL = cuenta cualquier actividad. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_actividad")
    private TipoActividad tipoActividad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoCompetencia tipo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoCompetencia estado = EstadoCompetencia.PENDIENTE;

    @Column(name = "duracion_ronda_horas", nullable = false)
    private Integer duracionRondaHoras;

    @Builder.Default
    @Column(name = "ronda_actual", nullable = false)
    private Integer rondaActual = 0;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Builder.Default
    @OneToMany(mappedBy = "competencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fechaInvitacion ASC")
    private List<ParticipanteCompetencia> participantes = new ArrayList<>();
}
