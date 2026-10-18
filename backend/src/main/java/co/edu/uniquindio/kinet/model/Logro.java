package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** Logro desbloqueable. criterio tiene la forma "METRICA>=VALOR" (ver CriterioLogro). */
@Entity
@Table(name = "LOGRO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Logro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_logro")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "criterio", nullable = false, length = 300)
    private String criterio;

    @Builder.Default
    @Column(name = "xp_bonus", nullable = false)
    private Integer xpBonus = 0;

    @Column(name = "icono_url", length = 300)
    private String iconoUrl;
}
