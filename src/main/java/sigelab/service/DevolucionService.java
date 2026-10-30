package sigelab.service;

import sigelab.config.DatabaseConnection;
import sigelab.enums.CondicionDevolucion;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DevolucionService {
    private final DevolucionRepository devRepo = new DevolucionRepository();
    private final EquipoEstadoRepository equipoRepo = new EquipoEstadoRepository();

    public void devolverEquipo(int idDetalle, CondicionDevolucion condicion, String obs,
                               int idUsuario, String nombreUsuario) {
        if (condicion == null)
            throw new BusinessException("Seleccione la condición en que regresa el equipo.");

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            var det = devRepo.buscarDetalle(con, idDetalle);
            if (det == null || ValoresBd.PRESTAMO_FINALIZADO.equalsIgnoreCase(det.estadoPrestamo))
                throw new BusinessException("El equipo no está pendiente en un préstamo activo.");   // RN-10
            if (devRepo.existeDevolucion(con, idDetalle))
                throw new BusinessException("Este equipo ya fue devuelto.");                          // RN-11


            boolean bueno = (condicion == CondicionDevolucion.BUENO);
            String condicionBd = bueno ? ValoresBd.COND_BUENO : ValoresBd.COND_DANADO;
            String detalleBd   = bueno ? ValoresBd.DETALLE_BUENO : ValoresBd.DETALLE_DANADO;
            String nuevoEstado = bueno ? ValoresBd.EQUIPO_DISPONIBLE : ValoresBd.EQUIPO_DANADO;       // RN-12 / RN-13

            devRepo.insertar(con, idDetalle, condicionBd, obs, idUsuario);
            devRepo.marcarDetalleDevuelto(con, idDetalle, detalleBd);
            equipoRepo.actualizarEstado(con, det.idEquipo, nuevoEstado);

            if (devRepo.contarPendientes(con, det.idPrestamo) == 0)                                   // RN-16
                devRepo.finalizarPrestamo(con, det.idPrestamo);

            BitacoraTx.registrar(con, idUsuario, nombreUsuario, "EQUIPO_DEVUELTO",                    // RN-18
                    "Equipo " + det.idEquipo + " devuelto como " + condicion + " (préstamo " + det.idPrestamo + ")");
            con.commit();
        } catch (BusinessException e) {
            rollback(con);
            throw e;
        } catch (SQLException e) {
            rollback(con);
            throw new DataAccessException("No se pudo registrar la devolución.", e);
        } finally {
            cerrar(con);
        }
    }

    public List<Object[]> pendientes(int idPrestamo) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return devRepo.listarPendientes(con, idPrestamo);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los equipos pendientes.", e);
        }

    }

    private void rollback(Connection c) { try { if (c != null) c.rollback(); } catch (SQLException ignored) {} }
    private void cerrar(Connection c) { try { if (c != null) { c.setAutoCommit(true); c.close(); } } catch (SQLException ignored) {} }
}