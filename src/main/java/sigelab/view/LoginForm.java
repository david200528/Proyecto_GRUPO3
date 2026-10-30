package sigelab.view;

import sigelab.controller.AuditoriaController;
import sigelab.controller.UsuarioController;
import sigelab.integration.facial.FacialAuthClient;
import sigelab.integration.facial.FacialVerificationResult;
import sigelab.model.Usuario;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Pantalla de entrada al sistema: usuario y contraseña, y después validación facial. */
public class LoginForm {

    private final JPanel panelPrincipal = new JPanel(new java.awt.BorderLayout(8, 8));
    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnIngresar = new JButton("INGRESAR");
    private final JButton btnSalir = new JButton("Salir");

    private final UsuarioController usuarioController;
    private final FacialAuthClient facialAuthClient;
    private final AuditoriaController auditoria = new AuditoriaController();
    private boolean autenticando;

    /** Resultado del proceso completo de login: el usuario aceptado o el motivo del rechazo. */
    private record ResultadoLogin(Usuario usuario, String motivoRechazo) {
        static ResultadoLogin aceptado(Usuario usuario) {
            return new ResultadoLogin(usuario, null);
        }

        static ResultadoLogin rechazado(String motivo) {
            return new ResultadoLogin(null, motivo);
        }

        boolean esAceptado() {
            return usuario != null;
        }
    }

    public LoginForm() {
        this(new UsuarioController(), new FacialAuthClient());
    }

    public LoginForm(UsuarioController usuarioController) {
        this(usuarioController, new FacialAuthClient());
    }

    public LoginForm(UsuarioController usuarioController, FacialAuthClient facialAuthClient) {
        this.usuarioController = usuarioController;
        this.facialAuthClient = facialAuthClient;
        construirPantalla();
        configurarEventos();
    }

    private void construirPantalla() {
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel titulo = new JLabel("SIGELAB", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        JLabel subtitulo = new JLabel("Sistema de Gestión de Laboratorios - Inicio de sesión", SwingConstants.CENTER);
        JPanel encabezado = new JPanel(new java.awt.GridLayout(2, 1));
        encabezado.add(titulo);
        encabezado.add(subtitulo);
        panelPrincipal.add(encabezado, java.awt.BorderLayout.NORTH);

        JPanel formulario = new JPanel(new java.awt.GridBagLayout());
        Ui.campo(formulario, 0, "Usuario", txtUsuario);
        Ui.campo(formulario, 1, "Contraseña", txtPassword);
        panelPrincipal.add(formulario, java.awt.BorderLayout.CENTER);

        JPanel botones = new JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        botones.add(btnIngresar);
        botones.add(btnSalir);
        panelPrincipal.add(botones, java.awt.BorderLayout.SOUTH);
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

        new SwingWorker<ResultadoLogin, String>() {
            @Override
            protected ResultadoLogin doInBackground() {
                // Factor 1: usuario, contraseña y usuario activo (RN-01)
                Optional<Usuario> autenticado = usuarioController.autenticar(usuario, password);
                if (autenticado.isEmpty()) {
                    auditoria.loginFallido(usuario, "Credenciales incorrectas o usuario inactivo");
                    return ResultadoLogin.rechazado("Usuario o contraseña incorrectos, o usuario inactivo.");
                }
                Usuario encontrado = autenticado.get();

                // Factor 2: rostro (RN-02). Cualquier fallo del servicio facial deniega el acceso.
                publish("Mire a la cámara...");
                try {
                    FacialVerificationResult resultado = facialAuthClient.verify(encontrado.getIdUsuario());
                    if (resultado.success() && resultado.matched()) {
                        auditoria.loginExitoso(encontrado);
                        return ResultadoLogin.aceptado(encontrado);
                    }
                    auditoria.loginFallido(usuario, "Validación facial no aprobada");
                    String motivo = resultado.message() == null || resultado.message().isBlank()
                            ? "El rostro no corresponde al usuario."
                            : resultado.message();
                    return ResultadoLogin.rechazado("Validación facial fallida: " + motivo);
                } catch (RuntimeException e) {
                    auditoria.loginFallido(usuario, "Servicio facial no disponible");
                    String detalle = e.getMessage() == null || e.getMessage().isBlank()
                            ? "Verifique que el servicio de reconocimiento facial esté encendido."
                            : e.getMessage();
                    return ResultadoLogin.rechazado("No fue posible validar el rostro. " + detalle);
                }
            }

            @Override
            protected void process(List<String> mensajes) {
                btnIngresar.setText(mensajes.get(mensajes.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    ResultadoLogin resultado = get();
                    if (resultado.esAceptado()) {
                        abrirMenu(resultado.usuario());
                    } else {
                        mostrarError(resultado.motivoRechazo());
                        txtPassword.selectAll();
                        txtPassword.requestFocus();
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
        JFrame ventana = new JFrame("SIGELAB");
        ventana.setContentPane(mainForm.getPanelPrincipal());
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(900, 640);
        ventana.setLocationRelativeTo(null);
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