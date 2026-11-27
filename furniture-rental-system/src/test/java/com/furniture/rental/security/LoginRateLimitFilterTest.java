package com.furniture.rental.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimitFilterTest {

    private LoginProtectionProperties properties;
    private LoginAttemptService attempts;
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new LoginProtectionProperties();
        properties.setMaxAttemptsPerUser(2);
        attempts = new LoginAttemptService(properties);
        filter = new LoginRateLimitFilter(attempts, properties);
    }

    private MockHttpServletRequest request(String method, String path, String username) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("10.0.0.1");
        request.setParameter("username", username);
        return request;
    }

    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain;
    }

    @Test
    void attemptsPassThroughWhileUnderTheLimit() throws Exception {
        attempts.recordFailure("10.0.0.1", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(request("POST", "/login", "alice"), response);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void aBlockedFormLoginIsSentBackToTheLoginPage() throws Exception {
        attempts.recordFailure("10.0.0.1", "alice");
        attempts.recordFailure("10.0.0.1", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(request("POST", "/login", "alice"), response);

        assertNull(chain.getRequest());
        assertEquals("/login?locked=true", response.getRedirectedUrl());
    }

    @Test
    void aBlockedApiLoginGets429WithRetryAfter() throws Exception {
        attempts.recordFailure("10.0.0.1", "alice");
        attempts.recordFailure("10.0.0.1", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(request("POST", "/api/auth/login", "alice"), response);

        assertNull(chain.getRequest());
        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains(LoginRateLimitFilter.LOCKED_MESSAGE));
    }

    @Test
    void otherRequestsAreNeverBlocked() throws Exception {
        attempts.recordFailure("10.0.0.1", "alice");
        attempts.recordFailure("10.0.0.1", "alice");

        assertNotNull(run(request("GET", "/login", "alice"), new MockHttpServletResponse()).getRequest());
        assertNotNull(run(request("POST", "/cart/add", "alice"), new MockHttpServletResponse()).getRequest());
    }

    @Test
    void theForwardedAddressIsOnlyUsedWhenTrusted() throws Exception {
        attempts.recordFailure("203.0.113.9", "alice");
        attempts.recordFailure("203.0.113.9", "alice");

        MockHttpServletRequest untrusted = request("POST", "/login", "alice");
        untrusted.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
        assertNotNull(run(untrusted, new MockHttpServletResponse()).getRequest());

        properties.setTrustForwardedFor(true);
        MockHttpServletRequest trusted = request("POST", "/login", "alice");
        trusted.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(run(trusted, response).getRequest());
        assertEquals("/login?locked=true", response.getRedirectedUrl());
    }

    @Test
    void nothingIsFilteredWhenTheProtectionIsOff() throws Exception {
        attempts.recordFailure("10.0.0.1", "alice");
        attempts.recordFailure("10.0.0.1", "alice");
        properties.setEnabled(false);

        assertNotNull(run(request("POST", "/login", "alice"), new MockHttpServletResponse()).getRequest());
    }
}
