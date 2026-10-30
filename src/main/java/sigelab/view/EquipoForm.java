package sigelab.view;

import sigelab.controller.EquipoController;
import sigelab.controller.LaboratorioController;
import sigelab.controller.TipoEquipoController;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.RolUsuario;
import sigelab.model.Equipo;
import sigelab.model.Laboratorio;
import sigelab.model.TipoEquipo;
import sigelab.model.Usuario;
import sigelab.util.Permisos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** Equipos del laboratorio, con generación y lectura de código QR. */
public class EquipoForm {
    private final JPanel panelPrincipal = Ui.contenedor("ADMINISTRACION DE EQUIPOS");
    private final EquipoController controller;
    private final TipoEquipoController tipoController;
    private final LaboratorioController labController;

    private final JTextField txtCodigo = new JTextField(14);
    private final JTextField txtNombre = new JTextField(22);
    private final JComboBox<TipoEquipo> cmbTipo = new JComboBox<>();
    private final JTextField txtMarca = new JTextField(14);
    private final JTextField txtModelo = new JTextField(14);
    private final JTextField txtSerie = new JTextField(18);
    private final JComboBox<Laboratorio> cmbLab = new JComboBox<>();
    private final JTextField txtObs = new JTextField(22);
    private final JLabel lblEstado = new JLabel("-");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnGuardarQr = new JButton("Guardar QR (PNG)");
    private final JButton btnLeerQr = new JButton("Leer QR desde imagen");
    private final JLabel lblQr = new JLabel("", SwingConstants.CENTER);
    private final JTextField txtBuscar = new JTextField(14);
    private final JComboBox<EstadoEquipo> cmbFiltro = new JComboBox<>();
    private final JTable tabla = Ui.tabla(Ui.modelo("ID", "Código", "Nombre", "Tipo", "Laboratorio", "Estado"));
    private List<Equipo> equipos = List.of();
    private Equipo seleccionado;
    private final boolean puedeEditar;

    public EquipoForm(Usuario actor) {
        controller = new EquipoController(actor);
        tipoController = new TipoEquipoController(actor);
        labController = new LaboratorioController(actor);
        puedeEditar = Permisos.tiene(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO);

        ComboBoxRenderers.configurar(cmbTipo, TipoEquipo::getNombre);
        ComboBoxRenderers.configurar(cmbLab, Laboratorio::toString);
        ComboBoxRenderers.configurarEnum(cmbFiltro);
        cmbFiltro.addItem(null);
        for (EstadoEquipo e : EstadoEquipo.values()) cmbFiltro.addItem(e);

        JPanel formulario = new JPanel(new GridBagLayout());
        Ui.campo(formulario, 0, "Código inventario", txtCodigo);
        Ui.campo(formulario, 1, "Nombre", txtNombre);
        Ui.campo(formulario, 2, "Tipo", cmbTipo);
        Ui.campo(formulario, 3, "Marca", txtMarca);
        Ui.campo(formulario, 4, "Modelo", txtModelo);
        Ui.campo(formulario, 5, "No. de serie", txtSerie);
        Ui.campo(formulario, 6, "Laboratorio", cmbLab);
        Ui.campo(formulario, 7, "Observaciones", txtObs);
        Ui.campo(formulario, 8, "Estado", lblEstado);

        lblQr.setPreferredSize(new Dimension(200, 200));
        lblQr.setBorder(BorderFactory.createTitledBorder("Código QR"));
        JPanel panelQr = new JPanel(new BorderLayout(4, 4));
        panelQr.add(lblQr, BorderLayout.CENTER);
        JPanel botonesQr = new JPanel(new GridLayout(2, 1, 4, 4));
        botonesQr.add(btnGuardarQr);
        botonesQr.add(btnLeerQr);
        panelQr.add(botonesQr, BorderLayout.SOUTH);

        JPanel superior = new JPanel(new BorderLayout(10, 4));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(panelQr, BorderLayout.EAST);
        superior.add(Ui.botones(btnNuevo, btnGuardar, btnActualizar), BorderLayout.SOUTH);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton btnBuscar = new JButton("Buscar");
        filtros.add(new JLabel("Buscar:"));
        filtros.add(txtBuscar);
        filtros.add(btnBuscar);
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbFiltro);

