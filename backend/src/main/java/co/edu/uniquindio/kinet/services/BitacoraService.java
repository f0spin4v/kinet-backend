package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.usuario.AuditoriaAdminDTO;
import co.edu.uniquindio.kinet.model.AuditoriaAdmin;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.repositories.AuditoriaAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bitácora de acciones administrativas (auditoría 03/11/2026, criterio 6.7.9). */
@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final AuditoriaAdminRepository repository;

    /** Se guarda en la misma transacción de la acción: si la acción falla, tampoco queda registro. */
    @Transactional
    public void registrar(Usuario actor, String accion, Usuario afectado, String anterior, String nuevo) {
        repository.save(AuditoriaAdmin.builder()
                .idActor(actor.getId())
                .correoActor(actor.getCorreo())
                .accion(accion)
                .idUsuarioAfectado(afectado == null ? null : afectado.getId())
                .correoAfectado(afectado == null ? null : afectado.getCorreo())
                .valorAnterior(recortar(anterior))
                .valorNuevo(recortar(nuevo))
                .build());
    }

    @Transactional(readOnly = true)
    public PaginaDTO<AuditoriaAdminDTO> listar(Pageable pagina) {
        return PaginaDTO.de(repository.findAllByOrderByFechaDescIdDesc(pagina).map(AuditoriaAdminDTO::de));
    }

    private static String recortar(String texto) {
        return texto == null || texto.length() <= 200 ? texto : texto.substring(0, 200);
    }
}
