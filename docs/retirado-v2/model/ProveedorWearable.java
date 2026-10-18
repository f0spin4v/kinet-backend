package co.edu.uniquindio.kinet.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PROVEEDOR_WEARABLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorWearable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;
}
