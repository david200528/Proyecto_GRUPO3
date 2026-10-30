package sigelab.controller;

import sigelab.model.Laboratorio;
import sigelab.model.Usuario;
import sigelab.service.LaboratorioService;

import java.util.List;

public class LaboratorioController {
    private final LaboratorioService service = new LaboratorioService();
    private final Usuario actor;

    public LaboratorioController(Usuario actor) { this.actor = actor; }

    public Laboratorio crear(Laboratorio l) { return service.crear(actor, l); }
    public boolean actualizar(Laboratorio l) { return service.actualizar(actor, l); }
    public List<Laboratorio> listar() { return service.listar(actor); }
    public List<Laboratorio> listarActivos() { return service.listarActivos(actor); }
}