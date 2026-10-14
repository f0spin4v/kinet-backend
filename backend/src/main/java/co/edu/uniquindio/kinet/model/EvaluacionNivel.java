package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "EVALUACION_NIVEL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionNivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "puntaje", nullable = false)
    private Integer puntaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_inicial", nullable = false, length = 20)
    private NivelInicial nivelInicial;

    @Builder.Default
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
