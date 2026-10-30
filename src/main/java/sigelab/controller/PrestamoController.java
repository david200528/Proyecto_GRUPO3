package sigelab.controller;

import sigelab.model.DetallePrestamo;
import sigelab.model.Equipo;
import sigelab.model.Prestamo;
import sigelab.model.Usuario;
import sigelab.service.PrestamoService;
import sigelab.service.QrService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PrestamoController {
    private final PrestamoService service = new PrestamoService();
    private final QrService qr = new QrService();
    private final Usuario actor;

    public PrestamoController(Usuario actor) { this.actor = actor; }

    public Equipo validarEquipo(String codigo, List<Equipo> agregados) { return service.validarEquipoParaPrestamo(actor, codigo, agregados); }
    public Prestamo crear(String solicitante, LocalDateTime fechaEsperada, String observaciones, List<Equipo> equipos) {
        return service.crear(actor, solicitante, fechaEsperada, observaciones, equipos);
    }
    public List<Prestamo> listarActivos() { return service.listarActivos(actor); }
    public Optional<Prestamo> buscar(int id) { return service.buscar(actor, id); }
    public List<DetallePrestamo> detalles(int idPrestamo) { return service.detalles(actor, idPrestamo); }
    public String leerQr(File imagen) { return qr.leer(imagen); }
}