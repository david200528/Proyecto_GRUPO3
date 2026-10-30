package sigelab.controller;

import sigelab.model.Usuario;
import sigelab.service.UsuarioService;

import java.util.List;
import java.util.Optional;

public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController() { this(new UsuarioService()); }
    public UsuarioController(UsuarioService service) { this.service = service; }

    public Usuario crearUsuario(Usuario actor, Usuario nuevo, String password) { return service.crearUsuario(actor, nuevo, password); }
    public Optional<Usuario> autenticar(String usuario, String password) { return service.autenticar(usuario, password); }
    public Optional<Usuario> buscar(int id) { return service.buscar(id); }
    public Optional<Usuario> buscarPorUsuario(String usuario) { return service.buscarPorUsuario(usuario); }
    public List<Usuario> listar(Usuario actor) { return service.listar(actor); }
    public boolean actualizar(Usuario actor, Usuario usuario, String passwordNueva) { return service.actualizar(actor, usuario, passwordNueva); }
    public boolean desactivar(Usuario actor, int id) { return service.desactivar(actor, id); }
}