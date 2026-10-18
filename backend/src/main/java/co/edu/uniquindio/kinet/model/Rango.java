package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

/** Rango dentro de una liga. El rango de un jugador es el de mayor puntosMinimo que no supera sus puntos. */
@Entity
@Table(name = "RANGO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rango {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rango")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_liga", nullable = false)
    private Liga liga;

    /** I, II o III. NULL si la liga no tiene divisiones. */
    @Column(name = "division", length = 3)
    private String division;

    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    @Column(name = "puntos_minimo", nullable = false, unique = true)
    private Integer puntosMinimo;

    /** Nombre para mostrar, por ejemplo "PLATA II" o "ELITE". */
    public String nombre() {
        return division == null ? liga.getNombre() : liga.getNombre() + " " + division;
    }
}
