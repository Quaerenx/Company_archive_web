package com.company.model;

import java.sql.Date;
import java.sql.Timestamp;

public class MaintenanceRecordDTO {
    private Long maintenanceId;
    private String creatorUserId;
    private String customerName;
    private String inspectorName;
    private Date inspectionDate;
    private String verticaVersion;
    private String note;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    // 신규 필드: 라이선스 크기(GB), 라이선스 사용량(GB), 라이선스 사용률(%) - 모두 선택 입력 (varchar)
    private String licenseSizeGb; // 예: "100"
    private String licenseUsageSize; // 예: "75"
    private String licenseUsagePct; // 예: "75"

    // 기본 생성자
    public MaintenanceRecordDTO() {}

    // 게터와 세터 메소드
    public Long getMaintenanceId() {
        return maintenanceId;
    }

    public void setMaintenanceId(Long maintenanceId) {
        this.maintenanceId = maintenanceId;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
    }

    public Date getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(Date inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public String getVerticaVersion() {
        return verticaVersion;
    }

    public void setVerticaVersion(String verticaVersion) {
        this.verticaVersion = verticaVersion;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLicenseSizeGb() {
        return licenseSizeGb;
    }

    public void setLicenseSizeGb(String licenseSizeGb) {
        this.licenseSizeGb = licenseSizeGb;
    }

    public String getLicenseUsagePct() {
        return licenseUsagePct;
    }

    public void setLicenseUsagePct(String licenseUsagePct) {
        this.licenseUsagePct = licenseUsagePct;
    }

    public String getLicenseUsageSize() {
        return licenseUsageSize;
    }

    public void setLicenseUsageSize(String licenseUsageSize) {
        this.licenseUsageSize = licenseUsageSize;
    }

}
