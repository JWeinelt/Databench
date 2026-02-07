package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
public class EditorTab implements IEditorTab, EditorCallBack {
    private final UUID id = UUID.randomUUID();
    private final DConnection connection;

    private final String content;
    private final BenchUI ui;
    private RSyntaxTextArea editorArea;
    private JPanel editorPanel;

    @Getter
    private File saveFile = null;
    @Getter
    private boolean fileSaved = false;

    private JButton runButton;

    public EditorTab(String content, BenchUI ui) {
        this.connection = null;
        this.content = content;
        this.ui = ui;
    }

    public EditorTab(DConnection connection, String content, BenchUI ui) {
        this.connection = connection;
        this.content = content;
        this.ui = ui;
    }

    public EditorTab(DConnection connection, String content, BenchUI ui, File saveFile) {
        this.connection = connection;
        this.content = content;
        this.ui = ui;
        this.saveFile = saveFile;
        fileSaved = true;
    }

    public void execute() {
        runButton.doClick();
    }

    public String getEditorContent() {
        return editorArea.getText();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {
        editorPanel = new JPanel(new BorderLayout());

        JToolBar editorToolBar = new JToolBar();
        editorToolBar.setFloatable(false);

        runButton = new JButton("▶ " + translate("editor.button.run"));
        JButton formatButton = new JButton(translate("editor.button.format"));

        editorToolBar.add(runButton);
        editorToolBar.add(formatButton);

        editorPanel.add(editorToolBar, BorderLayout.NORTH);

        editorArea = new RSyntaxTextArea(20, 60);
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editorArea.setCodeFoldingEnabled(true);
        applyOption(editorArea);
        editorArea.setAnimateBracketMatching(true);
        editorArea.setHighlightCurrentLine(true);
        editorArea.setMarkOccurrences(true);
        editorArea.setAutoIndentEnabled(true);
        editorArea.setAntiAliasingEnabled(true);
        editorArea.setText(content);

        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTab.this);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTab.this);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTab.this);
            }
        });

        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl ENTER"), "execute"
        );
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl S"), "save"
        );
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl shift S"), "saveAs"
        );
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("F5"), "execute"
        );
        editorArea.getActionMap().put("execute", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                runButton.doClick();
            }
        });
        editorArea.getActionMap().put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile(false);
            }
        });
        editorArea.getActionMap().put("saveAs", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile(true);
            }
        });

        try {
            if (Configuration.getConfiguration().getSelectedTheme().contains("Light")) {
                Theme theme = Theme.load(
                        getClass().getResourceAsStream(
                                "/org/fife/ui/rsyntaxtextarea/themes/light.xml"
                        )
                );
                theme.apply(editorArea);
            } else {
                Theme theme = Theme.load(
                        getClass().getResourceAsStream(
                                "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                        )
                );
                theme.apply(editorArea);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        autoCompleter(editorArea);

        RTextScrollPane editorScroll = new RTextScrollPane(editorArea);
        editorScroll.setFoldIndicatorEnabled(true);

        JTabbedPane bottomTabs = new JTabbedPane();

        JPanel bottomHeader = new JPanel(new BorderLayout());
        bottomHeader.add(bottomTabs, BorderLayout.CENTER);

        JTable resultTable = new JTable();

        JButton exportButton = new JButton();
        exportButton.setFocusable(false);
        exportButton.setToolTipText("Export results");

        Icon exportIcon = loadIcon("/icons/editor/export-light.png", 16);
        exportButton.setIcon(exportIcon);
        exportButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser(".");
            chooser.setDialogTitle(translate("dialog.export.title"));
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);

            FileNameExtensionFilter csv =
                    new FileNameExtensionFilter(translate("dialog.export.extension.csv"), ".csv");

            FileNameExtensionFilter xlsx =
                    new FileNameExtensionFilter(translate("dialog.export.extension.xlsx"), ".xlsx");

            chooser.addChoosableFileFilter(csv);
            chooser.setFileFilter(csv);
            chooser.addChoosableFileFilter(xlsx);

            if (chooser.showSaveDialog(ui.getFrame()) != JFileChooser.APPROVE_OPTION)
                return;

            File file = chooser.getSelectedFile();
            String extension =
                    ((FileNameExtensionFilter) chooser.getFileFilter())
                            .getExtensions()[0];

            TableModel model = resultTable.getModel();
            int totalRows = model.getRowCount();
            JProgressBar bar = new JProgressBar();
            JDialog dialog = createProgressDialog(ui.getFrame(), bar);

            SwingWorker<Void, Integer> worker = new SwingWorker<>() {

                protected Void doInBackground() {

                    TableExporter exporter =
                            new TableExporter(file, extension, EditorTab.this, resultTable);
                    exporter.exportWithProgress(progress -> {
                        int prog = (int) (1.0 * progress / totalRows);
                        //log.info("Export progress: {} / {} ({}%)", progress, totalRows, prog);
                        setProgress(prog);
                        publish(progress);
                    });

                    return null;
                }

                protected void process(java.util.List<Integer> chunks) {
                    int value = chunks.get(chunks.size() - 1);
                    bar.setValue(value);
                }

                protected void done() {
                    dialog.dispose();

                    JOptionPane.showMessageDialog(
                            ui.getFrame(),
                            "Export finished."
                    );
                }
            };

            worker.execute();
            dialog.setVisible(true);
        });

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        actionPanel.add(exportButton);

        bottomHeader.add(actionPanel, BorderLayout.EAST);

        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(Configuration.getConfiguration().getEditorFontObject());
        JScrollPane messageScroll = new JScrollPane(messageArea);

        bottomTabs.addTab(translate("connection.editor.result.tabs.message"), messageScroll);

        JScrollPane resultScroll = new JScrollPane(resultTable);

        Runnable showResultsTab = () -> {
            if (bottomTabs.indexOfTab(translate("connection.editor.result.tabs.results")) == -1) {
                bottomTabs.addTab(translate("connection.editor.result.tabs.results"), resultScroll);
            }
            bottomTabs.setSelectedComponent(resultScroll);
        };

        Runnable hideResultsTab = () -> {
            int idx = bottomTabs.indexOfTab(translate("connection.editor.result.tabs.results"));
            if (idx != -1) {
                bottomTabs.removeTabAt(idx);
            }
        };

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editorScroll,
                bottomHeader
        );
        splitPane.setResizeWeight(0.7);
        splitPane.setOneTouchExpandable(true);

        editorPanel.add(splitPane, BorderLayout.CENTER);

        runButton.addActionListener(e -> {

            messageArea.setText("");
            hideResultsTab.run();

            String sql = removeComments(editorArea.getText());

            SwingWorker<Void, String> worker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() {
                    long firstTime = System.currentTimeMillis();

                    String[] statements = sql.split(";");

                    for (String raw : statements) {
                        if (isCancelled()) break;

                        String st = raw.trim();
                        if (st.isEmpty()) continue;

                        try {
                            publish(translate("query.execute.running", Map.of("sql", st)));

                            if (connection.isQuery(st)) {

                                Object[][] data = connection.executeQuery(st);
                                String[] columns = connection.getColumnNames(st);

                                SwingUtilities.invokeLater(() -> {
                                    resultTable.setModel(new DefaultTableModel(data, columns));
                                    showResultsTab.run();
                                    bottomTabs.setSelectedIndex(1);
                                });

                                long nowTime = System.currentTimeMillis();

                                publish(translate("query.execute.success"));
                                publish(data.length + " rows returned.");
                                publish("Executed at " + Instant.now());
                                publish("Took " + (nowTime - firstTime) + " ms.");
                                publish("");

                            } else {

                                DConnection.SQLAnswer answer = connection.executeSQL(st);

                                if (answer.success()) {

                                    int affected = answer.updateCount();
                                    DateFormat df = DateFormat.getDateInstance(
                                            DateFormat.SHORT,
                                            Locale.forLanguageTag(
                                                    Configuration.getConfiguration()
                                                            .getLocale()
                                                            .replace("_", "-")
                                            )
                                    );

                                    publish(translate("query.execute.success"));
                                    publish(translate(
                                            "query.execute.rows",
                                            Map.of("rows", String.valueOf(affected))
                                    ));
                                    publish(translate(
                                            "query.execute.time",
                                            Map.of("time", df.format(Date.from(Instant.now())))
                                    ));
                                    publish("");

                                } else {
                                    publish(translate(
                                            "query.execute.error",
                                            Map.of("error", answer.message())
                                    ));
                                }
                            }

                        } catch (Exception ex) {
                            publish(translate(
                                    "query.execute.error",
                                    Map.of("error", ex.getMessage())
                            ));
                        }
                    }

                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String msg : chunks) {
                        messageArea.append(msg + "\n");
                    }
                }

                @Override
                protected void done() {
                    bottomTabs.setSelectedComponent(messageScroll);
                    splitPane.setDividerLocation(0.7);
                }
            };

            worker.execute();
        });

        return editorPanel;
    }


    @Override
    public String getTitle() {
        return (saveFile == null) ? "New Query" : saveFile.getName() + ((!fileSaved) ? " *" : "");
    }

    public void saveFile(boolean forceSaveAs) {
        if (saveFile != null && !forceSaveAs) {
            try (FileWriter w = new FileWriter(saveFile)) {
                w.write(editorArea.getText());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return;
        }

        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle(translate("dialog.save.title"));
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(translate("dialog.save.extension.sql"), "sql"));

        int returnValue = chooser.showSaveDialog(ui.getFrame());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            saveFile = chooser.getSelectedFile();
            if (saveFile.getName().toLowerCase().endsWith(".sql")) {
                saveFile = new File(saveFile.getAbsolutePath());
                try (FileWriter w = new FileWriter(saveFile)) {
                    w.write(editorArea.getText());

                    editorPanel.setName(saveFile.getName());
                    fileSaved = true;
                } catch (IOException e) {
                    log.error(e.getMessage());
                    JOptionPane.showMessageDialog(ui.getFrame(), translate("dialog.title.error.message", Map.of("error", e.getMessage())),
                            translate("dialog.title.error"), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                try (FileWriter w = new FileWriter(saveFile)) {
                    w.write(editorArea.getText());

                    editorPanel.setName(saveFile.getName());
                    JOptionPane.showMessageDialog(ui.getFrame(), translate("dialog.save.wrong-format"),
                            translate("dialog.title.warn"), JOptionPane.WARNING_MESSAGE);
                    fileSaved = true;
                    if (connection != null)
                        connection.updateTitle(this);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    JOptionPane.showMessageDialog(ui.getFrame(), translate("dialog.title.error.message", Map.of("error", e.getMessage())),
                            translate("dialog.title.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (returnValue == JFileChooser.ERROR) {
            JOptionPane.showMessageDialog(ui.getFrame(), translate("dialog.title.error.message",
                    Map.of("error", "Internal error. Code 1403")), translate("dialog.title.error")
                    , JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyOption(RSyntaxTextArea area) {
        SyntaxScheme scheme = area.getSyntaxScheme();
        Configuration.ColorSettings editorColors = Configuration.getConfiguration().getEditorColors();
        for (String s : editorColors.getAppliedColors()) {
            Color color = editorColors.getColorForName(s);
            if (color == null) continue;
            Style style = new Style(color);
            //style.font = new Font(Configuration.getConfiguration().getEditorFont(),
              //  Font.PLAIN, Configuration.getConfiguration().getEditorFontSize());
            int key = editorColors.getKey(s);
            if (key == -1) continue;
            scheme.setStyle(key, style);
        }
        area.setFont(new Font(
                Configuration.getConfiguration().getEditorFont(),
                Font.PLAIN,
                Configuration.getConfiguration().getEditorFontSize()
        ));
        area.setSyntaxScheme(scheme);
        area.revalidate();
        area.repaint();
    }

    private void autoCompleter(RSyntaxTextArea editorArea) {
        CompletionProvider provider = createCompletionProvider();

        AutoCompletion ac = new AutoCompletion(provider);
        ac.setParameterAssistanceEnabled(true);
        ac.setAutoCompleteEnabled(true);
        ac.setShowDescWindow(true);
        ac.setTriggerKey(KeyStroke.getKeyStroke("TAB"));
        ac.install(editorArea);
    }

    private CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        provider.addCompletion(new BasicCompletion(provider, "SELECT", "Select data from a table"
                , "Used to make queries and retrieve data from a table or view in a database."));
        provider.addCompletion(new BasicCompletion(provider, "FROM"));
        provider.addCompletion(new BasicCompletion(provider, "WHERE"));

        provider.addCompletion(new TemplateCompletion(provider, "CREATE TABLE",
                "Create a new table",
                "CREATE TABLE ${table_name} (\n    ${columns}\n);"));

        return provider;
    }

    private String removeComments(String input) {
        StringBuilder result = new StringBuilder();

        boolean inBlockComment = false;

        String[] lines = input.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();

            if (!inBlockComment && trimmed.startsWith("-- ")) {
                continue;
            }

            if (trimmed.startsWith("/*")) {
                inBlockComment = true;
            }

            if (!inBlockComment) {
                result.append(line).append(System.lineSeparator());
            }

            if (inBlockComment && trimmed.endsWith("*/")) {
                inBlockComment = false;
            }
        }

        return result.toString();
    }



    private JDialog createProgressDialog(JFrame parent, JProgressBar bar) {
        JDialog dialog = new JDialog(parent, "Exporting...", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        bar.setStringPainted(true);
        panel.add(new JLabel("Export in progress..."), BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);

        dialog.add(panel);

        dialog.setSize(300, 90);
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    private Icon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(getClassURL(path));
        Image image = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }
    public URL getClassURL(String file) {
        return getClass().getResource(file);
    }


    @Override
    public void call(String message, String type) {
        if (type.equals("error")) {
            JOptionPane.showMessageDialog(ui.getFrame(), message, translate("dialog.title.error"), JOptionPane.ERROR_MESSAGE);
        } else if (type.equals("warn")) {
            JOptionPane.showMessageDialog(ui.getFrame(), message, translate("dialog.title.warn"), JOptionPane.WARNING_MESSAGE);
        } else if (type.equals("info")) {
            JOptionPane.showMessageDialog(ui.getFrame(), message, translate("dialog.title.info"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            log.warn("Unknown message type from callback: {}", type);
        }
    }
}