package sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.RolUsuario;
import sigelab.model.Docente;
import sigelab.model.Usuario;
import sigelab.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean existeUsuario(String usuario) {
        return usuarioRepository.existeUsuario(usuario);
    }

    public Usuario crearUsuario(String nombreUsuario, String password, RolUsuario rol, Docente docente) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        Usuario usuario = new Usuario(nombreUsuario, passHash, rol, docente);
        boolean guardado = usuarioRepository.guardar(usuario);
        return guardado ? usuario : null;
    }

    public Optional<Usuario> autenticar(String nombreUsuario, String password) {
        Optional<Usuario> optUsuario = usuarioRepository.buscarPorUsuario(nombreUsuario);
        if (optUsuario.isPresent()) {
            Usuario u = optUsuario.get();
            // Validación segura utilizando BCrypt.checkpw
            if (u.isActivo() && u.getPasswordHash() != null && BCrypt.checkpw(password, u.getPasswordHash())) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    public Optional<Usuario> buscar(int id) {
        return usuarioRepository.buscarPorId(id);
    }

    public Optional<Usuario> buscarPorUsuario(String nombreUsuario) {
        return usuarioRepository.buscarPorUsuario(nombreUsuario);
    }

    public List<Usuario> listar() {
        return usuarioRepository.listar();
    }

    public boolean actualizar(Usuario usuario) {
        return usuarioRepository.actualizar(usuario);
    }

    public boolean actualizarConPassword(Usuario usuario, String password) {
        String passHash = BCrypt.hashpw(password, BCrypt.gensalt());
        usuario.setPasswordHash(passHash);
        return usuarioRepository.actualizar(usuario);
    }

    public boolean desactivar(int idUsuario) {
        return usuarioRepository.desactivar(idUsuario);
    }
}