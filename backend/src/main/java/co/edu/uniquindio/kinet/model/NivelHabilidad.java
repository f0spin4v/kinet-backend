package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** Nivel de habilidad que asigna la evaluación (INICIACION, INTERMEDIO, AVANZADO, ATLETA). */
@Entity
@Table(name = "NIVEL_HABILIDAD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NivelHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_habilidad")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
