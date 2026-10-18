package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Sesión de entrenamiento reportada por el jugador, ya validada o rechazada por el anti-trampa. */
@Entity
@Table(name = "ENTRENAMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrenamiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoActividad tipoActividad;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensidad", nullable = false, length = 10)
    private Intensidad intensidad;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoEntrenamiento estado;

    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;

    @Builder.Default
    @Column(name = "xp_otorgado", nullable = false)
    private Integer xpOtorgado = 0;

    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
