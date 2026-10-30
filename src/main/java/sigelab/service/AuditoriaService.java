package sigelab.service;

import sigelab.enums.AccionAuditoria;
import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;
import sigelab.repository.AuditoriaRepository;
import sigelab.util.Permisos;
import sigelab.util.TablaResultado;

public class AuditoriaService {
    private final AuditoriaRepository repository;

    public AuditoriaService() { this(new AuditoriaRepository()); }
    public AuditoriaService(AuditoriaRepository repository) { this.repository = repository; }

    /** RN-18: deja rastro de una operación hecha por un usuario. */
    public void registrar(Usuario actor, AccionAuditoria accion, String entidad, Integer idRegistro, String detalle) {
        repository.registrar(actor == null ? null : actor.getIdUsuario(),
                actor == null ? null : actor.getUsuario(), accion.name(), entidad, idRegistro, detalle);
    }

    /** Login fallido: el usuario puede no existir, por eso se guarda el texto digitado. */
    public void registrarLoginFallido(String usuarioDigitado, String motivo) {
        repository.registrar(null, usuarioDigitado, AccionAuditoria.LOGIN_FALLIDO.name(), "Usuario", null, motivo);
    }

    public TablaResultado listar(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        return repository.listar(500);
    }
}