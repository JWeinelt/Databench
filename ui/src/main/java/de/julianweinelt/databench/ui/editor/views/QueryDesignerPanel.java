package de.julianweinelt.databench.ui.editor.views;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class QueryDesignerPanel extends JPanel {

    private final List<TableNode> tables = new ArrayList<>();
    private final List<JoinEdge> joins = new ArrayList<>();

    public QueryDesignerPanel() {
        setLayout(null);
        setBackground(new Color(30, 30, 30));
    }

    public void addTable(TableNode node, int x, int y) {
        node.setLocation(x, y);
        tables.add(node);
        add(node);
        repaint();
    }

    public void addJoin(JoinEdge join) {
        joins.add(join);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        for (JoinEdge join : joins) {
            join.paint(g2);
        }
    }
}
