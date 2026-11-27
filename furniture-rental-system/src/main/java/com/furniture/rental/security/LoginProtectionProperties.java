package com.furniture.rental.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Limits on failed logins (app.security.login.*). */
@ConfigurationProperties(prefix = "app.security.login")
public class LoginProtectionProperties {

    private boolean enabled = true;
    /** Failed attempts allowed per username and address within the window. */
    private int maxAttemptsPerUser = 5;
    /** Failed attempts allowed per address, whatever the username, within the window. */
    private int maxAttemptsPerIp = 20;
    private int windowMinutes = 15;
    /** Use the last X-Forwarded-For entry as the address. Only enable behind a proxy you control. */
    private boolean trustForwardedFor = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttemptsPerUser() {
        return maxAttemptsPerUser;
    }

    public void setMaxAttemptsPerUser(int maxAttemptsPerUser) {
        this.maxAttemptsPerUser = maxAttemptsPerUser;
    }

    public int getMaxAttemptsPerIp() {
        return maxAttemptsPerIp;
    }

    public void setMaxAttemptsPerIp(int maxAttemptsPerIp) {
        this.maxAttemptsPerIp = maxAttemptsPerIp;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public boolean isTrustForwardedFor() {
        return trustForwardedFor;
    }

    public void setTrustForwardedFor(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }
}
