package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.converters.SiNoConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PREGUNTA_EVALUACION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Long id;

    @Column(name = "enunciado", nullable = false, length = 300)
    private String enunciado;

    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    @Builder.Default
    @Convert(converter = SiNoConverter.class)
    @Column(name = "activa", nullable = false, length = 1)
    private Boolean activa = true;

    @Builder.Default
    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<OpcionEvaluacion> opciones = new ArrayList<>();
}
