package com.expense.expense_categorizer.dto;

import java.math.BigDecimal;

public class BudgetDTO {
    private Long id;
    private String category;
    private BigDecimal monthlyLimit;

    public BudgetDTO() {}

    public BudgetDTO(Long id, String category, BigDecimal monthlyLimit) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}