package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** XP acumulado necesario para alcanzar cada nivel del jugador. */
@Entity
@Table(name = "NIVEL_PROGRESION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NivelProgresion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_progresion")
    private Long id;

    @Column(name = "numero_nivel", nullable = false, unique = true)
    private Integer numeroNivel;

    @Column(name = "xp_requerido", nullable = false, unique = true)
    private Integer xpRequerido;
}
