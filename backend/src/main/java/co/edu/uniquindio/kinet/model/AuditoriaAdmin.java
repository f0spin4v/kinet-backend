package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bitácora de acciones administrativas: quién hizo qué sobre qué cuenta, con el valor anterior y el nuevo.
 * Es de solo inserción (en Oracle, KINET_APP solo tiene SELECT e INSERT sobre esta tabla).
 */
@Entity
@Table(name = "AUDITORIA_ADMIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaAdmin {

    public static final String CAMBIO_ESTADO = "CAMBIO_ESTADO";
    public static final String CAMBIO_ROL = "CAMBIO_ROL";
    public static final String CREACION_CUENTA = "CREACION_CUENTA";
    public static final String AJUSTE_XP = "AJUSTE_XP";
    /** Registro o revocación de la autorización del representante legal de un menor (RN-33). */
    public static final String AUTORIZACION_MENOR = "AUTORIZACION_MENOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "id_actor", nullable = false)
    private Long idActor;

    @Column(name = "correo_actor", nullable = false, length = 120)
    private String correoActor;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "id_usuario_afectado")
    private Long idUsuarioAfectado;

    @Column(name = "correo_afectado", length = 120)
    private String correoAfectado;

    @Column(name = "valor_anterior", length = 200)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 200)
    private String valorNuevo;

    @Builder.Default
    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
