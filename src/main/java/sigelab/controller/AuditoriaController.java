package sigelab.controller;

import sigelab.enums.AccionAuditoria;
import sigelab.model.Usuario;
import sigelab.service.AuditoriaService;
import sigelab.util.TablaResultado;

public class AuditoriaController {
    private final AuditoriaService service = new AuditoriaService();

    public void loginExitoso(Usuario usuario) {
        service.registrar(usuario, AccionAuditoria.LOGIN_EXITOSO, "Usuario", usuario.getIdUsuario(), "Ingreso al sistema");
    }

    public void loginFallido(String usuarioDigitado, String motivo) { service.registrarLoginFallido(usuarioDigitado, motivo); }
    public TablaResultado listar(Usuario actor) { return service.listar(actor); }
}