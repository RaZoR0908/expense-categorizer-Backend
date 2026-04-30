package com.expense.expense_categorizer.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String merchant;

    private String category;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "is_corrected")
    private Boolean isCorrected = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}