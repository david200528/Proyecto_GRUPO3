package sigelab.service;

import sigelab.enums.AccionAuditoria;
import sigelab.enums.RolUsuario;
import sigelab.model.Laboratorio;
import sigelab.model.Usuario;
import sigelab.repository.LaboratorioRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.util.List;

public class LaboratorioService {
    private final LaboratorioRepository repository = new LaboratorioRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    public Laboratorio crear(Usuario actor, Laboratorio l) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        validar(l, true);
        l.setActivo(true);
        repository.guardar(l);                                   // RN-03: UNIQUE en base de datos
        auditoria.registrar(actor, AccionAuditoria.LABORATORIO_CREADO, "Laboratorio", l.getIdLaboratorio(),
                "Laboratorio " + l.getCodigo());
        return l;
    }

    public boolean actualizar(Usuario actor, Laboratorio l) {
        Permisos.requerir(actor, RolUsuario.ADMIN);
        validar(l, false);
        return repository.actualizar(l);
    }

    public List<Laboratorio> listar(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listar();
    }

    public List<Laboratorio> listarActivos(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listarActivos();
    }

    private void validar(Laboratorio l, boolean nuevo) {
        if (nuevo) l.setCodigo(Validacion.obligatorio(l.getCodigo(), "código", 50));
        l.setNombre(Validacion.obligatorio(l.getNombre(), "nombre", 100));
        l.setUbicacion(Validacion.opcional(l.getUbicacion(), "ubicación", 150));
        if (l.getCapacidad() < 0) throw new BusinessException("La capacidad no puede ser negativa.");
    }
}