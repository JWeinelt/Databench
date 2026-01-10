package de.julianweinelt.databench.api;

import de.julianweinelt.databench.DataBench;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Getter
@Slf4j
public class DriverManagerService {
    private final List<String> loadedDrivers = new ArrayList<>();

    private URLClassLoader driverLoader;

    public static DriverManagerService instance() {
        return DataBench.getInstance().getDriverManagerService();
    }

    public void preloadDrivers() throws SQLException, IOException {
        File folder = new File("drivers");
        File[] driverFiles = folder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (driverFiles == null || driverFiles.length == 0) {
            log.info("No additional drivers found");
            return;
        }

        URL[] urls = new URL[driverFiles.length];
        for (int i = 0; i < driverFiles.length; i++) {
            urls[i] = driverFiles[i].toURI().toURL();
        }

        driverLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        ServiceLoader<Driver> serviceLoader = ServiceLoader.load(Driver.class, driverLoader);

        int count = 0;
        for (Driver driver : serviceLoader) {
            if (loadedDrivers.contains(driver.getClass().getName())) {
                log.info("Driver {} is already loaded. Skipping...", driver.getClass().getName());
                continue;
            }
            DriverManager.registerDriver(new DriverShim(driver));
            log.info("Loaded driver: {}", driver.getClass().getName());
            loadedDrivers.add(driver.getClass().getName());
            count++;
        }

        if (count == 0) {
            log.warn("No JDBC drivers found in drivers folder");
        }
    }

}