package com.company.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomerFieldContractTest {
    @Test
    void everyContractFieldExistsInTheSharedCustomerForm() throws Exception {
        String form = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/includes/_customer_form_fields.jspf"));

        for (String parameter : CustomerFieldContract.formParameterNames()) {
            assertTrue(
                    form.contains("name=\"" + parameter + "\""),
                    () -> "Missing customer form field: " + parameter);
        }
    }

    @Test
    void formValuesRoundTripThroughInsertAndRowMapping() {
        Map<String, String> form = formValues();
        CustomerDTO submitted = CustomerFieldContract.fromForm(form::get);
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        assertTrue(dao.addCustomer(submitted));

        PaginationJdbcFixture.StatementRecord insert =
                jdbc.statements.getFirst();
        jdbc.enqueue(rowProducedByInsert(insert));
        CustomerDTO reloaded = dao.getCustomerByName("테스트 고객사");

        assertCustomerEquals(submitted, reloaded);
    }

    private static Map<String, String> formValues() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("customer_name", "테스트 고객사");
        form.put("db_name", "archive");
        form.put("vertica_version", "25.1.0-3");
        form.put("mode", "ENT");
        form.put("os", "RHEL 9.4");
        form.put("nodes", "4");
        form.put("license_size", "15TB");
        form.put("manager_name", "주 담당자");
        form.put("sub_manager_name", "부 담당자");
        form.put("said", "A-S100000001");
        form.put("customer_type", "정기점검 계약 고객사");
        form.put("first_introduction_year", "2026");
        form.put("vertica_eos", "2028-01-31");
        form.put("os_storage_config", "SAN storage");
        form.put("backup_config", "weekly VBR");
        form.put("etl_tool", "ETL");
        form.put("bi_tool", "BI");
        form.put("db_encryption", "enabled");
        form.put("cdc_tool", "CDC");
        form.put("note", "contract round trip");
        return form;
    }

    private static Map<String, Object> rowProducedByInsert(
            PaginationJdbcFixture.StatementRecord insert) {
        int columnsStart = insert.sql.indexOf('(') + 1;
        int columnsEnd = insert.sql.indexOf(") VALUES", columnsStart);
        List<String> columns = List.of(
                insert.sql.substring(columnsStart, columnsEnd).split(", "));
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 1; index <= insert.parameters.size(); index++) {
            row.put(columns.get(index - 1), insert.parameters.get(index));
        }
        row.put("is_deleted", 1);
        return row;
    }

    private static void assertCustomerEquals(
            CustomerDTO expected, CustomerDTO actual) {
        assertAll(
                () -> assertEquals(expected.getCustomerName(), actual.getCustomerName()),
                () -> assertEquals(expected.getDbName(), actual.getDbName()),
                () -> assertEquals(expected.getVerticaVersion(), actual.getVerticaVersion()),
                () -> assertEquals(expected.getMode(), actual.getMode()),
                () -> assertEquals(expected.getOs(), actual.getOs()),
                () -> assertEquals(expected.getNodes(), actual.getNodes()),
                () -> assertEquals(expected.getLicenseSize(), actual.getLicenseSize()),
                () -> assertEquals(expected.getManagerName(), actual.getManagerName()),
                () -> assertEquals(expected.getSubManagerName(), actual.getSubManagerName()),
                () -> assertEquals(expected.getSaid(), actual.getSaid()),
                () -> assertEquals(expected.getCustomerType(), actual.getCustomerType()),
                () -> assertEquals(
                        expected.getFirstIntroductionYear(),
                        actual.getFirstIntroductionYear()),
                () -> assertEquals(expected.getVerticaEos(), actual.getVerticaEos()),
                () -> assertEquals(
                        expected.getOsStorageConfig(),
                        actual.getOsStorageConfig()),
                () -> assertEquals(expected.getBackupConfig(), actual.getBackupConfig()),
                () -> assertEquals(expected.getEtlTool(), actual.getEtlTool()),
                () -> assertEquals(expected.getBiTool(), actual.getBiTool()),
                () -> assertEquals(expected.getDbEncryption(), actual.getDbEncryption()),
                () -> assertEquals(expected.getCdcTool(), actual.getCdcTool()),
                () -> assertEquals(expected.getNote(), actual.getNote()));
    }
}
