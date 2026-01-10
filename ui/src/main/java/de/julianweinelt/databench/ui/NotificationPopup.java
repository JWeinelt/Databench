package de.julianweinelt.databench.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class NotificationPopup extends JWindow {
    private Timer autoCloseTimer;
    private final NotificationType type;

    public NotificationPopup(
            JFrame owner,
            Component anchor,
            NotificationType type,
            String title,
            String message,
            String linkText,
            Runnable linkAction
    ) {
        super(owner);
        this.type = type;

        setSize(360, 120);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);

        // Accent panel links
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(4, 1));
        accent.setBackground(type.accentColor());

        // Root panel mit abgerundeten Ecken
        JPanel root = new RoundedPanel(16);
        root.setBackground(new Color(60, 63, 65));
        root.setBorder(new EmptyBorder(10, 12, 10, 12));
        root.setLayout(new BorderLayout(8, 8));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(
                type == NotificationType.ERROR || type == NotificationType.WARNING
                        ? type.accentColor()
                        : Color.WHITE
        );
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));

        JLabel close = new JLabel("✕");
        close.setForeground(new Color(180, 180, 180));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                close();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                close.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setForeground(new Color(180, 180, 180));
            }
        });

        header.add(titleLabel, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        // Content
        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setForeground(new Color(210, 210, 210));
        messageLabel.setFont(messageLabel.getFont().deriveFont(12f));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(messageLabel);

        if (linkText != null && linkAction != null) {
            content.add(Box.createVerticalStrut(6));
            JLabel link = new JLabel("<html><u>" + linkText + "</u></html>");
            link.setForeground(type.accentColor());
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.setFont(link.getFont().deriveFont(12f));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    close();
                    linkAction.run();
                }
            });
            content.add(link);
        }

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);

        // Wrapper mit Accent links
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(accent, BorderLayout.WEST);
        wrapper.add(root, BorderLayout.CENTER);

        setContentPane(wrapper);

        // Position relativ zum Anchor
        positionRelativeTo(anchor);

        // Auto-Close
        if (type.hasAutoClose()) {
            startAutoClose(type.autoCloseMillis());
        }
    }

    private void positionBottomRight(JFrame owner) {
        Rectangle screen = owner.getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(owner.getGraphicsConfiguration());

        int x = screen.x + screen.width - getWidth() - insets.right - 20;
        int y = screen.y + screen.height - getHeight() - insets.bottom - 20;

        setLocation(x, y);
    }

    private void positionRelativeTo(Component anchor) {
        try {
            Point p = anchor.getLocationOnScreen();
            int x = p.x + anchor.getWidth() - getWidth() - 16;
            int y = p.y + anchor.getHeight() - getHeight() - 16;
            setLocation(x, y);
        } catch (IllegalComponentStateException ignored) {
        }
    }

    public void showPopup() {
        setVisible(true);
    }

    private void startAutoClose(int millis) {
        autoCloseTimer = new Timer(millis, e -> dispose());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    private void close() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
        }
        dispose();
    }

    static class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight(),
                    radius, radius
            ));
            g2.dispose();
        }
    }
}