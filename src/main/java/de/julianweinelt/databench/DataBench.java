package de.julianweinelt.databench;

import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

@Slf4j
public class DataBench {

    private static final int PORT = 43210;

    @Getter
    private BenchUI ui;

    @Getter
    private static DataBench instance;

    @Getter
    private ConfigManager configManager;

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
        log.info("Starting DataBench");
        log.info("Preparing to load configurations...");

        configManager = new ConfigManager();
        configManager.loadConfig();

        log.info("Initializing UI...");
        ui = new BenchUI();
        ui.start();

        if (filesToOpen != null) {
            for (String path : filesToOpen) {
                File f = new File(path);
                if (f.exists() && ui != null) {
                    final File fileToOpen = f;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        //TODO: Open file in UI
                    });
                }
            }
        }

        startSocketListener();
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
                                javax.swing.SwingUtilities.invokeLater(() -> {
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