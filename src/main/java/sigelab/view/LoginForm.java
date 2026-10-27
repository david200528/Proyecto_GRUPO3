package sigelab.view;

import sigelab.controller.UsuarioController;
import sigelab.model.Usuario;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.Optional;

/** Pantalla de entrada al sistema. */
public class LoginForm {

    private JPanel panelPrincipal;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnSalir;

    private final UsuarioController usuarioController;
    private boolean autenticando;

    public LoginForm() {
        this(new UsuarioController());
    }

    public LoginForm(UsuarioController usuarioController) {
        this.usuarioController = usuarioController;
        configurarEventos();
    }

    private void configurarEventos() {
        btnIngresar.addActionListener(e -> ingresar());
        btnSalir.addActionListener(e -> System.exit(0));
        txtPassword.addActionListener(e -> ingresar());
    }

    private void ingresar() {
        if (autenticando) return;
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuario.isBlank()) {
            mostrarError("Ingrese el usuario.");
            txtUsuario.requestFocus();
            return;
        }
        if (password.isBlank()) {
            mostrarError("Ingrese la contraseña.");
            txtPassword.requestFocus();
            return;
        }

        autenticando = true;
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");

        new SwingWorker<Optional<Usuario>, Void>() {
            @Override
            protected Optional<Usuario> doInBackground() {
                return usuarioController.autenticar(usuario, password);
            }

            @Override
            protected void done() {
                try {
                    Optional<Usuario> usuarioAutenticado = get();
                    if (usuarioAutenticado.isEmpty()) {
                        mostrarError("Usuario o contraseña incorrectos, o usuario inactivo.");
                        txtPassword.selectAll();
                        txtPassword.requestFocus();
                    } else {
                        abrirMenu(usuarioAutenticado.get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("El proceso fue interrumpido.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException runtimeError) {
                        mostrarError(runtimeError.getMessage());
                    } else {
                        mostrarError("No fue posible completar la autenticación.");
                    }
                } finally {
                    autenticando = false;
                    btnIngresar.setEnabled(true);
                    btnIngresar.setText("INGRESAR");
                }
            }
        }.execute();
    }

    private void abrirMenu(Usuario usuario) {
        java.awt.Window ventanaActual = SwingUtilities.getWindowAncestor(panelPrincipal);
        if (ventanaActual != null) ventanaActual.dispose();

        MainForm mainForm = new MainForm(usuario);
        JFrame ventana = new JFrame("Sistema de Asistencia Universitaria");
        ventana.setContentPane(mainForm.getPanelPrincipal());
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(750, 650);
        ventana.setLocationRelativeTo(null);
        ventana.setResizable(false);
        ventana.setVisible(true);
    }

    private void mostrarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) mensaje = "Ocurrió un error inesperado.";
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Inicio de sesión", JOptionPane.ERROR_MESSAGE);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}