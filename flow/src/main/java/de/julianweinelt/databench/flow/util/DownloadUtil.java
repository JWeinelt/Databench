package de.julianweinelt.databench.flow.util;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
public class DownloadUtil {
    public void downloadFile(String fileURL, String outputPath) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Java Downloader");

        int contentLength = connection.getContentLength();
        if (contentLength < 0) {
            log.warn("Content-Length could not be determined.");
            contentLength = 0;
        }

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(outputPath);
             ProgressBar pb = new ProgressBarBuilder()
                     .setTaskName("Downloading")
                     .setStyle(ProgressBarStyle.ASCII)
                     .setUnit("B", 1024)
                     .setInitialMax(contentLength)
                     .build()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                pb.stepBy(bytesRead);
            }
        }

        log.info("Download finished.");
    }
}
