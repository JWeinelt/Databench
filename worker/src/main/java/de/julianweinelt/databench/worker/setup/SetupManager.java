package de.julianweinelt.databench.worker.setup;

import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.net.InetAddress;
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
        return List.of("true", "false", "yes", "no", "on", "off", "1", "0");
    }

    public boolean checkBoolInput(String input) {
        return List.of("true", "yes", "on", "1").contains(input.toLowerCase());
    }


    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    public void startCLI() {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            String databaseType = prompt(terminal, "Welcome to the DataBench DBX Worker! Before you can get started using this" +
                    " tool, some information for running is needed. First of all, what's your database type?", "",
                    List.of("MySQL", "SQLServer", "MariaDB", "PostgreSQL, Derby"));
            clearScreen();
            String userName = prompt(terminal, "You have to create a service account on your server. Please enter the username.",
                    "", List.of());
            clearScreen();
            String password = prompt(terminal, "Please enter the password.", "", List.of());
            String safePass = password.replaceAll("\\w", "*");
            clearScreen();
            System.out.println("""
                        INFORMATION
                    ===================
                    Where can I add the host and port?
                    --> DBX Worker is using the default values on setup. You can change it afterwards.
                    
                    Your entered information:
                    DATABASE: %s
                    HOST: localhost:%s
                    USERNAME: %s
                    PASSWORD: %s
                    """.formatted(databaseType, databaseType, userName, safePass));
            String makeTest = prompt(terminal, "Do you want to perform a connection test?", "yes", getBooleans());
            if (checkBoolInput(makeTest)) {

            }
        } catch (IOException e) {
            log.error("Failed to start terminal CLI: {}", e.getMessage());
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
