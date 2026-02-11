package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.ui.SettingsPanel;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import de.julianweinelt.databench.dbx.api.ui.components.*;
import de.julianweinelt.databench.dbx.util.LanguageManager;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;
import java.util.Map;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

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
        SettingsPanel panel = new SettingsPanel(translate("page.settings.general.title"));
        ComponentComboBox lang = new ComponentComboBox();
        lang.label(translate("page.settings.general.lang"));

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
                    .label(translate("page.settings.general.open-last-proj")));

        panel.add(new ComponentCheckbox().label(translate("page.settings.general.safe-query")));

        panel.add(new ComponentCheckbox()
                .label(translate("page.settings.general.stats"))
                .action(obj -> Configuration.getConfiguration().setSendAnonymousData(obj.value()))
                .initialValue(Configuration.getConfiguration().isSendAnonymousData())
        );
        panel.add(new ComponentCheckbox()
                .label(translate("page.settings.general.send-err"))
                .action(obj -> Configuration.getConfiguration().setSendErrorProtocols(obj.value()))
                .initialValue(Configuration.getConfiguration().isSendErrorProtocols())
        );
        panel.add(new ComponentComboBox()
                .label(translate("page.settings.general.close-action.title"))
                .option("ask", translate("page.settings.general.close-action.ask"),
                        () -> Configuration.getConfiguration().setCloseType("ask"))
                .option("save_disconnect", translate("page.settings.general.close-action.save_disconnect")
                        , () -> Configuration.getConfiguration().setCloseType("save_disconnect"))
                .option("ask_save_disconnect", translate("page.settings.general.close-action.ask_save_disconnect"),
                        () -> Configuration.getConfiguration().setCloseType("ask_save_disconnect"))
                .option("disconnect", translate("page.settings.general.close-action.disconnect"),
                        () -> Configuration.getConfiguration().setCloseType("disconnect"))
        );
        panel.add(new ComponentCheckbox()
                .label(translate("page.settings.general.save-on-close"))
                //TODO: Add action
        );
        panel.add(new ComponentHorizontalLine());
        panel.add(new ComponentLabel(true).text(translate("page.settings.general.updates.title")).font(new Font("Arial", Font.BOLD, 15)));
        panel.add(new ComponentCheckbox().label(translate("page.settings.general.updates.check")));
        panel.add(new ComponentComboBox()
                .label(translate("page.settings.general.updates.channel"))
                .option("stable", translate("page.settings.general.updates.channel.stable"))
                .option("beta", translate("page.settings.general.updates.channel.beta"))
                .option("dev", translate("page.settings.general.updates.channel.dev"))
                .action(mode -> Configuration.getConfiguration().setUpdateChannel(mode))
        );
        return panel;
    }

    private SettingsPanel createAppearancePanel() {
        SettingsPanel panel = new SettingsPanel(translate("page.settings.appearance.title"));

        ComponentComboBox themes = new ComponentComboBox()
                .label(translate("page.settings.appearance.theme"));

        Registry.instance().themeData().forEach((theme, plugin) ->
                themes.option(plugin + ":" + theme, translate("theme." + theme)));
        themes.action(themeDat -> {
            String theme = themeDat.split(":")[1];
            String plugin = themeDat.split(":")[0];
            log.debug("Selected theme {} by {}", theme, plugin);
            Configuration.getConfiguration().setSelectedTheme(plugin + ":" + theme);
            ThemeSwitcher.switchTheme(theme, Registry.instance().getPlugin(plugin));
        });
        themes.initialValue(Configuration.getConfiguration().getSelectedTheme());

        panel.add(themes);

        panel.add(new ComponentSpinner().label(translate("page.settings.appearance.font-size.editor"))
                .initialValue(Configuration.getConfiguration().getEditorFontSize())
                .action(i -> Configuration.getConfiguration().setEditorFontSize(i)));
        panel.add(new ComponentSpinner().label(translate("page.settings.appearance.font-size.general"))
                .initialValue(Configuration.getConfiguration().getGeneralFontSize())
                .action(i -> Configuration.getConfiguration().setGeneralFontSize(i)));
        panel.add(new ComponentSpinner().label(translate("page.settings.appearance.font-size.proj-tree"))
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
                .label(translate("page.settings.appearance.font.editor"));
        for (String font : fonts) {
            fontBox.option(font, font);
        }
        fontBox.initialValue(Configuration.getConfiguration().getEditorFont());
        fontBox.action(font -> Configuration.getConfiguration().setEditorFont(font));

        panel.add(fontBox);
        RSyntaxTextArea areaTemp = new RSyntaxTextArea(20, 60);
        areaTemp.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);

        panel.add(new ComponentHorizontalLine());
        panel.add(new ComponentLabel(true).text(translate("page.settings.appearance.colors"))
                .font(new Font("Arial", Font.BOLD, 15)));
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
