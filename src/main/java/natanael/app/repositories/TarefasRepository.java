package natanael.app.repositories;

import java.util.List;
import java.util.UUID;
import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import natanael.app.model.TarefasEntity;

@Repository
public interface TarefasRepository extends JpaRepository<TarefasEntity, UUID> {
    
    List<TarefasEntity> findByMetaId(UUID metaId);
    
    List<TarefasEntity> findByPrazoBetween(Date start, Date end);
    
    List<TarefasEntity> findByMetaUsuarioUsernameAndPrazoBetween(String username, Date start, Date end);
}
