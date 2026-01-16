package de.julianweinelt.databench.dbx.api.ui.menubar;

import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MenuManager {
    @Getter
    private final HashMap<DbxPlugin, List<Menu>> menus = new HashMap<>();

    public static MenuManager instance() {
        return UIService.instance().getMenuManager();
    }

    public void register(Menu menu, DbxPlugin plugin) {
        if (menus.containsKey(plugin)) {
            menus.get(plugin).add(menu);
        } else {
            List<Menu> menuList = new ArrayList<>();
            menuList.add(menu);
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
