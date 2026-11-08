package com.furniture.rental.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LegacyAwarePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return bcrypt.matches(rawPassword, storedPassword);
        }
        return MessageDigest.isEqual(rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean upgradeEncoding(String storedPassword) {
        return !isBcrypt(storedPassword);
    }

    private boolean isBcrypt(String password) {
        return password != null && password.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }
}
