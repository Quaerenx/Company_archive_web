package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerFieldContract;
import com.company.util.Pagination;
import com.company.util.SearchQueryPolicy;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;

final class CustomerRequestMapper {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final String UNUSED = "미사용";

    CustomerDTO mapCustomer(HttpServletRequest request) {
        return CustomerFieldContract.fromForm(request::getParameter);
    }

    CustomerDetailDTO mapCustomerDetail(HttpServletRequest request) throws ParseException {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName(trimmed(request, "customerName"));
        detail.setSystemName(request.getParameter("systemName"));
        detail.setCustomerManager(trimmed(request, "customerManager"));
        detail.setSiCompany(trimmed(request, "siCompany"));
        detail.setSiManager(trimmed(request, "siManager"));
        detail.setCreator(trimmed(request, "creator"));
        detail.setCreateDate(parseDate(request.getParameter("createDate")));
        detail.setMainManager(trimmed(request, "mainManager"));
        detail.setSubManager(trimmed(request, "subManager"));
        detail.setInstallDate(parseDate(request.getParameter("installDate")));
        detail.setIntroductionYear(trimmed(request, "introductionYear"));
        detail.setDbName(trimmed(request, "dbName"));
        detail.setDbMode(trimmed(request, "dbMode"));
        detail.setVerticaVersion(trimmed(request, "verticaVersion"));
        detail.setLicenseInfo(trimmed(request, "licenseInfo"));
        detail.setSaid(trimmed(request, "said"));
        detail.setNodeCount(trimmed(request, "nodeCount"));
        detail.setVerticaAdmin(trimmed(request, "verticaAdmin"));
        detail.setSubclusterYn(trimmed(request, "subclusterYn"));
        detail.setMcYn(trimmed(request, "mcYn"));
        detail.setMcHost(trimmed(request, "mcHost"));
        detail.setMcVersion(trimmed(request, "mcVersion"));
        detail.setMcAdmin(trimmed(request, "mcAdmin"));
        detail.setBackupYn(trimmed(request, "backupYn"));
        detail.setCustomResourcePoolYn(trimmed(request, "customResourcePoolYn"));
        detail.setBackupNote(trimmed(request, "backupNote"));
        detail.setOsInfo(trimmed(request, "osInfo"));
        detail.setMemoryInfo(trimmed(request, "memoryInfo"));
        detail.setSwapMemory(trimmed(request, "swapMemory"));
        detail.setInfraType(trimmed(request, "infraType"));
        detail.setCpuSocket(trimmed(request, "cpuSocket"));
        detail.setHyperThreading(trimmed(request, "hyperThreading"));
        detail.setCpuCore(trimmed(request, "cpuCore"));
        detail.setDataArea(trimmed(request, "dataArea"));
        detail.setDepotArea(trimmed(request, "depotArea"));
        detail.setCatalogArea(trimmed(request, "catalogArea"));
        detail.setObjectArea(trimmed(request, "objectArea"));
        detail.setPublicNetwork(trimmed(request, "publicNetwork"));
        detail.setPrivateNetwork(trimmed(request, "privateNetwork"));
        detail.setStorageNetwork(trimmed(request, "storageNetwork"));
        detail.setEtlTool(trimmed(request, "etlTool"));
        detail.setBiTool(trimmed(request, "biTool"));
        detail.setDbEncryption(trimmed(request, "dbEncryption"));
        detail.setCdcTool(trimmed(request, "cdcTool"));
        detail.setEosDate(parseDate(request.getParameter("eosDate")));
        detail.setCustomerType(trimmed(request, "customerType"));
        detail.setNote(trimmed(request, "note"));
        normalizeConditionalChildren(detail);
        return detail;
    }

    private static void normalizeConditionalChildren(CustomerDetailDTO detail) {
        if (equalsIgnoreCase(detail.getDbMode(), "ENT")) {
            detail.setDepotArea(UNUSED);
            detail.setObjectArea(UNUSED);
            detail.setStorageNetwork(UNUSED);
        }
        detail.setPublicYn(usageFlag(detail.getPublicNetwork()));
        detail.setPrivateYn(usageFlag(detail.getPrivateNetwork()));
        detail.setStorageYn(usageFlag(detail.getStorageNetwork()));
        if (equalsIgnoreCase(detail.getMcYn(), "N")) {
            detail.setMcHost(UNUSED);
            detail.setMcVersion(UNUSED);
            detail.setMcAdmin(UNUSED);
        }
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected.equalsIgnoreCase(value.trim());
    }

    private static String usageFlag(String networkValue) {
        return networkValue == null
                || networkValue.isBlank()
                || equalsIgnoreCase(networkValue, UNUSED)
                ? "N"
                : "Y";
    }

    CustomerEnvironment environment(HttpServletRequest request) {
        return CustomerEnvironment.fromExternalValue(request.getParameter("env"));
    }

    String decodedParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value;
    }

    int requestedPage(HttpServletRequest request) {
        return Pagination.requestedPage(request.getParameter("page"));
    }

    int requestedPageSize(HttpServletRequest request) {
        return Pagination.requestedPageSize(
                request.getParameter("pageSize"),
                DEFAULT_PAGE_SIZE,
                MAXIMUM_PAGE_SIZE);
    }

    String searchQuery(HttpServletRequest request) {
        return SearchQueryPolicy.normalize(request.getParameter("q"));
    }

    private static String trimmed(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static Date parseDate(String value) throws ParseException {
        return StrictDateParser.parseDate(value);
    }
}
