package com.expense.expense_categorizer.dto;

public class AuthResponseDTO {
    private Long userId;
    private String email;
    private String fullName;
    private String token;

    public AuthResponseDTO() {}

    public AuthResponseDTO(Long userId, String email, String fullName, String token) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.token = token;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}