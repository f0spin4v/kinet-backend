package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoEnfrentamiento;
import co.edu.uniquindio.kinet.model.enums.TipoEnfrentamiento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Enfrentamiento de 24 horas: cuenta el XP de entrenamientos que empezaron dentro de la ventana. */
@Entity
@Table(name = "ENFRENTAMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enfrentamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_enfrentamiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_temporada", nullable = false)
    private Temporada temporada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 12)
    private TipoEnfrentamiento tipo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoEnfrentamiento estado = EstadoEnfrentamiento.EN_CURSO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Builder.Default
    @OneToMany(mappedBy = "enfrentamiento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ParticipanteEnfrentamiento> participantes = new ArrayList<>();
}
