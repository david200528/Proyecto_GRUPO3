package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.model.Usuario;
import sigelab.model.Docente;
import sigelab.enums.RolUsuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

    public boolean existeUsuario(String usuario) {
        String sql = "SELECT COUNT(*) FROM dbo.Usuario WHERE usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existeUsuario(String usuario, int idUsuarioToExclude) {
        String sql = "SELECT COUNT(*) FROM dbo.Usuario WHERE usuario = ? AND id_usuario <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.setInt(2, idUsuarioToExclude);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean guardar(Usuario usuario) {
        String sql = "INSERT INTO dbo.Usuario (usuario, password_hash, rol, activo, id_docente) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getUsuario());
            stmt.setString(2, usuario.getPasswordHash());
            stmt.setString(3, usuario.getRol().name());
            stmt.setBoolean(4, usuario.isActivo());
            if (usuario.getDocente() != null && usuario.getDocente().getIdDocente() > 0) {
                stmt.setInt(5, usuario.getDocente().getIdDocente());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            int filasAfectadas = stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setIdUsuario(generatedKeys.getInt(1));
                }
            }
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Usuario> buscarPorId(int id) {
        String sql = "SELECT id_usuario, usuario, password_hash, rol, activo, id_docente FROM dbo.Usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRol(RolUsuario.valueOf(rs.getString("rol")));
                    u.setActivo(rs.getBoolean("activo"));
                    int idDocente = rs.getInt("id_docente");
                    if (!rs.wasNull() && idDocente > 0) {
                        Docente d = new Docente();
                        d.setIdDocente(idDocente);
                        u.setDocente(d);
                    }
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Usuario> buscarPorUsuario(String nombreUsuario) {
        String sql = "SELECT id_usuario, usuario, password_hash, rol, activo, id_docente FROM dbo.Usuario WHERE usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombreUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRol(RolUsuario.valueOf(rs.getString("rol")));
                    u.setActivo(rs.getBoolean("activo"));
                    int idDocente = rs.getInt("id_docente");
                    if (!rs.wasNull() && idDocente > 0) {
                        Docente d = new Docente();
                        d.setIdDocente(idDocente);
                        u.setDocente(d);
                    }
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, usuario, password_hash, rol, activo, id_docente FROM dbo.Usuario";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setUsuario(rs.getString("usuario"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRol(RolUsuario.valueOf(rs.getString("rol")));
                u.setActivo(rs.getBoolean("activo"));
                int idDocente = rs.getInt("id_docente");
                if (!rs.wasNull() && idDocente > 0) {
                    Docente d = new Docente();
                    d.setIdDocente(idDocente);
                    u.setDocente(d);
                }
                lista.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE dbo.Usuario SET password_hash = ?, rol = ?, activo = ?, id_docente = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getPasswordHash());
            stmt.setString(2, usuario.getRol().name());
            stmt.setBoolean(3, usuario.isActivo());
            if (usuario.getDocente() != null && usuario.getDocente().getIdDocente() > 0) {
                stmt.setInt(4, usuario.getDocente().getIdDocente());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setInt(5, usuario.getIdUsuario());
            int filas = stmt.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean desactivar(int idUsuario) {
        String sql = "UPDATE dbo.Usuario SET activo = 0 WHERE id_usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            int filas = stmt.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}