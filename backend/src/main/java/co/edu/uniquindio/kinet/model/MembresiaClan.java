package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.RolClan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** fechaSalida NULL = membresía vigente. Un jugador solo tiene una vigente y cada clan un solo líder. */
@Entity
@Table(name = "MEMBRESIA_CLAN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembresiaClan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_membresia")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_clan", nullable = false)
    private Clan clan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_clan", nullable = false, length = 10)
    private RolClan rolClan = RolClan.MIEMBRO;

    @Builder.Default
    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;
}
