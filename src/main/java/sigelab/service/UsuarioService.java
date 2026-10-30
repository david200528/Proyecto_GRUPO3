package sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.enums.AccionAuditoria;
import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;
import sigelab.repository.UsuarioRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.util.List;
import java.util.Optional;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria = new AuditoriaService();

    public UsuarioService() {
        this(new UsuarioRepository());
    }

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean existeUsuario(String usuario) {
        return usuarioRepository.existeUsuario(usuario);
    }

    public Usuario crearUsuario(Usuario actor, Usuario nuevo, String password) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        nuevo.setNombre(Validacion.obligatorio(nuevo.getNombre(), "nombre", 80));
        nuevo.setApellido(Validacion.obligatorio(nuevo.getApellido(), "apellido", 80));
        nuevo.setUsuario(Validacion.obligatorio(nuevo.getUsuario(), "usuario", 50));
        nuevo.setCorreo(Validacion.correo(nuevo.getCorreo()));
        Validacion.requerido(nuevo.getRol(), "Seleccione el rol del usuario.");
        validarPassword(password);
        if (usuarioRepository.existeUsuario(nuevo.getUsuario())) {
            throw new BusinessException("Ya existe el usuario " + nuevo.getUsuario() + ".");
        }
        nuevo.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        nuevo.setActivo(true);
        usuarioRepository.guardar(nuevo);
        auditoria.registrar(actor, AccionAuditoria.USUARIO_CREADO, "Usuario", nuevo.getIdUsuario(),
                "Usuario " + nuevo.getUsuario() + " (" + nuevo.getRol() + ")");
        return nuevo;
    }

    /** Valida usuario, contraseña y estado ACTIVO (RN-01). No registra auditoría: lo hace el login completo. */
    public Optional<Usuario> autenticar(String nombreUsuario, String password) {
        Optional<Usuario> opt = usuarioRepository.buscarPorUsuario(nombreUsuario);
        if (opt.isPresent()) {
            Usuario u = opt.get();
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

    public List<Usuario> listar(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        return usuarioRepository.listar();
    }

    public boolean actualizar(Usuario actor, Usuario usuario, String passwordNueva) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        usuario.setNombre(Validacion.obligatorio(usuario.getNombre(), "nombre", 80));
        usuario.setApellido(Validacion.obligatorio(usuario.getApellido(), "apellido", 80));
        usuario.setCorreo(Validacion.correo(usuario.getCorreo()));
        Validacion.requerido(usuario.getRol(), "Seleccione el rol del usuario.");
        if (passwordNueva != null && !passwordNueva.isBlank()) {
            validarPassword(passwordNueva);
            usuario.setPasswordHash(BCrypt.hashpw(passwordNueva, BCrypt.gensalt()));
        }
        if (!usuario.isActivo() && usuario.getIdUsuario() == actor.getIdUsuario()) {
            throw new BusinessException("No puede desactivar su propio usuario.");
        }
        return usuarioRepository.actualizar(usuario);
    }

    public boolean desactivar(Usuario actor, int idUsuario) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        if (idUsuario == actor.getIdUsuario()) throw new BusinessException("No puede desactivar su propio usuario.");
        boolean ok = usuarioRepository.desactivar(idUsuario);
        if (ok) auditoria.registrar(actor, AccionAuditoria.USUARIO_DESACTIVADO, "Usuario", idUsuario, "Usuario desactivado");
        return ok;
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 4) {
            throw new BusinessException("La contraseña debe tener al menos 4 caracteres.");
        }
    }
}