package de.julianweinelt.databench.worker.flow.auth;

import com.password4j.Password;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager {
    private static UserManager instance;

    private final List<User> users = new ArrayList<>();
    private final List<UserPermission> permissions = new ArrayList<>();

    public UserManager() {
        instance = this;
    }
    public static UserManager instance() {
        return instance;
    }

    public boolean verify(String username, String password) {
        User user = getUser(username).orElse(null);
        if (user == null) return false;
        return user.getPassword().equals(Password.hash(password).withArgon2().getResult());
    }

    public Optional<User> getUser(String username) {
        for (User user : users) {
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