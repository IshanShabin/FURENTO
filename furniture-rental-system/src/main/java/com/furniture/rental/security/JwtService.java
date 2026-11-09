package com.furniture.rental.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import com.furniture.rental.entity.RevokedToken;
import com.furniture.rental.repository.RevokedTokenRepository;
import org.springframework.context.annotation.Lazy;
import java.time.Instant;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String AUTH_COOKIE = "FURENTO_AUTH";
    private static final long JWT_EXPIRY_MS = 86_400_000L;
    private final SecretKey signingKey;
    private final RevokedTokenRepository revokedTokenRepository;    
    private final boolean secureCookie;

    public JwtService(@Value("${JWT_SECRET:}") String jwtSecret,
        @Value("${app.jwt.cookie-secure:false}") boolean secureCookie,
        @Lazy RevokedTokenRepository revokedTokenRepository) {
    byte[] secretBytes = jwtSecret == null ? new byte[0] : jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
        throw new IllegalStateException("Set JWT_SECRET to a private value of at least 32 characters.");
    }
    this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    this.secureCookie = secureCookie;
    this.revokedTokenRepository = revokedTokenRepository;
}

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_EXPIRY_MS);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername()) //same as below
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
            return claims.getExpiration().after(new Date()) && !isRevoked(claims.getId());
        } catch (Exception ex) {
            return false;
        }
    }

    public void revokeToken(String token) {
    try {
        Claims claims = parseClaims(token);
        if (claims.getId() != null) {
            revokedTokenRepository.save(new RevokedToken(claims.getId(), claims.getExpiration().toInstant()));
        }
    } catch (Exception ignored) {
        // invalid or already expired tokens need no revoking
    }
    revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
}

private boolean isRevoked(String tokenId) {
    return tokenId != null && revokedTokenRepository.existsById(tokenId);
}

    public String resolveToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        return header.substring(7);
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }
    return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
