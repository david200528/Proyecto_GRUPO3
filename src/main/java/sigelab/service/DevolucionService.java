package sigelab.service;

import sigelab.enums.AccionAuditoria;
import sigelab.enums.CondicionDevolucion;
import sigelab.enums.EstadoPrestamo;
import sigelab.enums.RolUsuario;
import sigelab.model.DetallePrestamo;
import sigelab.model.Devolucion;
import sigelab.model.Prestamo;
import sigelab.model.Usuario;
import sigelab.repository.PrestamoRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.Validacion;

public class DevolucionService {
    private final PrestamoRepository repository = new PrestamoRepository();
    private final AuditoriaService auditoria = new AuditoriaService();

    /**
     * Devuelve un equipo de un préstamo activo. Permite devoluciones parciales (RN-16): el préstamo
     * pasa a FINALIZADO solo cuando ya no queda ningún equipo pendiente.
     */
    public boolean devolver(Usuario actor, int idPrestamo, int idDetalle, CondicionDevolucion condicion,
                            String observaciones) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        Validacion.requerido(condicion, "Seleccione la condición del equipo (bueno o dañado).");
        Prestamo prestamo = repository.buscarPorId(idPrestamo)
                .orElseThrow(() -> new BusinessException("No se encontró el préstamo " + idPrestamo + "."));
        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {                         // RN-10
            throw new BusinessException("El préstamo " + idPrestamo + " ya está finalizado.");
        }
        DetallePrestamo detalle = prestamo.getDetalles().stream()
                .filter(d -> d.getIdDetalle() == idDetalle).findFirst()
                .orElseThrow(() -> new BusinessException("Ese equipo no pertenece al préstamo seleccionado."));
        if (detalle.isDevuelto()) {                                                  // RN-11
            throw new BusinessException("El equipo " + detalle.getEquipo().getCodigoInventario()
                    + " ya fue devuelto.");
        }
        Devolucion d = new Devolucion();
        d.setIdDetalle(idDetalle);
        d.setCondicion(condicion);
        d.setObservaciones(Validacion.opcional(observaciones, "observaciones", 255));
        d.setIdUsuarioRegistra(actor.getIdUsuario());
        boolean finalizado = repository.devolver(d, idPrestamo, detalle.getEquipo().getIdEquipo());   // RN-12, RN-13
        auditoria.registrar(actor, AccionAuditoria.EQUIPO_DEVUELTO, "DetallePrestamo", idDetalle,
                "Equipo " + detalle.getEquipo().getCodigoInventario() + " devuelto " + condicion
                        + " (préstamo " + idPrestamo + ")");
        if (finalizado) {
            auditoria.registrar(actor, AccionAuditoria.PRESTAMO_FINALIZADO, "Prestamo", idPrestamo,
                    "Todos los equipos fueron devueltos");
        }
        return finalizado;
    }
}