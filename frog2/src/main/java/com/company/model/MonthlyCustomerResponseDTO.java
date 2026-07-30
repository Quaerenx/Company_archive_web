package com.company.model;

import java.util.Date;

/**
 * 월별 고객 응대 기록 DTO
 */
public class MonthlyCustomerResponseDTO {
    private int id;
    private String userName;
    private Date responseDate;
    private String customerName;
    private String reason;
    private String actionContent;
    private String note;
    private Date createdAt;
    private Date updatedAt;
    
    // 기본 생성자
    public MonthlyCustomerResponseDTO() {
    }
    
    // 전체 생성자
    public MonthlyCustomerResponseDTO(int id, String userName, Date responseDate, String customerName, 
                                     String reason, String actionContent, String note, 
                                     Date createdAt, Date updatedAt) {
        this.id = id;
        this.userName = userName;
        this.responseDate = responseDate;
        this.customerName = customerName;
        this.reason = reason;
        this.actionContent = actionContent;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Date getResponseDate() {
        return responseDate;
    }
    
    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getActionContent() {
        return actionContent;
    }
    
    public void setActionContent(String actionContent) {
        this.actionContent = actionContent;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "MonthlyCustomerResponseDTO{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", responseDate=" + responseDate +
                ", customerName='" + customerName + '\'' +
                ", reason='" + reason + '\'' +
                ", actionContent='" + actionContent + '\'' +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
