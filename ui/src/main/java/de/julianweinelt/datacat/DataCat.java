package de.julianweinelt.datacat;

import com.formdev.flatlaf.FlatDarkLaf;
import de.julianweinelt.datacat.api.DConnection;
import de.julianweinelt.datacat.api.FileManager;
import de.julianweinelt.datacat.api.StatisticUtil;
import de.julianweinelt.datacat.data.ConfigManager;
import de.julianweinelt.datacat.data.Configuration;
import de.julianweinelt.datacat.data.ProjectManager;
import de.julianweinelt.datacat.data.SystemPlugin;
import de.julianweinelt.datacat.dbx.api.DbxAPI;
import de.julianweinelt.datacat.dbx.api.Registry;
import de.julianweinelt.datacat.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.datacat.dbx.api.events.Event;
import de.julianweinelt.datacat.dbx.api.plugins.PluginLoader;
import de.julianweinelt.datacat.dbx.api.plugins.YarnLoader;
import de.julianweinelt.datacat.dbx.api.ui.UIService;
import de.julianweinelt.datacat.dbx.util.LanguageManager;
import de.julianweinelt.datacat.service.UpdateChecker;
import de.julianweinelt.datacat.ui.BenchUI;
import de.julianweinelt.datacat.ui.DefaultUI;
import de.julianweinelt.datacat.ui.StartScreen;
import de.julianweinelt.datacat.util.SecretManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DataCat {
    public static String version = "unknown";

    private static final int PORT = 43210;
    public static boolean shouldUpdate = false;
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    private static boolean devMode = false;
    private int wrongPasswordCounter = 0;

    @Getter
    private BenchUI ui;

    @Getter
    private static DataCat instance;
    @Getter
    private final DbxAPI api;
    @Getter
    private PluginLoader pluginLoader;

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

    @Getter
    private YarnLoader yarnLoader;

    @Getter
    private UpdateChecker updateChecker;

    @Getter @Setter
    private JFrame overFrame = null;

    private final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) {
        if (isAnotherInstanceRunning(args)) {
            log.info("Another instance is already running. Forwarded files to it.");
            return;
        }

        if (Arrays.stream(args).toList().contains("--dev-mode")) devMode = true;

        instance = new DataCat();
        instance.start(args);
    }

    public DataCat() {
        log.info("Initializing DataCat...");
        if (devMode) log.info("############### DEV MODE ENABLED #############");
        Properties props = new Properties();
        try (InputStream is = DataCat.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            props.load(is);
        } catch (IOException e) {
            log.error("Could not load application.properties. Version could not be determined.");
            log.error(e.getMessage(), e);
        }

        String version = props.getProperty("app.version");
        log.info("Starting DataCat v{}", version);
        DataCat.version = version;
        log.info("Starting event queue...");
        api = new DbxAPI(new File("api"), new SystemPlugin());
    }

    public void start(String[] filesToOpen) {
        StartScreen startScreen = new StartScreen();
        startScreen.start();
        //try {Thread.sleep(1000);} catch (InterruptedException ignored) { /* Ignored */ }
        driverManagerService = new DriverManagerService();
        try {
            driverManagerService.preloadDrivers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "DataCat failed to load some Database drivers.\n\n" +
                    "Message reported by system: \n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }

        log.info("Starting DataCat");
        log.info("Preparing to load configurations...");

        if (!new File("datacat.config").exists()) {
            prepare();
            return;
        }

        if (configManager == null) configManager = new ConfigManager();
        log.info("Loading configuration...");
        configManager.loadConfig();
        configManager.getConfiguration().initHomeDirectories();
        log.info("Loaded DataCat pre-config with installation id: {}", configManager.getConfiguration().getInstallationID());

        if (configManager.getConfiguration().isSendAnonymousData()) StatisticUtil.sendStartup();

        log.info("Loading project data...");
        projectManager = new ProjectManager();
        String password = SecretManager.loadPassword();
        if (password == null) {
            password = askForPassword();
            while (true) {
                boolean success = projectManager.loadAllProjects(password);
                if (success) break;
                wrongPasswordCounter++;
                log.warn("Wrong password.");
                password = askForPassword();
            }
            SecretManager.save(password);
        } else {
            projectManager.loadAllProjects(password);
        }

        fileManager = new FileManager();

        languageManager = new LanguageManager(devMode);
        log.info("Loading language data...");
        api.getRegistry().registerListener(languageManager, api.getSystemPlugin());

        log.info("Starting plugin service...");
        log.info("Loading plugins...");
        yarnLoader = new YarnLoader();
        pluginLoader = new PluginLoader(api);
        pluginLoader.loadAll();
        languageManager.preload(Configuration.getConfiguration().getLocale()).thenAccept(v -> latch.countDown());

        log.info("Initializing UI...");
        ui = new BenchUI();
        ui.loadTheme(false);

        api.getRegistry().registerListener(ui, api.getSystemPlugin());

        try {
            latch.await();

            log.info("Initializing UI...");
            new DefaultUI(UIService.instance()).init();
            updateChecker = new UpdateChecker(ui);
            ui.init();
            Registry.instance().setMainFrame(ui.getFrame());


            if (filesToOpen != null) {
                for (String path : filesToOpen) {
                    File f = new File(path);
                    if (f.exists() && ui != null) {
                        final File fileToOpen = f;
                        SwingUtilities.invokeLater(() -> openFile(fileToOpen));
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        startSocketListener();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down DataCat...");
            Registry.instance().callEvent(new Event("DataCatShutdownEvent"));
            log.info("Disabling plugin service...");
            pluginLoader.unloadAll();
            log.info("Plugins unloaded.");

            Configuration.getConfiguration().setFirstStartup(false);

            if (shouldUpdate) {
                ProcessBuilder pb = new ProcessBuilder(
                        "DataCat.exe",
                        "--update"
                );
                try {
                    pb.start();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info("Closing UI...");
            ui.stop();
            log.info("Saving configuration...");
            configManager.saveConfig();
            log.info("Saved configuration to disk");
            log.info("Stopping socket...");
            stopSocket();
            log.info("Goodbye!");
        }));


        log.info("Initializing plugins...");
        pluginLoader.initializeAll();

        log.info("Startup finished.");
        log.info("Driver count: {}", DriverManagerService.instance().getLoadedDrivers().size());
        DriverManagerService.instance().getLoadedDrivers().forEach(d -> log.info("Driver found: {}", d));

        Registry.instance().callEvent(new Event("UIServiceEnabledEvent").set("service", UIService.instance()));

        startScreen.stop();
        Registry.instance().callEvent(new Event("UIInitializedEvent").set("frame", ui.getFrame()));
        SwingUtilities.invokeLater(() -> ui.loadTheme(true));

    }

    private void openFile(File fileToOpen) {
        if (fileToOpen.getName().endsWith(".dbproj")) {
            // Project file
            ui.importProfilePopupPreDefinedFile(fileToOpen);
            return;
        }

        if (ui.hasLightEdit()) {
            DConnection connection = ui.getLightEdit();
            if (connection == null) return; // Will NEVER happen
            connection.handleFileEvent(fileToOpen);
        } else {
            ui.createLightEdit().handleFileEvent(fileToOpen);
        }
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

        JLabel welcomeLabel = new JLabel("Welcome to DataCat! Please enter a strong encryption password to continue:");
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
                if (args.length == 0) {
                    out.println("!!ATTENTION!!");
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
                while (!shutdown.get()) {
                    try (Socket client = serverSocket.accept();
                         Scanner in = new Scanner(client.getInputStream())) {
                        while (in.hasNextLine()) {
                            String filePath = in.nextLine();
                            if (filePath.equals("!!ATTENTION!!")) {
                                ui.getFrame().requestFocus();
                                return;
                            }
                            final File file = new File(filePath);
                            if (file.exists() && ui != null) {
                                SwingUtilities.invokeLater(() -> {
                                    openFile(file);
                                    ui.getFrame().requestFocus();
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

    private void stopSocket() {
        shutdown.set(true);
    }

    private String askForPassword() {
        FlatDarkLaf.setup();

        JDialog dialog = new JDialog((Frame) null, "Encryption Password", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(400, 180);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("Please enter your encryption password.");
        JPasswordField passwordField = new JPasswordField();

        JLabel resetLabel = new JLabel("<html><a href='#'>Forgot password?</a></html>");
        resetLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        resetLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openResetProcedureWindow();
            }
        });

        content.add(infoLabel);
        content.add(Box.createVerticalStrut(10));
        content.add(passwordField);
        content.add(Box.createVerticalStrut(10));
        content.add(resetLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        final String[] result = new String[1];

        okButton.addActionListener(e -> {
            result[0] = new String(passwordField.getPassword());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);

        return result[0];
    }

    private void openResetProcedureWindow() {
        JDialog resetDialog = new JDialog((Frame) null, "Reset Password", true);
        resetDialog.setSize(450, 300);
        resetDialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel label = new JLabel("<html>Here you can implement your reset procedure.<br>Guide the user step by step.</html>");
        panel.add(label, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> resetDialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);

        resetDialog.add(panel, BorderLayout.CENTER);
        resetDialog.add(bottom, BorderLayout.SOUTH);

        resetDialog.setVisible(true);
    }
}