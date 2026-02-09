package de.julianweinelt.databench.dbx.api.drivers;

import de.julianweinelt.databench.dbx.api.DbxAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.IntConsumer;

public class DriverDownloader {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static CompletableFuture<File> download(String db, String version) {
        DriverDownloadWrapper.DriverDownload driver =
                DriverDownloadWrapper.getForDB(db, version);

        if (driver == null)
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unsupported DB: " + db));

        return download(driver, DbxAPI.driversFolder(), null);
    }

    public static CompletableFuture<File> download(
            String db,
            String version,
            IntConsumer progress
    ) {

        DriverDownloadWrapper.DriverDownload driver =
                DriverDownloadWrapper.getForDB(db, version);

        if (driver == null)
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unsupported DB: " + db));

        return download(driver, DbxAPI.driversFolder(), progress);
    }

    public static CompletableFuture<File> download(
            DriverDownloadWrapper.DriverDownload driver,
            File targetFolder,
            IntConsumer progress) {
        try {
            if (!targetFolder.exists())
                targetFolder.mkdirs();

            File target = new File(targetFolder, driver.fileName());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(driver.url()))
                    .GET()
                    .build();
            return CLIENT
                    .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenCompose(response -> {

                        if (response.statusCode() >= 400)
                            return CompletableFuture.failedFuture(
                                    new RuntimeException("HTTP " + response.statusCode()));

                        long contentLength = response.headers()
                                .firstValueAsLong("Content-Length")
                                .orElse(-1);

                        return CompletableFuture.supplyAsync(() -> {
                            try (InputStream in = response.body();
                                    FileOutputStream out = new FileOutputStream(target)) {

                                byte[] buffer = new byte[8192];

                                long readTotal = 0;
                                int read;
                                int lastPercent = 0;

                                while ((read = in.read(buffer)) != -1) {

                                    out.write(buffer, 0, read);
                                    readTotal += read;

                                    if (progress != null && contentLength > 0) {

                                        int percent = (int) ((readTotal * 100) / contentLength);

                                        if (percent != lastPercent) {
                                            lastPercent = percent;
                                            progress.accept(percent);
                                        }
                                    }
                                }

                                if (driver.zipped()) {
                                    DriverDownloadWrapper.postProcess(target);
                                    return new File(targetFolder, driver.fileName().replace(".tar.gz", ".jar"));
                                }

                                return target;
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            }
                        });

                    });

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
