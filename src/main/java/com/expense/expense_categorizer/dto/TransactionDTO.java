package com.expense.expense_categorizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDTO {
    private Long id;
    private LocalDate date;
    private BigDecimal amount;
    private String merchant;
    private String category;
    private Double confidenceScore;
    private Boolean isCorrected;

    public TransactionDTO() {}

    public TransactionDTO(Long id, LocalDate date, BigDecimal amount, String merchant, String category, Double confidenceScore, Boolean isCorrected) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.merchant = merchant;
        this.category = category;
        this.confidenceScore = confidenceScore;
        this.isCorrected = isCorrected;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Boolean getIsCorrected() { return isCorrected; }
    public void setIsCorrected(Boolean isCorrected) { this.isCorrected = isCorrected; }
}