package sigelab.controller;

import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Equipo;
import sigelab.model.Mantenimiento;
import sigelab.model.Usuario;
import sigelab.service.MantenimientoService;

import java.util.List;

public class MantenimientoController {
    private final MantenimientoService service = new MantenimientoService();
    private final Usuario actor;

    public MantenimientoController(Usuario actor) { this.actor = actor; }

    public Mantenimiento enviar(Equipo equipo, TipoMantenimiento tipo, String descripcion, String responsable, String obs) {
        return service.enviar(actor, equipo, tipo, descripcion, responsable, obs);
    }
    public void finalizar(int id, ResultadoMantenimiento resultado, String obs) { service.finalizar(actor, id, resultado, obs); }
    public List<Mantenimiento> listar(EstadoMantenimiento estado) { return service.listar(actor, estado); }
}