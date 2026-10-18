package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** Catálogo de actividades físicas. El administrador ajusta cuánto XP da cada minuto. */
@Entity
@Table(name = "TIPO_ACTIVIDAD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "xp_por_minuto", nullable = false)
    private Integer xpPorMinuto;
}
