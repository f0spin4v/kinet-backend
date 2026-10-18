package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ParticipanteEnfrentamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipanteEnfrentamientoRepository
        extends JpaRepository<ParticipanteEnfrentamiento, ParticipanteEnfrentamiento.Id> {
}
