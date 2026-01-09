package de.julianweinelt.databench.api;

import javax.swing.*;
import java.awt.*;

public class ImagePanel extends JPanel {

    private final Image image;

    public ImagePanel(Image image) {
        this.image = image;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }

}
