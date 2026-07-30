package com.company.model;

public class UserDTO {
    private String userId;
    private String password;
    private String userName;
    private String department;

    // 기본 생성자
    public UserDTO() {}

    // 매개변수가 있는 생성자
    public UserDTO(String userId, String password, String userName, String department) {
        this.userId = userId;
        this.password = password;
        this.userName = userName;
        this.department = department;
    }

    // Getter와 Setter 메소드
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}