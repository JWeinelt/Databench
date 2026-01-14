package de.julianweinelt.databench.dbx.api.drivers;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

@Slf4j
public class DriverShim implements Driver {
    private final Driver driver;

    public DriverShim(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        try {
            return driver.connect(url, info);
        } catch (NoClassDefFoundError e) {
            JOptionPane.showMessageDialog(null, "No suitable driver found for this project.", "Error opening project", JOptionPane.ERROR_MESSAGE);
            log.warn("SQL connection failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }
    public Driver getDelegate() {
        return driver;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }
}
