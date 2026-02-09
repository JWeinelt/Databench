package de.julianweinelt.databench.flow.setup;

import de.julianweinelt.databench.dbx.api.DbxAPI;
import de.julianweinelt.databench.dbx.api.drivers.DriverDownloadWrapper;
import de.julianweinelt.databench.dbx.api.drivers.DriverDownloader;
import de.julianweinelt.databench.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.databench.dbx.database.DatabaseMetaData;
import de.julianweinelt.databench.dbx.database.DatabaseRegistry;
import de.julianweinelt.databench.flow.Flow;
import de.julianweinelt.databench.flow.storage.DatabaseChecker;
import de.julianweinelt.databench.flow.storage.LocalStorage;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class SetupManager {
    public List<String> getAvailableHostNames() {
        List<String> availableHostNames = new java.util.ArrayList<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress address : addresses) {
                availableHostNames.add(address.getHostAddress());
            }
        } catch (Exception e) {
            log.error("Failed to get local host names: {}", e.getMessage());
        }
        availableHostNames.add("0.0.0.0");
        availableHostNames.add("localhost");
        return availableHostNames;
    }

    public List<String> getBooleans() {
        return List.of("true", "false", "yes", "no", "1", "0", "y", "n");
    }

    public boolean checkBoolInput(String input) {
        return List.of("true", "yes", "y", "1").contains(input.toLowerCase());
    }


    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    public void startCLI() {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            startCLI(terminal);
        } catch (IOException e) {
            log.error("Failed to start terminal CLI: {}", e.getMessage());
        }
    }


    public void startCLI(Terminal terminal) {
        String databaseType = prompt(terminal, "Welcome to DataBench Flow! Before you can get started using this" +
                " tool, some information for running is needed. First of all, what's your database type?", "",
                List.of("MySQL", "SQLServer", "MariaDB", "PostgreSQL, Derby"));
        clearScreen();
        if (!new File(DbxAPI.driversFolder(), databaseType.toLowerCase() + ".jar").exists()) {
            log.info("Downloading driver for {}...", databaseType);
            DriverDownloader.download(databaseType.toLowerCase(), DriverDownloadWrapper.latestVersion(databaseType.toLowerCase())).join();
            log.info("Driver downloaded successfully!");
            log.info("Registering downloaded file...");
            try {
                DriverManagerService.instance().preloadDrivers();
            } catch (Exception e) {
                log.error("Failed to register driver: {}", e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        } else {
            log.info("Driver for {} is already installed!", databaseType);
        }
        clearScreen();
        String userName = prompt(terminal, "You have to create a service account on your server. Please enter the username.",
                "", List.of());
        clearScreen();
        String password = prompt(terminal, "Please enter the password.", "", List.of());
        String safePass = password.replaceAll("\\w", "*");
        DatabaseMetaData metaData = DatabaseRegistry.instance().getMeta(databaseType.toLowerCase());
        clearScreen();
        System.out.printf("""
                    INFORMATION
                ===================
                Where can I add the host and port?
                --> Flow is using the default values on setup. You can change it afterwards.
                
                Your entered information:
                DATABASE: %s
                HOST: localhost:%s
                USERNAME: %s
                PASSWORD: %s
                %n""", databaseType, metaData.defaultPort(), userName, safePass);
        String correct = prompt(terminal, "Is everything correct?", "yes", getBooleans());
        if (checkBoolInput(correct)) {
            log.info("Testing connection...");
            String url = metaData.jdbcURL().replace("${server}", "localhost:" + metaData.defaultPort())
                    .replace("${database}", "flow_meta").replace("${parameters}", metaData.parameters(metaData.defaultParameters().build()));
            boolean found = DatabaseChecker.canConnect(url, userName, password, Duration.of(5, ChronoUnit.SECONDS));

            if (!found) {
                log.error("Could not connect to database! Please check your credentials.");
                startCLI(terminal);
                return;
            } else {
                log.info("Connection successful!");
            }

            log.info("Starting Flow... This may take a moment...");
            LocalStorage.instance().getConfig().setDbPassword(password);
            LocalStorage.instance().getConfig().setDbUser(userName);
            LocalStorage.instance().save();
            Flow.instance().restart();
        }

    }

    private String prompt(Terminal terminal, String promptMessage, String defaultValue, List<String> completions) {
        StringBuilder completionMessage = new StringBuilder();
        int idx = 0;
        for (String completion : completions) {
            completionMessage.append(completion);
            if (!(idx == completions.size()-1))
                completionMessage.append("; ");
            idx++;
        }

        String prompt = promptMessage;
        if (defaultValue != null && !defaultValue.isEmpty()) {
            prompt += " [" + defaultValue + "]";
        }

        if (!completionMessage.toString().isEmpty()) {
            prompt += "\nAvailable answers: ";
            prompt += completionMessage;
        }
        prompt += ": ";

        LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(terminal);

        if (!completions.isEmpty()) {
            readerBuilder.completer(new StringsCompleter(completions));
        }

        LineReader reader = readerBuilder.build();
        reader.setOpt(LineReader.Option.AUTO_LIST);
        reader.setOpt(LineReader.Option.MOUSE);

        String input = reader.readLine(prompt);
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        if (!completions.contains(input) && !completions.isEmpty()) {
            log.error("Invalid input! Please try again.");
            return prompt(terminal, promptMessage, defaultValue, completions);
        }
        return input.trim();
    }
}
