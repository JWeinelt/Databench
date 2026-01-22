package de.julianweinelt.databench.dbx.api.ui.menubar;

import javax.swing.*;

public abstract class MenuComponent<T extends JMenuItem> {

    protected MenuComponent() {}

    public abstract T create();
}