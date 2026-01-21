package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.api.FileType;
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
import javax.swing.tree.DefaultMutableTreeNode;
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
import java.util.UUID;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

@Slf4j
public class EditorTxtTab implements IEditorTab {
    private final UUID id = UUID.randomUUID();

    private final String content;
    private final BenchUI ui;
    private RSyntaxTextArea editorArea;
    private JPanel editorPanel;
    private FileType type;

    @Getter
    private File saveFile = null;
    @Getter
    private boolean fileSaved = false;

    public EditorTxtTab(String content, BenchUI ui, File saveFile, FileType type) {
        this.content = content;
        this.ui = ui;
        this.saveFile = saveFile;
        this.type = type;
        fileSaved = true;
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

        JButton formatButton = new JButton(translate("editor.button.format"));
        editorToolBar.add(formatButton);

        editorPanel.add(editorToolBar, BorderLayout.NORTH);

        editorArea = new RSyntaxTextArea(20, 60);
        if (type.equals(FileType.JSON))
            editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        editorArea.setCodeFoldingEnabled(true);

        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTxtTab.this);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTxtTab.this);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!fileSaved) return;
                fileSaved = false;
                connection.updateTitle(EditorTxtTab.this);
            }
        });

        applyOption(editorArea);
        editorArea.setAnimateBracketMatching(true);
        editorArea.setHighlightCurrentLine(true);
        editorArea.setMarkOccurrences(true);
        editorArea.setAutoIndentEnabled(true);
        editorArea.setAntiAliasingEnabled(true);
        editorArea.setText(content);
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl S"), "save"
        );
        editorArea.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl shift S"), "saveAs"
        );
        editorArea.getActionMap().put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile(false);
                connection.updateTitle(EditorTxtTab.this);
            }
        });
        editorArea.getActionMap().put("saveAs", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile(true);
                connection.updateTitle(EditorTxtTab.this);
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

        //JTree hintArea = new JTree(new DefaultMutableTreeNode());
        JTree hintArea = new JTree();
        //hintArea.setFont(Configuration.getConfiguration().getEditorFontObject());
        JScrollPane messageScroll = new JScrollPane(hintArea);

        bottomTabs.addTab(translate("connection.editor.result.tabs.message"), messageScroll);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editorScroll,
                bottomTabs
        );
        splitPane.setResizeWeight(0.7);
        splitPane.setOneTouchExpandable(true);

        editorPanel.add(splitPane, BorderLayout.CENTER);

        return editorPanel;
    }


    @Override
    public String getTitle() {
        return saveFile.getName() + ((!fileSaved) ? " *" : "");
    }

    public void saveFile(boolean forceSaveAs) {
        if (saveFile != null && !forceSaveAs) {
            try (FileWriter w = new FileWriter(saveFile)) {
                w.write(editorArea.getText());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            fileSaved = true;
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
}