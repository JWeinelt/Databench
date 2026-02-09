package de.julianweinelt.databench.ui.flow.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlowWindow {

    private JFrame frame;
    private JPanel content;
    private CardLayout cards;

    public void open() {
        frame = new JFrame("DataBench Flow");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        frame.add(createTopBar(), BorderLayout.NORTH);
        frame.add(createContent(), BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JComponent createTopBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton dashboard = new JButton("Dashboard");
        JButton jobs = new JButton("Jobs");
        JButton users = new JButton("Users");
        JButton admin = new JButton("Administration");

        dashboard.addActionListener(e -> cards.show(content, "dashboard"));
        jobs.addActionListener(e -> cards.show(content, "jobs"));
        users.addActionListener(e -> cards.show(content, "users"));
        admin.addActionListener(e -> cards.show(content, "admin"));

        bar.add(dashboard);
        bar.add(jobs);
        bar.add(users);
        bar.add(admin);

        return bar;
    }

    private JComponent createContent() {
        cards = new CardLayout();
        content = new JPanel(cards);

        content.add(createDashboardPanel(), "dashboard");
        content.add(createJobsPanel(), "jobs");
        content.add(createUsersPanel(), "users");
        content.add(createAdminPanel(), "admin");

        cards.show(content, "dashboard");
        return content;
    }

    private JComponent createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 16, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(createStatCard("Total Jobs", "12"));
        panel.add(createStatCard("Active Jobs", "4"));
        panel.add(createStatCard("Users", "8"));
        panel.add(createStatCard("Last Run", "2 min ago"));

        return panel;
    }

    private JComponent createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(lblValue.getFont().deriveFont(Font.PLAIN, 26f));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);

        return card;
    }

    private JComponent createJobsPanel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Daily Backup");
        model.addElement("Cleanup Logs");
        model.addElement("Sync Analytics");
        model.addElement("Nightly Export");

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String job = list.getSelectedValue();
                    if (job != null) {
                        openJobSettings(job);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder("Jobs"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void openJobSettings(String jobName) {
        JOptionPane.showMessageDialog(
                frame,
                "Settings for job:\n" + jobName,
                "Job Settings",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private JComponent createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel placeholder = new JLabel("User management will be implemented here.");
        placeholder.setFont(placeholder.getFont().deriveFont(Font.ITALIC));

        panel.add(placeholder, BorderLayout.NORTH);
        return panel;
    }

    private JComponent createAdminPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }
}