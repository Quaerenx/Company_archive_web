package com.company.controller;

import com.company.util.StrictDateParser;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
import com.company.web.JsonResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CustomerJsonResponse {
    private CustomerJsonResponse() {
    }

    static void writeDetail(
            HttpServletResponse response, CustomerDetailDTO detail, CustomerDTO customer)
            throws IOException {
        String body = detail == null ? basicCustomer(customer) : customerDetail(detail);
        write(response, body);
    }

    static void writeMaintenanceOptions(
            HttpServletResponse response, List<CustomerDTO> customers) throws IOException {
        Set<String> customerNames = new LinkedHashSet<>();
        Set<String> inspectorNames = new LinkedHashSet<>();
        for (CustomerDTO customer : customers) {
            addNonBlank(customerNames, customer.getCustomerName());
            addNonBlank(inspectorNames, customer.getManagerName());
            addNonBlank(inspectorNames, customer.getSubManagerName());
        }

        StringBuilder json = new StringBuilder("{\"customers\":[");
        appendStrings(json, customerNames);
        json.append("],\"inspectors\":[");
        appendStrings(json, inspectorNames);
        json.append("]}");
        write(response, json.toString());
    }

    private static String customerDetail(CustomerDetailDTO detail) {
        StringBuilder json = new StringBuilder("{");
        append(json, "customerName", detail.getCustomerName());
        append(json, "systemName", detail.getSystemName());
        append(json, "customerManager", detail.getCustomerManager());
        append(json, "siCompany", detail.getSiCompany());
        append(json, "siManager", detail.getSiManager());
        append(json, "creator", detail.getCreator());
        append(json, "createDate", formatDate(detail.getCreateDate()));
        append(json, "mainManager", detail.getMainManager());
        append(json, "subManager", detail.getSubManager());
        append(json, "installDate", formatDate(detail.getInstallDate()));
        append(json, "introductionYear", detail.getIntroductionYear());
        append(json, "dbName", detail.getDbName());
        append(json, "dbMode", detail.getDbMode());
        append(json, "verticaVersion", detail.getVerticaVersion());
        append(json, "licenseInfo", detail.getLicenseInfo());
        append(json, "said", detail.getSaid());
        append(json, "nodeCount", detail.getNodeCount());
        append(json, "verticaAdmin", detail.getVerticaAdmin());
        append(json, "subclusterYn", detail.getSubclusterYn());
        append(json, "mcYn", detail.getMcYn());
        append(json, "mcHost", detail.getMcHost());
        append(json, "mcVersion", detail.getMcVersion());
        append(json, "mcAdmin", detail.getMcAdmin());
        append(json, "backupYn", detail.getBackupYn());
        append(json, "customResourcePoolYn", detail.getCustomResourcePoolYn());
        append(json, "backupNote", detail.getBackupNote());
        append(json, "osInfo", detail.getOsInfo());
        append(json, "memoryInfo", detail.getMemoryInfo());
        append(json, "swapMemory", detail.getSwapMemory());
        append(json, "infraType", detail.getInfraType());
        append(json, "cpuSocket", detail.getCpuSocket());
        append(json, "hyperThreading", detail.getHyperThreading());
        append(json, "cpuCore", detail.getCpuCore());
        append(json, "dataArea", detail.getDataArea());
        append(json, "depotArea", detail.getDepotArea());
        append(json, "catalogArea", detail.getCatalogArea());
        append(json, "objectArea", detail.getObjectArea());
        append(json, "publicYn", detail.getPublicYn());
        append(json, "publicNetwork", detail.getPublicNetwork());
        append(json, "privateYn", detail.getPrivateYn());
        append(json, "privateNetwork", detail.getPrivateNetwork());
        append(json, "storageYn", detail.getStorageYn());
        append(json, "storageNetwork", detail.getStorageNetwork());
        append(json, "etlTool", detail.getEtlTool());
        append(json, "biTool", detail.getBiTool());
        append(json, "dbEncryption", detail.getDbEncryption());
        append(json, "cdcTool", detail.getCdcTool());
        append(json, "eosDate", formatDate(detail.getEosDate()));
        append(json, "customerType", detail.getCustomerType());
        appendLast(json, "note", detail.getNote());
        return json.append('}').toString();
    }

    private static String basicCustomer(CustomerDTO customer) {
        StringBuilder json = new StringBuilder("{");
        if (customer != null) {
            append(json, "customerName", customer.getCustomerName());
            append(json, "introductionYear", customer.getFirstIntroductionYear());
            append(json, "dbName", customer.getDbName());
            append(json, "verticaVersion", customer.getVerticaVersion());
            append(json, "eosDate", customer.getVerticaEos());
            append(json, "dbMode", customer.getMode());
            append(json, "osInfo", customer.getOs());
            append(json, "nodeCount", customer.getNodes());
            append(json, "licenseInfo", customer.getLicenseSize());
            append(json, "mainManager", customer.getManagerName());
            append(json, "subManager", customer.getSubManagerName());
            append(json, "said", customer.getSaid());
            append(json, "storageYn", customer.getOsStorageConfig());
            append(json, "storageNetwork", customer.getOsStorageConfig());
            append(json, "backupNote", customer.getBackupConfig());
            append(json, "customerType", customer.getCustomerType());
            append(json, "etlTool", customer.getEtlTool());
            append(json, "biTool", customer.getBiTool());
            append(json, "dbEncryption", customer.getDbEncryption());
            append(json, "cdcTool", customer.getCdcTool());
            appendLast(json, "note", customer.getNote());
        }
        return json.append('}').toString();
    }

    private static void append(StringBuilder json, String name, String value) {
        json.append('\"').append(name).append("\":").append(jsonString(value)).append(',');
    }

    private static void appendLast(StringBuilder json, String name, String value) {
        json.append('\"').append(name).append("\":").append(jsonString(value));
    }

    private static void appendStrings(StringBuilder json, Set<String> values) {
        boolean first = true;
        for (String value : values) {
            if (!first) {
                json.append(',');
            }
            json.append(jsonString(value));
            first = false;
        }
    }

    private static void addNonBlank(Set<String> values, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.add(value.trim());
        }
    }

    static String jsonString(String value) {
        return JsonResponse.nullableString(value);
    }

    private static String formatDate(Date date) {
        return StrictDateParser.formatDate(date);
    }

    private static void write(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(body);
    }
}
