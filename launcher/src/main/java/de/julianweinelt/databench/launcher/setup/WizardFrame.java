package de.julianweinelt.databench.launcher.setup;

import com.formdev.flatlaf.FlatDarkLaf;
import de.julianweinelt.databench.launcher.setup.wizard.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class WizardFrame extends JFrame {

    private final SetupState state = new SetupState();
    private final List<WizardPage> pages = new ArrayList<>();
    private int pageIndex = 0;

    private JButton backButton;
    private JButton nextButton;

    private final JPanel contentPanel = new JPanel(null); // für Animation
    private final JLayeredPane layeredPane = new JLayeredPane();

    public WizardFrame() {
        FlatDarkLaf.setup();
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        setIconImage(icon);

        setTitle("DataBench Setup");
        setSize(820, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        layeredPane.setLayout(null);
        layeredPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
        add(layeredPane, BorderLayout.CENTER);

        contentPanel.setBounds(0, 0, getWidth(), getHeight());
        layeredPane.add(contentPanel, JLayeredPane.DEFAULT_LAYER);

        initPages();
        initUI();

        WizardPage first = pages.get(0);
        JComponent view = first.getView();
        view.setBounds(0, 0, contentPanel.getWidth(), contentPanel.getHeight());
        contentPanel.add(view);
        contentPanel.revalidate();
        contentPanel.repaint();

        pageIndex = 0;
        first.onEnter();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(WizardFrame.this,
                        "Are you sure you want to exit the configuration wizard?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
        setMenuBar(new MenuBar());

        setVisible(true);
    }

    private void initPages() {
        pages.add(new WelcomePage(state));
        pages.add(new LanguagePage(state));
        pages.add(new DriverPage(state));
        pages.add(new ThemePage(state));
        pages.add(new AdvancedPage(state));
        pages.add(new DownloadPage(state));
        pages.add(new FinishPage(state));
    }

    private void initUI() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backButton = new JButton("Back");
        nextButton = new JButton("Next");

        backButton.addActionListener(e -> navigate(-1));
        nextButton.addActionListener(e -> navigate(1));

        navPanel.add(backButton);
        navPanel.add(nextButton);
        add(navPanel, BorderLayout.SOUTH);

        updateButtons();
    }

    private void navigate(int direction) {
        WizardPage current = pages.get(pageIndex);
        if (direction > 0 && !current.canGoNext()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        current.onLeave();
        showPage(pageIndex + direction, true);
    }

    private void showPage(int newIndex, boolean animated) {
        if (newIndex < 0 || newIndex >= pages.size()) return;

        WizardPage newPage = pages.get(newIndex);
        JComponent nextView = newPage.getView();
        nextView.setBounds(0, 0, contentPanel.getWidth(), contentPanel.getHeight());

        JComponent currentView = (JComponent) contentPanel.getComponent(0);

        if (animated) {
            animateTransition(currentView, nextView, newIndex > pageIndex);
        } else {
            contentPanel.removeAll();
            contentPanel.add(nextView);
            contentPanel.revalidate();
            contentPanel.repaint();
        }

        pageIndex = newIndex;
        newPage.onEnter();
        updateButtons();
    }

    private void updateButtons() {
        backButton.setEnabled(pageIndex > 0);
        nextButton.setText(pageIndex == pages.size() - 1 ? "Finish" : "Next");
    }

    private void animateTransition(JComponent from, JComponent to, boolean forward) {
        int width = contentPanel.getWidth();
        int height = contentPanel.getHeight();

        to.setBounds(forward ? width : -width, 0, width, height);
        layeredPane.add(to, JLayeredPane.PALETTE_LAYER);

        Timer timer = new Timer(5, null);
        timer.addActionListener(e -> {
            int step = 20;
            from.setLocation(from.getX() - (forward ? step : -step), 0);
            to.setLocation(to.getX() - (forward ? step : -step), 0);

            if ((forward && to.getX() <= 0) || (!forward && to.getX() >= 0)) {
                ((Timer) e.getSource()).stop();
                contentPanel.removeAll();
                contentPanel.add(to);
                contentPanel.revalidate();
                contentPanel.repaint();
                layeredPane.remove(to);
            }
        });
        timer.start();
    }
}