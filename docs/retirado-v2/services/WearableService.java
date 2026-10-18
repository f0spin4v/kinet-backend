package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.ResultadoEntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.wearable.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.ConexionWearable;
import co.edu.uniquindio.kinet.model.ProveedorWearable;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoConexion;
import co.edu.uniquindio.kinet.repositories.ConexionWearableRepository;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.ProveedorWearableRepository;
import co.edu.uniquindio.kinet.security.CifradoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Conexión con proveedores wearables e importación de actividades (ADR-05). La sincronización recibe las
 * actividades que entrega el proveedor; cada una pasa por el mismo anti-trampa que un registro manual.
 */
@Service
@RequiredArgsConstructor
public class WearableService {

    private final ConexionWearableRepository conexionRepository;
    private final ProveedorWearableRepository proveedorRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final EntrenamientoService entrenamientoService;
    private final UsuarioService usuarioService;
    private final CifradoService cifradoService;

    @Transactional(readOnly = true)
    public List<ConexionDTO> misConexiones(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return conexionRepository.findByUsuarioIdOrderByFechaVinculacionDesc(usuario.getId()).stream()
                .map(ConexionDTO::de).toList();
    }

    @Transactional
    public ConexionDTO conectar(String correo, ConectarWearableDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        ProveedorWearable proveedor = proveedorRepository.findById(dto.idProveedor())
                .orElseThrow(() -> new NoEncontradoException("El proveedor no existe."));
        if (conexionRepository.existsByUsuarioIdAndProveedorIdAndEstado(usuario.getId(), proveedor.getId(),
                EstadoConexion.ACTIVA)) {
            throw new ConflictoException("Ya tienes una conexión activa con " + proveedor.getNombre() + ".");
        }
        String cuenta = dto.idCuentaExterna().trim();
        if (conexionRepository.existsByProveedorIdAndIdCuentaExternaAndEstado(proveedor.getId(), cuenta,
                EstadoConexion.ACTIVA)) {
            throw new ConflictoException("Esa cuenta de " + proveedor.getNombre() + " ya está vinculada a otro jugador.");
        }
        ConexionWearable conexion = conexionRepository.save(ConexionWearable.builder()
                .usuario(usuario)
                .proveedor(proveedor)
                .idCuentaExterna(cuenta)
                .tokenAccesoCifrado(cifradoService.cifrar(dto.tokenAcceso()))
                .build());
        return ConexionDTO.de(conexion);
    }

    /** Revoca la conexión y borra el token: Kinet deja de poder leer la cuenta del proveedor. */
    @Transactional
    public ConexionDTO revocar(String correo, Long idConexion) {
        ConexionWearable conexion = propia(correo, idConexion);
        if (conexion.getEstado() == EstadoConexion.REVOCADA) {
            throw new ReglaNegocioException("La conexión ya estaba revocada.");
        }
        conexion.setEstado(EstadoConexion.REVOCADA);
        conexion.setTokenAccesoCifrado(null);
        return ConexionDTO.de(conexionRepository.save(conexion));
    }

    @Transactional
    public ResultadoSincronizacionDTO sincronizar(String correo, Long idConexion, SincronizacionDTO dto) {
        ConexionWearable conexion = propia(correo, idConexion);
        if (conexion.getEstado() != EstadoConexion.ACTIVA) {
            throw new ReglaNegocioException("La conexión no está activa.");
        }
        Usuario usuario = conexion.getUsuario();
        int duplicadas = 0;
        int validadas = 0;
        int pendientes = 0;
        int rechazadas = 0;
        int xp = 0;
        List<EntrenamientoDTO> importados = new ArrayList<>();
        // de la más antigua a la más reciente, para que la racha se calcule en orden
        List<ActividadExternaDTO> actividades = dto.actividades().stream()
                .sorted((a, b) -> a.fechaHoraInicio().compareTo(b.fechaHoraInicio())).toList();
        for (ActividadExternaDTO actividad : actividades) {
            if (entrenamientoRepository.existsByConexionIdAndIdActividadExterna(conexion.getId(),
                    actividad.idActividadExterna().trim())) {
                duplicadas++;
                continue;
            }
            ResultadoEntrenamientoDTO resultado = entrenamientoService.importar(usuario, conexion, actividad);
            importados.add(resultado.entrenamiento());
            xp += resultado.xpGanado();
            switch (resultado.entrenamiento().estado()) {
                case "VALIDADO" -> validadas++;
                case "PENDIENTE" -> pendientes++;
                default -> rechazadas++;
            }
        }
        return new ResultadoSincronizacionDTO(importados.size(), duplicadas, validadas, pendientes, rechazadas, xp,
                importados);
    }

    private ConexionWearable propia(String correo, Long idConexion) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return conexionRepository.findByIdAndUsuarioId(idConexion, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("La conexión no existe."));
    }
}
