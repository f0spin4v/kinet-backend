package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOGRO_USUARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogroUsuario {

    @EmbeddedId
    private Id id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @MapsId("idLogro")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_logro")
    private Logro logro;

    @Builder.Default
    @Column(name = "fecha_obtenido", nullable = false)
    private LocalDateTime fechaObtenido = LocalDateTime.now();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "id_usuario")
        private Long idUsuario;
        @Column(name = "id_logro")
        private Long idLogro;
    }
}
