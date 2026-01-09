package de.julianweinelt.databench.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Slf4j
public class FileUtil {
    public static String readFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) builder.append(line).append("\n");
            return builder.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }
}
