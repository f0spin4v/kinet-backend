package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.CanalAutorizacion;
import co.edu.uniquindio.kinet.model.enums.EstadoAutorizacion;
import co.edu.uniquindio.kinet.model.enums.Parentesco;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Autorización del representante legal para tratar los datos de un titular de 14 a 17 años
 * (Ley 1581 de 2012, art. 7; Decreto 1377 de 2013, art. 12). Una por cuenta. Del enlace del correo se guarda
 * solo el hash.
 */
@Entity
@Table(name = "AUTORIZACION_MENOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionMenor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autorizacion")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_representante", nullable = false, length = 160)
    private String nombreRepresentante;

    @Column(name = "correo_representante", nullable = false, length = 120)
    private String correoRepresentante;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", nullable = false, length = 12)
    private Parentesco parentesco;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoAutorizacion estado = EstadoAutorizacion.PENDIENTE;

    /** NULL mientras está PENDIENTE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 13)
    private CanalAutorizacion canal;

    /** SHA-256 del enlace enviado al representante. NULL cuando ya respondió o la registró el administrador. */
    @Column(name = "token_hash", unique = true, length = 64)
    private String tokenHash;

    @Builder.Default
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    /** Documento que respalda la autorización registrada por el administrador (qué es y dónde está guardado). */
    @Column(name = "soporte", length = 300)
    private String soporte;

    /** Administrador que registró o revocó la autorización. */
    @Column(name = "id_admin")
    private Long idAdmin;

    public boolean enlaceVigente() {
        return estado == EstadoAutorizacion.PENDIENTE && tokenHash != null
                && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
