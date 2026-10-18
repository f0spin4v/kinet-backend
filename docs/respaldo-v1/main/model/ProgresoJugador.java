package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** XP, nivel y racha acumulados del jugador. Se actualiza con cada entrenamiento validado. */
@Entity
@Table(name = "PROGRESO_JUGADOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgresoJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_progreso")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Builder.Default
    @Column(name = "xp_total", nullable = false)
    private Integer xpTotal = 0;

    @Builder.Default
    @Column(name = "nivel", nullable = false)
    private Integer nivel = 1;

    @Builder.Default
    @Column(name = "racha_actual", nullable = false)
    private Integer rachaActual = 0;

    @Builder.Default
    @Column(name = "racha_maxima", nullable = false)
    private Integer rachaMaxima = 0;

    @Column(name = "fecha_ultimo_entrenamiento")
    private LocalDate fechaUltimoEntrenamiento;
}
