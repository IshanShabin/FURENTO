package com.furniture.rental.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    /** A clock the test can move forward. */
    private static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private TestClock clock;
    private LoginProtectionProperties properties;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        properties = new LoginProtectionProperties();
        properties.setMaxAttemptsPerUser(3);
        properties.setMaxAttemptsPerIp(5);
        properties.setWindowMinutes(15);
        service = new LoginAttemptService(properties, clock);
    }

    private void fail(String ip, String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(ip, username);
        }
    }

    @Test
    void blocksAUserAfterTooManyFailuresFromOneAddress() {
        fail("1.1.1.1", "alice", 2);
        assertEquals(0, service.retryAfterSeconds("1.1.1.1", "alice"));

        fail("1.1.1.1", "alice", 1);
        long wait = service.retryAfterSeconds("1.1.1.1", "alice");
        assertTrue(wait > 0 && wait <= 900, "wait was " + wait);
    }

    @Test
    void otherUsersAndOtherAddressesAreNotAffected() {
        fail("1.1.1.1", "alice", 3);

        assertEquals(0, service.retryAfterSeconds("1.1.1.1", "bob"));
        assertEquals(0, service.retryAfterSeconds("2.2.2.2", "alice"));
    }

    @Test
    void usernamesAreComparedIgnoringCaseAndSpaces() {
        fail("1.1.1.1", "Alice", 2);
        fail("1.1.1.1", "  alice ", 1);

        assertTrue(service.retryAfterSeconds("1.1.1.1", "ALICE") > 0);
    }

    @Test
    void anAddressTryingManyUsernamesIsBlockedForAllOfThem() {
        for (int i = 0; i < 5; i++) {
            fail("1.1.1.1", "user" + i, 1);
        }

        assertTrue(service.retryAfterSeconds("1.1.1.1", "someone-new") > 0);
        assertEquals(0, service.retryAfterSeconds("2.2.2.2", "someone-new"));
    }

    @Test
    void theBlockEndsWhenTheWindowPasses() {
        fail("1.1.1.1", "alice", 3);
        assertTrue(service.retryAfterSeconds("1.1.1.1", "alice") > 0);

        clock.advance(Duration.ofMinutes(15));

        assertEquals(0, service.retryAfterSeconds("1.1.1.1", "alice"));
    }

    @Test
    void retryTimeFollowsTheOldestFailureInTheWindow() {
        fail("1.1.1.1", "alice", 1);
        clock.advance(Duration.ofMinutes(1));
        fail("1.1.1.1", "alice", 1);
        clock.advance(Duration.ofMinutes(1));
        fail("1.1.1.1", "alice", 1);

        // The first failure was 2 minutes ago and leaves the 15 minute window in 13 minutes.
        assertEquals(13 * 60, service.retryAfterSeconds("1.1.1.1", "alice"));
    }

    @Test
    void aSuccessfulLoginClearsTheUsersFailures() {
        fail("1.1.1.1", "alice", 2);
        service.recordSuccess("1.1.1.1", "alice");
        fail("1.1.1.1", "alice", 2);

        assertEquals(0, service.retryAfterSeconds("1.1.1.1", "alice"));
    }

    @Test
    void nothingIsBlockedWhenTheProtectionIsOff() {
        properties.setEnabled(false);
        fail("1.1.1.1", "alice", 50);

        assertEquals(0, service.retryAfterSeconds("1.1.1.1", "alice"));
    }
}
