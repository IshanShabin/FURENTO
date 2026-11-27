package com.furniture.rental.security;

import com.furniture.rental.config.SecurityConfig;
import com.furniture.rental.controller.AuthController;
import com.furniture.rental.controller.CartController;
import com.furniture.rental.controller.HomeController;
import com.furniture.rental.entity.RevokedToken;
import com.furniture.rental.repository.RevokedTokenRepository;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RazorpayService;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.hasItem;

import java.util.Collections;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { AuthController.class, HomeController.class, CartController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-test-secret-test-secret-1234",
        "app.jwt.cookie-secure=false"
})
class LogoutWebTest {

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
    private UserService userService;

    @MockBean
    private FurnitureService furnitureService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private RentalService rentalService;

    @MockBean
    private RazorpayService razorpayService;

    private String issueToken(String username, String role) {
        UserDetails details = User.withUsername(username).password("x").roles(role).build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(details);
        return jwtService.generateToken(details);
    }

    @Test
    void customerLogoutRedirectsHomeClearsCookieAndRevokesToken() throws Exception {
        String token = issueToken("ishan", "CUSTOMER");

        mockMvc.perform(post("/logout").with(csrf())
                .cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout=true"))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(allOf(containsString("FURENTO_AUTH="), containsString("Max-Age=0")))));

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void adminLogoutRedirectsToLoginClearsCookieAndRevokesToken() throws Exception {
        String token = issueToken("admin", "ADMIN");

        mockMvc.perform(post("/logout").with(csrf())
                .cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(allOf(containsString("FURENTO_AUTH="), containsString("Max-Age=0")))));

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void homeAndLoginPagesDisplayLogoutMessageAndPreserveExistingAlerts() throws Exception {
        when(furnitureService.getTrendingFurniture()).thenReturn(Collections.emptyList());
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have been logged out successfully.")));

        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have been logged out successfully.")));

        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password.")));

        mockMvc.perform(get("/login").param("locked", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LoginRateLimitFilter.LOCKED_MESSAGE)));
    }

    @Test
    void replayedRevokedCookieCannotAccessProtectedPages() throws Exception {
        String token = issueToken("ishan", "CUSTOMER");
        when(revokedTokenRepository.existsById(anyString())).thenReturn(true);

        mockMvc.perform(get("/customer/dashboard").cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/dashboard").cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/cart").cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logoutWithoutCsrfIsRejectedAndGetLogoutDoesNotLogOut() throws Exception {
        String token = issueToken("ishan", "CUSTOMER");

        mockMvc.perform(post("/logout").cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/logout").cookie(new Cookie(JwtService.AUTH_COOKIE, token)))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
    }

    @Test
    void authenticatedPagesIncludeNoStoreCacheHeaders() throws Exception {
        mockMvc.perform(get("/cart").with(user("ishan").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }
}