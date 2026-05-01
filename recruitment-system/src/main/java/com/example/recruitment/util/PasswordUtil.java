package com.example.recruitment.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public static boolean verify(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }

    @Deprecated
    public static String generateSalt() {
        return "";
    }

    @Deprecated
    public static String hashPassword(String rawPassword, String salt) {
        return hashPassword(rawPassword);
    }
}
