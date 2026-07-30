package com.company.model;

public class CustomerDTO {
    private String customerName;
    private String firstIntroductionYear;
    private String dbName;
    private String verticaVersion;
    private String verticaEos;
    private String mode;
    private String os;
    private String nodes;
    private String licenseSize;
    private String managerName;
    private String subManagerName;
    private String said;
    private String note;
    private String osStorageConfig;
    private String backupConfig;
    private String biTool;
    private String etlTool;
    private String dbEncryption;
    private String cdcTool;
    private String customerType;

    // 기본 생성자
    public CustomerDTO() {}

    // 게터와 세터 메소드
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getFirstIntroductionYear() {
        return firstIntroductionYear;
    }

    public void setFirstIntroductionYear(String firstIntroductionYear) {
        this.firstIntroductionYear = firstIntroductionYear;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getVerticaVersion() {
        return verticaVersion;
    }

    public void setVerticaVersion(String verticaVersion) {
        this.verticaVersion = verticaVersion;
    }

    public String getVerticaEos() {
        return verticaEos;
    }

    public void setVerticaEos(String verticaEos) {
        this.verticaEos = verticaEos;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getLicenseSize() {
        return licenseSize;
    }

    public void setLicenseSize(String licenseSize) {
        this.licenseSize = licenseSize;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getSubManagerName() {
        return subManagerName;
    }

    public void setSubManagerName(String subManagerName) {
        this.subManagerName = subManagerName;
    }

    public String getSaid() {
        return said;
    }

    public void setSaid(String said) {
        this.said = said;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOsStorageConfig() {
        return osStorageConfig;
    }

    public void setOsStorageConfig(String osStorageConfig) {
        this.osStorageConfig = osStorageConfig;
    }

    public String getBackupConfig() {
        return backupConfig;
    }

    public void setBackupConfig(String backupConfig) {
        this.backupConfig = backupConfig;
    }

    public String getBiTool() {
        return biTool;
    }

    public void setBiTool(String biTool) {
        this.biTool = biTool;
    }

    public String getEtlTool() {
        return etlTool;
    }

    public void setEtlTool(String etlTool) {
        this.etlTool = etlTool;
    }

    public String getDbEncryption() {
        return dbEncryption;
    }

    public void setDbEncryption(String dbEncryption) {
        this.dbEncryption = dbEncryption;
    }

    public String getCdcTool() {
        return cdcTool;
    }

    public void setCdcTool(String cdcTool) {
        this.cdcTool = cdcTool;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }
}