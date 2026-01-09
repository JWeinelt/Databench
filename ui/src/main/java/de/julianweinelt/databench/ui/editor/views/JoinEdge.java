package de.julianweinelt.databench.ui.editor.views;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class JoinEdge {

    private final TableNode left;
    private final TableNode right;
    private final String leftColumn;
    private final String rightColumn;
    private JoinType type = JoinType.LEFT;
    private int leftSize;
    private int rightSize;
    private int leftIdx;
    private int rightIdx;

    public JoinEdge(
            TableNode left,
            TableNode right,
            String leftColumn,
            String rightColumn
    ) {
        this.left = left;
        this.right = right;
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn;

        leftSize = left.getColumnTable().getRowCount();
        rightSize = right.getColumnTable().getRowCount();
        leftIdx = left.getColumnIndex(leftColumn);
        rightIdx = right.getColumnIndex(rightColumn);
    }

    public void paint(Graphics2D g) {
        Point p1 = SwingUtilities.convertPoint(
                left,
                left.getWidth(),
                60 + (18 * leftIdx),
                left.getParent()
        );

        Point p2 = SwingUtilities.convertPoint(
                right,
                0,
                50 + (18 * rightIdx),
                right.getParent()
        );

        g.setStroke(new BasicStroke(2));
        g.setColor(Color.WHITE);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);

        //g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        //g.drawString(type.name(), (p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    public enum JoinType {
        INNER, LEFT, RIGHT, FULL
    }
}