        JPanel inferior = new JPanel(new BorderLayout(4, 4));
        inferior.add(filtros, BorderLayout.NORTH);
        inferior.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        division.setResizeWeight(0.45);
        panelPrincipal.add(division, BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnGuardarQr.addActionListener(e -> guardarQr());
        btnLeerQr.addActionListener(e -> leerQr());
        btnBuscar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());
        cmbFiltro.addActionListener(e -> cargarTabla());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionarDeTabla();
        });

        btnGuardar.setVisible(puedeEditar);
        btnActualizar.setVisible(puedeEditar);
        btnNuevo.setVisible(puedeEditar);
        cargarCatalogos();
        limpiar();
        cargarTabla();
    }

    private void cargarCatalogos() {
        try {
            cmbTipo.removeAllItems();
            for (TipoEquipo t : tipoController.listar()) cmbTipo.addItem(t);
            cmbLab.removeAllItems();
            for (Laboratorio l : labController.listarActivos()) cmbLab.addItem(l);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private Equipo leer(Equipo base) {
        Equipo e = base != null ? base : new Equipo();
        e.setCodigoInventario(txtCodigo.getText());
        e.setNombre(txtNombre.getText());
        e.setTipo((TipoEquipo) cmbTipo.getSelectedItem());
        e.setMarca(txtMarca.getText());
        e.setModelo(txtModelo.getText());
        e.setNumeroSerie(txtSerie.getText());
        e.setLaboratorio((Laboratorio) cmbLab.getSelectedItem());
        e.setObservaciones(txtObs.getText());
        return e;
    }

    private void guardar() {
        try {
            Equipo e = controller.crear(leer(null));
            Ui.info(panelPrincipal, "Equipo registrado: " + e.getCodigoInventario());
            limpiar();
            cargarTabla();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void actualizar() {
        try {
            if (seleccionado == null) throw new IllegalArgumentException("Seleccione un equipo de la tabla.");
            controller.actualizar(leer(seleccionado));
            Ui.info(panelPrincipal, "Equipo actualizado.");
            limpiar();
            cargarTabla();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void seleccionarDeTabla() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int id = Integer.parseInt(tabla.getValueAt(fila, 0).toString());
        for (Equipo e : equipos) {
            if (e.getIdEquipo() == id) mostrar(e);
        }
    }

    private void mostrar(Equipo e) {
        seleccionado = e;
        txtCodigo.setText(e.getCodigoInventario());
        txtCodigo.setEditable(false);
        txtNombre.setText(e.getNombre());
        txtMarca.setText(e.getMarca());
        txtModelo.setText(e.getModelo());
        txtSerie.setText(e.getNumeroSerie());
        txtObs.setText(e.getObservaciones());
        seleccionarCombo(cmbTipo, e.getTipo() == null ? -1 : e.getTipo().getIdTipoEquipo());
        seleccionarCombo(cmbLab, e.getLaboratorio() == null ? -1 : e.getLaboratorio().getIdLaboratorio());
        lblEstado.setText(ComboBoxRenderers.texto(e.getEstado()));
        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        mostrarQr(e.getCodigoInventario());
    }

    private void seleccionarCombo(JComboBox<?> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            int actual = item instanceof TipoEquipo t ? t.getIdTipoEquipo() : ((Laboratorio) item).getIdLaboratorio();
            if (actual == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void mostrarQr(String codigo) {
        try {
            BufferedImage imagen = controller.generarQr(codigo, 180);
            lblQr.setIcon(new ImageIcon(imagen));
            lblQr.setText("");
            btnGuardarQr.setEnabled(true);
        } catch (RuntimeException e) {
            lblQr.setIcon(null);
            lblQr.setText("QR no disponible");
        }
    }

    private void guardarQr() {
        if (seleccionado == null) {
            Ui.error(panelPrincipal, new IllegalArgumentException("Seleccione un equipo para guardar su QR."));
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("QR-" + seleccionado.getCodigoInventario() + ".png"));
        if (selector.showSaveDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) return;
        try {
            controller.guardarQr(seleccionado.getCodigoInventario(), selector.getSelectedFile());
            Ui.info(panelPrincipal, "Imagen QR guardada.");
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    /** Lee el QR de una imagen y busca el equipo real en la base de datos (no modifica estados, RN-19). */
    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) return;
        try {
            String codigo = controller.leerQr(selector.getSelectedFile());
            Equipo e = controller.buscarPorCodigo(codigo).orElse(null);
            if (e == null) {
                Ui.info(panelPrincipal, "El QR indica " + codigo + ", pero ese equipo no existe en el sistema.");
                return;
            }
            cmbFiltro.setSelectedItem(null);
            txtBuscar.setText(codigo);
            cargarTabla();
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                if (Integer.parseInt(tabla.getValueAt(fila, 0).toString()) == e.getIdEquipo()) {
                    tabla.setRowSelectionInterval(fila, fila);
                }
            }
            Ui.info(panelPrincipal, "Equipo " + e.getCodigoInventario() + " - " + e.getNombre()
                    + "\nEstado: " + ComboBoxRenderers.texto(e.getEstado()));
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void limpiar() {
        seleccionado = null;
        txtCodigo.setText("");
        txtCodigo.setEditable(true);
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtSerie.setText("");
        txtObs.setText("");
        lblEstado.setText("Disponible (al registrar)");
        if (cmbTipo.getItemCount() > 0) cmbTipo.setSelectedIndex(0);
        if (cmbLab.getItemCount() > 0) cmbLab.setSelectedIndex(0);
        lblQr.setIcon(null);
        lblQr.setText("Seleccione un equipo");
        btnGuardarQr.setEnabled(false);
        tabla.clearSelection();
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    private void cargarTabla() {
        try {
            EstadoEquipo filtro = (EstadoEquipo) cmbFiltro.getSelectedItem();
            equipos = !txtBuscar.getText().isBlank() ? controller.buscar(txtBuscar.getText()) : controller.listar(filtro);
            if (filtro != null && !txtBuscar.getText().isBlank()) {
                equipos = equipos.stream().filter(e -> e.getEstado() == filtro).toList();
            }
            DefaultTableModel m = Ui.modelo("ID", "Código", "Nombre", "Tipo", "Laboratorio", "Estado");
            for (Equipo e : equipos) {
                m.addRow(new Object[]{e.getIdEquipo(), e.getCodigoInventario(), e.getNombre(), e.getTipo().getNombre(),
                        e.getLaboratorio().getNombre(), ComboBoxRenderers.texto(e.getEstado())});
            }
            tabla.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}