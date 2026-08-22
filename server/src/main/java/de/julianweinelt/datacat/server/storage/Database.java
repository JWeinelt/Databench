package de.julianweinelt.datacat.server.storage;

import de.julianweinelt.datacat.server.model.*;
import de.julianweinelt.datacat.server.util.ColorUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.UUID;

@Slf4j
public class Database {
    private static Database instance;

    public static Database instance() {
        return instance;
    }

    private Connection conn;

    private final Configuration.DatabaseConfig config;

    public Database(Configuration.DatabaseConfig config) {
        instance = this;
        this.config = config;
    }

    public boolean connect() {
        final String DB_URL = "jdbc:mariadb://" + config.getHost() + ":" + config.getPort() + "/"
                + config.getSchemaName() + constructParameters();

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            conn = DriverManager.getConnection(DB_URL, config.getUsername(), config.getPassword());
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to database: {}", e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            log.error("Failed to connect: MariaDB Driver class not found!");
        }
        return false;
    }

    public void disconnect() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close SQL: {}", e.getMessage(), e);
            }
        }
    }

    private boolean checkConnection() {
        if (conn == null) return false;
        try {
            if (conn.isClosed()) {
                return connect();
            } else return true;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private void afterSuccessFulConnection() {
        loadBadges();
        loadLinkTypes();
        loadAccountStatuses();
        loadVerificationStatuses();
    }

    private void loadBadges() {
        String sql = "SELECT BadgeID, Name, Description FROM badges";
        try (Statement st = conn.createStatement()) {
            ResultSet set = st.executeQuery(sql);
            while (set.next()) {
                new Badge(UUID.fromString(set.getString(1)), set.getString(2), set.getString(3)).register();
            }
        } catch (SQLException e) {
            log.error("Failed to get badge data: {}", e.getMessage(), e);
        }
    }

    private void loadAccountStatuses() {
        String sql = "SELECT StatusID, StatusName, Color FROM accounts_account_statuses";
        try (Statement st = conn.createStatement()) {
            ResultSet set = st.executeQuery(sql);
            while (set.next()) {
                new AccountStatus(UUID.fromString(set.getString(1)),
                        set.getString(2),
                        ColorUtil.fromText(set.getString(3))
                ).register();
            }
        } catch (SQLException e) {
            log.error("Failed to get badge data: {}", e.getMessage(), e);
        }
    }

    private void loadVerificationStatuses() {
        String sql = "SELECT StatusID, StatusName, Color FROM accounts_verification_statuses";
        try (Statement st = conn.createStatement()) {
            ResultSet set = st.executeQuery(sql);
            while (set.next()) {
                new VerificationStatus(UUID.fromString(set.getString(1)),
                        set.getString(2),
                        ColorUtil.fromText(set.getString(3))
                ).register();
            }
        } catch (SQLException e) {
            log.error("Failed to get badge data: {}", e.getMessage(), e);
        }
    }

    private void loadLinkTypes() {
        String sql = "SELECT LinkID, DisplayName, IconName, UrlRegex FROM link_type";
        try (Statement st = conn.createStatement()) {
            ResultSet set = st.executeQuery(sql);
            while (set.next()) {
                new LinkType(UUID.fromString(set.getString(1)),
                        set.getString(2),
                        set.getString(3),
                        set.getString(4)
                ).register();
            }
        } catch (SQLException e) {
            log.error("Failed to get link types: {}", e.getMessage(), e);
        }
    }

    @NotNull
    public Account loadAccount(UUID uniqueId) {
        Account account = new Account(uniqueId);
        try (PreparedStatement pS = conn.prepareStatement("SELECT UserID, eMail, Username, PasswordHashed, Created," +
                " LastLogin, AccountStatus, VerificationStatus FROM accounts WHERE UserID = ?")) {
            pS.setString(1, uniqueId.toString());
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                account.setStatus(AccountStatus.get(UUID.fromString(set.getString(7))));
                account.setVerificationStatus(VerificationStatus.get(UUID.fromString(set.getString(8))));
                account.setLastActivity(set.getLong(6));
                account.setUsername(set.getString(3));
                account.setEMail(set.getString(2));
                account.setPasswordHashed(set.getString(4));
                account.setCreated(set.getLong(5));
            }
        } catch (SQLException e) {
            log.error("Failed to load account: ", e);
        }
        loadAccountExtras(account);
        return account;
    }


    /**
     * Loads all additional information for an account from the database. This includes:
     * 1. Links associated with this account by user
     * 2. The badges an account has
     * 3. Account permissions on the site
     * 4. Additional details: About me, Subtitle
     * @param account The {@link Account} to write the fetched information into
     */
    private void loadAccountExtras(Account account) {
        try (PreparedStatement pS = conn.prepareStatement("SELECT LinkID, LinkUrl FROM account_links WHERE AccountID = ?")) {
            pS.setString(1, account.getUniqueID().toString());
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                UUID t = UUID.fromString(set.getString(1));
                LinkType type = LinkType.get(t);
                if (type == null) continue;
                account.getLinks().put(type, set.getString(2));
            }
        } catch (SQLException e) {
            log.error("Failed to load account links for {}", account.getUniqueID(), e);
        }

        try (PreparedStatement pS = conn.prepareStatement("SELECT Badge FROM account_badges WHERE AccountID = ?")) {
            pS.setString(1, account.getUniqueID().toString());
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                UUID b = UUID.fromString(set.getString(1));
                Badge badge = Badge.get(b).orElse(null);
                if (badge == null) continue;
                account.getBadges().add(badge);
            }
        } catch (SQLException e) {
            log.error("Failed to load badges of account {}", account.getUniqueID(), e);
        }

        try (PreparedStatement pS = conn.prepareStatement("SELECT PermissionName FROM account_permissions WHERE AccountID = ?")) {
            pS.setString(1, account.getUniqueID().toString());
            ResultSet set = pS.executeQuery();
            while (set.next()) {
                account.getPermissions().add(set.getString(1));
            }
        } catch (SQLException e) {
            log.error("Failed to get account permissions for {}", account.getUniqueID(), e);
        }

        try (PreparedStatement pS = conn.prepareStatement("SELECT AboutMe, SubTitle, DisplayName FROM account_details WHERE AccountID = ?")) {
            pS.setString(1, account.getUniqueID().toString());
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                account.setAboutMe(set.getString(1));
            }
        } catch (SQLException e) {
            log.error("Failed to get account details for {}", account.getUniqueID(), e);
        }
    }

    private String constructParameters() {
        StringBuilder b = new StringBuilder();
        int i = 0;
        for (String param : config.getParameters()) {
            if (i == 0) b.append("?");
            b.append(param);
            if (i < config.getParameters().size() - 1) b.append("&");
            i++;
        }
        return b.toString();
    }
}