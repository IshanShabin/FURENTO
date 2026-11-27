package com.furniture.rental.security;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Remembers recent failed logins in memory (sliding window) and says when to stop accepting attempts.
 * Two counters: one per username and address, and one per address alone.
 * State is lost on restart, which is fine for a single-server app.
 */
public class LoginAttemptService {

    private static final int SWEEP_THRESHOLD = 5_000;
    private static final int MAX_USERNAME_LENGTH = 100;

    private final LoginProtectionProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginProtectionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptService(LoginProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void recordFailure(String ip, String username) {
        if (!isEnabled()) {
            return;
        }
        long now = clock.millis();
        add(userKey(ip, username), now);
        add(ipKey(ip), now);
        if (failures.size() > SWEEP_THRESHOLD) {
            sweep(now);
        }
    }

    public void recordSuccess(String ip, String username) {
        failures.remove(userKey(ip, username));
    }

    /** Seconds until this address may try this username again, or 0 if it may try now. */
    public long retryAfterSeconds(String ip, String username) {
        if (!isEnabled()) {
            return 0;
        }
        long now = clock.millis();
        return Math.max(
                secondsToWait(userKey(ip, username), properties.getMaxAttemptsPerUser(), now),
                secondsToWait(ipKey(ip), properties.getMaxAttemptsPerIp(), now));
    }

    private long windowMillis() {
        return properties.getWindowMinutes() * 60_000L;
    }

    private void add(String key, long now) {
        Deque<Long> times = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            prune(times, now);
            times.addLast(now);
        }
    }

    private long secondsToWait(String key, int max, long now) {
        Deque<Long> times = failures.get(key);
        if (times == null) {
            return 0;
        }
        synchronized (times) {
            prune(times, now);
            if (times.size() < max) {
                return 0;
            }
            // Blocked until enough old failures age out to get back under the limit.
            Long[] recorded = times.toArray(new Long[0]);
            long unblockAt = recorded[recorded.length - max] + windowMillis();
            return Math.max(1, (unblockAt - now + 999) / 1000);
        }
    }

    private void prune(Deque<Long> times, long now) {
        while (!times.isEmpty() && now - times.peekFirst() >= windowMillis()) {
            times.removeFirst();
        }
    }

    private void sweep(long now) {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private static String userKey(String ip, String username) {
        String name = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (name.length() > MAX_USERNAME_LENGTH) {
            name = name.substring(0, MAX_USERNAME_LENGTH);
        }
        return "user:" + ip + "|" + name;
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}
