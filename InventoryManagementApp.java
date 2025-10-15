import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class InventoryManagementApp {
    // Table columns
    private static final String[] COLUMN_NAMES = {
        "SKU", "Item Name", "Quantity", "Cost Price", "Selling Price",
        "Category", "Location", "Min Stock Threshold"
    };
    private int skuCounter = 1;

    // Predefined categories and locations
    private static final String[] CATEGORIES = {
        "Electronics", "Clothing", "Food", "Other"
    };
    private static final String[] LOCATIONS = {
        "Warehouse A", "Warehouse B", "Shelf 1", "Shelf 2", "Other"
    };

    // Table model and sales log
    private DefaultTableModel model;
    private JTable inventoryTable;
    private List<SaleRecord> salesLog = new ArrayList<>();

    // For search/filter
    private TableRowSorter<DefaultTableModel> sorter;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryManagementApp().createAndShowGUI());
    }

    // Sale record for reporting
    private static class SaleRecord {
        String sku, name, category;
        int quantity;
        double sellingPrice;
        String timestamp;
        SaleRecord(String sku, String name, String category, int quantity, double sellingPrice, String timestamp) {
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.sellingPrice = sellingPrice;
            this.timestamp = timestamp;
        }
    }

    private void updateNameComboBox(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        for (int i = 0; i < model.getRowCount(); i++) {
            comboBox.addItem(model.getValueAt(i, 1).toString());
        }
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Inventory Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Table Model shared across tabs
        model = new DefaultTableModel(COLUMN_NAMES, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        inventoryTable = new JTable(model);
        inventoryTable.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>(model);
        inventoryTable.setRowSorter(sorter);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setFillsViewportHeight(true);

        // Custom cell renderer for low-stock highlighting
        inventoryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                try {
                    int qty = Integer.parseInt(model.getValueAt(modelRow, 2).toString());
                    int min = Integer.parseInt(model.getValueAt(modelRow, 7).toString());
                    if (qty <= min) {
                        c.setBackground(new Color(255, 200, 200));
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    }
                } catch (Exception ex) {
                    c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                }
                return c;
            }
        });

        // Entry Tab (GridBagLayout)
        JPanel entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField itemNameField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField costField = new JTextField();
        JTextField sellField = new JTextField();
        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        JComboBox<String> locationBox = new JComboBox<>(LOCATIONS);
        JTextField minStockField = new JTextField();
        JButton addButton = new JButton("Add Item");

        itemNameField.setToolTipText("Enter item name");
        quantityField.setToolTipText("Enter quantity (positive integer)");
        costField.setToolTipText("Enter cost price (e.g., 10.50)");
        sellField.setToolTipText("Enter selling price (e.g., 15.00)");
        categoryBox.setToolTipText("Select category");
        locationBox.setToolTipText("Select location");
        minStockField.setToolTipText("Enter minimum stock threshold");
        addButton.setToolTipText("Add item to inventory");

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1; entryPanel.add(itemNameField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; entryPanel.add(quantityField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Cost Price:"), gbc);
        gbc.gridx = 1; entryPanel.add(costField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Selling Price:"), gbc);
        gbc.gridx = 1; entryPanel.add(sellField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; entryPanel.add(categoryBox, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; entryPanel.add(locationBox, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; entryPanel.add(new JLabel("Min Stock Threshold:"), gbc);
        gbc.gridx = 1; entryPanel.add(minStockField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; entryPanel.add(addButton, gbc);

        addButton.addActionListener(e -> {
            String name = itemNameField.getText().trim();
            String qtyStr = quantityField.getText().trim();
            String costStr = costField.getText().trim();
            String sellStr = sellField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            String location = (String) locationBox.getSelectedItem();
            String minStr = minStockField.getText().trim();
            // Validation
            if (name.isEmpty() || qtyStr.isEmpty() || costStr.isEmpty() || sellStr.isEmpty() || minStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields!"); return;
            }
            int qty, min;
            double cost, sell;
            try {
                qty = Integer.parseInt(qtyStr); min = Integer.parseInt(minStr);
                cost = Double.parseDouble(costStr); sell = Double.parseDouble(sellStr);
                if (qty < 0 || min < 0 || cost < 0 || sell < 0) throw new Exception();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid numeric values. All must be positive."); return;
            }
            String sku = String.format("UQ%03d", skuCounter++);
            model.addRow(new Object[]{sku, name, qty, cost, sell, category, location, min});
            itemNameField.setText(""); quantityField.setText(""); costField.setText(""); sellField.setText(""); minStockField.setText("");
        });

        tabbedPane.addTab("Entry", entryPanel);

        // Inventory Tab
        JPanel listPanel = new JPanel(new BorderLayout(10,10));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Search by Name, SKU, or Category");
        JButton lowStockButton = new JButton("Check Low Stock");
        lowStockButton.setToolTipText("Show items with low stock");
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setToolTipText("Delete selected item");

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(lowStockButton);
        searchPanel.add(deleteButton);

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

        deleteButton.addActionListener(e -> {
            int selectedRow = inventoryTable.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
                model.removeRow(modelRow);
            } else {
                JOptionPane.showMessageDialog(frame, "Please select an item to delete.");
            }
        });

        lowStockButton.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                int qty = Integer.parseInt(model.getValueAt(i, 2).toString());
                int min = Integer.parseInt(model.getValueAt(i, 7).toString());
                if (qty <= min) {
                    sb.append("SKU: ").append(model.getValueAt(i, 0)).append(", Name: ").append(model.getValueAt(i, 1)).append(", Qty: ").append(qty).append(", Min: ").append(min).append("\n");
                }
            }
            if (sb.length() == 0) sb.append("No low-stock items.");
            JOptionPane.showMessageDialog(frame, sb.toString(), "Low Stock Items", JOptionPane.INFORMATION_MESSAGE);
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 5));
                }
            }
        });

        tabbedPane.addTab("Inventory", listPanel);

        // Add restock controls to Entry tab
        JComboBox<String> restockSKUBox = new JComboBox<>();
        JTextField restockQtyField = new JTextField();
        JButton restockButton = new JButton("Restock Item");
        restockButton.setToolTipText("Add stock to existing item");

        model.addTableModelListener(e -> {
            restockSKUBox.removeAllItems();
            for (int i = 0; i < model.getRowCount(); i++) {
                restockSKUBox.addItem(model.getValueAt(i, 0).toString());
            }
        });

        GridBagConstraints rbc = new GridBagConstraints();
        rbc.insets = new Insets(5, 5, 5, 5);
        rbc.fill = GridBagConstraints.HORIZONTAL;
        int restockRow = 0;
        rbc.gridx = 2; rbc.gridy = restockRow; rbc.gridwidth = 1; entryPanel.add(new JLabel("Restock Existing Item (SKU):"), rbc);
        rbc.gridx = 3; entryPanel.add(restockSKUBox, rbc);
        restockRow++;
        rbc.gridx = 2; rbc.gridy = restockRow; entryPanel.add(new JLabel("Quantity to Add:"), rbc);
        rbc.gridx = 3; entryPanel.add(restockQtyField, rbc);
        restockRow++;
        rbc.gridx = 2; rbc.gridy = restockRow; rbc.gridwidth = 2; entryPanel.add(restockButton, rbc);

        restockButton.addActionListener(e -> {
            String sku = (String) restockSKUBox.getSelectedItem();
            String qtyStr = restockQtyField.getText().trim();
            if (sku == null || qtyStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select SKU and enter quantity."); return;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); if (qty <= 0) throw new Exception(); } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid quantity."); return;
            }
            for (int i = 0; i < model.getRowCount(); i++) {
                if (sku.equals(model.getValueAt(i, 0))) {
                    int currentQty = Integer.parseInt(model.getValueAt(i, 2).toString());
                    model.setValueAt(currentQty + qty, i, 2);
                    JOptionPane.showMessageDialog(frame, "Stock updated. New quantity: " + (currentQty + qty));
                    restockQtyField.setText("");
                    break;
                }
            }
        });

        // Exit Tab (Sales)
        JPanel exitPanel = new JPanel(new GridBagLayout());
        exitPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints xgbc = new GridBagConstraints();
        xgbc.insets = new Insets(5, 5, 5, 5);
        xgbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<String> exitComboBox = new JComboBox<>();
        JTextField exitQtyField = new JTextField();
        JButton exitButton = new JButton("Confirm Sale");
        exitButton.setToolTipText("Sell item (reduce stock)");

        updateNameComboBox(exitComboBox);
        model.addTableModelListener(e -> updateNameComboBox(exitComboBox));

        int xrow = 0;
        xgbc.gridx = 0; xgbc.gridy = xrow; exitPanel.add(new JLabel("Select Item:"), xgbc);
        xgbc.gridx = 1; exitPanel.add(exitComboBox, xgbc);
        xrow++;
        xgbc.gridx = 0; xgbc.gridy = xrow; exitPanel.add(new JLabel("Quantity to Sell:"), xgbc);
        xgbc.gridx = 1; exitPanel.add(exitQtyField, xgbc);
        xrow++;
        xgbc.gridx = 0; xgbc.gridy = xrow; xgbc.gridwidth = 2; exitPanel.add(exitButton, xgbc);

        exitButton.addActionListener(e -> {
            String name = (String) exitComboBox.getSelectedItem();
            String qtyStr = exitQtyField.getText().trim();
            if (name == null || qtyStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select item and enter quantity."); return;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); if (qty <= 0) throw new Exception(); } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid quantity."); return;
            }
            for (int i = 0; i < model.getRowCount(); i++) {
                if (name.equals(model.getValueAt(i, 1))) {
                    int currentQty = Integer.parseInt(model.getValueAt(i, 2).toString());
                    if (qty > currentQty) {
                        JOptionPane.showMessageDialog(frame, "Sale quantity exceeds stock."); return;
                    }
                    model.setValueAt(currentQty - qty, i, 2);
                    // Log sale
                    String sku = model.getValueAt(i, 0).toString();
                    String category = model.getValueAt(i, 5).toString();
                    double sellPrice = Double.parseDouble(model.getValueAt(i, 4).toString());
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    this.salesLog.add(new SaleRecord(sku, name, category, qty, sellPrice, timestamp));
                    JOptionPane.showMessageDialog(frame, "Sale confirmed. Remaining stock: " + (currentQty - qty));
                    exitQtyField.setText("");
                    break;
                }
            }
        });

        tabbedPane.addTab("Exit", exitPanel);

        // Reports Tab
        JPanel reportsPanel = new JPanel(new BorderLayout(10,10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton stockSummaryButton = new JButton("Stock Summary");
        JButton salesReportButton = new JButton("Sales Report");
        JButton exportCSVButton = new JButton("Save to CSV");
        buttonPanel.add(stockSummaryButton);
        buttonPanel.add(salesReportButton);
        buttonPanel.add(exportCSVButton);
        reportsPanel.add(buttonPanel, BorderLayout.NORTH);

        // Table for sales report
        String[] salesColumns = {"Time", "SKU", "Name", "Category", "Qty", "Selling Price"};
        DefaultTableModel salesModel = new DefaultTableModel(salesColumns, 0);
        JTable salesTable = new JTable(salesModel);
        JScrollPane salesScroll = new JScrollPane(salesTable);

        salesReportButton.addActionListener(e -> {
            salesModel.setRowCount(0);
            for (SaleRecord sr : salesLog) {
                salesModel.addRow(new Object[]{sr.timestamp, sr.sku, sr.name, sr.category, sr.quantity, sr.sellingPrice});
            }
            JOptionPane.showMessageDialog(frame, salesScroll, "Sales Report", JOptionPane.INFORMATION_MESSAGE);
        });

        stockSummaryButton.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                sb.append("SKU: ").append(model.getValueAt(i, 0))
                  .append(", Name: ").append(model.getValueAt(i, 1))
                  .append(", Qty: ").append(model.getValueAt(i, 2))
                  .append(", Category: ").append(model.getValueAt(i, 5))
                  .append("\n");
            }
            if (sb.length() == 0) sb.append("No items in inventory.");
            JOptionPane.showMessageDialog(frame, sb.toString(), "Stock Summary", JOptionPane.INFORMATION_MESSAGE);
        });

        exportCSVButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int ret = chooser.showSaveDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (PrintWriter pw = new PrintWriter(file)) {
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        pw.print(model.getColumnName(i));
                        if (i < model.getColumnCount() - 1) pw.print(",");
                    }
                    pw.println();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        for (int j = 0; j < model.getColumnCount(); j++) {
                            pw.print(model.getValueAt(i, j));
                            if (j < model.getColumnCount() - 1) pw.print(",");
                        }
                        pw.println();
                    }
                    JOptionPane.showMessageDialog(frame, "Exported to CSV!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error exporting CSV: " + ex.getMessage());
                }
            }
        });

        tabbedPane.addTab("Reports", reportsPanel);

        frame.setContentPane(tabbedPane);
        frame.setVisible(true);
    }
}