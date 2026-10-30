package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    private static final String SELECT = "SELECT id_usuario, nombre, apellido, usuario, password_hash, correo, rol, "
            + "activo, fecha_creacion FROM dbo.Usuario ";

    public boolean existeUsuario(String usuario) {
        return buscarPorUsuario(usuario).isPresent();
    }

    public void guardar(Usuario u) {
        String sql = "INSERT INTO dbo.Usuario (nombre, apellido, usuario, password_hash, correo, rol, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            JdbcSupport.asignar(stmt, u.getNombre(), u.getApellido(), u.getUsuario(), u.getPasswordHash(),
                    u.getCorreo(), u.getRol(), u.isActivo());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) u.setIdUsuario(keys.getInt(1));
            }
        } catch (SQLException e) {
            if (JdbcSupport.esDuplicado(e)) {
                throw new BusinessException("Ya existe un usuario con el nombre de usuario o el correo indicado.");
            }
            throw JdbcSupport.error("guardar el usuario", e);
        }
    }

    public Optional<Usuario> buscarPorId(int id) {
        return uno(SELECT + "WHERE id_usuario = ?", id);
    }

    public Optional<Usuario> buscarPorUsuario(String nombreUsuario) {
        return uno(SELECT + "WHERE usuario = ?", nombreUsuario);
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT + "ORDER BY usuario")) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los usuarios", e);
        }
        return lista;
    }

    public boolean actualizar(Usuario u) {
        String sql = "UPDATE dbo.Usuario SET nombre = ?, apellido = ?, correo = ?, password_hash = ?, rol = ?, "
                + "activo = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, u.getNombre(), u.getApellido(), u.getCorreo(), u.getPasswordHash(),
                    u.getRol(), u.isActivo(), u.getIdUsuario());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (JdbcSupport.esDuplicado(e)) throw new BusinessException("Ya existe otro usuario con ese correo.");
            throw JdbcSupport.error("actualizar el usuario", e);
        }
    }

    public boolean desactivar(int idUsuario) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE dbo.Usuario SET activo = 0 WHERE id_usuario = ?")) {
            stmt.setInt(1, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("desactivar el usuario", e);
        }
    }

    private Optional<Usuario> uno(String sql, Object param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, param);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar el usuario", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setUsuario(rs.getString("usuario"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setCorreo(rs.getString("correo"));
        u.setRol(RolUsuario.valueOf(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        u.setFechaCreacion(JdbcSupport.localDateTime(rs, "fecha_creacion"));
        return u;
    }
}