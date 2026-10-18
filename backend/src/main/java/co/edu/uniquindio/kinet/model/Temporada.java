package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Temporada competitiva. Solo una puede estar ACTIVA. porcentajeConservado pasa a la siguiente. */
@Entity
@Table(name = "TEMPORADA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_temporada")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Builder.Default
    @Column(name = "porcentaje_conservado", nullable = false)
    private Integer porcentajeConservado = 50;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoTemporada estado = EstadoTemporada.PROGRAMADA;
}
