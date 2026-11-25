package utilities;

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;

/**
 * Passwords.java
 *
 * Creates and verifies salted PBKDF2 password hashes. It enforces password length, stores the
 * hash parameters with each hash and clears temporary password data where applicable.
 */
public final class Passwords {
    private Passwords() {}

    private static final int ITERATIONS = 600_000;

    // Check password length before doing the expensive hash calculation.
    public static void validate(char[] password) {
        Validation.require(
                password != null && password.length >= 12 && password.length <= 128,
                "Password must contain 12–128 characters. Spaces and Unicode are allowed.");
    }

    // Generate a new random salt; save the algorithm, iteration count, salt and hash together.
    public static String hash(char[] password) {
        validate(password);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2-sha256$"
                + ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    // Read the stored hash format, repeat its calculation and compare the resulting bytes.
    public static boolean verify(char[] password, String encoded) {
        try {
            String[] p = encoded.split("\\$");
            if (p.length != 4 || !p[0].equals("pbkdf2-sha256")) return false;
            int iterations = Integer.parseInt(p[1]);
            if (iterations != ITERATIONS) return false;
            byte[] salt = Base64.getDecoder().decode(p[2]),
                    expected = Base64.getDecoder().decode(p[3]);
            return salt.length == 16
                    && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Run PBKDF2 and clear the temporary password held by the key specification afterward.
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is unavailable.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
