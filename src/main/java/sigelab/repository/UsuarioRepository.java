package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.enums.RolUsuario;
import sigelab.model.Docente;
import sigelab.model.Usuario;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    private static final String SELECT = "SELECT u.id_usuario, u.nombre, u.apellido, u.username, u.password_hash, u.correo, u.id_rol, u.estado, " +
            "CAST(NULL AS INT) AS id_docente, CAST(NULL AS VARCHAR(50)) AS codigo_empleado, CAST(NULL AS VARCHAR(100)) AS docente_nombres, " +
            "CAST(NULL AS VARCHAR(100)) AS docente_apellidos, CAST(NULL AS VARCHAR(100)) AS docente_correo, CAST(NULL AS BIT) AS docente_activo " +
            "FROM Usuario u ";

    public boolean guardar(Usuario usuario) {
        String sql = "INSERT INTO Usuario (nombre, apellido, username, password_hash, correo, id_rol, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, "Admin");
            s.setString(2, "Sistema");
            s.setString(3, usuario.getUsuario());
            s.setString(4, usuario.getPasswordHash());
            s.setString(5, usuario.getUsuario() + "@umg.edu.gt");

            int idRol = 3;
            String rolName = usuario.getRol() != null ? usuario.getRol().name().toUpperCase() : "";
            if (rolName.contains("ADMIN")) {
                idRol = 1;
            } else if (rolName.contains("ENCARGADO")) {
                idRol = 2;
            }
            s.setInt(6, idRol);
            s.setString(7, usuario.isActivo() ? "ACTIVO" : "INACTIVO");

            if (s.executeUpdate() == 0) return false;
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) return false; usuario.setIdUsuario(keys.getInt(1)); return true; }
        } catch (SQLException e) { throw JdbcSupport.error("guardar el usuario", e); }
    }

    public Optional<Usuario> buscarPorId(int id) { return buscar(SELECT + "WHERE u.id_usuario = ?", id); }

    public Optional<Usuario> buscarPorUsuario(String nombre) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(SELECT + "WHERE u.username = ?")) {
            s.setString(1, nombre);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
        catch (SQLException e) { throw JdbcSupport.error("buscar el usuario", e); }
    }

    public List<Usuario> listar() {
        List<Usuario> r = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(SELECT + "ORDER BY u.username"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) r.add(mapear(rs));
            return r;
        } catch (SQLException e) { throw JdbcSupport.error("consultar usuarios", e); }
    }

    public boolean existeUsuario(String nombre) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT 1 FROM Usuario WHERE username = ?")) {
            s.setString(1, nombre);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw JdbcSupport.error("verificar el usuario", e); }
    }

    public boolean existeUsuario(String nombre, int idExcluido) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT 1 FROM Usuario WHERE username = ? AND id_usuario <> ?")) {
            s.setString(1, nombre);
            s.setInt(2, idExcluido);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw JdbcSupport.error("verificar el usuario", e); }
    }

    public boolean actualizar(Usuario u) {
        String sql = "UPDATE Usuario SET username = ?, id_rol = ?, estado = ? WHERE id_usuario = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, u.getUsuario());

            int idRol = 3;
            String rolName = u.getRol() != null ? u.getRol().name().toUpperCase() : "";
            if (rolName.contains("ADMIN")) {
                idRol = 1;
            } else if (rolName.contains("ENCARGADO")) {
                idRol = 2;
            }
            s.setInt(2, idRol);
            s.setString(3, u.isActivo() ? "ACTIVO" : "INACTIVO");
            s.setInt(4, u.getIdUsuario());
            return s.executeUpdate() > 0;
        }
        catch (SQLException e) { throw JdbcSupport.error("actualizar el usuario", e); }
    }

    public boolean desactivar(int id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = ?")) {
            s.setInt(1, id);
            return s.executeUpdate() > 0;
        } catch (SQLException e) { throw JdbcSupport.error("desactivar el usuario", e); }
    }

    public int contar() {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM Usuario"); ResultSet rs = s.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) { throw JdbcSupport.error("contar usuarios", e); }
    }

    private Optional<Usuario> buscar(String sql, int id) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw JdbcSupport.error("buscar el usuario", e); }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Docente docente = null;
        if (rs.getObject("id_docente") != null) {
            docente = new Docente(rs.getInt("id_docente"), rs.getString("codigo_empleado"), rs.getString("docente_nombres"), rs.getString("docente_apellidos"), rs.getString("docente_correo"), rs.getBoolean("docente_activo"));
        }

        int idRol = rs.getInt("id_rol");
        RolUsuario rol;
        if (idRol == 1) {
            rol = RolUsuario.valueOf("ADMIN");
        } else if (idRol == 2) {
            try {
                rol = RolUsuario.valueOf("ENCARGADO");
            } catch (Exception e) {
                rol = RolUsuario.values()[1];
            }
        } else {
            rol = RolUsuario.valueOf("DOCENTE");
        }

        boolean activo = "ACTIVO".equalsIgnoreCase(rs.getString("estado"));

        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rol,
                docente,
                activo,
                LocalDateTime.now()
        );
    }
}