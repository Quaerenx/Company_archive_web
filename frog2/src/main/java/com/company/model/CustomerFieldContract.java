package com.company.model;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Canonical mapping between the basic customer form, DTO and database row.
 */
public final class CustomerFieldContract {
    private static final Field CUSTOMER_NAME = text(
            "customer_name",
            "customer_name",
            CustomerDTO::getCustomerName,
            CustomerDTO::setCustomerName);

    private static final List<Field> MUTABLE_FIELDS = List.of(
            text("db_name", "db_name", CustomerDTO::getDbName, CustomerDTO::setDbName),
            text(
                    "vertica_version",
                    "vertica_version",
                    CustomerDTO::getVerticaVersion,
                    CustomerDTO::setVerticaVersion),
            text("mode", "db_mode", CustomerDTO::getMode, CustomerDTO::setMode),
            text("os", "os_info", CustomerDTO::getOs, CustomerDTO::setOs),
            text("nodes", "node_count", CustomerDTO::getNodes, CustomerDTO::setNodes),
            text(
                    "license_size",
                    "license_info",
                    CustomerDTO::getLicenseSize,
                    CustomerDTO::setLicenseSize),
            text(
                    "manager_name",
                    "main_manager",
                    CustomerDTO::getManagerName,
                    CustomerDTO::setManagerName),
            text(
                    "sub_manager_name",
                    "sub_manager",
                    CustomerDTO::getSubManagerName,
                    CustomerDTO::setSubManagerName),
            text("said", "said", CustomerDTO::getSaid, CustomerDTO::setSaid),
            text(
                    "customer_type",
                    "customer_type",
                    CustomerDTO::getCustomerType,
                    CustomerDTO::setCustomerType),
            text(
                    "first_introduction_year",
                    "introduction_year",
                    CustomerDTO::getFirstIntroductionYear,
                    CustomerDTO::setFirstIntroductionYear),
            date(
                    "vertica_eos",
                    "eos_date",
                    CustomerDTO::getVerticaEos,
                    CustomerDTO::setVerticaEos),
            text(
                    "os_storage_config",
                    "storage_network",
                    CustomerDTO::getOsStorageConfig,
                    CustomerDTO::setOsStorageConfig),
            text(
                    "backup_config",
                    "backup_note",
                    CustomerDTO::getBackupConfig,
                    CustomerDTO::setBackupConfig),
            text("etl_tool", "etl_tool", CustomerDTO::getEtlTool, CustomerDTO::setEtlTool),
            text("bi_tool", "bi_tool", CustomerDTO::getBiTool, CustomerDTO::setBiTool),
            text(
                    "db_encryption",
                    "db_encryption",
                    CustomerDTO::getDbEncryption,
                    CustomerDTO::setDbEncryption),
            text("cdc_tool", "cdc_tool", CustomerDTO::getCdcTool, CustomerDTO::setCdcTool),
            text("note", "note", CustomerDTO::getNote, CustomerDTO::setNote));

    private static final List<Field> ALL_FIELDS = java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(CUSTOMER_NAME), MUTABLE_FIELDS.stream())
            .toList();
    private static final String MUTABLE_ASSIGNMENTS = MUTABLE_FIELDS.stream()
            .map(field -> field.columnName() + " = ?")
            .collect(Collectors.joining(", "));
    private static final String INSERT_COLUMNS = joinColumns(ALL_FIELDS, "");
    private static final String INSERT_PLACEHOLDERS = ALL_FIELDS.stream()
            .map(ignored -> "?")
            .collect(Collectors.joining(", "));

    private CustomerFieldContract() {
    }

    public static CustomerDTO fromForm(
            Function<String, String> parameterLookup) {
        Objects.requireNonNull(parameterLookup, "parameterLookup");
        CustomerDTO customer = new CustomerDTO();
        for (Field field : ALL_FIELDS) {
            field.setter().accept(
                    customer, parameterLookup.apply(field.formParameter()));
        }
        return customer;
    }

    static String selectColumns(String alias) {
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
        return joinColumns(ALL_FIELDS, prefix);
    }

    static String mutableAssignments() {
        return MUTABLE_ASSIGNMENTS;
    }

    static String insertColumns() {
        return INSERT_COLUMNS;
    }

    static String insertPlaceholders() {
        return INSERT_PLACEHOLDERS;
    }

    static int bindMutableFields(
            PreparedStatement statement,
            int startIndex,
            CustomerDTO customer) throws SQLException {
        int parameter = startIndex;
        for (Field field : MUTABLE_FIELDS) {
            bind(statement, parameter++, field, customer);
        }
        return parameter;
    }

    static int bindInsertFields(
            PreparedStatement statement,
            int startIndex,
            CustomerDTO customer) throws SQLException {
        statement.setString(startIndex, customer.getCustomerName());
        return bindMutableFields(statement, startIndex + 1, customer);
    }

    static CustomerDTO read(ResultSet resultSet) throws SQLException {
        CustomerDTO customer = new CustomerDTO();
        for (Field field : ALL_FIELDS) {
            String value;
            if (field.type() == FieldType.DATE) {
                Date dateValue = resultSet.getDate(field.columnName());
                value = dateValue == null ? null : dateValue.toLocalDate().toString();
            } else {
                value = resultSet.getString(field.columnName());
            }
            field.setter().accept(customer, value);
        }
        return customer;
    }

    static List<String> formParameterNames() {
        return ALL_FIELDS.stream().map(Field::formParameter).toList();
    }

    private static void bind(
            PreparedStatement statement,
            int parameterIndex,
            Field field,
            CustomerDTO customer) throws SQLException {
        String value = field.getter().apply(customer);
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(
                    parameterIndex,
                    field.type() == FieldType.DATE ? Types.DATE : Types.VARCHAR);
            return;
        }
        String normalized = value.trim();
        if (field.type() == FieldType.DATE) {
            statement.setDate(parameterIndex, Date.valueOf(normalized));
        } else {
            statement.setString(parameterIndex, normalized);
        }
    }

    private static String joinColumns(List<Field> fields, String prefix) {
        return fields.stream()
                .map(field -> prefix + field.columnName())
                .collect(Collectors.joining(", "));
    }

    private static Field text(
            String formParameter,
            String columnName,
            Function<CustomerDTO, String> getter,
            BiConsumer<CustomerDTO, String> setter) {
        return new Field(
                formParameter, columnName, getter, setter, FieldType.TEXT);
    }

    private static Field date(
            String formParameter,
            String columnName,
            Function<CustomerDTO, String> getter,
            BiConsumer<CustomerDTO, String> setter) {
        return new Field(
                formParameter, columnName, getter, setter, FieldType.DATE);
    }

    private record Field(
            String formParameter,
            String columnName,
            Function<CustomerDTO, String> getter,
            BiConsumer<CustomerDTO, String> setter,
            FieldType type) {
    }

    private enum FieldType {
        TEXT,
        DATE
    }
}
