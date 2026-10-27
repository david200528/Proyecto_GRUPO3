package sigelab.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import sigelab.repository.AuditoriaRepository;

public class AuditoriaForm {
    private JPanel panelPrincipal;
    private JTable tableBitacora;
    private JScrollPane scrollPane;
    private JButton btnActualizar;
    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaForm() {
        auditoriaRepository = new AuditoriaRepository();
        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Título de la sección
        JLabel lblTitulo = new JLabel("Historial de Auditoría y Bitácora del Sistema", JLabel.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 16));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // Configuración de la tabla
        tableBitacora = new JTable();
        scrollPane = new JScrollPane(tableBitacora);
        panelPrincipal.add(scrollPane, BorderLayout.CENTER);

        // Panel inferior con botón para refrescar registros
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnActualizar = new JButton("Actualizar Registros");
        btnActualizar.addActionListener(e -> cargarDatosBitacora());
        panelInferior.add(btnActualizar);
        panelPrincipal.add(panelInferior, BorderLayout.SOUTH);

        // Cargar los datos iniciales al abrir
        cargarDatosBitacora();
    }

    private void cargarDatosBitacora() {
        String[] columnas = {"ID Auditoría", "Usuario", "Acción", "Fecha y Hora"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Evita que editen las celdas directamente
            }
        };

        List<String[]> registros = auditoriaRepository.listarBitacora();
        for (String[] fila : registros) {
            model.addRow(fila);
        }
        tableBitacora.setModel(model);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}