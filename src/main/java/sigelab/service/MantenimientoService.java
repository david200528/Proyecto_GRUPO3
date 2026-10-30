package sigelab.service;

import sigelab.config.DatabaseConnection;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.exception.BusinessException;
import sigelab.exception.DataAccessException;
import sigelab.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MantenimientoService {
    private final MantenimientoRepository mantRepo = new MantenimientoRepository();
    private final EquipoEstadoRepository equipoRepo = new EquipoEstadoRepository();

    public void enviarAMantenimiento(int idEquipo, TipoMantenimiento tipo, String descripcion,
                                     String responsable, String obs, int idUsuario, String nombreUsuario) {
        if (tipo == null) throw new BusinessException("Seleccione el tipo de mantenimiento.");
        if (descripcion == null || descripcion.isBlank()) throw new BusinessException("Ingrese la descripción.");
        if (responsable == null || responsable.isBlank()) throw new BusinessException("Ingrese el responsable.");

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            String estado = equipoRepo.obtenerEstado(con, idEquipo);
            if (estado == null) throw new BusinessException("El equipo no existe.");
            boolean danado = estado.equals(ValoresBd.EQUIPO_DANADO);
            boolean disponible = estado.equals(ValoresBd.EQUIPO_DISPONIBLE);

            if (!danado && !disponible)
                throw new BusinessException("Solo un equipo DAÑADO o DISPONIBLE puede enviarse a mantenimiento. Estado actual: " + estado);
            if (tipo == TipoMantenimiento.CORRECTIVO && disponible)
                throw new BusinessException("El mantenimiento correctivo es para equipos DAÑADOS. Use PREVENTIVO.");
            if (mantRepo.tieneAbierto(con, idEquipo))
                throw new BusinessException("El equipo ya tiene un mantenimiento en proceso.");

            int id = mantRepo.insertar(con, idEquipo, tipo.name(), descripcion.trim(), responsable.trim(), obs);
            equipoRepo.actualizarEstado(con, idEquipo, "MANTENIMIENTO");
            BitacoraTx.registrar(con, idUsuario, nombreUsuario, "EQUIPO_ENVIADO_MANTENIMIENTO",
                    "Equipo " + idEquipo + " a mantenimiento " + tipo + " (id " + id + ")");
            con.commit();
        } catch (BusinessException e) {
            rollback(con); throw e;
        } catch (SQLException e) {
            rollback(con); throw new DataAccessException("No se pudo enviar el equipo a mantenimiento.", e);
        } finally { cerrar(con); }
    }

    public void finalizarMantenimiento(int idMantenimiento, ResultadoMantenimiento resultado, String obs,
                                       int idUsuario, String nombreUsuario) {
        if (resultado == null) throw new BusinessException("Seleccione el resultado del mantenimiento.");

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            Object[] m = mantRepo.buscar(con, idMantenimiento);
            if (m == null) throw new BusinessException("El mantenimiento no existe.");
            if (!"EN_PROCESO".equals(m[1])) throw new BusinessException("Este mantenimiento ya fue finalizado.");
            int idEquipo = (int) m[0];


            mantRepo.finalizar(con, idMantenimiento, resultado.name(), obs);
            String nuevo = (resultado == ResultadoMantenimiento.REPARADO) ? ValoresBd.EQUIPO_DISPONIBLE : "BAJA";   // RN-15
            equipoRepo.actualizarEstado(con, idEquipo, nuevo);
            BitacoraTx.registrar(con, idUsuario, nombreUsuario, "MANTENIMIENTO_FINALIZADO",
                    "Equipo " + idEquipo + " quedó " + nuevo + " (mantenimiento " + idMantenimiento + ")");
            con.commit();
        } catch (BusinessException e) {
            rollback(con); throw e;
        } catch (SQLException e) {
            rollback(con); throw new DataAccessException("No se pudo finalizar el mantenimiento.", e);
        } finally { cerrar(con); }
    }

    public List<Object[]> listar(boolean soloAbiertos) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return mantRepo.listar(con, soloAbiertos);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el mantenimiento.", e);
        }
    }

    private void rollback(Connection c) { try { if (c != null) c.rollback(); } catch (SQLException ignored) {} }
    private void cerrar(Connection c) { try { if (c != null) { c.setAutoCommit(true); c.close(); } } catch (SQLException ignored) {} }
}