package com.khesam.dezhban.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class KeycloakPbkdf2PasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int DEFAULT_KEY_LENGTH = 512;

    @Override
    public @Nullable String encode(@Nullable CharSequence rawPassword) {
        throw new UnsupportedOperationException("This encoder is for migration/verification only.");
    }

    @Override
    public boolean matches(@Nullable CharSequence rawPassword, @Nullable String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // encodedPassword looks like: salt$hash$iterations
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 3) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            int iterations = Integer.parseInt(parts[2]);

            byte[] actualHash = hashPassword(rawPassword.toString(), salt, iterations);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            return false;
        }
    }

    private byte[] hashPassword(String password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, DEFAULT_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
}
