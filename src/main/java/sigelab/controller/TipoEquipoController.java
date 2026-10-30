package sigelab.controller;

import sigelab.model.TipoEquipo;
import sigelab.model.Usuario;
import sigelab.service.TipoEquipoService;

import java.util.List;

public class TipoEquipoController {
    private final TipoEquipoService service = new TipoEquipoService();
    private final Usuario actor;

    public TipoEquipoController(Usuario actor) { this.actor = actor; }

    public TipoEquipo crear(TipoEquipo t) { return service.crear(actor, t); }
    public boolean actualizar(TipoEquipo t) { return service.actualizar(actor, t); }
    public List<TipoEquipo> listar() { return service.listar(actor); }
}