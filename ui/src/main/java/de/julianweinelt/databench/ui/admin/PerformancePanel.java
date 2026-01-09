package de.julianweinelt.databench.ui.admin;

import javax.swing.*;
import java.awt.*;

public class PerformancePanel extends JPanel implements Refreshable {

    public PerformancePanel() {
        setLayout(new GridLayout(2, 3, 12, 12));

        add(stat("Threads", "12"));
        add(stat("QPS", "245"));
        add(stat("Slow Queries", "3"));
        add(stat("Buffer Pool", "78 %"));
        add(stat("Temp Tables", "12"));
        add(stat("Uptime", "2d 4h"));
    }

    private JComponent stat(String name, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(name), BorderLayout.NORTH);
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 18f));
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {

    }
}
