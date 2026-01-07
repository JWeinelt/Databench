package de.julianweinelt.databench.ui.driver;

import de.julianweinelt.databench.api.DriverShim;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

public class DriverManagerDialog extends JDialog {

    private final DefaultListModel<Driver> driverListModel = new DefaultListModel<>();
    private final JList<Driver> driverList = new JList<>(driverListModel);

    private final JTextArea infoArea = new JTextArea();

    public DriverManagerDialog(Window parent) {
        super(parent, translate("drivmanage.title"), ModalityType.APPLICATION_MODAL);
        setSize(700, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createRightPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        loadDrivers();
        setupListeners();
    }

    private Component createLeftPanel() {
        driverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driverList.setCellRenderer(new DriverListRenderer());

        JScrollPane scroll = new JScrollPane(driverList);
        scroll.setBorder(new TitledBorder(translate("drivmanage.installed.title")));
        scroll.setPreferredSize(new Dimension(260, 0));
        return scroll;
    }

    private Component createRightPanel() {
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(infoArea);
        scroll.setBorder(new TitledBorder(translate("drivmanage.driver.info")));
        return scroll;
    }

    private Component createButtonPanel() {
        JButton install = new JButton(translate("drivmanage.button.install"));
        JButton uninstall = new JButton(translate("drivmanage.button.uninstall"));
        JButton reload = new JButton(translate("drivmanage.button.reload"));
        JButton close = new JButton(translate("drivmanage.button.close"));

        install.addActionListener(e -> {
            dispose();
            new DriverDownloadDialog(DriverManagerDialog.this.getOwner(), true).setVisible(true);
        });

        uninstall.addActionListener(e -> uninstallSelected());
        reload.addActionListener(e -> loadDrivers());
        close.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(install);
        panel.add(uninstall);
        panel.add(reload);
        panel.add(close);
        return panel;
    }

    private void loadDrivers() {
        driverListModel.clear();

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            driverListModel.addElement(drivers.nextElement());
        }

        infoArea.setText("");
    }

    private void setupListeners() {
        driverList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDriverInfo(driverList.getSelectedValue());
            }
        });
    }

    private void showDriverInfo(Driver driver) {
        if (driver == null) {
            infoArea.setText("");
            return;
        }

        Driver realDriver = driver;

        if (driver instanceof DriverShim shim) {
            realDriver = shim.getDelegate();
        }

        String className = realDriver.getClass().getName();
        int major = realDriver.getMajorVersion();
        int minor = realDriver.getMinorVersion();
        boolean compliant = realDriver.jdbcCompliant();

        StringBuilder sb = new StringBuilder();
        sb.append(translate("drivmanage.info.class")).append("\n")
          .append(className).append("\n\n");

        sb.append("Version:\n")
          .append(major)
          .append(".")
          .append(minor)
          .append("\n\n");

        sb.append("JDBC Compliant:\n")
          .append(compliant)
          .append("\n\n");

        sb.append(translate("drivmanage.info.urls")).append("\n");

        List<String> knownUrls = guessJdbcUrls(realDriver);
        for (String url : knownUrls) {
            sb.append("• ").append(url).append("\n");
        }

        infoArea.setText(sb.toString());
    }

    private List<String> guessJdbcUrls(Driver driver) {
        List<String> urls = new ArrayList<>();

        String name = driver.getClass().getName().toLowerCase();
        if (name.contains("mysql")) urls.add("jdbc:mysql://host:port/db");
        if (name.contains("mariadb")) urls.add("jdbc:mariadb://host:port/db");
        if (name.contains("sqlserver")) urls.add("jdbc:sqlserver://host");

        if (urls.isEmpty()) {
            urls.add(translate("drivmanage.info.urls.unknown"));
        }

        return urls;
    }

    private void uninstallSelected() {
        Driver driver = driverList.getSelectedValue();
        if (driver == null) return;

        int result = JOptionPane.showConfirmDialog(
                this,
                translate("drivmanage.popup.uninstall.confirm", Map.of("driver", driver.getClass().getSimpleName())),
                translate("drivmanage.popup.uninstall.title"),
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) return;

        try {
            DriverManager.deregisterDriver(driver);
            deleteDriverJar(driver);
            loadDrivers();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to uninstall driver:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteDriverJar(Driver driver) {
        File dir = new File("drivers");
    }
}
