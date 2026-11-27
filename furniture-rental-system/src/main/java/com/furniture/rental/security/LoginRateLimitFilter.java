package com.furniture.rental.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Stops login attempts from an address and username that have failed too often. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    public static final String LOCKED_MESSAGE =
            "Too many failed login attempts. Please wait a few minutes and try again.";

    private static final String FORM_LOGIN = "/login";
    private static final String API_LOGIN = "/api/auth/login";

    private final LoginAttemptService attempts;
    private final LoginProtectionProperties properties;

    public LoginRateLimitFilter(LoginAttemptService attempts, LoginProtectionProperties properties) {
        this.attempts = attempts;
        this.properties = properties;
    }

    static boolean isLoginRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = pathOf(request);
        return FORM_LOGIN.equals(path) || API_LOGIN.equals(path);
    }

    private static String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !attempts.isEnabled() || !isLoginRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String ip = ClientIp.of(request, properties.isTrustForwardedFor());
        long retryAfter = attempts.retryAfterSeconds(ip, request.getParameter("username"));
        if (retryAfter <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        if (API_LOGIN.equals(pathOf(request))) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"" + LOCKED_MESSAGE + "\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?locked=true");
        }
    }
}
