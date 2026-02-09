package de.julianweinelt.databench.worker.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.julianweinelt.databench.worker.storage.LocalStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

@Slf4j
public class JWTUtil {
    private final JWTVerifier verifier;
    private static JWTUtil instance;

    public JWTUtil() {
        String secret = LocalStorage.instance().getConfig().getJwtSecret();
        if (secret == null) secret = CryptoUtil.generateSecret(20);
        if (secret.isEmpty()) {
            String newSecret = CryptoUtil.generateSecret(20);
            secret = newSecret;
            LocalStorage.instance().getConfig().setJwtSecret(newSecret);
            LocalStorage.instance().save();
        }
        verifier = JWT.require(Algorithm.HMAC256(secret)).build();
        instance = this;
    }

    public static JWTUtil instance() {
        return instance;
    }

    /**
     * Generate a JWT token for the given username.
     * @param username the username as a {@link String}
     * @return the generated JWT token as a {@link String} using the {@link JWT} library
     */
    public String token(String username) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, LocalStorage.instance().getConfig().getTokenLifetime());
        Date expiration = calendar.getTime();
        return JWT.create()
                .withSubject(username)
                .withIssuer("flow")
                .withClaim("tokenID", LocalStorage.instance().getConfig().getTokenClaim())
                .withClaim("scope", "authToken")
                .withNotBefore(now)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(LocalStorage.instance().getConfig().getJwtSecret()));
    }


    public String refreshToken(String token) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, LocalStorage.instance().getConfig().getTokenLifetime() * 2);
        Date expiration = calendar.getTime();
        return JWT.create()
                .withSubject(token)
                .withIssuer("flow")
                .withClaim("tokenID", LocalStorage.instance().getConfig().getTokenClaim())
                .withClaim("scope", "refreshToken")
                .withNotBefore(now)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(LocalStorage.instance().getConfig().getJwtSecret()));
    }

    /**
     * Decode the given JWT token.
     * @param token the JWT token as a {@link String}
     * @return the decoded JWT token as a {@link DecodedJWT}, or null if the token is invalid
     */
    public DecodedJWT decode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.error("Failed to decode JWT token: {}", e.getMessage());
            return null;
        }
    }
    public String getUsername(String token) {
        DecodedJWT jwt = decode(token);
        if (jwt == null) return null;
        return jwt.getSubject();
    }
    public String getScope(String token) {
        DecodedJWT jwt = decode(token);
        if (jwt == null) return null;
        return jwt.getClaim("scope").asString().replace("\"", "");
    }

    /**
     * Verifies if the given {@link String} is a valid JWT token.
     * @param token the JWT token as a {@link String}
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean verify(String token, Consumer<String> errorConsumer) {
        DecodedJWT jwt = decode(token);
        if (jwt == null) {
            errorConsumer.accept("Invalid token.");
            return false;
        }
        if (!jwt.getIssuer().equals("flow")) {
            errorConsumer.accept("Invalid issuer.");
            return false;
        }
        if (jwt.getExpiresAt().before(new Date())) {
            errorConsumer.accept("Token expired.");
            return false;
        }
        if (!jwt.getClaim("tokenID").asString().equals(LocalStorage.instance().getConfig().getTokenClaim())) {
            errorConsumer.accept("Invalid token claim.");
            return false;
        }
        if (!jwt.getClaim("scope").asString().equals("authToken")) {
            errorConsumer.accept("Invalid token scope.");
            return false;
        }
        return true;
    }

    /**
     * Verifies if the given {@link String} is a valid JWT token.
     * @param token the JWT token as a {@link String}
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean verifyRefresh(String token, Consumer<String> errorConsumer) {
        DecodedJWT jwt = decode(token);
        if (jwt == null) {
            errorConsumer.accept("Invalid token.");
            return false;
        }
        if (!jwt.getIssuer().equals("flow")) {
            errorConsumer.accept("Invalid issuer.");
            return false;
        }
        if (jwt.getExpiresAt().before(new Date())) {
            errorConsumer.accept("Token expired.");
            return false;
        }
        if (!jwt.getClaim("tokenID").asString().equals(LocalStorage.instance().getConfig().getTokenClaim())) {
            errorConsumer.accept("Invalid token claim.");
            return false;
        }
        if (!jwt.getClaim("scope").asString().equals("refreshToken")) {
            errorConsumer.accept("Invalid token scope.");
            return false;
        }
        return true;
    }
}