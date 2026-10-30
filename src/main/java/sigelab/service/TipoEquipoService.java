package sigelab.service;

import sigelab.enums.RolUsuario;
import sigelab.model.TipoEquipo;
import sigelab.model.Usuario;
import sigelab.repository.TipoEquipoRepository;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.util.List;

public class TipoEquipoService {
    private final TipoEquipoRepository repository = new TipoEquipoRepository();

    public TipoEquipo crear(Usuario actor, TipoEquipo t) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        t.setNombre(Validacion.obligatorio(t.getNombre(), "nombre", 50));
        t.setDescripcion(Validacion.opcional(t.getDescripcion(), "descripción", 255));
        repository.guardar(t);
        return t;
    }

    public boolean actualizar(Usuario actor, TipoEquipo t) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        t.setNombre(Validacion.obligatorio(t.getNombre(), "nombre", 50));
        t.setDescripcion(Validacion.opcional(t.getDescripcion(), "descripción", 255));
        return repository.actualizar(t);
    }

    public List<TipoEquipo> listar(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listar();
    }
}