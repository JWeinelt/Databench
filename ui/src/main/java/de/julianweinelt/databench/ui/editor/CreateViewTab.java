package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.editor.views.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateViewTab implements IEditorTab {
    private final UUID id = UUID.randomUUID();

    private TableDefinition table;
    private TableDefinition originalTable;

    private Object[][] tableData;

    private final DConnection connection;
    private boolean existingTable = false;
    private String tableName = null;

    public CreateViewTab(DConnection connection) {
        this.connection = connection;
    }

    public CreateViewTab ofRealTable(String database, String tableName) {
        this.tableName = tableName;

        this.table = connection.getTableDefinition(database, tableName);
        this.originalTable = deepCopy(this.table);

        this.tableData = toTableData(this.table.getColumns());
        this.existingTable = true;
        return this;
    }

    public CreateViewTab newTable() {
        this.table = new TableDefinition();
        this.table.setTableName("");

        table.addColumn(new TableColumn("id", "INT", 11, true, true, true));
        table.addColumn(new TableColumn("name", "VARCHAR", 255, false, false, false));

        this.tableData = toTableData(table.getColumns());
        this.existingTable = false;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton executeSQL = new JButton("Execute");
        toolBar.add(executeSQL);

        root.add(toolBar, BorderLayout.NORTH);

        QueryDesignerPanel designer = new QueryDesignerPanel();
        QueryModel queryModel = new QueryModel();

        TableNode stats = new TableNode(
                "stats",
                "s",
                List.of(
                        "uuid", "level", "xp", "prestige",
                        "wins", "lose", "kills", "deaths"
                ),
                queryModel
        );

        TableNode player = new TableNode(
                "player",
                "p",
                List.of("UUID", "Name", "TeamID"),
                queryModel
        );

        designer.addTable(stats, 40, 40);
        designer.addTable(player, 340, 100);

        designer.addJoin(
                new JoinEdge(stats, player, "uuid", "UUID")
        );

        JScrollPane designerScroll = new JScrollPane(designer);
        designerScroll.setBorder(
                BorderFactory.createTitledBorder("Query Designer")
        );

        SelectedColumnGridModel gridModel =
                new SelectedColumnGridModel(queryModel);
        queryModel.getGridModels().add(gridModel);

        JTable columnGrid = new JTable(gridModel);
        columnGrid.setRowHeight(22);

        JScrollPane gridScroll = new JScrollPane(columnGrid);
        gridScroll.setBorder(
                BorderFactory.createTitledBorder("Columns")
        );


        RSyntaxTextArea sqlPreview = new RSyntaxTextArea(6, 80);
        sqlPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        sqlPreview.setCodeFoldingEnabled(true);
        sqlPreview.setAnimateBracketMatching(true);
        sqlPreview.setHighlightCurrentLine(true);
        sqlPreview.setMarkOccurrences(true);
        sqlPreview.setAutoIndentEnabled(true);
        sqlPreview.setAntiAliasingEnabled(true);
        sqlPreview.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl ENTER"), "execute"
        );

        RTextScrollPane sqlScroll = new RTextScrollPane(sqlPreview);

        sqlScroll.setBorder(
                BorderFactory.createTitledBorder("Generated SQL")
        );

        JSplitPane topSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                designerScroll,
                gridScroll
        );
        topSplit.setResizeWeight(0.65);
        topSplit.setOneTouchExpandable(true);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                topSplit,
                sqlScroll
        );
        mainSplit.setResizeWeight(0.8);
        mainSplit.setOneTouchExpandable(true);

        root.add(mainSplit, BorderLayout.CENTER);

        return root;
    }

    @Override
    public String getTitle() {
        return tableName == null ? "New Table" : tableName;
    }

    private TableDefinition deepCopy(TableDefinition src) {
        TableDefinition copy = new TableDefinition();
        copy.setTableName(src.getTableName());
        for (TableColumn c : src.getColumns()) {
            copy.addColumn(new TableColumn(
                    c.getName(), c.getType(), c.getSize(),
                    c.isPrimaryKey(), c.isNotNull(), c.isAutoIncrement()
            ));
        }
        return copy;
    }

    private Object[][] toTableData(List<TableColumn> columns) {
        Object[][] data = new Object[columns.size()][6];
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            data[i] = new Object[]{
                    c.getName(),
                    c.getType(),
                    c.getSize() != null ? c.getSize() : "",
                    c.isPrimaryKey(),
                    c.isNotNull(),
                    c.isAutoIncrement()
            };
        }
        return data;
    }

    private List<TableColumn> fromTableModel(DefaultTableModel model) {
        List<TableColumn> columns = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            columns.add(new TableColumn(
                    model.getValueAt(i, 0).toString(),
                    model.getValueAt(i, 1).toString(),
                    model.getValueAt(i, 2).toString().isEmpty()
                            ? null
                            : Integer.parseInt(model.getValueAt(i, 2).toString()),
                    (Boolean) model.getValueAt(i, 3),
                    (Boolean) model.getValueAt(i, 4),
                    (Boolean) model.getValueAt(i, 5)
            ));
        }
        return columns;
    }
}
