
package pharmacybillingsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Pharmacy Billing System");
            frame.setSize(900, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            JLabel heading = new JLabel(
                "PHARMACY BILLING SYSTEM",
                SwingConstants.CENTER
            );
            heading.setFont(new Font("Arial", Font.BOLD, 26));

            JButton dashboard = new JButton("Dashboard");
            JButton medicine = new JButton("Manage Medicines");
            JButton billing = new JButton("New Billing");
            JButton history = new JButton("Bill History");
            JButton exit = new JButton("Exit");

            JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
            panel.add(dashboard);
            panel.add(medicine);
            panel.add(billing);
            panel.add(history);
            panel.add(exit);

            dashboard.addActionListener(e ->
                JOptionPane.showMessageDialog(
                    frame, "Welcome to Pharmacy Dashboard"
                )
            );

            medicine.addActionListener(e -> showMedicineWindow());

            billing.addActionListener(e -> showBillingWindow());

            history.addActionListener(e -> showBillHistory());

            exit.addActionListener(e -> System.exit(0));

            frame.add(heading, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    // DATABASE CONNECTION

    private static Connection openConnection() throws SQLException {

        Connection con = DBConnection.getConnection();

        if (con == null) {
            throw new SQLException("Database connection failed!");
        }

        return con;
    }

    // MEDICINE MANAGEMENT WINDOW

    public static void showMedicineWindow() {

        JFrame medicineFrame = new JFrame("Medicine Management");
        medicineFrame.setSize(800, 500);
        medicineFrame.setLocationRelativeTo(null);
        medicineFrame.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
            "MEDICINE MANAGEMENT",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));

        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField expiryField = new JTextField();

        form.add(new JLabel("Medicine Name:"));
        form.add(nameField);
        form.add(new JLabel("Price:"));
        form.add(priceField);
        form.add(new JLabel("Quantity:"));
        form.add(quantityField);
        form.add(new JLabel("Expiry Date (YYYY-MM-DD):"));
        form.add(expiryField);

        JButton addButton = new JButton("Add Medicine");
        JButton updateButton = new JButton("Update Medicine");
        JButton deleteButton = new JButton("Delete Medicine");
        JButton clearButton = new JButton("Clear");

        form.add(addButton);
        form.add(updateButton);
        form.add(deleteButton);
        form.add(clearButton);

        JPanel leftPanel = new JPanel(new BorderLayout());
        form.setPreferredSize(new Dimension(400, 250));
        leftPanel.add(form, BorderLayout.NORTH);

        String[] columns = {
            "ID", "Medicine Name", "Price",
            "Quantity", "Expiry Date"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        loadMedicines(model);

        // ADD MEDICINE

        addButton.addActionListener(e -> {

            try {

                String name = nameField.getText().trim();
                String priceText = priceField.getText().trim();
                String quantityText = quantityField.getText().trim();
                String expiryText = expiryField.getText().trim();

                if (name.isEmpty() || priceText.isEmpty()
                        || quantityText.isEmpty()
                        || expiryText.isEmpty()) {

                    JOptionPane.showMessageDialog(
                        medicineFrame, "Please fill all fields!"
                    );
                    return;
                }

                double price = Double.parseDouble(priceText);
                int quantity = Integer.parseInt(quantityText);
                Date expiry = Date.valueOf(expiryText);

                if (!Double.isFinite(price) || price <= 0 || quantity < 0) {
                    JOptionPane.showMessageDialog(
                        medicineFrame, "Enter valid price and quantity!"
                    );
                    return;
                }

                String sql = "INSERT INTO medicines "
                    + "(name, price, quantity, expiry_date) "
                    + "VALUES (?, ?, ?, ?)";

                try (Connection con = openConnection();
                     PreparedStatement pst = con.prepareStatement(sql)) {

                    pst.setString(1, name);
                    pst.setDouble(2, price);
                    pst.setInt(3, quantity);
                    pst.setDate(4, expiry);
                    pst.executeUpdate();
                }

                loadMedicines(model);
                clearFields(nameField, priceField, quantityField, expiryField);
                table.clearSelection();

                JOptionPane.showMessageDialog(
                    medicineFrame, "Medicine Added Successfully!"
                );

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Enter valid price and quantity!"
                );
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Expiry date must be YYYY-MM-DD!"
                );
            } catch (SQLException ex) {
                showDatabaseError(medicineFrame, ex);
            }
        });

        // SELECT MEDICINE

        table.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {

                int row = table.getSelectedRow();

                if (row != -1) {
                    nameField.setText(model.getValueAt(row, 1).toString());
                    priceField.setText(model.getValueAt(row, 2).toString());
                    quantityField.setText(model.getValueAt(row, 3).toString());
                    expiryField.setText(model.getValueAt(row, 4).toString());
                }
            }
        });

        // UPDATE MEDICINE

        updateButton.addActionListener(e -> {

            int row = table.getSelectedRow();

            if (row == -1) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Please select a medicine!"
                );
                return;
            }

            try {

                int id = Integer.parseInt(
                    model.getValueAt(row, 0).toString()
                );

                String name = nameField.getText().trim();
                String priceText = priceField.getText().trim();
                String quantityText = quantityField.getText().trim();
                String expiryText = expiryField.getText().trim();

                if (name.isEmpty() || priceText.isEmpty()
                        || quantityText.isEmpty()
                        || expiryText.isEmpty()) {

                    JOptionPane.showMessageDialog(
                        medicineFrame, "Please fill all fields!"
                    );
                    return;
                }

                double price = Double.parseDouble(priceText);
                int quantity = Integer.parseInt(quantityText);
                Date expiry = Date.valueOf(expiryText);

                if (!Double.isFinite(price) || price <= 0 || quantity < 0) {
                    JOptionPane.showMessageDialog(
                        medicineFrame, "Enter valid price and quantity!"
                    );
                    return;
                }

                String sql = "UPDATE medicines SET "
                    + "name=?, price=?, quantity=?, expiry_date=? "
                    + "WHERE id=?";

                try (Connection con = openConnection();
                     PreparedStatement pst = con.prepareStatement(sql)) {

                    pst.setString(1, name);
                    pst.setDouble(2, price);
                    pst.setInt(3, quantity);
                    pst.setDate(4, expiry);
                    pst.setInt(5, id);

                    int updated = pst.executeUpdate();

                    if (updated == 0) {
                        JOptionPane.showMessageDialog(
                            medicineFrame, "Medicine record not found!"
                        );
                        return;
                    }
                }

                loadMedicines(model);
                clearFields(nameField, priceField, quantityField, expiryField);
                table.clearSelection();

                JOptionPane.showMessageDialog(
                    medicineFrame, "Medicine Updated Successfully!"
                );

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Enter valid price and quantity!"
                );
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Expiry date must be YYYY-MM-DD!"
                );
            } catch (SQLException ex) {
                showDatabaseError(medicineFrame, ex);
            }
        });

        // DELETE MEDICINE

        deleteButton.addActionListener(e -> {

            int row = table.getSelectedRow();

            if (row == -1) {
                JOptionPane.showMessageDialog(
                    medicineFrame, "Please select a medicine!"
                );
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                medicineFrame,
                "Do you want to delete this medicine?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {

                try {

                    int id = Integer.parseInt(
                        model.getValueAt(row, 0).toString()
                    );

                    String sql = "DELETE FROM medicines WHERE id=?";

                    try (Connection con = openConnection();
                         PreparedStatement pst = con.prepareStatement(sql)) {

                        pst.setInt(1, id);
                        int deleted = pst.executeUpdate();

                        if (deleted == 0) {
                            JOptionPane.showMessageDialog(
                                medicineFrame, "Medicine record not found!"
                            );
                            return;
                        }
                    }

                    loadMedicines(model);
                    clearFields(nameField, priceField, quantityField, expiryField);
                    table.clearSelection();

                    JOptionPane.showMessageDialog(
                        medicineFrame, "Medicine Deleted Successfully!"
                    );

                } catch (SQLException ex) {
                    showDatabaseError(medicineFrame, ex);
                }
            }
        });

        // CLEAR FIELDS

        clearButton.addActionListener(e -> {
            clearFields(nameField, priceField, quantityField, expiryField);
            table.clearSelection();
        });

        medicineFrame.add(heading, BorderLayout.NORTH);
        medicineFrame.add(leftPanel, BorderLayout.WEST);
        medicineFrame.add(scrollPane, BorderLayout.CENTER);
        medicineFrame.setVisible(true);
    }

    // LOAD MEDICINES FROM MYSQL

    private static void loadMedicines(DefaultTableModel model) {

        model.setRowCount(0);

        String sql = "SELECT * FROM medicines ORDER BY id";

        try (Connection con = openConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity"),
                    rs.getDate("expiry_date")
                });
            }

        } catch (SQLException ex) {
            showDatabaseError(null, ex);
        }
    }

    // CLEAR INPUT FIELDS

    private static void clearFields(
        JTextField name,
        JTextField price,
        JTextField quantity,
        JTextField expiry
    ) {
        name.setText("");
        price.setText("");
        quantity.setText("");
        expiry.setText("");
    }

    // SHOW DATABASE ERROR

    private static void showDatabaseError(
        Component parent, SQLException ex
    ) {

        JOptionPane.showMessageDialog(
            parent,
            "Database Error: " + ex.getMessage(),
            "MySQL Error",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }

    // BILLING MEDICINE DATA

    static class MedicineOption {

        int id;
        String name;
        double price;
        int stock;

        MedicineOption(int id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return name + " | Rs. " + price + " | Stock: " + stock;
        }
    }

    static class CartItem {

        int medicineId;
        String medicineName;
        double price;
        int quantity;

        CartItem(int medicineId, String medicineName,
                 double price, int quantity) {

            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.price = price;
            this.quantity = quantity;
        }

        double getTotal() {
            return price * quantity;
        }
    }

    private static double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    // NEW BILLING WINDOW

    public static void showBillingWindow() {

        JFrame billingFrame = new JFrame("New Pharmacy Bill");
        billingFrame.setSize(950, 650);
        billingFrame.setLocationRelativeTo(null);
        billingFrame.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
            "GENERATE NEW BILL",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        // CUSTOMER DETAILS

        JPanel customerPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        customerPanel.setBorder(
            BorderFactory.createTitledBorder("Customer Details")
        );

        JTextField customerField = new JTextField();
        JComboBox<MedicineOption> medicineCombo = new JComboBox<>();

        customerPanel.add(new JLabel("Customer Name:"));
        customerPanel.add(customerField);
        customerPanel.add(new JLabel("Select Medicine:"));
        customerPanel.add(medicineCombo);

        // MEDICINE DETAILS

        JPanel detailsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        detailsPanel.setBorder(
            BorderFactory.createTitledBorder("Medicine Details")
        );

        JTextField priceField = new JTextField();
        JTextField stockField = new JTextField();
        JTextField quantityField = new JTextField("1");

        priceField.setEditable(false);
        stockField.setEditable(false);

        detailsPanel.add(new JLabel("Price:"));
        detailsPanel.add(priceField);
        detailsPanel.add(new JLabel("Available Stock:"));
        detailsPanel.add(stockField);
        detailsPanel.add(new JLabel("Purchase Quantity:"));
        detailsPanel.add(quantityField);

        JButton addButton = new JButton("Add to Bill");
        JButton removeButton = new JButton("Remove Selected");

        detailsPanel.add(addButton);
        detailsPanel.add(removeButton);

        // CART TABLE

        String[] columns = {
            "Medicine ID", "Medicine Name",
            "Price", "Quantity", "Total"
        };

        DefaultTableModel cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable cartTable = new JTable(cartModel);
        JScrollPane cartScroll = new JScrollPane(cartTable);

        // BILL SUMMARY

        JPanel totalPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        totalPanel.setBorder(
            BorderFactory.createTitledBorder("Bill Summary")
        );

        JLabel subtotalLabel = new JLabel("Rs. 0.00");
        JLabel gstLabel = new JLabel("Rs. 0.00");
        JLabel grandTotalLabel = new JLabel("Rs. 0.00");

        totalPanel.add(new JLabel("Subtotal:"));
        totalPanel.add(subtotalLabel);
        totalPanel.add(new JLabel("GST (5% Demo Rate):"));
        totalPanel.add(gstLabel);
        totalPanel.add(new JLabel("Grand Total:"));
        totalPanel.add(grandTotalLabel);

        JButton generateButton = new JButton("Generate Bill");
        JButton clearButton = new JButton("Clear Bill");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(totalPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        actionPanel.add(generateButton);
        actionPanel.add(clearButton);

        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        List<CartItem> cart = new ArrayList<>();

        // LOAD MEDICINES

        Runnable loadBillingMedicines = () -> {

            medicineCombo.removeAllItems();

            try (Connection con = openConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id, name, price, quantity "
                     + "FROM medicines ORDER BY name"
                 )) {

                while (rs.next()) {

                    medicineCombo.addItem(
                        new MedicineOption(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                        )
                    );
                }

            } catch (SQLException ex) {
                showDatabaseError(billingFrame, ex);
            }
        };

        loadBillingMedicines.run();

        // UPDATE PRICE AND STOCK

        medicineCombo.addActionListener(e -> {

            MedicineOption selected =
                (MedicineOption) medicineCombo.getSelectedItem();

            if (selected != null) {
                priceField.setText(String.valueOf(selected.price));
                stockField.setText(String.valueOf(selected.stock));
            } else {
                priceField.setText("");
                stockField.setText("");
            }
        });

        // CALCULATE TOTALS

        Runnable calculateTotals = () -> {

            double subtotal = 0;

            for (CartItem item : cart) {
                subtotal += item.getTotal();
            }

            subtotal = roundMoney(subtotal);

            double gst = roundMoney(subtotal * 0.05);
            double grandTotal = roundMoney(subtotal + gst);

            subtotalLabel.setText("Rs. " + subtotal);
            gstLabel.setText("Rs. " + gst);
            grandTotalLabel.setText("Rs. " + grandTotal);
        };

        // REFRESH CART

        Runnable refreshCart = () -> {

            cartModel.setRowCount(0);

            for (CartItem item : cart) {
                cartModel.addRow(new Object[]{
                    item.medicineId,
                    item.medicineName,
                    item.price,
                    item.quantity,
                    roundMoney(item.getTotal())
                });
            }

            calculateTotals.run();
        };

        // ADD TO BILL

        addButton.addActionListener(e -> {

            MedicineOption selected =
                (MedicineOption) medicineCombo.getSelectedItem();

            if (selected == null) {
                JOptionPane.showMessageDialog(
                    billingFrame, "No medicine available!"
                );
                return;
            }

            try {

                int quantity = Integer.parseInt(
                    quantityField.getText().trim()
                );

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(
                        billingFrame,
                        "Quantity must be greater than zero!"
                    );
                    return;
                }

                int existingQuantity = 0;

                for (CartItem item : cart) {
                    if (item.medicineId == selected.id) {
                        existingQuantity = item.quantity;
                        break;
                    }
                }

                if (existingQuantity + quantity > selected.stock) {
                    JOptionPane.showMessageDialog(
                        billingFrame,
                        "Insufficient medicine stock!"
                    );
                    return;
                }

                boolean found = false;

                for (CartItem item : cart) {

                    if (item.medicineId == selected.id) {
                        item.quantity += quantity;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    cart.add(
                        new CartItem(
                            selected.id,
                            selected.name,
                            selected.price,
                            quantity
                        )
                    );
                }

                refreshCart.run();
                quantityField.setText("1");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    billingFrame, "Enter a valid quantity!"
                );
            }
        });

        // REMOVE SELECTED ITEM

        removeButton.addActionListener(e -> {

            int row = cartTable.getSelectedRow();

            if (row == -1) {
                JOptionPane.showMessageDialog(
                    billingFrame, "Please select an item to remove!"
                );
                return;
            }

            cart.remove(row);
            refreshCart.run();
        });

        // CLEAR BILL

        clearButton.addActionListener(e -> {

            int confirm = JOptionPane.showConfirmDialog(
                billingFrame,
                "Clear all items from this bill?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                cart.clear();
                refreshCart.run();
                customerField.setText("");
                quantityField.setText("1");
            }
        });

        // GENERATE AND SAVE BILL

        generateButton.addActionListener(e -> {

            String customerName = customerField.getText().trim();

            if (customerName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    billingFrame, "Please enter customer name!"
                );
                return;
            }

            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(
                    billingFrame, "Please add at least one medicine!"
                );
                return;
            }

            double subtotal = 0;

            for (CartItem item : cart) {
                subtotal += item.getTotal();
            }

            subtotal = roundMoney(subtotal);

            double gst = roundMoney(subtotal * 0.05);
            double grandTotal = roundMoney(subtotal + gst);

            String insertBill =
                "INSERT INTO bills "
                + "(customer_name, subtotal, gst_amount, grand_total) "
                + "VALUES (?, ?, ?, ?)";

            String insertItem =
                "INSERT INTO bill_items "
                + "(bill_id, medicine_id, medicine_name, price, quantity, total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

            String updateStock =
                "UPDATE medicines SET quantity = quantity - ? "
                + "WHERE id = ? AND quantity >= ?";

            int billId;

            try (Connection con = openConnection()) {

                con.setAutoCommit(false);

                try {

                    // SAVE BILL

                    try (PreparedStatement pst = con.prepareStatement(
                            insertBill, Statement.RETURN_GENERATED_KEYS)) {

                        pst.setString(1, customerName);
                        pst.setDouble(2, subtotal);
                        pst.setDouble(3, gst);
                        pst.setDouble(4, grandTotal);

                        pst.executeUpdate();

                        try (ResultSet keys = pst.getGeneratedKeys()) {

                            if (!keys.next()) {
                                throw new SQLException(
                                    "Bill ID could not be generated!"
                                );
                            }

                            billId = keys.getInt(1);
                        }
                    }

                    // SAVE ITEMS AND UPDATE STOCK

                    for (CartItem item : cart) {

                        try (PreparedStatement pst =
                                con.prepareStatement(updateStock)) {

                            pst.setInt(1, item.quantity);
                            pst.setInt(2, item.medicineId);
                            pst.setInt(3, item.quantity);

                            int updated = pst.executeUpdate();

                            if (updated == 0) {
                                throw new SQLException(
                                    "Insufficient stock for "
                                    + item.medicineName
                                );
                            }
                        }

                        try (PreparedStatement pst =
                                con.prepareStatement(insertItem)) {

                            pst.setInt(1, billId);
                            pst.setInt(2, item.medicineId);
                            pst.setString(3, item.medicineName);
                            pst.setDouble(4, item.price);
                            pst.setInt(5, item.quantity);
                            pst.setDouble(6, roundMoney(item.getTotal()));

                            pst.executeUpdate();
                        }
                    }

                    con.commit();

                } catch (Exception ex) {

                    try {
                        con.rollback();
                    } catch (SQLException rollbackEx) {
                        ex.addSuppressed(rollbackEx);
                    }

                    throw ex;
                }

            } catch (Exception ex) {

                JOptionPane.showMessageDialog(
                    billingFrame,
                    "Bill could not be generated: " + ex.getMessage(),
                    "Billing Error",
                    JOptionPane.ERROR_MESSAGE
                );

                ex.printStackTrace();
                return;
            }

            // DISPLAY RECEIPT AFTER SUCCESSFUL SAVE

            StringBuilder receipt = new StringBuilder();

            receipt.append("PHARMACY BILLING SYSTEM\n");
            receipt.append("--------------------------------\n");
            receipt.append("Bill ID: ").append(billId).append("\n");
            receipt.append("Customer: ").append(customerName).append("\n");
            receipt.append("--------------------------------\n");
            receipt.append("Medicine          Qty      Total\n");

            for (CartItem item : cart) {
                receipt.append(item.medicineName)
                       .append("     ")
                       .append(item.quantity)
                       .append("     Rs. ")
                       .append(roundMoney(item.getTotal()))
                       .append("\n");
            }

            receipt.append("--------------------------------\n");
            receipt.append("Subtotal: Rs. ").append(subtotal).append("\n");
            receipt.append("GST (5%): Rs. ").append(gst).append("\n");
            receipt.append("Grand Total: Rs. ")
                   .append(grandTotal).append("\n");

            JTextArea receiptArea = new JTextArea(receipt.toString());
            receiptArea.setEditable(false);
            receiptArea.setFont(
                new Font("Monospaced", Font.PLAIN, 14)
            );

            JOptionPane.showMessageDialog(
                billingFrame,
                new JScrollPane(receiptArea),
                "Bill Generated Successfully",
                JOptionPane.INFORMATION_MESSAGE
            );

            cart.clear();
            refreshCart.run();
            customerField.setText("");
            quantityField.setText("1");

            loadBillingMedicines.run();
        });

        // WINDOW LAYOUT

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(heading, BorderLayout.NORTH);
        topPanel.add(customerPanel, BorderLayout.CENTER);
        topPanel.add(detailsPanel, BorderLayout.SOUTH);

        billingFrame.add(topPanel, BorderLayout.NORTH);
        billingFrame.add(cartScroll, BorderLayout.CENTER);
        billingFrame.add(bottomPanel, BorderLayout.SOUTH);

        billingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        billingFrame.setVisible(true);
    }

    // ==========================================
    // BILL HISTORY MODULE
    // ==========================================

    public static void showBillHistory() {

        JFrame historyFrame = new JFrame("Bill History");
        historyFrame.setSize(1000, 600);
        historyFrame.setLocationRelativeTo(null);
        historyFrame.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
            "BILL HISTORY",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));

        // SEARCH PANEL

        JPanel searchPanel = new JPanel(new FlowLayout());

        JTextField searchField = new JTextField(25);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        searchPanel.add(new JLabel("Bill ID / Customer Name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);

        // BILL TABLE

        String[] columns = {
            "Bill ID",
            "Customer Name",
            "Bill Date",
            "Subtotal",
            "GST",
            "Grand Total"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);

        // LOAD BILLS FROM DATABASE

        Runnable loadBills = () -> {

            model.setRowCount(0);

            String search = searchField.getText().trim();

            String sql =
                "SELECT * FROM bills "
                + "WHERE CAST(bill_id AS CHAR) LIKE ? "
                + "OR customer_name LIKE ? "
                + "ORDER BY bill_id DESC";

            try (Connection con = openConnection();
                 PreparedStatement pst = con.prepareStatement(sql)) {

                pst.setString(1, "%" + search + "%");
                pst.setString(2, "%" + search + "%");

                try (ResultSet rs = pst.executeQuery()) {

                    while (rs.next()) {

                        model.addRow(new Object[]{
                            rs.getInt("bill_id"),
                            rs.getString("customer_name"),
                            rs.getTimestamp("bill_date"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("gst_amount"),
                            rs.getDouble("grand_total")
                        });
                    }
                }

            } catch (SQLException ex) {
                showDatabaseError(historyFrame, ex);
            }
        };

        // LOAD ALL BILLS INITIALLY

        loadBills.run();

        // SEARCH BUTTON

        searchButton.addActionListener(e -> loadBills.run());

        // SHOW ALL BUTTON

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadBills.run();
        });

        // ACTION BUTTONS

        JButton detailsButton = new JButton("View Details");
        JButton printButton = new JButton("Print Bill");
        JButton refreshButton = new JButton("Refresh");

        // GET SELECTED BILL RECEIPT

        java.util.function.Supplier<String> getReceipt = () -> {

            int row = table.getSelectedRow();

            if (row == -1) {
                JOptionPane.showMessageDialog(
                    historyFrame,
                    "Please select a bill first!"
                );
                return null;
            }

            int billId = Integer.parseInt(
                model.getValueAt(row, 0).toString()
            );

            StringBuilder receipt = new StringBuilder();

            String billSql =
                "SELECT * FROM bills WHERE bill_id = ?";

            String itemSql =
                "SELECT * FROM bill_items WHERE bill_id = ?";

            try (Connection con = openConnection()) {

                // BILL INFORMATION

                try (PreparedStatement pst =
                        con.prepareStatement(billSql)) {

                    pst.setInt(1, billId);

                    try (ResultSet rs = pst.executeQuery()) {

                        if (rs.next()) {

                            receipt.append("PHARMACY BILLING SYSTEM\n");
                            receipt.append("--------------------------------\n");

                            receipt.append("Bill ID: ")
                                   .append(rs.getInt("bill_id")).append("\n");

                            receipt.append("Customer: ")
                                   .append(rs.getString("customer_name"))
                                   .append("\n");

                            receipt.append("Date: ")
                                   .append(rs.getTimestamp("bill_date"))
                                   .append("\n");

                            receipt.append("--------------------------------\n");
                            receipt.append("MEDICINE DETAILS\n");
                            receipt.append("--------------------------------\n");
                            receipt.append(
                                "Medicine       Price    Qty    Total\n"
                            );
                        } else {
                            JOptionPane.showMessageDialog(
                                historyFrame, "Bill record not found!"
                            );
                            return null;
                        }
                    }
                }

                // BILL ITEMS

                try (PreparedStatement pst =
                        con.prepareStatement(itemSql)) {

                    pst.setInt(1, billId);

                    try (ResultSet rs = pst.executeQuery()) {

                        while (rs.next()) {

                            receipt.append(rs.getString("medicine_name"))
                                   .append("    Rs.")
                                   .append(rs.getDouble("price"))
                                   .append("    ")
                                   .append(rs.getInt("quantity"))
                                   .append("    Rs.")
                                   .append(rs.getDouble("total"))
                                   .append("\n");
                        }
                    }
                }

                // BILL TOTALS

                try (PreparedStatement pst =
                        con.prepareStatement(billSql)) {

                    pst.setInt(1, billId);

                    try (ResultSet rs = pst.executeQuery()) {

                        if (rs.next()) {

                            receipt.append("--------------------------------\n");

                            receipt.append("Subtotal: Rs.")
                                   .append(rs.getDouble("subtotal"))
                                   .append("\n");

                            receipt.append("GST: Rs.")
                                   .append(rs.getDouble("gst_amount"))
                                   .append("\n");

                            receipt.append("Grand Total: Rs.")
                                   .append(rs.getDouble("grand_total"))
                                   .append("\n");

                            receipt.append("--------------------------------\n");
                            receipt.append("Thank You!\n");
                        }
                    }
                }

                return receipt.toString();

            } catch (SQLException ex) {

                showDatabaseError(historyFrame, ex);
                return null;
            }
        };

        // VIEW BILL DETAILS

        detailsButton.addActionListener(e -> {

            String receipt = getReceipt.get();

            if (receipt == null) {
                return;
            }

            JTextArea area = new JTextArea(receipt);
            area.setEditable(false);
            area.setFont(
                new Font("Monospaced", Font.PLAIN, 14)
            );

            JOptionPane.showMessageDialog(
                historyFrame,
                new JScrollPane(area),
                "Bill Details",
                JOptionPane.INFORMATION_MESSAGE
            );
        });

        // PRINT BILL

        printButton.addActionListener(e -> {

            String receipt = getReceipt.get();

            if (receipt == null) {
                return;
            }

            JTextArea area = new JTextArea(receipt);
            area.setFont(
                new Font("Monospaced", Font.PLAIN, 12)
            );

            try {

                area.print();

            } catch (java.awt.print.PrinterException ex) {

                JOptionPane.showMessageDialog(
                    historyFrame,
                    "Printing Error: " + ex.getMessage(),
                    "Print Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // REFRESH BILL HISTORY

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadBills.run();
        });

        // BOTTOM BUTTON PANEL

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(detailsButton);
        buttonPanel.add(printButton);
        buttonPanel.add(refreshButton);

        // WINDOW LAYOUT

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        historyFrame.add(topPanel, BorderLayout.NORTH);
        historyFrame.add(scrollPane, BorderLayout.CENTER);
        historyFrame.add(buttonPanel, BorderLayout.SOUTH);

        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setVisible(true);
    }
}
