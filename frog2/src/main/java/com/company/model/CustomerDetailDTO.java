package com.company.model;

import java.util.Date;

public class CustomerDetailDTO {
    // 메타정보
    private String customerName;
    private String systemName;
    private String customerManager;
    private String siCompany;
    private String siManager;
    private String creator;
    private Date createDate;
    private String mainManager;
    private String subManager;
    private Date installDate;
    private String introductionYear;


    // Vertica 정보
    private String dbName;
    private String dbMode;
    private String verticaVersion;
    private String licenseInfo;
    private String said;
    private String nodeCount;
    private String verticaAdmin;
    private String subclusterYn;
    private String mcYn;
    private String mcHost;
    private String mcVersion;
    private String mcAdmin;
    private String backupYn;
    private String customResourcePoolYn;
    private String backupNote;

    // 환경 정보
    private String osInfo;
    private String memoryInfo;
    private String infraType;
    private String cpuSocket;
    private String hyperThreading;
    private String cpuCore;
    private String dataArea;
    private String depotArea;
    private String catalogArea;
    private String objectArea;
    private String publicYn;
    private String publicNetwork;
    private String privateYn;
    private String privateNetwork;
    private String storageYn;
    private String storageNetwork;

    // 외부 솔루션
    private String etlTool;
    private String biTool;
    private String dbEncryption;
    private String cdcTool;

    // 기타
    private Date eosDate;
    private String customerType;
    private String note;

    // 기본 생성자
    public CustomerDetailDTO() {}

    // Getter/Setter 메소드들
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerManager() {
        return customerManager;
    }

    public void setCustomerManager(String customerManager) {
        this.customerManager = customerManager;
    }

    public String getSiCompany() {
        return siCompany;
    }

    public void setSiCompany(String siCompany) {
        this.siCompany = siCompany;
    }

    public String getSiManager() {
        return siManager;
    }

    public void setSiManager(String siManager) {
        this.siManager = siManager;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getMainManager() {
        return mainManager;
    }

    public void setMainManager(String mainManager) {
        this.mainManager = mainManager;
    }

    public String getSubManager() {
        return subManager;
    }

    public void setSubManager(String subManager) {
        this.subManager = subManager;
    }

    public Date getInstallDate() {
        return installDate;
    }

    public void setInstallDate(Date installDate) {
        this.installDate = installDate;
    }

    public String getIntroductionYear() {
        return introductionYear;
    }

    public void setIntroductionYear(String introductionYear) {
        this.introductionYear = introductionYear;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbMode() {
        return dbMode;
    }

    public void setDbMode(String dbMode) {
        this.dbMode = dbMode;
    }

    public String getVerticaVersion() {
        return verticaVersion;
    }

    public void setVerticaVersion(String verticaVersion) {
        this.verticaVersion = verticaVersion;
    }

    public String getLicenseInfo() {
        return licenseInfo;
    }

    public void setLicenseInfo(String licenseInfo) {
        this.licenseInfo = licenseInfo;
    }

    public String getSaid() {
        return said;
    }

    public void setSaid(String said) {
        this.said = said;
    }

    public String getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(String nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getVerticaAdmin() {
        return verticaAdmin;
    }

    public void setVerticaAdmin(String verticaAdmin) {
        this.verticaAdmin = verticaAdmin;
    }

    public String getSubclusterYn() {
        return subclusterYn;
    }

    public void setSubclusterYn(String subclusterYn) {
        this.subclusterYn = subclusterYn;
    }

    public String getMcYn() {
        return mcYn;
    }

    public void setMcYn(String mcYn) {
        this.mcYn = mcYn;
    }

    public String getMcHost() {
        return mcHost;
    }

    public void setMcHost(String mcHost) {
        this.mcHost = mcHost;
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public String getMcAdmin() {
        return mcAdmin;
    }

    public void setMcAdmin(String mcAdmin) {
        this.mcAdmin = mcAdmin;
    }

    public String getBackupYn() {
        return backupYn;
    }

    public void setBackupYn(String backupYn) {
        this.backupYn = backupYn;
    }

    public String getCustomResourcePoolYn() {
        return customResourcePoolYn;
    }

    public void setCustomResourcePoolYn(String customResourcePoolYn) {
        this.customResourcePoolYn = customResourcePoolYn;
    }

    public String getBackupNote() {
        return backupNote;
    }

    public void setBackupNote(String backupNote) {
        this.backupNote = backupNote;
    }

    public String getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }

    public String getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(String memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public String getInfraType() {
        return infraType;
    }

    public void setInfraType(String infraType) {
        this.infraType = infraType;
    }

    public String getCpuSocket() {
        return cpuSocket;
    }

    public void setCpuSocket(String cpuSocket) {
        this.cpuSocket = cpuSocket;
    }

    public String getHyperThreading() {
        return hyperThreading;
    }

    public void setHyperThreading(String hyperThreading) {
        this.hyperThreading = hyperThreading;
    }

    public String getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(String cpuCore) {
        this.cpuCore = cpuCore;
    }

    public String getDataArea() {
        return dataArea;
    }

    public void setDataArea(String dataArea) {
        this.dataArea = dataArea;
    }

    public String getDepotArea() {
        return depotArea;
    }

    public void setDepotArea(String depotArea) {
        this.depotArea = depotArea;
    }

    public String getCatalogArea() {
        return catalogArea;
    }

    public void setCatalogArea(String catalogArea) {
        this.catalogArea = catalogArea;
    }

    public String getObjectArea() {
        return objectArea;
    }

    public void setObjectArea(String objectArea) {
        this.objectArea = objectArea;
    }

    public String getPublicYn() {
        return publicYn;
    }

    public void setPublicYn(String publicYn) {
        this.publicYn = publicYn;
    }

    public String getPublicNetwork() {
        return publicNetwork;
    }

    public void setPublicNetwork(String publicNetwork) {
        this.publicNetwork = publicNetwork;
    }

    public String getPrivateYn() {
        return privateYn;
    }

    public void setPrivateYn(String privateYn) {
        this.privateYn = privateYn;
    }

    public String getPrivateNetwork() {
        return privateNetwork;
    }

    public void setPrivateNetwork(String privateNetwork) {
        this.privateNetwork = privateNetwork;
    }

    public String getStorageYn() {
        return storageYn;
    }

    public void setStorageYn(String storageYn) {
        this.storageYn = storageYn;
    }

    public String getStorageNetwork() {
        return storageNetwork;
    }

    public void setStorageNetwork(String storageNetwork) {
        this.storageNetwork = storageNetwork;
    }

    public String getEtlTool() {
        return etlTool;
    }

    public void setEtlTool(String etlTool) {
        this.etlTool = etlTool;
    }

    public String getBiTool() {
        return biTool;
    }

    public void setBiTool(String biTool) {
        this.biTool = biTool;
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

    public Date getEosDate() {
        return eosDate;
    }

    public void setEosDate(Date eosDate) {
        this.eosDate = eosDate;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }
}