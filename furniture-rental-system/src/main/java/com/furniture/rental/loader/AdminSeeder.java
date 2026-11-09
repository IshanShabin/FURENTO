package com.furniture.rental.loader;

import com.furniture.rental.entity.User;
import com.furniture.rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:}") String username,
            @Value("${ADMIN_PASSWORD:}") String password,
            @Value("${ADMIN_EMAIL:admin@furento.com}") String email) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            System.out.println("ADMIN_PASSWORD must be at least 8 characters; admin not created.");
            return;
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User admin = new User(username, passwordEncoder.encode(password), email, "Admin", "User", "ADMIN");
        userRepository.save(admin);
        System.out.println("Admin account '" + username + "' created.");
    }
}