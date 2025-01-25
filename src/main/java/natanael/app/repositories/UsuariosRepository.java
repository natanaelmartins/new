package natanael.app.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import natanael.app.model.UsuariosEntity;

@Repository
public interface UsuariosRepository extends JpaRepository<UsuariosEntity, UUID> {

    Optional<UsuariosEntity> findByEmail(String email);

    Optional<UsuariosEntity> findByUsername(String username);
     
}