package de.julianweinelt.datacat.server.storage;

import de.julianweinelt.datacat.server.util.CryptoUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter @Setter
public class Configuration {
    private DatabaseConfig database = new DatabaseConfig();
    private HostConfig storeEndpoint = new HostConfig(51385);
    private MailConfig mail = new MailConfig();

    private String jwtSecret = CryptoUtil.generateSecret(20);
    private int tokenLifetime = 30;
    private String lifeTimeUnit = "DAY_OF_THE_MONTH";

    private String tokenClaim = UUID.randomUUID().toString();

    @Getter @Setter @AllArgsConstructor
    public static class DatabaseConfig {
        private String host = "localhost";
        private int port = 3306;
        private String schemaName = "datacat_portal";
        private String username = "portal";
        private String password = "secret";

        private List<String> parameters = new ArrayList<>();

        public DatabaseConfig() {
            parameters.add("useJDBCCompliantTimezoneShift=true");
            parameters.add("useLegacyDatetimeCode=false");
            parameters.add("serverTimezone=UTC");
            parameters.add("autoReconnect=true");
        }
    }

    @Getter @Setter
    public static class HostConfig {
        private String host = "0.0.0.0";
        private int port = 51385;

        public HostConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public HostConfig(int port) {
            this.port = port;
        }
        public HostConfig() {}
    }

    @Getter @Setter
    public static class MailConfig {
        private String smtpHost = "smtp.example.com";
        private int smtpPort = 587;

        private String username = "noreply@datacat.de";
        private String password = "secret";
        private String senderMail = "noreply@datacat.de";
        private String senderName = "DataCat Portal";
        private final HashMap<String, String> properties = new HashMap<>();

        public MailConfig() {
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
        }
    }
}
