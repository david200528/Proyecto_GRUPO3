package sigelab.view;

import sigelab.controller.AuditoriaController;
import sigelab.controller.UsuarioController;
import sigelab.integration.facial.FacialAuthClient;
import sigelab.integration.facial.FacialEnrollmentResult;
import sigelab.integration.facial.FacialStatus;
import sigelab.integration.facial.FacialVerificationResult;
import sigelab.model.Usuario;

import javax.swing.*;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de entrada al sistema: usuario y contraseña, y después validación facial.
 * Si el usuario aún no tiene rostro registrado, se le ofrece registrarlo en ese momento.
 */
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

    /** Resultado de cada paso del login: entrar, rechazar o pedir el registro del rostro. */
    private enum TipoPaso { ACEPTADO, RECHAZADO, NECESITA_REGISTRO }

    private record Paso(TipoPaso tipo, Usuario usuario, String mensaje) {
        static Paso aceptado(Usuario usuario) {
            return new Paso(TipoPaso.ACEPTADO, usuario, null);
        }

        static Paso rechazado(String mensaje) {
            return new Paso(TipoPaso.RECHAZADO, null, mensaje);
        }

        static Paso necesitaRegistro(Usuario usuario) {
            return new Paso(TipoPaso.NECESITA_REGISTRO, usuario, null);
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

        bloquear("Verificando...");
        ejecutar(() -> validarCredenciales(usuario, password));
    }

    /* ---------------------------------------------------------------------
       Flujo del login
       1. Usuario y contraseña (RN-01).
       2. Si el usuario YA tiene rostro: se verifica con la cámara (RN-02).
       3. Si NO tiene rostro: se ofrece registrarlo en ese momento, y después
          se verifica para confirmar que el reconocimiento funciona.
       Cualquier fallo del servicio facial deniega el acceso.
       --------------------------------------------------------------------- */

    /** Paso 1 (en segundo plano): contraseña y consulta de si ya tiene rostro registrado. */
    private Paso validarCredenciales(String usuario, String password) {
        Optional<Usuario> autenticado = usuarioController.autenticar(usuario, password);
        if (autenticado.isEmpty()) {
            auditoria.loginFallido(usuario, "Credenciales incorrectas o usuario inactivo");
            return Paso.rechazado("Usuario o contraseña incorrectos, o usuario inactivo.");
        }
        Usuario encontrado = autenticado.get();
        try {
            FacialStatus estado = facialAuthClient.status(encontrado.getIdUsuario());
            if (!estado.enrolled()) return Paso.necesitaRegistro(encontrado);
        } catch (RuntimeException e) {
            return servicioNoDisponible(encontrado, e);
        }
        avisar("Mire a la cámara...");
        return verificarRostro(encontrado);
    }

    /** Paso 2b (en segundo plano): registra el rostro y luego lo verifica. */
    private Paso registrarYVerificar(Usuario usuario) {
        avisar("Registrando rostro...");
        try {
            FacialEnrollmentResult registro = facialAuthClient.enroll(usuario.getIdUsuario(), false);
            if (!registro.success()) {
                return Paso.rechazado("No fue posible registrar el rostro: " + registro.message());
            }
            auditoria.rostroRegistrado(usuario, registro.samples());
        } catch (RuntimeException e) {
            return Paso.rechazado("No fue posible registrar el rostro. " + detalleError(e));
        }
        avisar("Verificando rostro...");
        return verificarRostro(usuario);
    }

    /** Paso 2a (en segundo plano): compara el rostro frente a la cámara con el registrado. */
    private Paso verificarRostro(Usuario usuario) {
        try {
            FacialVerificationResult resultado = facialAuthClient.verify(usuario.getIdUsuario());
            if (resultado.success() && resultado.matched()) {
                auditoria.loginExitoso(usuario);
                return Paso.aceptado(usuario);
            }
            auditoria.loginFallido(usuario.getUsuario(), "Validación facial no aprobada");
            String motivo = resultado.message() == null || resultado.message().isBlank()
                    ? "El rostro no corresponde al usuario."
                    : resultado.message();
            return Paso.rechazado("Validación facial fallida: " + motivo);
        } catch (RuntimeException e) {
            return servicioNoDisponible(usuario, e);
        }
    }

    /** Se ejecuta en la pantalla (hilo de Swing) cuando termina cada paso. */
    private void continuar(Paso paso) {
        switch (paso.tipo()) {
            case ACEPTADO -> {
                desbloquear();
                abrirMenu(paso.usuario());
            }
            case RECHAZADO -> {
                desbloquear();
                mostrarError(paso.mensaje());
                txtPassword.selectAll();
                txtPassword.requestFocus();
            }
            case NECESITA_REGISTRO -> {
                int opcion = JOptionPane.showConfirmDialog(panelPrincipal,
                        "Hola " + paso.usuario().getNombre() + ", todavía no tienes un rostro registrado.\n\n"
                                + "¿Deseas registrarlo ahora?\n"
                                + "Se abrirá la cámara: mira de frente y mueve un poco la cabeza\n"
                                + "hasta que termine. Después se verificará tu rostro para ingresar.",
                        "Registrar rostro", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (opcion == JOptionPane.YES_OPTION) {
                    bloquear("Registrando rostro...");
                    ejecutar(() -> registrarYVerificar(paso.usuario()));
                } else {
                    auditoria.loginFallido(paso.usuario().getUsuario(), "Sin rostro registrado");
                    desbloquear();
                    mostrarError("Para ingresar a SIGELAB necesitas registrar tu rostro.");
                }
            }
        }
    }

    private Paso servicioNoDisponible(Usuario usuario, RuntimeException e) {
        auditoria.loginFallido(usuario.getUsuario(), "Servicio facial no disponible");
        return Paso.rechazado("No fue posible validar el rostro. " + detalleError(e));
    }

    private static String detalleError(RuntimeException e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "Verifique que el servicio de reconocimiento facial esté encendido (facial-api/iniciar_facial.bat)."
                : e.getMessage();
    }

    /** Corre una tarea larga (BD, cámara) fuera del hilo de Swing y luego llama a continuar(). */
    private void ejecutar(java.util.function.Supplier<Paso> tarea) {
        new SwingWorker<Paso, Void>() {
            @Override
            protected Paso doInBackground() {
                return tarea.get();
            }

            @Override
            protected void done() {
                try {
                    continuar(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continuar(Paso.rechazado("El proceso fue interrumpido."));
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    continuar(Paso.rechazado(causa instanceof RuntimeException && causa.getMessage() != null
                            ? causa.getMessage()
                            : "No fue posible completar la autenticación."));
                }
            }
        }.execute();
    }

    private void avisar(String texto) {
        SwingUtilities.invokeLater(() -> btnIngresar.setText(texto));
    }

    private void bloquear(String texto) {
        autenticando = true;
        btnIngresar.setEnabled(false);
        btnIngresar.setText(texto);
    }

    private void desbloquear() {
        autenticando = false;
        btnIngresar.setEnabled(true);
        btnIngresar.setText("INGRESAR");
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