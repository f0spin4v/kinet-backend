package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ROL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    public static final String JUGADOR = "JUGADOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ENTRENADOR = "ENTRENADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
