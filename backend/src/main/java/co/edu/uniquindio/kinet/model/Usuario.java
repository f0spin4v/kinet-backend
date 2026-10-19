package co.edu.uniquindio.kinet.model;

import co.edu.uniquindio.kinet.model.converters.SiNoConverter;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Builder.Default
    @Convert(converter = SiNoConverter.class)
    @Column(name = "acepta_politica_datos", nullable = false, length = 1)
    private Boolean aceptaPoliticaDatos = false;

    @Column(name = "fecha_acepta_politica")
    private LocalDate fechaAceptaPolitica;

    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PerfilJugador perfil;

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public boolean tieneRol(String nombreRol) {
        return rol != null && nombreRol.equals(rol.getNombre());
    }
}
