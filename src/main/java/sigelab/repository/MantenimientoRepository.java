package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.enums.*;
import sigelab.model.Equipo;
import sigelab.model.Mantenimiento;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MantenimientoRepository {
    private static final String SELECT = "SELECT m.id_mantenimiento, m.id_equipo, e.codigo_inventario, e.nombre, "
            + "m.fecha_ingreso, m.tipo_mantenimiento, m.descripcion, m.responsable, m.fecha_salida, m.resultado, "
            + "m.estado, m.observaciones FROM dbo.Mantenimiento m JOIN dbo.Equipo e ON e.id_equipo = m.id_equipo ";

    /** Equipo DANADO o DISPONIBLE -> MANTENIMIENTO y alta del registro, en una transacción. */
    public void enviar(Mantenimiento m, EstadoEquipo estadoActual) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE dbo.Equipo SET estado = 'MANTENIMIENTO' WHERE id_equipo = ? AND estado = ?")) {
                JdbcSupport.asignar(stmt, m.getEquipo().getIdEquipo(), estadoActual);
                if (stmt.executeUpdate() != 1) {
                    throw new BusinessException("El estado del equipo cambió. Actualice la lista e inténtelo de nuevo.");
                }
            }
            m.setFechaIngreso(LocalDateTime.now());
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO dbo.Mantenimiento (id_equipo, fecha_ingreso, tipo_mantenimiento, descripcion, "
                            + "responsable, estado, observaciones) VALUES (?, ?, ?, ?, ?, 'EN_PROCESO', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                JdbcSupport.asignar(stmt, m.getEquipo().getIdEquipo(), m.getFechaIngreso(), m.getTipo(),
                        m.getDescripcion(), m.getResponsable(), m.getObservaciones());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) m.setIdMantenimiento(keys.getInt(1));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            JdbcSupport.rollback(conn);
            throw JdbcSupport.error("enviar el equipo a mantenimiento", e);
        } catch (RuntimeException e) {
            JdbcSupport.rollback(conn);
            throw e;
        } finally {
            cerrar(conn);
        }
    }

    /** RN-15: REPARADO -> DISPONIBLE, NO_REPARABLE -> BAJA. */
    public void finalizar(int idMantenimiento, int idEquipo, ResultadoMantenimiento resultado, String observaciones) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE dbo.Mantenimiento SET estado = 'FINALIZADO', resultado = ?, fecha_salida = ?, "
                            + "observaciones = COALESCE(?, observaciones) "
                            + "WHERE id_mantenimiento = ? AND estado = 'EN_PROCESO'")) {
                JdbcSupport.asignar(stmt, resultado, LocalDateTime.now(), observaciones, idMantenimiento);
                if (stmt.executeUpdate() != 1) throw new BusinessException("Ese mantenimiento ya estaba finalizado.");
            }
            EstadoEquipo nuevo = resultado == ResultadoMantenimiento.REPARADO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.BAJA;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE dbo.Equipo SET estado = ? WHERE id_equipo = ? AND estado = 'MANTENIMIENTO'")) {
                JdbcSupport.asignar(stmt, nuevo, idEquipo);
                if (stmt.executeUpdate() != 1) {
                    throw new BusinessException("El equipo no está en mantenimiento.");
                }
            }
            conn.commit();
        } catch (SQLException e) {
            JdbcSupport.rollback(conn);
            throw JdbcSupport.error("finalizar el mantenimiento", e);
        } catch (RuntimeException e) {
            JdbcSupport.rollback(conn);
            throw e;
        } finally {
            cerrar(conn);
        }
    }

    public Optional<Mantenimiento> buscarPorId(int id) {
        List<Mantenimiento> l = consultar(SELECT + "WHERE m.id_mantenimiento = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Mantenimiento> listar(EstadoMantenimiento estado) {
        return estado == null
                ? consultar(SELECT + "ORDER BY m.fecha_ingreso DESC")
                : consultar(SELECT + "WHERE m.estado = ? ORDER BY m.fecha_ingreso DESC", estado);
    }

    private List<Mantenimiento> consultar(String sql, Object... params) {
        List<Mantenimiento> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Mantenimiento m = new Mantenimiento();
                    m.setIdMantenimiento(rs.getInt("id_mantenimiento"));
                    Equipo e = new Equipo();
                    e.setIdEquipo(rs.getInt("id_equipo"));
                    e.setCodigoInventario(rs.getString("codigo_inventario"));
                    e.setNombre(rs.getString("nombre"));
                    m.setEquipo(e);
                    m.setFechaIngreso(JdbcSupport.localDateTime(rs, "fecha_ingreso"));
                    m.setTipo(TipoMantenimiento.valueOf(rs.getString("tipo_mantenimiento")));
                    m.setDescripcion(rs.getString("descripcion"));
                    m.setResponsable(rs.getString("responsable"));
                    m.setFechaSalida(JdbcSupport.localDateTime(rs, "fecha_salida"));
                    String res = rs.getString("resultado");
                    m.setResultado(res == null ? null : ResultadoMantenimiento.valueOf(res));
                    m.setEstado(EstadoMantenimiento.valueOf(rs.getString("estado")));
                    m.setObservaciones(rs.getString("observaciones"));
                    lista.add(m);
                }
            }
        } catch (SQLException ex) {
            throw JdbcSupport.error("consultar el mantenimiento", ex);
        }
        return lista;
    }

    private static void cerrar(Connection conn) {
        if (conn == null) return;
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ignored) {
            // la conexión ya no se usa
        }
    }
}