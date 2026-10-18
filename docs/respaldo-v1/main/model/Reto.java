package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.converters.SiNoConverter;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import co.edu.uniquindio.kinet.model.enums.TipoMeta;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reto o misión que el jugador puede aceptar.
 * Si tipoActividad es null, cuenta cualquier actividad. Si nivelRequerido es null, es para todos los niveles.
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

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_meta", nullable = false, length = 10)
    private TipoMeta tipoMeta;

    @Column(name = "meta_valor", nullable = false)
    private Integer metaValor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo")
    private TipoActividad tipoActividad;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_requerido", length = 20)
    private NivelInicial nivelRequerido;

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;

    @Column(name = "xp_recompensa", nullable = false)
    private Integer xpRecompensa;

    @Builder.Default
    @Convert(converter = SiNoConverter.class)
    @Column(name = "activo", nullable = false, length = 1)
    private Boolean activo = true;

    /** Indica si este reto está disponible para un jugador con el nivel dado. */
    public boolean aplicaParaNivel(NivelInicial nivel) {
        return nivelRequerido == null || nivelRequerido == nivel;
    }
}
