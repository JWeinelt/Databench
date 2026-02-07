package de.julianweinelt.databench.dbx.api.ui.menubar;

import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MenuManager {
    @Getter
    private final HashMap<DbxPlugin, List<Menu>> menus = new HashMap<>();

    public static MenuManager instance() {
        return UIService.instance().getMenuManager();
    }

    public void register(Menu menu, DbxPlugin plugin) {
        register(plugin, menu);
    }

    public void register(DbxPlugin plugin, Menu... m) {
        int i = 0;
        for (Menu m1 : m) if (m1.getPriority() == -1) {
            m1.priority(100 - i);
            i++;
        }
        log.info("Registering {} toolbar menus for plugin {}", m.length, plugin.getName());
        if (menus.containsKey(plugin)) {
            menus.get(plugin).addAll(Arrays.asList(m));
        } else {
            List<Menu> menuList = new ArrayList<>(Arrays.asList(m));
            menus.putIfAbsent(plugin, menuList);
        }
        revalidate();
    }

    public List<Menu> getAllMenus() {
        List<Menu> menus = new ArrayList<>();
        for (DbxPlugin pl : this.menus.keySet()) {
            menus.addAll(this.menus.get(pl));
        }
        return menus;
    }


    public void unregister(DbxPlugin plugin) {
        this.menus.remove(plugin);
        revalidate();
    }

    private void revalidate() {
        Registry.instance().callEvent(new Event("UIMenuBarRevalidateEvent"));
    }
}