package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.AmbitoReto;
import co.edu.uniquindio.kinet.model.enums.EstadoReto;
import co.edu.uniquindio.kinet.model.enums.MetricaReto;
import co.edu.uniquindio.kinet.model.enums.Periodicidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reto del catálogo. tipoActividad NULL = cualquier actividad; nivelHabilidad NULL = todos los niveles;
 * temporada solo para periodicidad TEMPORADA.
 */
@Entity
@Table(name = "RETO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reto")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_actividad")
    private TipoActividad tipoActividad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel_habilidad")
    private NivelHabilidad nivelHabilidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_temporada")
    private Temporada temporada;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "condiciones", length = 300)
    private String condiciones;

    @Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 20)
    private MetricaReto metrica;

    @Column(name = "valor_meta", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMeta;

    @Column(name = "xp_recompensa", nullable = false)
    private Integer xpRecompensa;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidad", nullable = false, length = 10)
    private Periodicidad periodicidad;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ambito", nullable = false, length = 10)
    private AmbitoReto ambito = AmbitoReto.INDIVIDUAL;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoReto estado = EstadoReto.BORRADOR;
}
