package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.api.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class StartScreen {
    private JFrame frame;

    public void start() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        frame = new JFrame("");
        frame.setIconImage(icon);
        frame.setType(Window.Type.UTILITY);
        frame.setSize(1024, 606);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);

        BufferedImage image;
        try {
            image = ImageIO.read(getClass().getResource("/icons/boot_splash.png"));
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
