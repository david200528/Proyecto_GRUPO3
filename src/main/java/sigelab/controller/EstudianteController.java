package sigelab.controller;

import sigelab.model.Estudiante;
import sigelab.service.EstudianteService;

import java.util.List;
import java.util.Optional;

public class EstudianteController {
    private final EstudianteService service;
    public EstudianteController() { this(new EstudianteService()); }
    public EstudianteController(EstudianteService service) { this.service = service; }
    public boolean guardar(Estudiante e) { return service.guardar(e); }
    public List<Estudiante> listar() { return service.listar(); }
    public List<Estudiante> listarActivos() { return service.listarActivos(); }
    public Optional<Estudiante> buscar(int id) { return service.buscar(id); }
    public Optional<Estudiante> buscarPorCarnet(String carnet) { return service.buscarPorCarnet(carnet); }
    public boolean actualizar(Estudiante e) { return service.actualizar(e); }
    public boolean desactivar(int id) { return service.desactivar(id); }
}
