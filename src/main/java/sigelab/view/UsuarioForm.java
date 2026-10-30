package sigelab.view;

import sigelab.controller.UsuarioController;
import sigelab.enums.RolUsuario;
import sigelab.integration.facial.FacialAuthClient;
import sigelab.integration.facial.FacialClientException;
import sigelab.integration.facial.FacialEnrollmentResult;
import sigelab.integration.facial.FacialStatus;
import sigelab.model.Usuario;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import java.util.concurrent.ExecutionException;

/** Administración de usuarios sin exponer el hash de contraseña. */
public class UsuarioForm {

    private final JPanel panelPrincipal = Ui.contenedor("ADMINISTRACION DE USUARIOS");
    private final JTextField txtId = new JTextField(10);
    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtApellido = new JTextField(20);
    private final JTextField txtCorreo = new JTextField(25);
    private final JTextField txtUsuario = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JComboBox<RolUsuario> cmbRol = new JComboBox<>();
    private final JCheckBox chkActivo = new JCheckBox("Activo");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnDesactivar = new JButton("Desactivar");
    private final JTextField txtBuscar = new JTextField(20);
    private final JButton btnBuscar = new JButton("Buscar");
    private final JTable tblUsuarios = new JTable();
    private final JButton btnSalir = new JButton("Cerrar");
    private final JButton btnRegistrarRostro = new JButton("Registrar rostro");
    private final JButton btnEliminarRostro = new JButton("Eliminar rostro");
    private final JLabel lblEstadoRostro = new JLabel("Seleccione un usuario.");

    private final UsuarioController usuarioController;
    private final FacialAuthClient facialAuthClient;
    private final Usuario usuarioAutenticado;
    private Usuario usuarioSeleccionado;
    private boolean perfilFacialRegistrado;

    public UsuarioForm() {
        this((Usuario) null);
    }

    public UsuarioForm(Usuario usuarioAutenticado) {
        this(new UsuarioController(), new FacialAuthClient(), usuarioAutenticado);
    }

    public UsuarioForm(UsuarioController usuarioController) {
        this(usuarioController, new FacialAuthClient(), null);
    }

    public UsuarioForm(UsuarioController usuarioController,
                       Usuario usuarioAutenticado) {
        this(usuarioController, new FacialAuthClient(), usuarioAutenticado);
    }

    public UsuarioForm(UsuarioController usuarioController,
                       FacialAuthClient facialAuthClient, Usuario usuarioAutenticado) {
        this.usuarioController = usuarioController;
        this.facialAuthClient = facialAuthClient;
        this.usuarioAutenticado = usuarioAutenticado;
        construirPantalla();
        configurarFormulario();
        configurarEventos();
        cargarUsuarios();
    }

