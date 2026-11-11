package com.furniture.rental.service;

import com.furniture.rental.entity.User;
import com.furniture.rental.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User validUser() {
        return new User("newuser", "password123", "new@example.com", "New", "User", "CUSTOMER");
    }

    @Test
    void savesCustomerWithEncodedPassword() {
        when(passwordEncoder.encode("password123")).thenReturn("ENCODED");

        userService.registerUser(validUser());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("ENCODED", saved.getValue().getPassword());
        assertEquals("CUSTOMER", saved.getValue().getRole());
    }

    @Test
    void ignoresClientSuppliedIdAndRole() {
        when(passwordEncoder.encode(any())).thenReturn("ENCODED");
        User attacker = validUser();
        attacker.setId(1L);
        attacker.setRole("ADMIN");

        userService.registerUser(attacker);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNull(saved.getValue().getId());
        assertEquals("CUSTOMER", saved.getValue().getRole());
    }

    @Test
    void rejectsTakenUsername() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(new User()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validUser()));

        assertTrue(error.getMessage().contains("already taken"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsShortPassword() {
        User user = validUser();
        user.setPassword("short");
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidEmail() {
        User user = validUser();
        user.setEmail("not-an-email");
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(user));
        verify(userRepository, never()).save(any());
    }
}