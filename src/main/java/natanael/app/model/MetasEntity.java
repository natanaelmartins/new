package natanael.app.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity(name = "Metas")
public class MetasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tituloMeta;

    @Column(length = 1000)
    private String analise;

    @ManyToOne
    @JoinColumn(name = "usuario_name")
    private UsuariosEntity usuario;

    public MetasEntity(UUID id, String tituloMeta, UsuariosEntity usuario) {
        this.id = id;
        this.tituloMeta = tituloMeta;
        this.usuario = usuario;
    }
}