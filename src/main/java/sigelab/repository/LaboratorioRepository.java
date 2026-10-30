package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.model.Laboratorio;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LaboratorioRepository {
    private static final String SELECT = "SELECT id_laboratorio, codigo, nombre, ubicacion, capacidad, activo "
            + "FROM dbo.Laboratorio ";

    public void guardar(Laboratorio l) {
        String sql = "INSERT INTO dbo.Laboratorio (codigo, nombre, ubicacion, capacidad, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            JdbcSupport.asignar(stmt, l.getCodigo(), l.getNombre(), l.getUbicacion(), l.getCapacidad(), l.isActivo());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) l.setIdLaboratorio(keys.getInt(1));
            }
        } catch (SQLException e) {
            if (JdbcSupport.esDuplicado(e)) {
                throw new BusinessException("Ya existe un laboratorio con el código " + l.getCodigo() + ".");
            }
            throw JdbcSupport.error("guardar el laboratorio", e);
        }
    }

    public boolean actualizar(Laboratorio l) {
        String sql = "UPDATE dbo.Laboratorio SET nombre = ?, ubicacion = ?, capacidad = ?, activo = ? "
                + "WHERE id_laboratorio = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, l.getNombre(), l.getUbicacion(), l.getCapacidad(), l.isActivo(),
                    l.getIdLaboratorio());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("actualizar el laboratorio", e);
        }
    }

    public Optional<Laboratorio> buscarPorId(int id) {
        List<Laboratorio> lista = consultar(SELECT + "WHERE id_laboratorio = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public List<Laboratorio> listar() {
        return consultar(SELECT + "ORDER BY codigo");
    }

    public List<Laboratorio> listarActivos() {
        return consultar(SELECT + "WHERE activo = 1 ORDER BY codigo");
    }

    private List<Laboratorio> consultar(String sql, Object... params) {
        List<Laboratorio> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Laboratorio l = new Laboratorio();
                    l.setIdLaboratorio(rs.getInt("id_laboratorio"));
                    l.setCodigo(rs.getString("codigo"));
                    l.setNombre(rs.getString("nombre"));
                    l.setUbicacion(rs.getString("ubicacion"));
                    l.setCapacidad(rs.getInt("capacidad"));
                    l.setActivo(rs.getBoolean("activo"));
                    lista.add(l);
                }
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los laboratorios", e);
        }
        return lista;
    }
}