package de.julianweinelt.databench.data;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

public class ProjectEncryptionUtil {

    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int SALT_SIZE = 16;
    private static final int ITERATIONS = 65536;
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final String PBKDF_ALGO = "PBKDF2WithHmacSHA256";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom secureRandom = new SecureRandom();

    public static void encryptProject(Object project, File targetFile, String password) throws Exception {
        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);

        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);

        SecretKey key = deriveKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] jsonBytes = objectMapper.writeValueAsBytes(project);
        byte[] encrypted = cipher.doFinal(jsonBytes);

        byte[] output = new byte[SALT_SIZE + IV_SIZE + encrypted.length];
        System.arraycopy(salt, 0, output, 0, SALT_SIZE);
        System.arraycopy(iv, 0, output, SALT_SIZE, IV_SIZE);
        System.arraycopy(encrypted, 0, output, SALT_SIZE + IV_SIZE, encrypted.length);

        Files.write(targetFile.toPath(), output);
    }

    public static <T> T decryptProject(File file, String password, Class<T> type) throws Exception {
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        if (fileBytes.length < SALT_SIZE + IV_SIZE) {
            throw new IllegalArgumentException("Ungültige Datei oder zu kurz");
        }

        byte[] salt = new byte[SALT_SIZE];
        byte[] iv = new byte[IV_SIZE];
        byte[] encrypted = new byte[fileBytes.length - SALT_SIZE - IV_SIZE];

        System.arraycopy(fileBytes, 0, salt, 0, SALT_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE, iv, 0, IV_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE + IV_SIZE, encrypted, 0, encrypted.length);

        SecretKey key = deriveKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] jsonBytes = cipher.doFinal(encrypted);
        return objectMapper.readValue(jsonBytes, type);
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
