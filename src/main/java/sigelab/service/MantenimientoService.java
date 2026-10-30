package sigelab.service;

import sigelab.enums.*;
import sigelab.model.Equipo;
import sigelab.model.Mantenimiento;
import sigelab.model.Usuario;
import sigelab.repository.EquipoRepository;
import sigelab.repository.MantenimientoRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

import java.util.List;

public class MantenimientoService {
    private final MantenimientoRepository repository = new MantenimientoRepository();
    private final EquipoRepository equipoRepository = new EquipoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    /** DANADO o DISPONIBLE (preventivo) -> MANTENIMIENTO. Un equipo PRESTADO o de BAJA no entra. */
    public Mantenimiento enviar(Usuario actor, Equipo equipo, TipoMantenimiento tipo, String descripcion,
                                String responsable, String observaciones) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Validacion.requerido(equipo, "Seleccione el equipo.");
        Validacion.requerido(tipo, "Seleccione el tipo de mantenimiento.");
        Equipo actual = equipoRepository.buscarPorId(equipo.getIdEquipo())
                .orElseThrow(() -> new BusinessException("El equipo ya no existe."));
        if (actual.getEstado() != EstadoEquipo.DANADO && actual.getEstado() != EstadoEquipo.DISPONIBLE) {
            throw new BusinessException("El equipo " + actual.getCodigoInventario() + " está en estado "
                    + actual.getEstado().name().toLowerCase() + " y no puede enviarse a mantenimiento.");
        }
        if (tipo == TipoMantenimiento.PREVENTIVO && actual.getEstado() == EstadoEquipo.DANADO) {
            // un equipo dañado requiere reparación: se registra como correctivo
            tipo = TipoMantenimiento.CORRECTIVO;
        }
        Mantenimiento m = new Mantenimiento();
        m.setEquipo(actual);
        m.setTipo(tipo);
        m.setDescripcion(Validacion.obligatorio(descripcion, "descripción", 255));
        m.setResponsable(Validacion.obligatorio(responsable, "responsable", 100));
        m.setObservaciones(Validacion.opcional(observaciones, "observaciones", 255));
        repository.enviar(m, actual.getEstado());
        auditoria.registrar(actor, AccionAuditoria.EQUIPO_ENVIADO_MANTENIMIENTO, "Mantenimiento",
                m.getIdMantenimiento(), "Equipo " + actual.getCodigoInventario() + " (" + tipo + ")");
        return m;
    }

    /** RN-15: REPARADO -> DISPONIBLE; NO_REPARABLE -> BAJA. */
    public void finalizar(Usuario actor, int idMantenimiento, ResultadoMantenimiento resultado, String observaciones) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Validacion.requerido(resultado, "Seleccione el resultado del mantenimiento.");
        Mantenimiento m = repository.buscarPorId(idMantenimiento)
                .orElseThrow(() -> new BusinessException("No se encontró el mantenimiento."));
        if (m.getEstado() != EstadoMantenimiento.EN_PROCESO) {
            throw new BusinessException("Ese mantenimiento ya está finalizado.");
        }
        repository.finalizar(idMantenimiento, m.getEquipo().getIdEquipo(), resultado,
                Validacion.opcional(observaciones, "observaciones", 255));
        auditoria.registrar(actor, AccionAuditoria.MANTENIMIENTO_FINALIZADO, "Mantenimiento", idMantenimiento,
                "Equipo " + m.getEquipo().getCodigoInventario() + ": " + resultado);
    }

    public List<Mantenimiento> listar(Usuario actor, EstadoMantenimiento estado) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        return repository.listar(estado);
    }
}