package sigelab.service;

import sigelab.enums.AccionAuditoria;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.RolUsuario;
import sigelab.model.DetallePrestamo;
import sigelab.model.Equipo;
import sigelab.model.Prestamo;
import sigelab.model.Usuario;
import sigelab.repository.EquipoRepository;
import sigelab.repository.PrestamoRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PrestamoService {
    private final PrestamoRepository repository = new PrestamoRepository();
    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    /** Valida el equipo antes de agregarlo a un préstamo en armado (RN-07, RN-08, RN-14). */
    public Equipo validarEquipoParaPrestamo(Usuario actor, String codigoInventario, List<Equipo> yAgregados) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Optional<Equipo> opt = equipoRepository.buscarPorCodigo(codigoInventario == null ? "" : codigoInventario.trim());
        Equipo e = opt.orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigoInventario + "."));
        if (e.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new BusinessException("El equipo " + e.getCodigoInventario() + " no está disponible (estado: "
                    + e.getEstado().name().toLowerCase() + ").");
        }
        for (Equipo agregado : yAgregados) {
            if (agregado.getIdEquipo() == e.getIdEquipo()) {                       // RN-06
                throw new BusinessException("El equipo " + e.getCodigoInventario() + " ya está en este préstamo.");
            }
        }
        return e;
    }

    public Prestamo crear(Usuario actor, String solicitante, LocalDateTime fechaEsperada, String observaciones,
                          List<Equipo> equipos) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        String quien = Validacion.obligatorio(solicitante, "solicitante", 150);
        Validacion.requerido(fechaEsperada, "Indique la fecha esperada de devolución.");
        if (!fechaEsperada.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha esperada de devolución debe ser posterior a este momento.");
        }
        if (equipos == null || equipos.isEmpty()) {                                  // RN-05
            throw new BusinessException("El préstamo debe incluir al menos un equipo.");
        }
        Set<Integer> ids = new HashSet<>();
        for (Equipo e : equipos) {
            if (!ids.add(e.getIdEquipo())) {                                         // RN-06
                throw new BusinessException("El equipo " + e.getCodigoInventario() + " está repetido.");
            }
            Equipo actual = equipoRepository.buscarPorId(e.getIdEquipo())
                    .orElseThrow(() -> new BusinessException("El equipo " + e.getCodigoInventario() + " ya no existe."));
            if (actual.getEstado() != EstadoEquipo.DISPONIBLE) {                     // RN-07, RN-08
                throw new BusinessException("El equipo " + actual.getCodigoInventario() + " ya no está disponible.");
            }
        }
        Prestamo p = new Prestamo();
        p.setSolicitante(quien);
        p.setFechaEsperada(fechaEsperada);
        p.setObservaciones(Validacion.opcional(observaciones, "observaciones", 255));
        p.setIdUsuarioRegistra(actor.getIdUsuario());
        repository.crear(p, equipos.stream().map(Equipo::getIdEquipo).toList());    // RN-09 en la transacción
        auditoria.registrar(actor, AccionAuditoria.PRESTAMO_CREADO, "Prestamo", p.getIdPrestamo(),
                equipos.size() + " equipo(s) para " + quien);
        return p;
    }

    public List<Prestamo> listarActivos(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listarActivos();
    }

    public Optional<Prestamo> buscar(Usuario actor, int idPrestamo) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.buscarPorId(idPrestamo);
    }

    public List<DetallePrestamo> detalles(Usuario actor, int idPrestamo) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.detalles(idPrestamo);
    }
}