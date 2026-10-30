package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Equipo;
import sigelab.model.Laboratorio;
import sigelab.model.TipoEquipo;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipoRepository {
    static final String SELECT = "SELECT e.id_equipo, e.codigo_inventario, e.nombre, e.marca, e.modelo, "
            + "e.numero_serie, e.estado, e.observaciones, t.id_tipo_equipo, t.nombre AS tipo_nombre, "
            + "l.id_laboratorio, l.codigo AS lab_codigo, l.nombre AS lab_nombre "
            + "FROM dbo.Equipo e JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = e.id_tipo_equipo "
            + "JOIN dbo.Laboratorio l ON l.id_laboratorio = e.id_laboratorio ";

    public void guardar(Equipo e) {
        String sql = "INSERT INTO dbo.Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo, numero_serie, "
                + "id_laboratorio, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            JdbcSupport.asignar(stmt, e.getCodigoInventario(), e.getNombre(), e.getTipo().getIdTipoEquipo(),
                    e.getMarca(), e.getModelo(), e.getNumeroSerie(), e.getLaboratorio().getIdLaboratorio(),
                    e.getEstado(), e.getObservaciones());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) e.setIdEquipo(keys.getInt(1));
            }
        } catch (SQLException ex) {
            if (JdbcSupport.esDuplicado(ex)) {
                throw new BusinessException("Ya existe un equipo con el código " + e.getCodigoInventario() + ".");
            }
            throw JdbcSupport.error("guardar el equipo", ex);
        }
    }

    /** Actualiza datos descriptivos; el estado solo cambia mediante préstamos, devoluciones o mantenimiento. */
    public boolean actualizar(Equipo e) {
        String sql = "UPDATE dbo.Equipo SET nombre = ?, id_tipo_equipo = ?, marca = ?, modelo = ?, numero_serie = ?, "
                + "id_laboratorio = ?, observaciones = ? WHERE id_equipo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, e.getNombre(), e.getTipo().getIdTipoEquipo(), e.getMarca(), e.getModelo(),
                    e.getNumeroSerie(), e.getLaboratorio().getIdLaboratorio(), e.getObservaciones(), e.getIdEquipo());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw JdbcSupport.error("actualizar el equipo", ex);
        }
    }

    /** Cambio de estado atómico: solo se aplica si el equipo sigue en el estado esperado. */
    public boolean cambiarEstado(int idEquipo, EstadoEquipo esperado, EstadoEquipo nuevo) {
        String sql = "UPDATE dbo.Equipo SET estado = ? WHERE id_equipo = ? AND estado = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, nuevo, idEquipo, esperado);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw JdbcSupport.error("cambiar el estado del equipo", ex);
        }
    }

    public Optional<Equipo> buscarPorId(int id) {
        List<Equipo> l = consultar(SELECT + "WHERE e.id_equipo = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Optional<Equipo> buscarPorCodigo(String codigo) {
        List<Equipo> l = consultar(SELECT + "WHERE e.codigo_inventario = ?", codigo);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Equipo> listar(EstadoEquipo estado) {
        return estado == null
                ? consultar(SELECT + "ORDER BY e.codigo_inventario")
                : consultar(SELECT + "WHERE e.estado = ? ORDER BY e.codigo_inventario", estado);
    }

    public List<Equipo> buscar(String texto) {
        String like = "%" + texto.trim() + "%";
        return consultar(SELECT + "WHERE e.codigo_inventario LIKE ? OR e.nombre LIKE ? OR e.marca LIKE ? "
                + "ORDER BY e.codigo_inventario", like, like, like);
    }

    static List<Equipo> consultar(String sql, Object... params) {
        List<Equipo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            throw JdbcSupport.error("consultar los equipos", ex);
        }
        return lista;
    }

    static Equipo mapear(ResultSet rs) throws SQLException {
        Equipo e = new Equipo();
        e.setIdEquipo(rs.getInt("id_equipo"));
        e.setCodigoInventario(rs.getString("codigo_inventario"));
        e.setNombre(rs.getString("nombre"));
        e.setMarca(rs.getString("marca"));
        e.setModelo(rs.getString("modelo"));
        e.setNumeroSerie(rs.getString("numero_serie"));
        e.setEstado(EstadoEquipo.valueOf(rs.getString("estado")));
        e.setObservaciones(rs.getString("observaciones"));
        TipoEquipo t = new TipoEquipo();
        t.setIdTipoEquipo(rs.getInt("id_tipo_equipo"));
        t.setNombre(rs.getString("tipo_nombre"));
        e.setTipo(t);
        Laboratorio l = new Laboratorio();
        l.setIdLaboratorio(rs.getInt("id_laboratorio"));
        l.setCodigo(rs.getString("lab_codigo"));
        l.setNombre(rs.getString("lab_nombre"));
        e.setLaboratorio(l);
        return e;
    }
}