package com.furniture.rental.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String AUTH_COOKIE = "FURENTO_AUTH";
    private static final long JWT_EXPIRY_MS = 86_400_000L;
    private final SecretKey signingKey;
    private final boolean secureCookie;

    public JwtService(@Value("${JWT_SECRET:}") String jwtSecret,
            @Value("${app.jwt.cookie-secure:false}") boolean secureCookie) {
        byte[] secretBytes = jwtSecret == null ? new byte[0] : jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("Set JWT_SECRET to a private value of at least 32 characters.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.secureCookie = secureCookie;
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_EXPIRY_MS);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from(AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(JWT_EXPIRY_MS / 1000)
                .build();
    }

    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
