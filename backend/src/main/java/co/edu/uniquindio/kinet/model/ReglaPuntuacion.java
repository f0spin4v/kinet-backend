package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.Intensidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** XP por minuto de una actividad e intensidad durante una vigencia. vigenteHasta NULL = regla vigente. */
@Entity
@Table(name = "REGLA_PUNTUACION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaPuntuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_actividad", nullable = false)
    private TipoActividad tipoActividad;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensidad", nullable = false, length = 10)
    private Intensidad intensidad;

    @Column(name = "xp_por_minuto", nullable = false, precision = 6, scale = 2)
    private BigDecimal xpPorMinuto;

    @Column(name = "duracion_minima_min", nullable = false)
    private Integer duracionMinimaMin;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;
}
