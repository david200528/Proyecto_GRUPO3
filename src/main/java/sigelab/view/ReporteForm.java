package sigelab.view;

import sigelab.controller.ReporteController;
import sigelab.enums.EstadoEquipo;
import sigelab.model.Usuario;
import sigelab.util.TablaResultado;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/** Consultas y reportes (6.11). */
public class ReporteForm {
    private static final String[] REPORTES = {
            "Equipos por estado (resumen)", "Equipos de un estado", "Préstamos activos", "Préstamos finalizados",
            "Préstamos vencidos", "Préstamos entre fechas", "Historial de un equipo", "Historial de mantenimiento"};

    private final JPanel panelPrincipal = Ui.contenedor("DASHBOARD Y REPORTES");
    private final ReporteController controller;
    private final JComboBox<String> cmbReporte = new JComboBox<>(REPORTES);
    private final JComboBox<EstadoEquipo> cmbEstado = new JComboBox<>(EstadoEquipo.values());
    private final JTextField txtCodigo = new JTextField(12);
    private final JSpinner spnDesde = Ui.selectorFecha(LocalDate.now().minusMonths(1));
    private final JSpinner spnHasta = Ui.selectorFecha(LocalDate.now());
    private final JTable tabla = Ui.tabla(Ui.modelo());

    public ReporteForm(Usuario actor) {
        controller = new ReporteController(actor);
        ComboBoxRenderers.configurarEnum(cmbEstado);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton btnConsultar = new JButton("Consultar");
        filtros.add(new JLabel("Reporte:"));
        filtros.add(cmbReporte);
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbEstado);
        filtros.add(new JLabel("Código equipo:"));
        filtros.add(txtCodigo);
        filtros.add(new JLabel("Desde:"));
        filtros.add(spnDesde);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(spnHasta);
        filtros.add(btnConsultar);
        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panelPrincipal.add(centro, BorderLayout.CENTER);
        btnConsultar.addActionListener(e -> consultar());
        consultar();
    }

    private void consultar() {
        try {
            TablaResultado datos = switch (cmbReporte.getSelectedIndex()) {
                case 0 -> controller.equiposPorEstado(null);
                case 1 -> controller.equiposPorEstado((EstadoEquipo) cmbEstado.getSelectedItem());
                case 2 -> controller.prestamos("ACTIVO");
                case 3 -> controller.prestamos("FINALIZADO");
                case 4 -> controller.prestamos("VENCIDO");
                case 5 -> controller.prestamosEntre(Ui.valorFecha(spnDesde), Ui.valorFecha(spnHasta));
                case 6 -> controller.historialEquipo(txtCodigo.getText());
                default -> controller.historialMantenimiento(txtCodigo.getText());
            };
            Ui.llenar(tabla, datos);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}