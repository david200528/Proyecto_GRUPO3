package sigelab.view;

import sigelab.controller.ReporteController;
import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;
import sigelab.util.Permisos;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Supplier;

/** Menú principal con dashboard; las opciones dependen del rol (y los servicios lo vuelven a validar). */
public class MainForm {
    private final JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
    private final Usuario usuarioAutenticado;
    private final JPanel tarjetas = new JPanel(new GridLayout(2, 4, 8, 8));

    public MainForm(Usuario usuarioAutenticado) {
        this.usuarioAutenticado = usuarioAutenticado;
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel titulo = new JLabel("SIGELAB - Sistema de Gestión de Laboratorios", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sesion = new JLabel("Sesión: " + usuarioAutenticado.getNombreCompleto() + " ("
                + ComboBoxRenderers.texto(usuarioAutenticado.getRol()) + ")", SwingConstants.CENTER);
        JPanel encabezado = new JPanel(new GridLayout(2, 1));
        encabezado.add(titulo);
        encabezado.add(sesion);
        panelPrincipal.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createTitledBorder("Resumen"));
        centro.add(tarjetas, BorderLayout.CENTER);
        JButton btnRefrescar = new JButton("Actualizar resumen");
        btnRefrescar.addActionListener(e -> cargarDashboard());
        centro.add(Ui.botones(btnRefrescar), BorderLayout.SOUTH);
        panelPrincipal.add(centro, BorderLayout.CENTER);

        panelPrincipal.add(construirMenu(), BorderLayout.SOUTH);
        cargarDashboard();
    }

    private JPanel construirMenu() {
        JPanel menu = new JPanel(new GridLayout(0, 4, 8, 8));
        RolUsuario[] todos = RolUsuario.values();
        agregar(menu, "Usuarios", () -> new UsuarioForm(usuarioAutenticado).getPanelPrincipal(), 1050, 650, RolUsuario.ADMIN);
        agregar(menu, "Laboratorios", () -> new LaboratorioForm(usuarioAutenticado).getPanelPrincipal(), 800, 550, RolUsuario.ADMIN);
        agregar(menu, "Tipos de equipo", () -> new TipoEquipoForm(usuarioAutenticado).getPanelPrincipal(), 800, 500, RolUsuario.ADMIN);
        agregar(menu, "Equipos y QR", () -> new EquipoForm(usuarioAutenticado).getPanelPrincipal(), 1100, 720, todos);
        agregar(menu, "Préstamos", () -> new PrestamoForm(usuarioAutenticado).getPanelPrincipal(), 1000, 720, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        agregar(menu, "Devoluciones", () -> new DevolucionForm(usuarioAutenticado).getPanelPrincipal(), 1000, 650, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        agregar(menu, "Mantenimiento", () -> new MantenimientoForm(usuarioAutenticado).getPanelPrincipal(), 1000, 650, RolUsuario.ADMIN, RolUsuario.ENCARGADO);
        agregar(menu, "Reportes", () -> new ReporteForm(usuarioAutenticado).getPanelPrincipal(), 1100, 600, todos);
        agregar(menu, "Auditoría", () -> new AuditoriaForm(usuarioAutenticado).getPanelPrincipal(), 1000, 600, RolUsuario.ADMIN);
        JButton salir = new JButton("Salir");
        salir.addActionListener(e -> System.exit(0));
        menu.add(salir);
        return menu;
    }

    /** Solo se muestra la opción si el rol la permite; el servicio vuelve a comprobarlo al operar. */
    private void agregar(JPanel menu, String texto, Supplier<JPanel> pantalla, int ancho, int alto, RolUsuario... roles) {
        if (!Permisos.tiene(usuarioAutenticado, roles)) return;
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> abrirVentana(texto, pantalla, ancho, alto));
        menu.add(boton);
    }

    private void cargarDashboard() {
        tarjetas.removeAll();
        try {
            for (Map.Entry<String, Integer> dato : new ReporteController(usuarioAutenticado).dashboard().entrySet()) {
                JPanel tarjeta = new JPanel(new GridLayout(2, 1));
                tarjeta.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                JLabel valor = new JLabel(String.valueOf(dato.getValue()), SwingConstants.CENTER);
                valor.setFont(valor.getFont().deriveFont(Font.BOLD, 28f));
                tarjeta.add(valor);
                tarjeta.add(new JLabel(dato.getKey(), SwingConstants.CENTER));
                tarjetas.add(tarjeta);
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
        tarjetas.revalidate();
        tarjetas.repaint();
    }

    private void abrirVentana(String titulo, Supplier<JPanel> pantalla, int ancho, int alto) {
        try {
            JFrame ventana = new JFrame(titulo);
            ventana.setContentPane(pantalla.get());
            ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            Dimension p = Toolkit.getDefaultToolkit().getScreenSize();
            ventana.setSize(Math.min(ancho, p.width - 40), Math.min(alto, p.height - 80));
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}