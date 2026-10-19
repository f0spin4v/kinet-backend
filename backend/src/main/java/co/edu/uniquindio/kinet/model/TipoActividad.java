package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.enums.EstadoActividad;
import jakarta.persistence.*;
import lombok.*;

/** Catálogo de actividades. El XP por minuto de cada una está en REGLA_PUNTUACION. INACTIVA: no se puede usar. */
@Entity
@Table(name = "TIPO_ACTIVIDAD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_actividad")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoActividad estado = EstadoActividad.ACTIVA;
}
