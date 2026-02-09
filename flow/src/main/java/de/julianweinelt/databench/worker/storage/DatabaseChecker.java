package de.julianweinelt.databench.worker.storage;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.*;

@Slf4j
public class DatabaseChecker {
    public static boolean canConnect(
            String jdbcUrl,
            String username,
            String password,
            Duration timeout
    ) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<Boolean> future = executor.submit(() -> {
                Properties props = new Properties();
                props.put("user", username);
                props.put("password", password);
                props.put("loginTimeout", String.valueOf(timeout.toSeconds()));

                try (Connection ignored = DriverManager.getConnection(jdbcUrl, props)) {
                    return true;
                }
            });

            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.error(e.getMessage(), e);
            return false;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            if (e.getCause() instanceof SQLException sql) {
                return false;
            }
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            executor.shutdownNow();
        }
    }
}
