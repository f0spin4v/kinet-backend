package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Evaluación de nivel. Se puede repetir; el nivel vigente es el de la más reciente. */
@Entity
@Table(name = "EVALUACION_USUARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_nivel_habilidad", nullable = false)
    private NivelHabilidad nivelHabilidad;

    @Column(name = "version_algoritmo", nullable = false, length = 20)
    private String versionAlgoritmo;

    @Builder.Default
    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion = LocalDateTime.now();

    /** Opciones elegidas (tabla RESPUESTA_EVALUACION). */
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "RESPUESTA_EVALUACION",
            joinColumns = @JoinColumn(name = "id_evaluacion"),
            inverseJoinColumns = @JoinColumn(name = "id_opcion"))
    private List<OpcionEvaluacion> respuestas = new ArrayList<>();
}
