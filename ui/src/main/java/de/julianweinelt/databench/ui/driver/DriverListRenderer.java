package de.julianweinelt.databench.ui.driver;

import de.julianweinelt.databench.dbx.api.drivers.DriverShim;

import javax.swing.*;
import java.awt.*;
import java.sql.Driver;

public class DriverListRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Driver driver) {

            Driver realDriver = driver;
            if (driver instanceof DriverShim shim) {
                realDriver = shim.getDelegate();
            }

            String simpleName = realDriver.getClass().getSimpleName();
            String fqcn = realDriver.getClass().getName();

            setText(simpleName);
            setToolTipText(fqcn);
        }

        return this;
    }
}
