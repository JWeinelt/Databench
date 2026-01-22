package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.dbx.api.ui.SettingsPanel;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import de.julianweinelt.databench.dbx.api.ui.components.*;
import de.julianweinelt.databench.dbx.util.LanguageManager;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;
import java.util.Map;

@Slf4j
public class DefaultUI {
    private final UIService service;

    public DefaultUI(UIService service) {
        this.service = service;
    }

    public void init() {
        service.addSettingsPanel(createGeneralPanel());
        service.addSettingsPanel(createAppearancePanel());
    }

    private SettingsPanel createGeneralPanel() {
        SettingsPanel panel = new SettingsPanel("General");
        ComponentComboBox lang = new ComponentComboBox();
        lang.label("Language:");

        LanguageManager.instance().getFriendlyNames().forEach(la ->
                lang.option(LanguageManager.instance().toId(la), la));
        lang.action(sel -> {
            String nLang = LanguageManager.instance().toId(sel);
            Configuration.getConfiguration().setLocale(nLang);
            log.info("Updated language to {}", nLang);
        });

        lang.initialValue(LanguageManager.instance().toFriendlyName(Configuration.getConfiguration().getLocale()));

        panel.add(lang);

        panel.add(new ComponentCheckbox()
                    .action(obj ->
                            Configuration.getConfiguration().setOpenProjectOnStartup(obj.value()))
                    .initialValue(Configuration.getConfiguration().isOpenProjectOnStartup())
                    .label("Open last project on startup"));

        panel.add(new ComponentCheckbox().label("Enable safe querying mode"));

        panel.add(new ComponentCheckbox()
                .label("Send anonymous statistics")
                .action(obj -> Configuration.getConfiguration().setSendAnonymousData(obj.value()))
                .initialValue(Configuration.getConfiguration().isSendAnonymousData())
        );
        panel.add(new ComponentCheckbox()
                .label("Automatically send error protocols")
                .action(obj -> Configuration.getConfiguration().setSendErrorProtocols(obj.value()))
                .initialValue(Configuration.getConfiguration().isSendErrorProtocols())
        );
        panel.add(new ComponentComboBox()
                .label("Action when closing IDE")
                .option("ask", "Ask", () -> Configuration.getConfiguration().setCloseType("ask"))
                .option("save_disconnect", "Save files", () -> Configuration.getConfiguration().setCloseType("save_disconnect"))
                .option("ask_save_disconnect", "Ask to save files", () -> Configuration.getConfiguration().setCloseType("ask_save_disconnect"))
                .option("disconnect", "Close everything", () -> Configuration.getConfiguration().setCloseType("disconnect"))
        );
        panel.add(new ComponentCheckbox()
                .label("Save files when closing project")
                //TODO: Add action
        );
        panel.add(new ComponentHorizontalLine());
        panel.add(new ComponentLabel(true).text("Updates").font(new Font("Arial", Font.BOLD, 15)));
        panel.add(new ComponentCheckbox().label("Check for updates on startup"));
        panel.add(new ComponentComboBox()
                .label("Update Channel:")
                .option("stable", "Stable")
                .option("beta", "Beta")
                .option("dev", "Development")
                .action(mode -> Configuration.getConfiguration().setUpdateChannel(mode))
        );
        return panel;
    }

    private SettingsPanel createAppearancePanel() {
        SettingsPanel panel = new SettingsPanel("Appearance");

        ComponentComboBox themes = new ComponentComboBox()
                .label("Theme:");

        for (String theme : new String[]{"Dark", "Light", "Darcula", "Dark (MacOS)", "Light (MacOS)", "IntelliJ"})
            themes.option(theme, theme, () -> {
                Configuration.getConfiguration().setSelectedTheme(theme);
            });

        panel.add(themes);

        panel.add(new ComponentSpinner().label("Editor font size:")
                .initialValue(Configuration.getConfiguration().getEditorFontSize())
                .action(i -> Configuration.getConfiguration().setEditorFontSize(i)));
        panel.add(new ComponentSpinner().label("General font size:")
                .initialValue(Configuration.getConfiguration().getGeneralFontSize())
                .action(i -> Configuration.getConfiguration().setGeneralFontSize(i)));
        panel.add(new ComponentSpinner().label("Project Tree font size:")
                .initialValue(Configuration.getConfiguration().getProjectTreeFontSize())
                .action(i -> Configuration.getConfiguration().setProjectTreeFontSize(i)));

        panel.add(new ComponentComboBox()
                .label("Editor theme:")
                .option("software", "Same as Main Theme")
                .option("light", "Light")
                .option("dark", "Dark")
                .action(sel -> Configuration.getConfiguration().setEditorTheme(sel))
        );

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();

        ComponentComboBox fontBox = new ComponentComboBox()
                .label("Editor font:");
        for (String font : fonts) {
            fontBox.option(font, font);
        }
        fontBox.initialValue(Configuration.getConfiguration().getEditorFont());
        fontBox.action(font -> Configuration.getConfiguration().setEditorFont(font));

        panel.add(fontBox);
        RSyntaxTextArea areaTemp = new RSyntaxTextArea(20, 60);
        areaTemp.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

        panel.add(new ComponentHorizontalLine());
        panel.add(new ComponentLabel(true).text("Colors").font(new Font("Arial", Font.BOLD, 15)));
        Map<String, Integer> tokenTypes = Configuration.ColorSettings.TOKEN_TYPES;
        for (String t : tokenTypes.keySet()) {
            ComponentColorPicker picker = new ComponentColorPicker();
            Color defCol = areaTemp.getSyntaxScheme().getStyle(tokenTypes.get(t)).foreground;
            picker.defaultColor(defCol);
            Color savedVal = Configuration.getConfiguration()
                    .getEditorColors().getColor(tokenTypes.get(t));
            if (savedVal != null) picker.initialValue(savedVal);
            else picker.initialValue(defCol);
            picker.label(t.toLowerCase())
                    .resetAction(() -> Configuration.getConfiguration().getEditorColors().removeColor(tokenTypes.get(t)))
                    .action(color ->
                        Configuration.getConfiguration().getEditorColors().setColor(tokenTypes.get(t), color));
            panel.add(picker);
        }

        return panel;
    }
}
