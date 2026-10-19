package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoEnvio;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Aviso al jugador. Se crea PENDIENTE y el envío en segundo plano lo pasa a ENVIADA o FALLIDA. */
@Entity
@Table(name = "NOTIFICACION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25)
    private TipoNotificacion tipo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalNotificacion canal = CanalNotificacion.PUSH;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 300)
    private String mensaje;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 10)
    private EstadoEnvio estadoEnvio = EstadoEnvio.PENDIENTE;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    /** NULL = sin leer. */
    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
}
