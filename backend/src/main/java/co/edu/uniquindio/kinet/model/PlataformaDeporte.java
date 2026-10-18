package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.converters.SiNoConverter;
import jakarta.persistence.*;
import lombok.*;

/**
 * App deportiva cuyos enlaces se aceptan como evidencia. "dominio" es el host (también vale cualquier subdominio)
 * y "patronRuta" una expresión regular que debe cumplir la ruta del enlace. Apagar "activa" deja de aceptarla.
 */
@Entity
@Table(name = "PLATAFORMA_DEPORTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlataformaDeporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plataforma")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    @Column(name = "dominio", nullable = false, unique = true, length = 100)
    private String dominio;

    @Column(name = "patron_ruta", nullable = false, length = 200)
    private String patronRuta;

    @Builder.Default
    @Convert(converter = SiNoConverter.class)
    @Column(name = "activa", nullable = false, length = 1)
    private Boolean activa = true;
}
