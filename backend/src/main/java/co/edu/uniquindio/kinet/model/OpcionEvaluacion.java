package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** Opción de respuesta: "vota" por un nivel con un peso (puntaje). */
@Entity
@Table(name = "OPCION_EVALUACION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaEvaluacion pregunta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_nivel_habilidad", nullable = false)
    private NivelHabilidad nivelHabilidad;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "puntaje", nullable = false)
    private Integer puntaje;
}
