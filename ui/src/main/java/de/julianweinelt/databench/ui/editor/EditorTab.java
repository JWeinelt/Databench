package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

@Slf4j
public class EditorTab implements IEditorTab {
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
        this.content = content;
        this.ui = ui;
    }

    public EditorTab(String content, BenchUI ui, File saveFile) {
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
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {
        editorPanel = new JPanel(new BorderLayout());

    /* =======================
       Toolbar
       ======================= */
        JToolBar editorToolBar = new JToolBar();
        editorToolBar.setFloatable(false);

        runButton = new JButton("▶ " + translate("editor.button.run"));
        JButton formatButton = new JButton(translate("editor.button.format"));

        editorToolBar.add(runButton);
        editorToolBar.add(formatButton);

        editorPanel.add(editorToolBar, BorderLayout.NORTH);

    /* =======================
       SQL Editor
       ======================= */
        editorArea = new RSyntaxTextArea(20, 60);
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editorArea.setCodeFoldingEnabled(true);
        editorArea.setAnimateBracketMatching(true);
        editorArea.setHighlightCurrentLine(true);
        editorArea.setMarkOccurrences(true);
        editorArea.setAutoIndentEnabled(true);
        editorArea.setAntiAliasingEnabled(true);
        editorArea.setText(content);
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl ENTER"), "execute"
        );
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl S"), "save"
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
                saveFile();
            }
        });

        try {
            Theme theme = Theme.load(
                    getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                    )
            );
            theme.apply(editorArea);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        autoCompleter(editorArea);

        RTextScrollPane editorScroll = new RTextScrollPane(editorArea);
        editorScroll.setFoldIndicatorEnabled(true);

    /* =======================
       Bottom Tabs
       ======================= */
        JTabbedPane bottomTabs = new JTabbedPane();

        // ---- Messages Tab (immer sichtbar) ----
        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane messageScroll = new JScrollPane(messageArea);

        bottomTabs.addTab("connection.editor.result.tabs.message", messageScroll);

        // ---- Results Tab (optional) ----
        JTable resultTable = new JTable();
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

    /* =======================
       SplitPane
       ======================= */
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editorScroll,
                bottomTabs
        );
        splitPane.setResizeWeight(0.7);
        splitPane.setOneTouchExpandable(true);

        editorPanel.add(splitPane, BorderLayout.CENTER);

    /* =======================
       Execute Logic
       ======================= */
        runButton.addActionListener(e -> {

            messageArea.setText("");
            hideResultsTab.run();

            String sql = removeComments(editorArea.getText());

            SwingWorker<Void, String> worker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() {

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

                                publish(translate("query.execute.success"));
                                publish(data.length + " rows returned.");
                                publish("Executed at " + Instant.now());
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
                                            "query.execute.fail",
                                            Map.of("msg", answer.message())
                                    ));
                                }
                            }

                        } catch (Exception ex) {
                            publish(translate(
                                    "query.execute.fail",
                                    Map.of("msg", ex.getMessage())
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
        return (saveFile == null) ? "New Query" : saveFile.getName() + ((fileSaved) ? " *" : "");
    }

    public void saveFile() {
        if (saveFile != null) {
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
        log.info(returnValue + "");
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
}