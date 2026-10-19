package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Enlace de un solo uso para restablecer la contraseña. Se guarda el hash, nunca el token. */
@Entity
@Table(name = "TOKEN_RECUPERACION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /** NULL = todavía no se ha usado. */
    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    public boolean estaVigente() {
        return fechaUso == null && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
