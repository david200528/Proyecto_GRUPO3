package sigelab.controller;

import sigelab.enums.CondicionDevolucion;
import sigelab.model.Usuario;
import sigelab.service.DevolucionService;

public class DevolucionController {
    private final DevolucionService service = new DevolucionService();
    private final Usuario actor;

    public DevolucionController(Usuario actor) { this.actor = actor; }

    /** Devuelve true si con esta devolución el préstamo quedó finalizado. */
    public boolean devolver(int idPrestamo, int idDetalle, CondicionDevolucion condicion, String observaciones) {
        return service.devolver(actor, idPrestamo, idDetalle, condicion, observaciones);
    }
}