package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.converters.SiNoConverter;
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

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Builder.Default
    @Convert(converter = SiNoConverter.class)
    @Column(name = "usado", nullable = false, length = 1)
    private Boolean usado = false;

    public boolean estaVigente() {
        return !Boolean.TRUE.equals(usado) && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
