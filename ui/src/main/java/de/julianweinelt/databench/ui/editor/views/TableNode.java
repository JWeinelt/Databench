package de.julianweinelt.databench.ui.editor.views;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TableNode extends JPanel {
    @Getter
    private final String tableName;
    @Getter
    private final String alias;
    @Getter
    private final JTable columnTable;
    private final List<String> columns;

    public TableNode(String tableName, String alias, List<String> columns,
                     QueryModel queryModel) {
        this.tableName = tableName;
        this.alias = alias;
        this.columns = columns;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setBackground(Color.GRAY);

        JLabel title = new JLabel(" " + alias + " (" + tableName + ")");
        title.setOpaque(true);
        title.setBackground(new Color(35, 35, 35));
        title.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        add(title, BorderLayout.NORTH);

        columnTable = new JTable(
                new ColumnSelectTableModel(columns, queryModel, this)
        );
        columnTable.setRowHeight(18);

        add(new JScrollPane(columnTable), BorderLayout.CENTER);

        setSize(200, 220);
        enableDragging();
    }

    public int getColumnIndex(String column) {
        return columns.indexOf(column);
    }

    private void enableDragging() {
        MouseAdapter adapter = new MouseAdapter() {
            Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                offset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(
                        getX() + e.getX() - offset.x,
                        getY() + e.getY() - offset.y
                );
                getParent().repaint();
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }
}