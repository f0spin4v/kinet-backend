package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.CausaCambioRango;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Cambio de rango de un jugador en una temporada. Solo se inserta: no se edita ni se borra. */
@Entity
@Table(name = "HISTORIAL_RANGO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialRango {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_rango")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", nullable = false),
            @JoinColumn(name = "id_temporada", referencedColumnName = "id_temporada", nullable = false)
    })
    private ParticipacionTemporada participacion;

    /** NULL en la inscripción. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rango_anterior")
    private Rango rangoAnterior;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rango_nuevo", nullable = false)
    private Rango rangoNuevo;

    @Column(name = "puntos", nullable = false)
    private Integer puntos;

    @Enumerated(EnumType.STRING)
    @Column(name = "causa", nullable = false, length = 15)
    private CausaCambioRango causa;

    @Builder.Default
    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();
}
