import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ClientDashboard extends JFrame {

    private JTable recordsTable;
    private DefaultTableModel tableModel;

    public ClientDashboard() {
        setTitle("Client Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Full-screen mode
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Create main panel and header
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createRecordsTable(), BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerPanel.add(createButton("Update Profile", Color.decode("#2196F3"), e -> showUpdateProfileDialog()));
        headerPanel.add(createButton("Logout", Color.decode("#f44336"), e -> logout()));
        headerPanel.add(createButton("Upload Document", Color.decode("#4CAF50"), e -> showUploadDialog()));
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

        recordsTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        return new JScrollPane(recordsTable);
    }

    private void showUploadDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a CSV File");
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToUpload = fileChooser.getSelectedFile();
            processUploadedFile(fileToUpload);
        }
    }

    private void processUploadedFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(","); // Assuming CSV format
                if (data.length == 6) {
                    addRecord(data[0], data[1], data[2], data[3], data[4], data[5]);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Logged out successfully.");
        dispose(); // Close the dashboard
    }

    private void showUpdateProfileDialog() {
        JOptionPane.showMessageDialog(this, "Profile update dialog would open here.");
    }

    public void addRecord(String documentName, String submissionDate, String category, String status,
                          String expirationDate, String supervisorName) {
        Object[] rowData = {documentName, submissionDate, category, status, expirationDate, supervisorName, "Delete"};
        tableModel.addRow(rowData);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientDashboard dashboard = new ClientDashboard();
            dashboard.setVisible(true);
            // Sample data for testing
            dashboard.addRecord("Report.docx", "2023-10-01", "Reports", "Submitted", "2024-10-01", "John Doe");
            dashboard.addRecord("Invoice.pdf", "2023-09-20", "Invoices", "Pending", "2023-12-20", "Jane Smith");
        });
    }
}

// Custom renderer for the delete button


// Custom editor for the delete button
class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean isPushed;

    public ButtonEditor(JCheckBox checkBox) {
        super(checkBox);
        button = new JButton();
        button.setOpaque(true);
//        button.addActionListener(this::actionPerformed);
    }

//    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row) {
        label = (value == null) ? "Delete" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            // Additional actions can be performed here if needed
        }
        isPushed = false;
        return label;
    }

//    private void actionPerformed(ActionEvent e) {
//        fireEditingStopped();
//        int row = button.getParent().getParent().getParent().mouseEnter();
//        if (row >= 0) {
//            ((DefaultTableModel) button.getParent().getParent().getModel()).removeRow(row);
//        }
    }

