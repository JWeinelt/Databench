package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.ui.BenchUI;

import javax.swing.*;
import java.util.UUID;

public interface IEditorTab {
    UUID id = UUID.randomUUID();

    JPanel getTabComponent(BenchUI ui, DConnection connection);
    String getTitle();
}
