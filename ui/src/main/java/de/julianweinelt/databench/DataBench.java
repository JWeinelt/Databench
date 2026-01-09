package de.julianweinelt.databench;

import com.formdev.flatlaf.FlatDarkLaf;
import de.julianweinelt.databench.api.DriverManagerService;
import de.julianweinelt.databench.api.DriverShim;
import de.julianweinelt.databench.api.FileManager;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.LanguageManager;
import de.julianweinelt.databench.ui.StartScreen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class DataBench {

    private static final int PORT = 43210;

    @Getter
    private BenchUI ui;

    @Getter
    private static DataBench instance;

    @Getter
    private DriverManagerService driverManagerService;

    @Getter
    private LanguageManager languageManager;
    @Getter
    private ConfigManager configManager = null;
    @Getter
    private ProjectManager projectManager;
    @Getter
    private FileManager fileManager;

    private final CountDownLatch latch = new CountDownLatch(1);

    private StartScreen startScreen;

    public static void main(String[] args) {
        // Prüfen, ob schon eine Instanz läuft
        if (isAnotherInstanceRunning(args)) {
            log.info("Another instance is already running. Forwarded files to it.");
            return;
        }

        instance = new DataBench();
        instance.start(args);
    }

    public void start(String[] filesToOpen) {
        startScreen = new StartScreen();
        startScreen.start();
        try {Thread.sleep(1000);} catch (InterruptedException ignored) { /* Ignored */ }
        driverManagerService = new DriverManagerService();
        try {
            driverManagerService.preloadDrivers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "DataBench failed to load some Database drivers.\n\n" +
                    "Message reported by system: \n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }

        log.info("Starting DataBench");
        log.info("Preparing to load configurations...");

        if (!new File("databench.config").exists()) {
            prepare();
            return;
        }

        if (configManager == null) configManager = new ConfigManager();
        log.info("Loading configuration...");
        configManager.loadConfig();

        log.info("Loading project data...");
        projectManager = new ProjectManager();
        projectManager.loadAllProjects(configManager.getConfiguration().getEncryptionPassword());

        fileManager = new FileManager();
        languageManager = new LanguageManager();
        log.info("Loading language data...");
        languageManager.preload().thenAccept(v -> {
            latch.countDown();
        });

        try {
            latch.await();

            log.info("Initializing UI...");
            ui = new BenchUI();
            ui.start();

            startScreen.stop();

            if (filesToOpen != null) {
                for (String path : filesToOpen) {
                    File f = new File(path);
                    if (f.exists() && ui != null) {
                        final File fileToOpen = f;
                        SwingUtilities.invokeLater(() -> {
                            //TODO: Open file in UI
                        });
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        startSocketListener();
    }

    private void prepare() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("Welcome!");
        frame.setIconImage(icon);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcomeLabel = new JLabel("Welcome to DataBench! Please enter a strong encryption password to continue:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(welcomeLabel, gbc);

        JPasswordField passwordField = new JPasswordField();
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(passwordField, gbc);

        JButton okButton = new JButton("Save");
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(okButton, gbc);

        JButton cancelButton = new JButton("Cancel");
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        okButton.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            Configuration configuration = new Configuration();
            configuration.setLocale("en_us");
            configuration.setEncryptionPassword(password);
            configuration.setSelectedTheme("dark");
            configManager = new ConfigManager(configuration);
            configManager.saveConfig();
            start(new String[0]);
            frame.dispose();
        });

        cancelButton.addActionListener(e -> System.exit(0));

        frame.add(panel);
        frame.setVisible(true);

    }

    private static boolean isAnotherInstanceRunning(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            serverSocket.close();
            return false;
        } catch (Exception e) {
            try (Socket socket = new Socket("localhost", PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                for (String filePath : args) {
                    out.println(filePath);
                }
            } catch (Exception ex) {
                log.error("Failed to send files to running instance", ex);
            }
            return true;
        }
    }

    private void startSocketListener() {
        Thread listenerThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    try (Socket client = serverSocket.accept();
                         Scanner in = new Scanner(client.getInputStream())) {
                        while (in.hasNextLine()) {
                            String filePath = in.nextLine();
                            final File file = new File(filePath);
                            if (file.exists() && ui != null) {
                                SwingUtilities.invokeLater(() -> {
                                    //TODO: Open file in UI
                                });
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error handling incoming file", e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to start server socket", e);
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}