package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;

final class CustomerRequestMapper {
    CustomerDTO mapCustomer(HttpServletRequest request) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(request.getParameter("customer_name"));
        customer.setFirstIntroductionYear(request.getParameter("first_introduction_year"));
        customer.setDbName(request.getParameter("db_name"));
        customer.setVerticaVersion(request.getParameter("vertica_version"));
        customer.setVerticaEos(request.getParameter("vertica_eos"));
        customer.setMode(request.getParameter("mode"));
        customer.setOs(request.getParameter("os"));
        customer.setNodes(request.getParameter("nodes"));
        customer.setLicenseSize(request.getParameter("license_size"));
        customer.setManagerName(request.getParameter("manager_name"));
        customer.setSubManagerName(request.getParameter("sub_manager_name"));
        customer.setSaid(request.getParameter("said"));
        customer.setNote(request.getParameter("note"));
        customer.setOsStorageConfig(request.getParameter("os_storage_config"));
        customer.setBackupConfig(request.getParameter("backup_config"));
        customer.setBiTool(request.getParameter("bi_tool"));
        customer.setEtlTool(request.getParameter("etl_tool"));
        customer.setDbEncryption(request.getParameter("db_encryption"));
        customer.setCdcTool(request.getParameter("cdc_tool"));
        customer.setCustomerType(request.getParameter("customer_type"));
        return customer;
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
        detail.setInfraType(trimmed(request, "infraType"));
        detail.setCpuSocket(trimmed(request, "cpuSocket"));
        detail.setHyperThreading(trimmed(request, "hyperThreading"));
        detail.setCpuCore(trimmed(request, "cpuCore"));
        detail.setDataArea(trimmed(request, "dataArea"));
        detail.setDepotArea(trimmed(request, "depotArea"));
        detail.setCatalogArea(trimmed(request, "catalogArea"));
        detail.setObjectArea(trimmed(request, "objectArea"));
        detail.setPublicYn(trimmed(request, "publicYn"));
        detail.setPublicNetwork(trimmed(request, "publicNetwork"));
        detail.setPrivateYn(trimmed(request, "privateYn"));
        detail.setPrivateNetwork(trimmed(request, "privateNetwork"));
        detail.setStorageYn(trimmed(request, "storageYn"));
        detail.setStorageNetwork(trimmed(request, "storageNetwork"));
        detail.setEtlTool(trimmed(request, "etlTool"));
        detail.setBiTool(trimmed(request, "biTool"));
        detail.setDbEncryption(trimmed(request, "dbEncryption"));
        detail.setCdcTool(trimmed(request, "cdcTool"));
        detail.setEosDate(parseDate(request.getParameter("eosDate")));
        detail.setCustomerType(trimmed(request, "customerType"));
        detail.setNote(trimmed(request, "note"));
        return detail;
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
