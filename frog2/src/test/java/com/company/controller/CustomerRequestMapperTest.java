package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import com.company.util.StrictDateParser;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomerRequestMapperTest {
    private final CustomerRequestMapper mapper = new CustomerRequestMapper();

    @Test
    void mapsLegacyCustomerFormParameterNames() {
        Map<String, String> parameters = new HashMap<>();
        for (String name : new String[] {
                "customer_name", "first_introduction_year", "db_name", "vertica_version",
                "vertica_eos", "mode", "os", "nodes", "license_size", "manager_name",
                "sub_manager_name", "said", "note", "os_storage_config", "backup_config",
                "bi_tool", "etl_tool", "db_encryption", "cdc_tool", "customer_type"}) {
            parameters.put(name, name + "-value");
        }

        CustomerDTO customer = mapper.mapCustomer(request(parameters));

        assertEquals("customer_name-value", customer.getCustomerName());
        assertEquals("first_introduction_year-value", customer.getFirstIntroductionYear());
        assertEquals("db_name-value", customer.getDbName());
        assertEquals("vertica_version-value", customer.getVerticaVersion());
        assertEquals("vertica_eos-value", customer.getVerticaEos());
        assertEquals("mode-value", customer.getMode());
        assertEquals("os-value", customer.getOs());
        assertEquals("nodes-value", customer.getNodes());
        assertEquals("license_size-value", customer.getLicenseSize());
        assertEquals("manager_name-value", customer.getManagerName());
        assertEquals("sub_manager_name-value", customer.getSubManagerName());
        assertEquals("said-value", customer.getSaid());
        assertEquals("note-value", customer.getNote());
        assertEquals("os_storage_config-value", customer.getOsStorageConfig());
        assertEquals("backup_config-value", customer.getBackupConfig());
        assertEquals("bi_tool-value", customer.getBiTool());
        assertEquals("etl_tool-value", customer.getEtlTool());
        assertEquals("db_encryption-value", customer.getDbEncryption());
        assertEquals("cdc_tool-value", customer.getCdcTool());
        assertEquals("customer_type-value", customer.getCustomerType());
    }

    @Test
    void mapsAndNormalizesLegacyDetailForm() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("customerName", "  customer  ");
        parameters.put("systemName", " system unchanged ");
        parameters.put("customerManager", "  manager  ");
        parameters.put("subclusterYn", " ");
        parameters.put("swapMemory", " 32GB ");
        parameters.put("createDate", "2026-07-21");
        parameters.put("installDate", "");
        parameters.put("eosDate", "2027-01-02");

        CustomerDetailDTO detail = mapper.mapCustomerDetail(request(parameters));

        assertEquals("customer", detail.getCustomerName());
        assertEquals(" system unchanged ", detail.getSystemName());
        assertEquals("manager", detail.getCustomerManager());
        assertNull(detail.getSubclusterYn());
        assertEquals("32GB", detail.getSwapMemory());
        assertEquals(StrictDateParser.parseDate("2026-07-21"), detail.getCreateDate());
        assertNull(detail.getInstallDate());
        assertEquals(StrictDateParser.parseDate("2027-01-02"), detail.getEosDate());
    }

    @Test
    void marksChildrenUnusedWhenEntAndMcAreDisabled() throws Exception {
        CustomerDetailDTO detail = mapper.mapCustomerDetail(request(Map.of(
                "dbMode", "ENT",
                "depotArea", "legacy depot",
                "objectArea", "legacy object",
                "storageNetwork", "legacy storage",
                "publicNetwork", "10.10.0.0/24",
                "privateNetwork", "미사용",
                "mcYn", "N",
                "mcHost", "legacy host",
                "mcVersion", "legacy version",
                "mcAdmin", "legacy admin")));

        assertAll(
                () -> assertEquals("미사용", detail.getDepotArea()),
                () -> assertEquals("미사용", detail.getObjectArea()),
                () -> assertEquals("N", detail.getStorageYn()),
                () -> assertEquals("미사용", detail.getStorageNetwork()),
                () -> assertEquals("Y", detail.getPublicYn()),
                () -> assertEquals("N", detail.getPrivateYn()),
                () -> assertEquals("미사용", detail.getMcHost()),
                () -> assertEquals("미사용", detail.getMcVersion()),
                () -> assertEquals("미사용", detail.getMcAdmin()));
    }

    @Test
    void preservesChildrenWhenTheirParentsAreEnabledOrUnregistered() throws Exception {
        CustomerDetailDTO enabled = mapper.mapCustomerDetail(request(Map.of(
                "dbMode", "EON",
                "depotArea", "2TB",
                "objectArea", "20TB",
                "storageNetwork", "storage subnet",
                "publicNetwork", "public subnet",
                "privateNetwork", "private subnet",
                "mcYn", "Y",
                "mcHost", "mc.example",
                "mcVersion", "25.1",
                "mcAdmin", "mcadmin")));
        CustomerDetailDTO unregistered = mapper.mapCustomerDetail(request(Map.of(
                "depotArea", "legacy depot",
                "objectArea", "legacy object",
                "mcHost", "legacy host")));

        assertAll(
                () -> assertEquals("2TB", enabled.getDepotArea()),
                () -> assertEquals("20TB", enabled.getObjectArea()),
                () -> assertEquals("Y", enabled.getPublicYn()),
                () -> assertEquals("Y", enabled.getPrivateYn()),
                () -> assertEquals("Y", enabled.getStorageYn()),
                () -> assertEquals("storage subnet", enabled.getStorageNetwork()),
                () -> assertEquals("mc.example", enabled.getMcHost()),
                () -> assertEquals("25.1", enabled.getMcVersion()),
                () -> assertEquals("mcadmin", enabled.getMcAdmin()),
                () -> assertEquals("legacy depot", unregistered.getDepotArea()),
                () -> assertEquals("legacy object", unregistered.getObjectArea()),
                () -> assertEquals("N", unregistered.getPublicYn()),
                () -> assertEquals("N", unregistered.getPrivateYn()),
                () -> assertEquals("N", unregistered.getStorageYn()),
                () -> assertEquals("legacy host", unregistered.getMcHost()));
    }

    @Test
    void rejectsInvalidCalendarDatesBeforeDaoCall() {
        Map<String, String> parameters = Map.of("createDate", "2026-02-30");

        assertThrows(ParseException.class, () -> mapper.mapCustomerDetail(request(parameters)));
    }

    @Test
    void environmentAllowsKnownValuesAndKeepsOnlyBlankLegacyProdFallback() {
        assertEquals(CustomerEnvironment.PROD, mapper.environment(request(Map.of())));
        assertEquals(CustomerEnvironment.PROD,
                mapper.environment(request(Map.of("env", "  "))));
        assertEquals(CustomerEnvironment.STAGING,
                mapper.environment(request(Map.of("env", "STG"))));
        assertEquals(CustomerEnvironment.DEVELOPMENT,
                mapper.environment(request(Map.of("env", " dev "))));
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.environment(request(Map.of("env", "arbitrary_table"))));
    }

    @Test
    void preservesValuesAlreadyDecodedByTheServletContainer() {
        assertAll(
                () -> assertEquals(
                        "Acme Corp",
                        mapper.decodedParameter(
                                request(Map.of("customerName", "Acme Corp")), "customerName")),
                () -> assertEquals(
                        "A+B",
                        mapper.decodedParameter(
                                request(Map.of("customerName", "A+B")), "customerName")),
                () -> assertEquals(
                        "100%",
                        mapper.decodedParameter(
                                request(Map.of("customerName", "100%")), "customerName")));
    }

    @Test
    void jsonStringEscapesJsonControlCharacters() {
        assertEquals("null", CustomerJsonResponse.jsonString(null));
        assertEquals(
                "\"a\\\"b\\\\c\\n\\b\\f\\u0001\"",
                CustomerJsonResponse.jsonString("a\"b\\c\n\b\f\u0001"));
    }

    @Test
    void normalizesListPaginationAndSearch() {
        HttpServletRequest request = request(Map.of(
                "page", "999999999999",
                "pageSize", "1000",
                "q", "  Acme  "));

        assertEquals(Integer.MAX_VALUE, mapper.requestedPage(request));
        assertEquals(100, mapper.requestedPageSize(request));
        assertEquals("Acme", mapper.searchQuery(request));
        assertEquals(1, mapper.requestedPage(request(Map.of("page", "0"))));
        assertEquals(50, mapper.requestedPageSize(request(Map.of())));
        assertNull(mapper.searchQuery(request(Map.of("q", "  "))));
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.searchQuery(request(Map.of("q", "a"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.searchQuery(
                        request(Map.of("q", "가".repeat(101)))));
    }

    private static HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> {
                    if ("getParameter".equals(call.getName())) {
                        return parameters.get((String) args[0]);
                    }
                    return defaultValue(call.getReturnType());
                });
    }

}
