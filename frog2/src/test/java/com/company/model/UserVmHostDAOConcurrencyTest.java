package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class UserVmHostDAOConcurrencyTest {
    @Test
    void mutationLockRegistryHasAFixedBound() throws Exception {
        java.lang.reflect.Field field = UserVmHostDAO.class
                .getDeclaredField("MUTATION_LOCKS");
        field.setAccessible(true);

        ReentrantLock[] locks = (ReentrantLock[]) field.get(null);
        assertEquals(64, locks.length);
    }

    @RepeatedTest(10)
    @Timeout(10)
    void concurrentCreatesForDifferentOwnersCannotClaimTheSameIp()
            throws Exception {
        InMemoryHostJdbc jdbc = new InMemoryHostJdbc();
        String targetIp = "192.168.40.200";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<UserVmHostDAO.MutationResult> first = executor.submit(() -> {
                start.await();
                return new UserVmHostDAO(jdbc::open).saveNormalized(
                        host(targetIp, "owner-1"), null);
            });
            Future<UserVmHostDAO.MutationResult> second = executor.submit(() -> {
                start.await();
                return new UserVmHostDAO(jdbc::open).saveNormalized(
                        host(targetIp, "owner-2"), null);
            });

            start.countDown();
            List<UserVmHostDAO.MutationResult> results =
                    List.of(first.get(), second.get());

            assertEquals(1, results.stream()
                    .filter(UserVmHostDAO.MutationResult.SAVED::equals)
                    .count());
            assertEquals(1, results.stream()
                    .filter(UserVmHostDAO.MutationResult
                            .DUPLICATE_OTHER_IP::equals)
                    .count());
            assertEquals(1, jdbc.countIp(targetIp));
        } finally {
            executor.shutdownNow();
        }
    }

    @RepeatedTest(10)
    @Timeout(10)
    void oppositeIpUpdatesAcquireOriginalAndTargetLocksWithoutDeadlock()
            throws Exception {
        InMemoryHostJdbc jdbc = new InMemoryHostJdbc();
        String firstIp = "192.168.40.201";
        String secondIp = "192.168.40.202";
        jdbc.add(firstIp, "owner-1");
        jdbc.add(secondIp, "owner-2");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<UserVmHostDAO.MutationResult> first = executor.submit(() -> {
                start.await();
                return new UserVmHostDAO(jdbc::open).saveNormalized(
                        host(secondIp, "owner-1"), firstIp);
            });
            Future<UserVmHostDAO.MutationResult> second = executor.submit(() -> {
                start.await();
                return new UserVmHostDAO(jdbc::open).saveNormalized(
                        host(firstIp, "owner-2"), secondIp);
            });

            start.countDown();
            List<UserVmHostDAO.MutationResult> results =
                    List.of(first.get(), second.get());

            assertEquals(2, results.stream()
                    .filter(UserVmHostDAO.MutationResult
                            .DUPLICATE_OTHER_IP::equals)
                    .count());
            assertEquals(1, jdbc.countIp(firstIp));
            assertEquals(1, jdbc.countIp(secondIp));
        } finally {
            executor.shutdownNow();
        }
    }

    @RepeatedTest(10)
    @Timeout(10)
    void concurrentCreatesForOneOwnerCannotExceedTheLimit() throws Exception {
        InMemoryHostJdbc jdbc = new InMemoryHostJdbc();
        for (int index = 1; index <= 19; index++) {
            jdbc.add("192.168.40." + index, "owner-1");
        }

        int requestCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<UserVmHostDAO.MutationResult>> futures =
                    new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                int lastOctet = 100 + index;
                futures.add(executor.submit(() -> {
                    start.await();
                    UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);
                    return dao.saveNormalized(
                            host("192.168.40." + lastOctet), null);
                }));
            }

            start.countDown();
            List<UserVmHostDAO.MutationResult> results = new ArrayList<>();
            for (Future<UserVmHostDAO.MutationResult> future : futures) {
                results.add(future.get());
            }

            assertEquals(1, results.stream()
                    .filter(UserVmHostDAO.MutationResult.SAVED::equals)
                    .count());
            assertEquals(requestCount - 1L, results.stream()
                    .filter(UserVmHostDAO.MutationResult.HOST_LIMIT_REACHED::equals)
                    .count());
            assertEquals(20, jdbc.count("owner-1"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedWriteReleasesAllMutationLocks() {
        InMemoryHostJdbc jdbc = new InMemoryHostJdbc();
        jdbc.failNextInsert();
        UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);

        assertThrows(
                DataAccessException.class,
                () -> dao.saveNormalized(host("192.168.40.100"), null));
        assertEquals(
                UserVmHostDAO.MutationResult.SAVED,
                dao.saveNormalized(host("192.168.40.101"), null));
        assertEquals(1, jdbc.count("owner-1"));
    }

    private static UserVmHostDTO host(String ip) {
        return host(ip, "owner-1");
    }

    private static UserVmHostDTO host(String ip, String ownerUserId) {
        UserVmHostDTO host = new UserVmHostDTO();
        host.setIp(ip);
        host.setOwnerUserId(ownerUserId);
        host.setOwnerUserName("Owner");
        host.setPurpose("Test host");
        return host;
    }

    private static final class InMemoryHostJdbc {
        private final List<HostRecord> hosts = new ArrayList<>();
        private boolean failNextInsert;

        synchronized void add(String ip, String ownerUserId) {
            hosts.add(new HostRecord(ip, ownerUserId));
        }

        synchronized int count(String ownerUserId) {
            return (int) hosts.stream()
                    .map(HostRecord::ownerUserId)
                    .filter(ownerUserId::equals)
                    .count();
        }

        synchronized int countIp(String ip) {
            return (int) hosts.stream()
                    .map(HostRecord::ip)
                    .filter(ip::equals)
                    .count();
        }

        synchronized void failNextInsert() {
            failNextInsert = true;
        }

        Connection open() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "prepareStatement" -> statement((String) args[0]);
                        case "close" -> null;
                        case "isClosed" -> false;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private PreparedStatement statement(String sql) {
            Map<Integer, Object> parameters = new LinkedHashMap<>();
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "setString" -> {
                            parameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> resultSet(query(sql, parameters));
                        case "executeUpdate" -> update(sql, parameters);
                        case "close" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private synchronized List<Map<String, Object>> query(
                String sql, Map<Integer, Object> parameters) {
            if (sql.startsWith("SELECT COUNT(*)")) {
                return List.of(row("count", count((String) parameters.get(1))));
            }
            if (sql.contains("FROM user_vm_hosts WHERE ip = ?")) {
                String ip = (String) parameters.get(1);
                String requestedOwnerUserId =
                        (String) parameters.get(2);
                HostRecord host = hosts.stream()
                        .filter(candidate -> candidate.ip().equals(ip))
                        .filter(candidate -> requestedOwnerUserId == null
                                || candidate.ownerUserId().equals(
                                        requestedOwnerUserId))
                        .findFirst()
                        .orElse(null);
                if (host == null) {
                    return List.of();
                }
                return List.of(hostRow(ip, host.ownerUserId()));
            }
            throw new IllegalStateException("Unexpected SQL: " + sql);
        }

        private int update(String sql, Map<Integer, Object> parameters)
                throws Exception {
            if (!sql.startsWith("INSERT INTO user_vm_hosts")) {
                throw new IllegalStateException("Unexpected SQL: " + sql);
            }

            // Widen the COUNT-to-INSERT window so this test reliably exposes
            // a missing owner lock without depending on scheduler timing.
            Thread.sleep(20);
            synchronized (this) {
                if (failNextInsert) {
                    failNextInsert = false;
                    throw new java.sql.SQLException("planned write failure");
                }
                String ip = (String) parameters.get(1);
                hosts.add(new HostRecord(ip, (String) parameters.get(2)));
                return 1;
            }
        }

        private record HostRecord(String ip, String ownerUserId) {
        }

        private static Map<String, Object> hostRow(
                String ip, String ownerUserId) {
            Timestamp now = Timestamp.valueOf("2026-08-25 10:00:00");
            return row(
                    "ip", ip,
                    "owner_user_id", ownerUserId,
                    "owner_user_name", "Owner",
                    "purpose", "Test host",
                    "os_info", null,
                    "vertica_version", null,
                    "remote_host", null,
                    "note", null,
                    "status", "ACTIVE",
                    "created_at", now,
                    "updated_at", now);
        }

        private static Map<String, Object> row(Object... values) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < values.length; index += 2) {
                row.put((String) values[index], values[index + 1]);
            }
            return row;
        }

        private static ResultSet resultSet(List<Map<String, Object>> rows) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[] {ResultSet.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "next" -> ++cursor[0] < rows.size();
                        case "getString" -> {
                            Object value = rows.get(cursor[0]).get(
                                    String.valueOf(args[0]));
                            yield value == null ? null : value.toString();
                        }
                        case "getInt" -> {
                            Object key = args[0];
                            Object value = key instanceof Integer
                                    ? rows.get(cursor[0]).values().stream()
                                            .skip((Integer) key - 1L)
                                            .findFirst()
                                            .orElse(0)
                                    : rows.get(cursor[0]).get(String.valueOf(key));
                            yield value instanceof Number number
                                    ? number.intValue() : 0;
                        }
                        case "getTimestamp" -> rows.get(cursor[0]).get(
                                String.valueOf(args[0]));
                        case "close" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }

    }
}
