package com.company.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Original database values used both for the edit revision and the atomic update predicate. */
final class CustomerDetailSnapshot {
    private static final Set<String> DATE_COLUMNS = Set.of("create_date", "install_date", "eos_date");
    private final List<Value> values;

    private CustomerDetailSnapshot(List<Value> values) {
        this.values = List.copyOf(values);
    }

    static CustomerDetailSnapshot read(ResultSet row, boolean assigneeIds, boolean references)
            throws SQLException {
        List<Value> values = new ArrayList<>();
        for (String column : CustomerDetailDAO.requiredColumnNames()) {
            values.add(new Value(column, DATE_COLUMNS.contains(column)
                    ? row.getTimestamp(column) : row.getString(column)));
        }
        if (assigneeIds) {
            values.add(new Value(CustomerAssignmentSupport.MAIN_USER_ID_COLUMN,
                    row.getString(CustomerAssignmentSupport.MAIN_USER_ID_COLUMN)));
            values.add(new Value(CustomerAssignmentSupport.SUB_USER_ID_COLUMN,
                    row.getString(CustomerAssignmentSupport.SUB_USER_ID_COLUMN)));
        }
        if (references) values.add(new Value("customer_id", row.getString("customer_id")));
        return new CustomerDetailSnapshot(values);
    }

    String revision() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Value field : values) {
                byte[] name = field.column().getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(name.length).array());
                digest.update(name);
                if (field.value() == null) {
                    digest.update((byte) 0);
                } else {
                    digest.update((byte) 1);
                    byte[] value = field.value().toString().getBytes(StandardCharsets.UTF_8);
                    digest.update(ByteBuffer.allocate(4).putInt(value.length).array());
                    digest.update(value);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    String predicate() {
        return values.stream().map(field -> field.column()
                + (field.value() == null ? " IS NULL"
                        : "customer_id".equals(field.column()) ? " = CAST(? AS UUID)" : " = ?"))
                .collect(java.util.stream.Collectors.joining(" AND "));
    }

    int bind(PreparedStatement statement, int index) throws SQLException {
        for (Value field : values) {
            if (field.value() instanceof Timestamp timestamp) {
                statement.setTimestamp(index++, timestamp);
            } else if (field.value() != null) {
                statement.setString(index++, (String) field.value());
            }
        }
        return index;
    }

    CustomerAssignmentSupport.AssignmentState assignmentState() {
        return new CustomerAssignmentSupport.AssignmentState(
                string("main_manager"), string("sub_manager"),
                string(CustomerAssignmentSupport.MAIN_USER_ID_COLUMN),
                string(CustomerAssignmentSupport.SUB_USER_ID_COLUMN));
    }

    private String string(String column) {
        for (Value field : values) {
            if (field.column().equals(column)) return (String) field.value();
        }
        return null;
    }

    private record Value(String column, Object value) {
    }
}
