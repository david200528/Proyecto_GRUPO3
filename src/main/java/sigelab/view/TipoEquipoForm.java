package sigelab.view;

import sigelab.controller.TipoEquipoController;
import sigelab.model.TipoEquipo;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Catálogo de tipos de equipo (solo ADMIN). */
public class TipoEquipoForm {
    private final JPanel panelPrincipal = Ui.contenedor("CATALOGO DE TIPOS DE EQUIPO");
    private final TipoEquipoController controller;
    private final JTextField txtNombre = new JTextField(25);
    private final JTextField txtDescripcion = new JTextField(35);
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JTable tabla = Ui.tabla(Ui.modelo("ID", "Nombre", "Descripción"));
    private TipoEquipo seleccionado;

    public TipoEquipoForm(Usuario actor) {
        controller = new TipoEquipoController(actor);
        JPanel formulario = new JPanel(new GridBagLayout());
        Ui.campo(formulario, 0, "Nombre", txtNombre);
        Ui.campo(formulario, 1, "Descripción", txtDescripcion);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(formulario, BorderLayout.CENTER);
        arriba.add(Ui.botones(btnNuevo, btnGuardar, btnActualizar), BorderLayout.SOUTH);
        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panelPrincipal.add(centro, BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> {
            try {
                TipoEquipo t = new TipoEquipo(txtNombre.getText(), txtDescripcion.getText());
                controller.crear(t);
                Ui.info(panelPrincipal, "Tipo de equipo creado: " + t.getNombre());
                limpiar();
                cargar();
            } catch (RuntimeException ex) {
                Ui.error(panelPrincipal, ex);
            }
        });
        btnActualizar.addActionListener(e -> {
            try {
                if (seleccionado == null) throw new IllegalArgumentException("Seleccione un tipo de la tabla.");
                seleccionado.setNombre(txtNombre.getText());
                seleccionado.setDescripcion(txtDescripcion.getText());
                controller.actualizar(seleccionado);
                Ui.info(panelPrincipal, "Tipo de equipo actualizado.");
                limpiar();
                cargar();
            } catch (RuntimeException ex) {
                Ui.error(panelPrincipal, ex);
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (e.getValueIsAdjusting() || fila < 0) return;
            int id = Integer.parseInt(tabla.getValueAt(fila, 0).toString());
            for (TipoEquipo t : controller.listar()) {
                if (t.getIdTipoEquipo() == id) {
                    seleccionado = t;
                    txtNombre.setText(t.getNombre());
                    txtDescripcion.setText(t.getDescripcion());
                    btnGuardar.setEnabled(false);
                    btnActualizar.setEnabled(true);
                }
            }
        });
        limpiar();
        cargar();
    }

    private void limpiar() {
        seleccionado = null;
        txtNombre.setText("");
        txtDescripcion.setText("");
        tabla.clearSelection();
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    private void cargar() {
        try {
            DefaultTableModel m = Ui.modelo("ID", "Nombre", "Descripción");
            for (TipoEquipo t : controller.listar()) m.addRow(new Object[]{t.getIdTipoEquipo(), t.getNombre(), t.getDescripcion()});
            tabla.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}