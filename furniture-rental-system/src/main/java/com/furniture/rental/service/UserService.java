package com.furniture.rental.service;

import com.furniture.rental.entity.User;
import com.furniture.rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
    String username = user.getUsername() == null ? "" : user.getUsername().trim();
    if (username.length() < 3) {
        throw new IllegalArgumentException("Username must be at least 3 characters.");
    }
    if (isBlank(user.getFirstName()) || isBlank(user.getLastName())) {
        throw new IllegalArgumentException("First and last name are required.");
    }
    if (user.getEmail() == null || !user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
        throw new IllegalArgumentException("Enter a valid email address.");
    }
    if (user.getPassword() == null || user.getPassword().length() < 8) {
        throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    if (userRepository.findByUsername(username).isPresent()) {
        throw new IllegalArgumentException("That username is already taken.");
    }

    user.setId(null);
    user.setAddresses(null);
    user.setUsername(username);
    user.setRole("CUSTOMER");
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    userRepository.save(user);
    }

        private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
