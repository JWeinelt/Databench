package de.julianweinelt.datacat.ui;

import de.julianweinelt.datacat.DataCat;
import de.julianweinelt.datacat.api.ImagePanel;

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
        DataCat.getInstance().setOverFrame(frame);
        frame.setIconImage(icon);
        frame.setType(Window.Type.UTILITY);
        frame.setSize(900, 506);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);

        JLabel versionLabel = new JLabel("v" + DataCat.version);
        versionLabel.setBounds(20, frame.getHeight() - 30, 100, 20);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        versionLabel.setForeground(Color.BLACK);
        frame.add(versionLabel);

        JLabel licenseLabel = new JLabel("Licensed under GNU GPLv3");
        licenseLabel.setBounds(90, frame.getHeight() - 30, 400, 20);
        licenseLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        licenseLabel.setForeground(Color.BLACK);
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