    private void construirPantalla() {
        JPanel formulario = new JPanel(new java.awt.GridBagLayout());
        Ui.campo(formulario, 0, "ID", txtId);
        Ui.campo(formulario, 1, "Nombre", txtNombre);
        Ui.campo(formulario, 2, "Apellido", txtApellido);
        Ui.campo(formulario, 3, "Correo", txtCorreo);
        Ui.campo(formulario, 4, "Usuario", txtUsuario);
        Ui.campo(formulario, 5, "Contraseña", txtPassword);
        Ui.campo(formulario, 6, "Rol", cmbRol);
        JPanel estado = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        estado.add(chkActivo);
        estado.add(lblEstadoRostro);
        Ui.campo(formulario, 7, "Estado", estado);

        JPanel arriba = new JPanel(new java.awt.BorderLayout());
        arriba.add(formulario, java.awt.BorderLayout.CENTER);
        arriba.add(Ui.botones(btnNuevo, btnGuardar, btnActualizar, btnDesactivar,
                btnRegistrarRostro, btnEliminarRostro, btnSalir), java.awt.BorderLayout.SOUTH);

        JPanel busqueda = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 2));
        busqueda.add(new JLabel("Buscar usuario:"));
        busqueda.add(txtBuscar);
        busqueda.add(btnBuscar);

        JPanel centro = new JPanel(new java.awt.BorderLayout(4, 4));
        centro.add(busqueda, java.awt.BorderLayout.NORTH);
        centro.add(new javax.swing.JScrollPane(tblUsuarios), java.awt.BorderLayout.CENTER);

        JPanel contenido = new JPanel(new java.awt.BorderLayout(6, 6));
        contenido.add(arriba, java.awt.BorderLayout.NORTH);
        contenido.add(centro, java.awt.BorderLayout.CENTER);
        panelPrincipal.add(contenido, java.awt.BorderLayout.CENTER);
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        chkActivo.setSelected(true);
        chkActivo.setEnabled(false);
        cmbRol.removeAllItems();
        cmbRol.addItem(null);
        for (RolUsuario rol : RolUsuario.values()) cmbRol.addItem(rol);
        ComboBoxRenderers.configurarRoles(cmbRol);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        lblEstadoRostro.setText("Seleccione un usuario.");
        actualizarTextoEstado(true);
        configurarTabla();
    }

    private void configurarTabla() {
        tblUsuarios.setModel(new DefaultTableModel(
                new Object[]{"ID", "Usuario", "Nombre", "Correo", "Rol", "Activo", "Fecha creación"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> cambiarEstado());
        btnRegistrarRostro.addActionListener(e -> registrarRostro());
        btnEliminarRostro.addActionListener(e -> eliminarRostro());
        btnBuscar.addActionListener(e -> buscar());
        btnSalir.addActionListener(e -> salir());
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionarUsuario();
        });
    }

    private void guardar() {
        try {
            Usuario nuevo = new Usuario(txtNombre.getText(), txtApellido.getText(), txtUsuario.getText(),
                    null, txtCorreo.getText(), (RolUsuario) cmbRol.getSelectedItem());
            Usuario usuario = usuarioController.crearUsuario(usuarioAutenticado, nuevo,
                    new String(txtPassword.getPassword()));
            JOptionPane.showMessageDialog(panelPrincipal, "Usuario creado correctamente: " + usuario.getUsuario());
            limpiarFormulario();
            cargarUsuarios();
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void actualizar() {
        try {
            if (usuarioSeleccionado == null) throw new IllegalArgumentException("Seleccione un usuario.");
            if (!confirmar("¿Desea actualizar este usuario?", "Confirmar actualización")) return;
            Usuario usuario = construirUsuario(chkActivo.isSelected());
            String password = new String(txtPassword.getPassword());
            boolean actualizado = usuarioController.actualizar(usuarioAutenticado, usuario, password);
            if (actualizado) {
                JOptionPane.showMessageDialog(panelPrincipal, "Usuario actualizado correctamente.");
                cargarUsuarios();
                seleccionarFilaPorId(usuario.getIdUsuario());
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void cambiarEstado() {
        try {
            if (usuarioSeleccionado == null) throw new IllegalArgumentException("Seleccione un usuario.");
            if (esUsuarioAutenticadoActivo(usuarioSeleccionado)) {
                throw new IllegalArgumentException("No puede desactivar el usuario con el que inició sesión.");
            }
            boolean nuevoEstado = !chkActivo.isSelected();
            String accion = nuevoEstado ? "activar" : "desactivar";
            if (!confirmar("¿Desea " + accion + " este usuario?", "Confirmar cambio de estado")) return;
            boolean cambiado = nuevoEstado
                    ? usuarioController.actualizar(usuarioAutenticado, construirUsuario(true), null)
                    : usuarioController.desactivar(usuarioAutenticado, usuarioSeleccionado.getIdUsuario());
            if (cambiado) {
                chkActivo.setSelected(nuevoEstado);
                actualizarTextoEstado(nuevoEstado);
                JOptionPane.showMessageDialog(panelPrincipal,
                        nuevoEstado ? "Usuario activado correctamente." : "Usuario desactivado correctamente.");
                cargarUsuarios();
                seleccionarFilaPorId(usuarioSeleccionado.getIdUsuario());
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private Usuario construirUsuario(boolean activo) {
        Usuario u = new Usuario(txtNombre.getText(), txtApellido.getText(), usuarioSeleccionado.getUsuario(),
                usuarioSeleccionado.getPasswordHash(), txtCorreo.getText(), (RolUsuario) cmbRol.getSelectedItem());
        u.setIdUsuario(obtenerId());
        u.setActivo(activo);
        u.setFechaCreacion(usuarioSeleccionado.getFechaCreacion());
        return u;
    }

    private void cargarUsuarios() {
        try {
            DefaultTableModel modelo = (DefaultTableModel) tblUsuarios.getModel();
            modelo.setRowCount(0);
            for (Usuario usuario : usuarioController.listar(usuarioAutenticado)) {
                modelo.addRow(new Object[]{usuario.getIdUsuario(), usuario.getUsuario(),
                        usuario.getNombreCompleto(), usuario.getCorreo(), ComboBoxRenderers.texto(usuario.getRol()),
                        usuario.isActivo() ? "Sí" : "No",
                        ComboBoxRenderers.fechaHora(usuario.getFechaCreacion())});
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void buscar() {
        String nombre = txtBuscar.getText().trim();
        if (nombre.isBlank()) {
            cargarUsuarios();
            return;
        }
        try {
            usuarioController.buscarPorUsuario(nombre).ifPresentOrElse(this::mostrarUsuario,
                    () -> JOptionPane.showMessageDialog(panelPrincipal, "No se encontró el usuario."));
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void seleccionarUsuario() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) return;
        try {
            usuarioController.buscar(Integer.parseInt(tblUsuarios.getValueAt(fila, 0).toString()))
                    .ifPresent(this::mostrarUsuario);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void mostrarUsuario(Usuario usuario) {
        usuarioSeleccionado = usuario;
        txtId.setText(String.valueOf(usuario.getIdUsuario()));
        txtUsuario.setEditable(false);
        txtUsuario.setText(usuario.getUsuario());
        txtNombre.setText(usuario.getNombre());
        txtApellido.setText(usuario.getApellido());
        txtCorreo.setText(usuario.getCorreo());
        txtPassword.setText("");
        cmbRol.setSelectedItem(usuario.getRol());
        chkActivo.setSelected(usuario.isActivo());
        actualizarTextoEstado(usuario.isActivo());
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnDesactivar.setEnabled(!esUsuarioAutenticadoActivo(usuario));
        consultarEstadoRostro(usuario.getIdUsuario());
    }

    private int obtenerId() {
        if (txtId.getText().isBlank()) throw new IllegalArgumentException("Seleccione un usuario.");
        return Integer.parseInt(txtId.getText().trim());
    }

    private void seleccionarFilaPorId(int id) {
        for (int fila = 0; fila < tblUsuarios.getRowCount(); fila++) {
            if (Integer.parseInt(tblUsuarios.getValueAt(fila, 0).toString()) == id) {
                tblUsuarios.setRowSelectionInterval(fila, fila);
                tblUsuarios.scrollRectToVisible(tblUsuarios.getCellRect(fila, 0, true));
                return;
            }
        }
    }

    private void nuevo() {
        limpiarFormulario();
        txtUsuario.requestFocus();
    }

    private void limpiarFormulario() {
        usuarioSeleccionado = null;
        perfilFacialRegistrado = false;
        txtId.setText("");
        txtUsuario.setEditable(true);
        txtUsuario.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        txtPassword.setText("");
        chkActivo.setSelected(true);
        txtBuscar.setText("");
        cmbRol.setSelectedItem(RolUsuario.ENCARGADO);
        tblUsuarios.clearSelection();
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        lblEstadoRostro.setText("Seleccione un usuario.");
        actualizarTextoEstado(true);
    }

    private void registrarRostro() {
        if (usuarioSeleccionado == null) {
            mostrarError("Seleccione un usuario.");
            return;
        }
        int userId = usuarioSeleccionado.getIdUsuario();
        boolean reemplazar = perfilFacialRegistrado;
        String accion = reemplazar ? "actualizar" : "registrar";
        if (!confirmar("¿Desea " + accion + " el rostro de " + usuarioSeleccionado.getUsuario() + "?",
                "Registro facial")) return;
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        lblEstadoRostro.setText("Cámara activa. Siga las instrucciones...");

        new SwingWorker<FacialEnrollmentResult, Void>() {
            @Override
            protected FacialEnrollmentResult doInBackground() {
                return facialAuthClient.enroll(userId, reemplazar);
            }

            @Override
            protected void done() {
                try {
                    FacialEnrollmentResult result = get();
                    JOptionPane.showMessageDialog(panelPrincipal,
                            result.message(), "Registro facial", JOptionPane.INFORMATION_MESSAGE);
                    consultarEstadoRostro(userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("El registro facial fue interrumpido.");
                } catch (ExecutionException e) {
                    mostrarError(mensajeError(e.getCause(), "No fue posible registrar el rostro."));
                    consultarEstadoRostro(userId);
                }
            }
        }.execute();
    }

    private void eliminarRostro() {
        if (usuarioSeleccionado == null) {
            mostrarError("Seleccione un usuario.");
            return;
        }
        if (usuarioAutenticado == null || usuarioAutenticado.getRol() != RolUsuario.ADMIN) {
            mostrarError("Solo un administrador puede eliminar perfiles faciales.");
            return;
        }
        int userId = usuarioSeleccionado.getIdUsuario();
        if (!confirmar("¿Desea eliminar el perfil facial de " + usuarioSeleccionado.getUsuario() + "?",
                "Eliminar perfil facial")) return;
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                facialAuthClient.deleteProfile(userId, usuarioAutenticado.getIdUsuario());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(panelPrincipal,
                            "Perfil facial eliminado correctamente.", "Perfil facial",
                            JOptionPane.INFORMATION_MESSAGE);
                    consultarEstadoRostro(userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La eliminación del perfil fue interrumpida.");
                } catch (ExecutionException e) {
                    mostrarError(mensajeError(e.getCause(), "No fue posible eliminar el perfil facial."));
                    consultarEstadoRostro(userId);
                }
            }
        }.execute();
    }

    private void consultarEstadoRostro(int userId) {
        lblEstadoRostro.setText("Consultando perfil facial...");
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        new SwingWorker<FacialStatus, Void>() {
            @Override
            protected FacialStatus doInBackground() {
                return facialAuthClient.status(userId);
            }

            @Override
            protected void done() {
                if (usuarioSeleccionado == null || usuarioSeleccionado.getIdUsuario() != userId) return;
                try {
                    FacialStatus status = get();
                    perfilFacialRegistrado = status.enrolled();
                    lblEstadoRostro.setText(status.enrolled()
                            ? "Registrado (" + status.model() + ", " + status.samples() + " muestras)"
                            : "No registrado");
                    btnRegistrarRostro.setText(status.enrolled() ? "Actualizar rostro" : "Registrar rostro");
                    btnRegistrarRostro.setEnabled(true);
                    btnEliminarRostro.setEnabled(status.enrolled()
                            && usuarioAutenticado != null
                            && usuarioAutenticado.getRol() == RolUsuario.ADMIN);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblEstadoRostro.setText("Estado facial no disponible");
                } catch (ExecutionException e) {
                    perfilFacialRegistrado = false;
                    lblEstadoRostro.setText("Servicio facial no disponible");
                    mostrarError(mensajeError(e.getCause(), "No fue posible consultar el perfil facial."));
                }
            }
        }.execute();
    }

    private String mensajeError(Throwable error, String defecto) {
        if (error instanceof FacialClientException facial && facial.getMessage() != null) {
            return facial.getMessage();
        }
        return defecto;
    }

    private void actualizarTextoEstado(boolean activo) {
        btnDesactivar.setText(activo ? "Desactivar" : "Activar");
    }

    private boolean esUsuarioAutenticadoActivo(Usuario usuario) {
        return usuarioAutenticado != null && usuario != null
                && usuario.getIdUsuario() == usuarioAutenticado.getIdUsuario()
                && usuario.isActivo();
    }

    private boolean confirmar(String mensaje, String titulo) {
        return JOptionPane.showConfirmDialog(panelPrincipal, mensaje, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void mostrarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) mensaje = "Ocurrió un error inesperado.";
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Usuarios", JOptionPane.ERROR_MESSAGE);
    }

    private void salir() {
        int respuesta = JOptionPane.showConfirmDialog(panelPrincipal,
                "¿Desea regresar al menú principal?", "Regresar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            java.awt.Window ventana = SwingUtilities.getWindowAncestor(panelPrincipal);
            if (ventana != null) ventana.dispose();
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}
