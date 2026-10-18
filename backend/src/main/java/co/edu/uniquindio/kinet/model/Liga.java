package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Liga competitiva. Al empezar la temporada siguiente, los cuposAscenso mejores de cada liga arrancan
 * como mínimo en la liga de arriba y los cuposDescenso peores como máximo en la de abajo.
 */
@Entity
@Table(name = "LIGA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liga")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    @Builder.Default
    @Column(name = "cupos_ascenso", nullable = false)
    private Integer cuposAscenso = 0;

    @Builder.Default
    @Column(name = "cupos_descenso", nullable = false)
    private Integer cuposDescenso = 0;
}
