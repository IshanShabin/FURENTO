package com.furniture.rental.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Feeds login results from Spring Security into the attempt counters. */
public class LoginAttemptListener {

    private static final String RECORDED = LoginAttemptListener.class.getName() + ".failureRecorded";

    private final LoginAttemptService attempts;
    private final LoginProtectionProperties properties;

    public LoginAttemptListener(LoginAttemptService attempts, LoginProtectionProperties properties) {
        this.attempts = attempts;
        this.properties = properties;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        HttpServletRequest request = currentLoginRequest();
        // The same failure can be reported by more than one authentication manager, so count it once.
        if (request == null || request.getAttribute(RECORDED) != null) {
            return;
        }
        request.setAttribute(RECORDED, Boolean.TRUE);
        attempts.recordFailure(ClientIp.of(request, properties.isTrustForwardedFor()),
                event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        HttpServletRequest request = currentLoginRequest();
        if (request == null) {
            return;
        }
        attempts.recordSuccess(ClientIp.of(request, properties.isTrustForwardedFor()),
                event.getAuthentication().getName());
    }

    private HttpServletRequest currentLoginRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return LoginRateLimitFilter.isLoginRequest(request) ? request : null;
        }
        return null;
    }
}
