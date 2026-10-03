package com.company.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test
    void accountIsBlockedAtTheConfiguredFailureThreshold() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                clock,
                2,
                100,
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                100);

        assertTrue(limiter.recordFailure("127.0.0.1", "tester").allowed());
        LoginAttemptLimiter.Decision blocked =
                limiter.recordFailure("127.0.0.1", "tester");

        assertFalse(blocked.allowed());
        assertTrue(blocked.retryAfterSeconds() >= 60);
        assertFalse(limiter.check("127.0.0.1", "tester").allowed());

        clock.advance(Duration.ofMinutes(6));
        assertTrue(limiter.check("127.0.0.1", "tester").allowed());
    }

    @Test
    void clientLimitStopsPasswordSprayingAcrossDifferentAccounts() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                clock,
                10,
                3,
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                100);

        assertTrue(limiter.recordFailure("127.0.0.1", "one").allowed());
        assertTrue(limiter.recordFailure("127.0.0.1", "two").allowed());
        assertFalse(limiter.recordFailure(
                "127.0.0.1", "three").allowed());
        assertFalse(limiter.check(
                "127.0.0.1", "unseen-account").allowed());
    }

    @Test
    void successfulLoginClearsTheAccountFailureState() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                clock,
                2,
                100,
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                100);

        assertTrue(limiter.recordFailure("127.0.0.1", "tester").allowed());
        limiter.recordSuccess("tester");

        assertTrue(limiter.recordFailure(
                "127.0.0.2", "tester").allowed());
    }

    @Test
    void singleFailureAccountLimitBlocksNewAndExpiredWindowsImmediately() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                clock, 1, 100, Duration.ofMinutes(5), Duration.ofMinutes(1), 100);

        LoginAttemptLimiter.Decision first = limiter.recordFailure("127.0.0.1", "tester");
        assertFalse(first.allowed());
        assertEquals(60, first.retryAfterSeconds());
        assertFalse(limiter.check("127.0.0.2", "tester").allowed());

        clock.advance(Duration.ofMinutes(6));
        assertFalse(limiter.recordFailure("127.0.0.1", "tester").allowed());
        limiter.recordSuccess("tester");
        assertTrue(limiter.check("127.0.0.2", "tester").allowed());
    }

    @Test
    void singleFailureClientLimitBlocksOtherAccountsWithoutExtendingAnActiveBlock() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                clock, 100, 1, Duration.ofMinutes(5), Duration.ofMinutes(1), 100);

        assertFalse(limiter.recordFailure("127.0.0.1", "first").allowed());
        assertFalse(limiter.check("127.0.0.1", "unseen-account").allowed());
        assertTrue(limiter.check("127.0.0.2", "unseen-account").allowed());

        clock.advance(Duration.ofSeconds(30));
        LoginAttemptLimiter.Decision repeated = limiter.recordFailure("127.0.0.1", "second");
        assertFalse(repeated.allowed());
        assertEquals(30, repeated.retryAfterSeconds());
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-31T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
