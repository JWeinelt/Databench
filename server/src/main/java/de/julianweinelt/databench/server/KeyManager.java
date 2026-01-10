package de.julianweinelt.databench.server;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;

@Slf4j
public class KeyManager {

    private static final String CHARSET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static void generateKey(boolean override) {
        File file = new File("key.txt");
        if (file.exists() && !override) return;
        StringBuilder sb = new StringBuilder(100);
        for (int i = 0; i < 100; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        try (FileWriter w = new FileWriter(file)) {
            w.write(sb.toString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    public static boolean checkKey(String key) {
        try {
            String k = Files.readString(new File("key.txt").toPath());
            return k.equals(key);
        } catch (IOException e) {
            return false;
        }
    }
}
