package com.furniture.rental.controller;

import com.furniture.rental.config.SecurityConfig;
import com.furniture.rental.entity.RevokedToken;
import com.furniture.rental.repository.RevokedTokenRepository;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.security.JwtAuthenticationFilter;
import com.furniture.rental.security.JwtService;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { ApiController.class, AuthApiController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-test-secret-test-secret-1234",
        "app.jwt.cookie-secure=false"
})
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockBean
    private FurnitureService furnitureService;

    @MockBean
    private RentalService rentalService;

    private String tokenFor(String username, String role) {
        UserDetails user = User.withUsername(username).password("x").roles(role).build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        return jwtService.generateToken(user);
    }

    @Test
    void noTokenGivesJson401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required."));
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        String token = tokenFor("ishan", "CUSTOMER");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ishan"));
    }

    @Test
    void tamperedTokenGivesJson401() throws Exception {
        String token = tokenFor("ishan", "CUSTOMER");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReachAdminRoute() throws Exception {
        String token = tokenFor("jaseem", "CUSTOMER");
        mockMvc.perform(get("/api/admin/rentals").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void adminCanReachAdminRoute() throws Exception {
        String token = tokenFor("admin", "ADMIN");
        mockMvc.perform(get("/api/admin/rentals").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        String token = tokenFor("ishan", "CUSTOMER");
        when(revokedTokenRepository.existsById(anyString())).thenReturn(true);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = tokenFor("ishan", "CUSTOMER");
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out."));
        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void wrongPasswordGivesJson401() throws Exception {
        when(userDetailsService.loadUserByUsername("nobody"))
                .thenThrow(new UsernameNotFoundException("not found"));
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "nobody")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void correctPasswordReturnsToken() throws Exception {
        UserDetails user = User.withUsername("ishan")
                .password(new BCryptPasswordEncoder().encode("secret123"))
                .roles("CUSTOMER")
                .build();
        when(userDetailsService.loadUserByUsername("ishan")).thenReturn(user);
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "ishan")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("ishan"));
    }
}