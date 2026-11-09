package com.furniture.rental.security;

import com.furniture.rental.entity.RevokedToken;
import com.furniture.rental.repository.RevokedTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";
    private static final String OTHER_SECRET = "another-secret-another-secret-another-99";

    private RevokedTokenRepository repository;
    private JwtService service;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        repository = mock(RevokedTokenRepository.class);
        service = new JwtService(SECRET, false, repository);
        user = User.withUsername("ishan").password("x").roles("CUSTOMER").build();
    }

    @Test
    void generatedTokenContainsUsername() {
        String token = service.generateToken(user);
        assertEquals("ishan", service.extractUsername(token));
    }

    @Test
    void freshTokenIsValid() {
        assertTrue(service.validateToken(service.generateToken(user)));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String[] parts = service.generateToken(user).split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        assertFalse(service.validateToken(String.join(".", parts)));
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        String foreign = new JwtService(OTHER_SECRET, false, repository).generateToken(user);
        assertFalse(service.validateToken(foreign));
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("ishan")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();
        assertFalse(service.validateToken(expired));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", false, repository));
    }

    @Test
    void everyTokenGetsItsOwnId() {
        assertNotEquals(service.generateToken(user), service.generateToken(user));
    }

    @Test
    void revokedTokenIsRejected() {
        String token = service.generateToken(user);
        assertTrue(service.validateToken(token));

        service.revokeToken(token);

        ArgumentCaptor<RevokedToken> saved = ArgumentCaptor.forClass(RevokedToken.class);
        verify(repository).save(saved.capture());
        when(repository.existsById(saved.getValue().getTokenId())).thenReturn(true);

        assertFalse(service.validateToken(token));
    }

    @Test
    void authCookieIsHttpOnlyAndLax() {
        ResponseCookie cookie = service.createAuthCookie("abc");
        assertTrue(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals(86_400, cookie.getMaxAge().getSeconds());
    }

    @Test
    void secureFlagFollowsConfiguration() {
        assertFalse(service.createAuthCookie("abc").isSecure());
        assertTrue(new JwtService(SECRET, true, repository).createAuthCookie("abc").isSecure());
    }

    @Test
    void resolvesTokenFromBearerHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc123");
        assertEquals("abc123", service.resolveToken(request));
    }

    @Test
    void resolvesTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(JwtService.AUTH_COOKIE, "xyz789"));
        assertEquals("xyz789", service.resolveToken(request));
    }

    @Test
    void returnsNullWhenNoTokenPresent() {
        assertNull(service.resolveToken(new MockHttpServletRequest()));
    }
}