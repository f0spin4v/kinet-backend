package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Perfil público y estado de juego del usuario (1 a 1, comparte la llave con USUARIO).
 * xpTotal es el saldo del libro MOVIMIENTO_XP; se guarda para no sumar el historial en cada lectura.
 * El nivel no se guarda: se deduce de xpTotal con NIVEL_PROGRESION.
 */
@Entity
@Table(name = "PERFIL_JUGADOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilJugador {

    @Id
    @Column(name = "id_usuario")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "gamertag", nullable = false, unique = true, length = 30)
    private String gamertag;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "avatar_url", length = 300)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "xp_total", nullable = false)
    private Integer xpTotal = 0;

    @Builder.Default
    @Column(name = "racha_actual", nullable = false)
    private Integer rachaActual = 0;

    @Builder.Default
    @Column(name = "racha_maxima", nullable = false)
    private Integer rachaMaxima = 0;

    @Column(name = "fecha_ultima_actividad")
    private LocalDate fechaUltimaActividad;
}
