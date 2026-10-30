package sigelab.service;

import sigelab.enums.AccionAuditoria;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.RolUsuario;
import sigelab.model.Equipo;
import sigelab.model.Usuario;
import sigelab.repository.EquipoRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.util.List;
import java.util.Optional;

public class EquipoService {
    private final EquipoRepository repository = new EquipoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public Equipo crear(Usuario actor, Equipo e) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validar(e, true);
        e.setEstado(EstadoEquipo.DISPONIBLE);
        repository.guardar(e);                                   // RN-04: UNIQUE en base de datos
        auditoria.registrar(actor, AccionAuditoria.EQUIPO_CREADO, "Equipo", e.getIdEquipo(),
                "Equipo " + e.getCodigoInventario());
        return e;
    }

    public boolean actualizar(Usuario actor, Equipo e) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        validar(e, false);
        boolean ok = repository.actualizar(e);
        if (ok) auditoria.registrar(actor, AccionAuditoria.EQUIPO_ACTUALIZADO, "Equipo", e.getIdEquipo(),
                "Equipo " + e.getCodigoInventario());
        return ok;
    }

    public List<Equipo> listar(Usuario actor, EstadoEquipo estado) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listar(estado);
    }

    public List<Equipo> buscar(Usuario actor, String texto) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return texto == null || texto.isBlank() ? repository.listar(null) : repository.buscar(texto);
    }

    /** Consulta el equipo real en la base de datos a partir del código (leído o escrito). RN-19. */
    public Optional<Equipo> buscarPorCodigo(Usuario actor, String codigo) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        return repository.buscarPorCodigo(codigo.trim());
    }

    private void validar(Equipo e, boolean nuevo) {
        if (nuevo) e.setCodigoInventario(Validacion.obligatorio(e.getCodigoInventario(), "código de inventario", 50));
        e.setNombre(Validacion.obligatorio(e.getNombre(), "nombre", 100));
        e.setMarca(Validacion.opcional(e.getMarca(), "marca", 50));
        e.setModelo(Validacion.opcional(e.getModelo(), "modelo", 50));
        e.setNumeroSerie(Validacion.opcional(e.getNumeroSerie(), "número de serie", 100));
        e.setObservaciones(Validacion.opcional(e.getObservaciones(), "observaciones", 255));
        if (e.getTipo() == null) throw new BusinessException("Seleccione el tipo de equipo.");
        if (e.getLaboratorio() == null) throw new BusinessException("Seleccione el laboratorio.");
    }
}