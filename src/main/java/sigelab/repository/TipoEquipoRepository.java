package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.model.TipoEquipo;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipoEquipoRepository {

    public void guardar(TipoEquipo t) {
        String sql = "INSERT INTO dbo.TipoEquipo (nombre, descripcion) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            JdbcSupport.asignar(stmt, t.getNombre(), t.getDescripcion());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) t.setIdTipoEquipo(keys.getInt(1));
            }
        } catch (SQLException e) {
            if (JdbcSupport.esDuplicado(e)) {
                throw new BusinessException("Ya existe un tipo de equipo llamado " + t.getNombre() + ".");
            }
            throw JdbcSupport.error("guardar el tipo de equipo", e);
        }
    }

    public boolean actualizar(TipoEquipo t) {
        String sql = "UPDATE dbo.TipoEquipo SET nombre = ?, descripcion = ? WHERE id_tipo_equipo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, t.getNombre(), t.getDescripcion(), t.getIdTipoEquipo());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (JdbcSupport.esDuplicado(e)) {
                throw new BusinessException("Ya existe un tipo de equipo llamado " + t.getNombre() + ".");
            }
            throw JdbcSupport.error("actualizar el tipo de equipo", e);
        }
    }

    public List<TipoEquipo> listar() {
        List<TipoEquipo> lista = new ArrayList<>();
        String sql = "SELECT id_tipo_equipo, nombre, descripcion FROM dbo.TipoEquipo ORDER BY nombre";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TipoEquipo t = new TipoEquipo();
                t.setIdTipoEquipo(rs.getInt("id_tipo_equipo"));
                t.setNombre(rs.getString("nombre"));
                t.setDescripcion(rs.getString("descripcion"));
                lista.add(t);
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los tipos de equipo", e);
        }
        return lista;
    }
}