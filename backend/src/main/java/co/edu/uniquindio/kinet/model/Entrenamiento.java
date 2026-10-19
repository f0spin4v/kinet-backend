package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sesión medida con el cronómetro de la app. Al iniciar se guarda la hora de inicio del servidor (EN_CURSO); al
 * finalizar, la de fin y la duración. El jugador nunca escribe la duración. Distancia y calorías son informativas.
 * El XP que dio está en MOVIMIENTO_XP.
 */
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
    @JoinColumn(name = "id_tipo_actividad", nullable = false)
    private TipoActividad tipoActividad;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    /** NULL mientras está EN_CURSO. */
    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    /** Minutos entre inicio y fin, calculados por el servidor. NULL mientras está EN_CURSO. */
    @Column(name = "duracion_min")
    private Integer duracionMin;

    /** La elige el jugador al finalizar. */
    @Enumerated(EnumType.STRING)
    @Column(name = "intensidad", length = 10)
    private Intensidad intensidad;

    @Column(name = "distancia_km", precision = 7, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "calorias_kcal")
    private Integer caloriasKcal;

    /** Fuerza y calistenia: número de series, repeticiones por serie y peso (kg). Informativos. */
    @Column(name = "series")
    private Integer series;

    @Column(name = "repeticiones")
    private Integer repeticiones;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "notas", length = 500)
    private String notas;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_validacion", nullable = false, length = 10)
    private EstadoValidacion estadoValidacion = EstadoValidacion.EN_CURSO;

    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;

    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
