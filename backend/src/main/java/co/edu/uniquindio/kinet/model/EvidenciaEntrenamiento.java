package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Respaldo de un entrenamiento: una imagen (captura del reloj o de una app) o el enlace a la actividad en una app
 * deportiva reconocida (PLATAFORMA_DEPORTE). Una u otra, nunca las dos. Da un bonus de XP y no se valida
 * automáticamente: un entrenador la revisa si hace falta. El enlace es el que cuenta como prueba obligatoria
 * (sesiones de más de 180 minutos y competencias).
 */
@Entity
@Table(name = "EVIDENCIA_ENTRENAMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evidencia")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrenamiento", nullable = false, unique = true)
    private Entrenamiento entrenamiento;

    /** Solo en las evidencias de imagen. */
    @Column(name = "nombre_archivo", length = 200)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", length = 30)
    private String tipoContenido;

    @Column(name = "tamano_bytes")
    private Integer tamanoBytes;

    /** La imagen. Las consultas de listado no la cargan (usan EvidenciaDTO). */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "contenido")
    private byte[] contenido;

    /** Solo en las evidencias de enlace: la dirección normalizada y la app que la reconoció. */
    @Column(name = "url_externa", unique = true, length = 500)
    private String urlExterna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plataforma")
    private PlataformaDeporte plataforma;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_revision", nullable = false, length = 12)
    private EstadoEvidencia estadoRevision = EstadoEvidencia.SIN_REVISAR;

    @Column(name = "nota_revision", length = 200)
    private String notaRevision;

    @Builder.Default
    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida = LocalDateTime.now();

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    public boolean esEnlace() {
        return urlExterna != null;
    }
}
