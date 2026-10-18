package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoConexion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Vínculo con la cuenta del jugador en un proveedor. El token va cifrado y se borra al revocar. */
@Entity
@Table(name = "CONEXION_WEARABLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConexionWearable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conexion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private ProveedorWearable proveedor;

    @Column(name = "id_cuenta_externa", nullable = false, length = 100)
    private String idCuentaExterna;

    @Column(name = "token_acceso_cifrado", length = 500)
    private String tokenAccesoCifrado;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoConexion estado = EstadoConexion.ACTIVA;

    @Builder.Default
    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDateTime fechaVinculacion = LocalDateTime.now();
}
