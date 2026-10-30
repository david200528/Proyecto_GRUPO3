package sigelab.controller;

import sigelab.enums.EstadoEquipo;
import sigelab.model.Usuario;
import sigelab.service.ReporteService;
import sigelab.util.TablaResultado;

import java.time.LocalDate;
import java.util.Map;

public class ReporteController {
    private final ReporteService service = new ReporteService();
    private final Usuario actor;

    public ReporteController(Usuario actor) { this.actor = actor; }

    public Map<String, Integer> dashboard() { return service.dashboard(actor); }
    public TablaResultado equiposPorEstado(EstadoEquipo estado) { return service.equiposPorEstado(actor, estado); }
    public TablaResultado prestamos(String filtro) { return service.prestamos(actor, filtro); }
    public TablaResultado prestamosEntre(LocalDate desde, LocalDate hasta) { return service.prestamosEntre(actor, desde, hasta); }
    public TablaResultado historialEquipo(String codigo) { return service.historialEquipo(actor, codigo); }
    public TablaResultado historialMantenimiento(String codigo) { return service.historialMantenimiento(actor, codigo); }
}