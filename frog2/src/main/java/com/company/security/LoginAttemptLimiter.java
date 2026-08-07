package com.company.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LoginAttemptLimiter {
    private static final int DEFAULT_ACCOUNT_FAILURES = 5;
    private static final int DEFAULT_CLIENT_FAILURES = 30;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(5);
    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_KEYS = 10_000;
    private static final int MAX_KEY_PART_LENGTH = 128;

    private final Clock clock;
    private final int accountFailureLimit;
    private final int clientFailureLimit;
    private final long windowMillis;
    private final long blockMillis;
    private final int maxKeys;
    private final ConcurrentMap<String, AttemptState> attempts =
            new ConcurrentHashMap<>();

    public LoginAttemptLimiter() {
        this(
                Clock.systemUTC(),
                DEFAULT_ACCOUNT_FAILURES,
                DEFAULT_CLIENT_FAILURES,
                DEFAULT_WINDOW,
                DEFAULT_BLOCK_DURATION,
                DEFAULT_MAX_KEYS);
    }

    public LoginAttemptLimiter(
            Clock clock,
            int accountFailureLimit,
            int clientFailureLimit,
            Duration window,
            Duration blockDuration,
            int maxKeys) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.accountFailureLimit = positive(
                accountFailureLimit, "accountFailureLimit");
        this.clientFailureLimit = positive(
                clientFailureLimit, "clientFailureLimit");
        this.windowMillis = positiveMillis(window, "window");
        this.blockMillis = positiveMillis(blockDuration, "blockDuration");
        this.maxKeys = positive(maxKeys, "maxKeys");
    }

    public Decision check(String clientAddress, String userId) {
        long now = clock.millis();
        Decision account = checkKey(accountKey(userId), now);
        Decision client = checkKey(clientKey(clientAddress), now);
        return Decision.longest(account, client);
    }

    public Decision recordFailure(String clientAddress, String userId) {
        long now = clock.millis();
        Decision account = recordFailure(
                accountKey(userId), accountFailureLimit, now);
        Decision client = recordFailure(
                clientKey(clientAddress), clientFailureLimit, now);
        trimIfNeeded(now);
        return Decision.longest(account, client);
    }

    public void recordSuccess(String userId) {
        attempts.remove(accountKey(userId));
    }

    int trackedKeyCount() {
        return attempts.size();
    }

    private Decision checkKey(String key, long now) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return Decision.permit();
        }
        if (state.blockedUntilMillis() > now) {
            return Decision.blocked(
                    retryAfterSeconds(state.blockedUntilMillis(), now));
        }
        if (isExpired(state, now)) {
            attempts.remove(key, state);
        }
        return Decision.permit();
    }

    private Decision recordFailure(String key, int limit, long now) {
        AttemptState state = attempts.compute(key, (ignored, current) -> {
            if (current == null || isExpired(current, now)) {
                return new AttemptState(now, 1, 0);
            }
            if (current.blockedUntilMillis() > now) {
                return current;
            }
            int failures = current.failures() + 1;
            long blockedUntil = failures >= limit
                    ? saturatedAdd(now, blockMillis)
                    : 0;
            return new AttemptState(
                    current.windowStartedMillis(),
                    failures,
                    blockedUntil);
        });
        return state.blockedUntilMillis() > now
                ? Decision.blocked(
                        retryAfterSeconds(state.blockedUntilMillis(), now))
                : Decision.permit();
    }

    private boolean isExpired(AttemptState state, long now) {
        if (state.blockedUntilMillis() > now) {
            return false;
        }
        return now - state.windowStartedMillis() >= windowMillis;
    }

    private void trimIfNeeded(long now) {
        if (attempts.size() <= maxKeys) {
            return;
        }
        attempts.entrySet().removeIf(
                entry -> isExpired(entry.getValue(), now));
        if (attempts.size() <= maxKeys) {
            return;
        }
        Iterator<String> keys = attempts.keySet().iterator();
        while (attempts.size() > maxKeys && keys.hasNext()) {
            attempts.remove(keys.next());
        }
    }

    private static String accountKey(String userId) {
        return "account:" + normalize(userId, true);
    }

    private static String clientKey(String clientAddress) {
        return "client:" + normalize(clientAddress, false);
    }

    private static String normalize(String value, boolean lowerCase) {
        String normalized = value == null ? "" : value.trim();
        if (lowerCase) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        if (normalized.length() > MAX_KEY_PART_LENGTH) {
            normalized = normalized.substring(0, MAX_KEY_PART_LENGTH);
        }
        return normalized.isEmpty() ? "<missing>" : normalized;
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static long positiveMillis(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        long millis = duration.toMillis();
        if (millis <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return millis;
    }

    private static long retryAfterSeconds(long blockedUntil, long now) {
        long remainingMillis = Math.max(1, blockedUntil - now);
        long seconds = remainingMillis / 1_000;
        if (remainingMillis % 1_000 != 0) {
            seconds++;
        }
        return Math.max(1, seconds);
    }

    private static long saturatedAdd(long first, long second) {
        return first > Long.MAX_VALUE - second
                ? Long.MAX_VALUE
                : first + second;
    }

    private record AttemptState(
            long windowStartedMillis,
            int failures,
            long blockedUntilMillis) {
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
        private static Decision permit() {
            return new Decision(true, 0);
        }

        private static Decision blocked(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }

        private static Decision longest(Decision first, Decision second) {
            if (first.allowed() && second.allowed()) {
                return permit();
            }
            return blocked(Math.max(
                    first.retryAfterSeconds(),
                    second.retryAfterSeconds()));
        }
    }
}
