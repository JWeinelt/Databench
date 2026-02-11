package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.api.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class StartScreen {
    private JFrame frame;

    public void start() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        frame = new JFrame("");
        DataBench.getInstance().setOverFrame(frame);
        frame.setIconImage(icon);
        frame.setType(Window.Type.UTILITY);
        frame.setSize(1024, 606);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);

        JLabel versionLabel = new JLabel("v" + DataBench.version);
        versionLabel.setBounds(frame.getWidth() - 100, frame.getHeight() - 30, 100, 20);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        versionLabel.setForeground(Color.WHITE);
        frame.add(versionLabel);

        JLabel licenseLabel = new JLabel("Licensed under GNU GPLv3");
        licenseLabel.setBounds(2, frame.getHeight() - 30, 400, 20);
        licenseLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        licenseLabel.setForeground(Color.WHITE);
        frame.add(licenseLabel);

        BufferedImage image;
        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResource("/icons/boot_splash.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ImagePanel imagePanel = new ImagePanel(image);
        imagePanel.setSize(1024, 606);
        frame.add(imagePanel);

        frame.setVisible(true);
    }

    public void stop() {
        frame.dispose();
    }
}
