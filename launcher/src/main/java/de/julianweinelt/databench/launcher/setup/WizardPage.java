package de.julianweinelt.databench.launcher.setup;

import javax.swing.*;

public interface WizardPage {

    String getId();
    JComponent getView();

    default void onEnter() {}
    default void onLeave() {}

    boolean canGoNext();
}
