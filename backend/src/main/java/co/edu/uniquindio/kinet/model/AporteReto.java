package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Avance sumado a una participación. entrenamiento NULL = aporte manual (retos de REPETICIONES). */
@Entity
@Table(name = "APORTE_RETO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AporteReto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aporte")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participacion_reto", nullable = false)
    private ParticipacionReto participacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrenamiento")
    private Entrenamiento entrenamiento;

    @Builder.Default
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "nota", length = 200)
    private String nota;
}
