package de.julianweinelt.databench.flow.flow.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.password4j.Password;
import de.julianweinelt.databench.flow.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UserManager {
    private final Gson GSON = new Gson();
    private static UserManager instance;

    private final List<User> users = new ArrayList<>();
    private final List<UserPermission> permissions = new ArrayList<>();

    public UserManager() {
        instance = this;
        log.info("Loading users...");
        load();
        log.info("Users loaded ({} in total)", users.size());
    }

    public static UserManager instance() {
        return instance;
    }

    public void createUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(Password.hash(password).withArgon2().getResult());
        users.add(user);

        System.out.println("""
                ############# WARNING ################
                CREATING A USER VIA CLI GIVES IT FULL PERMISSIONS!
                If you want to create normal users, use the client UI.
                ######################################
                """);

        save();
    }

    public void save() {
        File userFolder = new File("users");
        if (userFolder.mkdirs()) log.debug("Created users folder");
        try (FileWriter w = new FileWriter(new File(userFolder, "users.dat"))) {
            String usersDat = CryptoUtil.instance().encrypt(GSON.toJson(users));
            w.write(usersDat);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        try (FileWriter w = new FileWriter(new File(userFolder, "permissions.dat"))) {
            String data = CryptoUtil.instance().encrypt(GSON.toJson(permissions));
            w.write(data);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void load() {
        users.clear();
        permissions.clear();
        File usersFile = new File("users", "users.dat");
        File permissionsFile = new File("users", "permissions.dat");

        try (BufferedReader br = new BufferedReader(new FileReader(usersFile))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) b.append(line);
            String usersDat = CryptoUtil.instance().decrypt(b.toString());
            Type type = new TypeToken<List<User>>(){}.getType();
            users.addAll(GSON.fromJson(usersDat, type));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(permissionsFile))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) b.append(line);
            String permsDat = CryptoUtil.instance().decrypt(b.toString());
            Type type = new TypeToken<List<UserPermission>>(){}.getType();
            permissions.addAll(GSON.fromJson(permsDat, type));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean verify(String username, String password) {
        User user = getUser(username).orElse(null);
        if (user == null) {
            log.debug("User {} not found", username);
            return false;
        }
        String userPW = user.getPassword();
        return Password.check(password, userPW).withArgon2();
    }

    public Optional<User> getUser(String username) {
        log.debug("Looking up user: {}", username);
        for (User user : users) {
            log.debug("Checking user: {}", user.getUsername());
            if (user.getUsername().equals(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public record UserState(String name) {
        public static UserState ENABLED = new UserState("enabled");
        public static UserState DISABLED = new UserState("disabled");
        public static UserState COMPROMISED = new UserState("compromised");
    }
}