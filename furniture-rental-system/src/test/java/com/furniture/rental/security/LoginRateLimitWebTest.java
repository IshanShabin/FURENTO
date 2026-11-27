package com.furniture.rental.security;

import com.furniture.rental.config.SecurityConfig;
import com.furniture.rental.controller.AuthController;
import com.furniture.rental.repository.RevokedTokenRepository;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real Spring Security wiring: failed logins are counted once each and then blocked. */
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-test-secret-test-secret-1234",
        "app.jwt.cookie-secure=false",
        "app.security.login.max-attempts-per-user=3",
        "app.security.login.max-attempts-per-ip=50"
})
class LoginRateLimitWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockBean
    private UserService userService;

    @BeforeEach
    void everyLoginFails() {
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("no such user"));
    }

    private ResultActions badLogin(String username) throws Exception {
        return mockMvc.perform(post("/login").with(csrf())
                .param("username", username)
                .param("password", "wrong-password"));
    }

    // Each test uses its own usernames, because the counters live as long as the test context.

    @Test
    void repeatedBadPasswordsLockOutThatUserFromThisAddress() throws Exception {
        for (int i = 0; i < 3; i++) {
            badLogin("web-locked-user").andExpect(redirectedUrl("/login?error=true"));
        }

        badLogin("web-locked-user").andExpect(redirectedUrl("/login?locked=true"));
        badLogin("web-other-user").andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void theApiLoginIsLockedByTheSameFailures() throws Exception {
        for (int i = 0; i < 3; i++) {
            badLogin("web-api-user");
        }

        mockMvc.perform(post("/api/auth/login")
                .param("username", "web-api-user")
                .param("password", "anything"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void theLoginPageExplainsALockout() throws Exception {
        mockMvc.perform(get("/login").param("locked", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Too many failed login attempts")));
    }
}
