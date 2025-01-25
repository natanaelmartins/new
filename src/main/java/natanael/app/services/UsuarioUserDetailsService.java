package natanael.app.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import natanael.app.model.UsuariosEntity;
import natanael.app.repositories.UsuariosRepository;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

	@Autowired
	UsuariosRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UsuariosEntity> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isPresent()) {
            var userObj = usuario.get();
            return User.builder()
                    .username(userObj.getUsername())
                    .password(userObj.getPassword())
                    .roles(getRoles(userObj))
                    .build();
        } else {
            throw new UsernameNotFoundException(email);
        }
    }

    private String[] getRoles(UsuariosEntity user) {
        if (user.getRole() == null) {
            return new String[]{"USER"};
        }
        return user.getRole().split(",");
    }
}