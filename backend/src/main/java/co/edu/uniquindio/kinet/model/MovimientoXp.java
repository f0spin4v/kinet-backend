package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.OrigenXp;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Libro de XP: cada suma o resta queda registrada con su origen y exactamente una referencia
 * (entrenamiento, participación en reto, logro o evidencia). Un AJUSTE no tiene referencia pero sí motivo.
 */
@Entity
@Table(name = "MOVIMIENTO_XP")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoXp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Temporada activa cuando se ganó; NULL si no había ninguna. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_temporada")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrenamiento", unique = true)
    private Entrenamiento entrenamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participacion_reto", unique = true)
    private ParticipacionReto participacionReto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_logro")
    private Logro logro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evidencia", unique = true)
    private EvidenciaEntrenamiento evidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_competencia")
    private Competencia competencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 15)
    private OrigenXp origen;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Builder.Default
    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento = LocalDateTime.now();
}
