package com.expense.expense_categorizer.dto;

public class RegisterRequestDTO {
    private String email;
    private String password;
    private String fullName;

    public RegisterRequestDTO() {}

    public RegisterRequestDTO(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}