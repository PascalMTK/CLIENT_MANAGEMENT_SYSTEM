import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("unused")
public class AdministratorDashboard extends JFrame {

    private JTable recordsTable;
    private DefaultTableModel tableModel;
    private List<String> clients; // To store client names
    private JLabel clientCountLabel;

    public AdministratorDashboard() {
        setTitle("Admin Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        clients = new ArrayList<>(); // Initialize the client list

        // Create main panel and header
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createRecordsTable(), BorderLayout.CENTER);
        mainPanel.add(createClientInfoPanel(), BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerPanel.add(createButton("Update Profile", Color.decode("#2196F3"), e -> showUpdateProfileDialog()));
        headerPanel.add(createButton("Logout", Color.decode("#f44336"), e -> logout()));
        return headerPanel;
    }

    private JButton createButton(String text, Color backgroundColor, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.white);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.addActionListener(actionListener);
        return button;
    }

    private JScrollPane createRecordsTable() {
        String[] columnNames = {
                "Document Name", "Submission Date", "Category", "Status", "Expiration Date", "Supervisor Name", "Action"
        };
        tableModel = new DefaultTableModel(columnNames, 0);
        recordsTable = new JTable(tableModel);
        recordsTable.setFont(new Font("Arial", Font.PLAIN, 16));
        recordsTable.setRowHeight(30);
        recordsTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        recordsTable.getColumnModel().getColumn(6).setCellEditor(new ActionButtonEditor(new JCheckBox(), recordsTable));

        return new JScrollPane(recordsTable);
    }

    private JPanel createClientInfoPanel() {
        JPanel clientPanel = new JPanel();
        clientPanel.setLayout(new BoxLayout(clientPanel, BoxLayout.Y_AXIS));
        clientCountLabel = new JLabel("Total Number of Clients: " + clients.size());
        clientPanel.add(clientCountLabel);
        return clientPanel;
    }

    private void showUpdateProfileDialog() {
        JOptionPane.showMessageDialog(this, "Profile update dialog would open here.");
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Logged out successfully.");
        dispose(); // Close the dashboard
    }

    public void addRecord(String documentName, String submissionDate, String category, String status,
                          String expirationDate, String supervisorName) {
        Object[] rowData = {documentName, submissionDate, category, status, expirationDate, supervisorName, "Action"};
        tableModel.addRow(rowData);
    }

    public void addClient(String clientName) {
        if (!clients.contains(clientName)) {
            clients.add(clientName);
            clientCountLabel.setText("Total Number of Clients: " + clients.size());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdministratorDashboard dashboard = new AdministratorDashboard();
            dashboard.setVisible(true);
            // Sample data for testing
            dashboard.addRecord("Report.docx", "2023-10-01", "Reports", "Submitted", "2024-10-01", "John Doe");
            dashboard.addRecord("Invoice.pdf", "2023-09-20", "Invoices", "Pending", "2023-12-20", "Jane Smith");
            dashboard.addClient("Client A");
            dashboard.addClient("Client B");
        });
    }
}

// Custom renderer for the action buttons
class ButtonRenderer extends ButtonRenderer2 {
    public ButtonRenderer() {
        setOpaque(true);
    }

}

// Custom editor for the action buttons
class ActionButtonEditor extends DefaultCellEditor {
    private JButton button;
    private boolean isPushed;

    public ActionButtonEditor(JCheckBox checkBox, JTable recordsTable) {
        super(checkBox);
        button = new JButton("Action");
        button.setOpaque(true);
        button.addActionListener(e -> showActionMenu(e, recordsTable));
    }

    private void showActionMenu(ActionEvent e, JTable recordsTable) {
        // Get the row index
        int row = recordsTable.getSelectedRow();
        if (row >= 0) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem viewItem = new JMenuItem("View");
            viewItem.addActionListener(ev -> JOptionPane.showMessageDialog(button, "Viewing document: " + recordsTable.getValueAt(row, 0)));

            JMenuItem approveItem = new JMenuItem("Approve");
            approveItem.addActionListener(ev -> JOptionPane.showMessageDialog(button, "Approved document: " + recordsTable.getValueAt(row, 0)));

            JMenuItem disapproveItem = new JMenuItem("Disapprove");
            disapproveItem.addActionListener(ev -> JOptionPane.showMessageDialog(button, "Disapproved document: " + recordsTable.getValueAt(row, 0)));

            menu.add(viewItem);
            menu.add(approveItem);
            menu.add(disapproveItem);
            menu.show(button, button.getWidth() / 2, button.getHeight() / 2);
        }
    }

//    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row) {
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            // Additional actions can be performed here if needed
        }
        isPushed = false;
        return button.getText();
    }
}
