package com.maanmeal.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordUtil {
    public static String hash(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean check(String password, String hashed) {
        if (password == null || hashed == null) return false;
        if (password.equals(hashed)) return true;
        if (hash(password).equals(hashed)) return true;
        // Fallback for trimming
        if (password.trim().equals(hashed.trim())) return true;
        if (hash(password.trim()).equals(hashed.trim())) return true;
        return false;
    }
}
