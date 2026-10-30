package sigelab.controller;

import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;
import sigelab.model.Usuario;
import sigelab.service.EquipoService;
import sigelab.service.QrService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class EquipoController {
    private final EquipoService service = new EquipoService();
    private final QrService qr = new QrService();
    private final Usuario actor;

    public EquipoController(Usuario actor) { this.actor = actor; }

    public Equipo crear(Equipo e) { return service.crear(actor, e); }
    public boolean actualizar(Equipo e) { return service.actualizar(actor, e); }
    public List<Equipo> listar(EstadoEquipo estado) { return service.listar(actor, estado); }
    public List<Equipo> buscar(String texto) { return service.buscar(actor, texto); }
    public Optional<Equipo> buscarPorCodigo(String codigo) { return service.buscarPorCodigo(actor, codigo); }
    public BufferedImage generarQr(String codigo, int tamano) { return qr.generar(codigo, tamano); }
    public void guardarQr(String codigo, File destino) { qr.guardarPng(codigo, destino); }
    public String leerQr(File imagen) { return qr.leer(imagen); }
}