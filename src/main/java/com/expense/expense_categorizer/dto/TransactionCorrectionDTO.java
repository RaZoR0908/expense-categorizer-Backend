package com.expense.expense_categorizer.dto;

public class TransactionCorrectionDTO {
    private Long transactionId;
    private String newCategory;

    public TransactionCorrectionDTO() {}

    public TransactionCorrectionDTO(Long transactionId, String newCategory) {
        this.transactionId = transactionId;
        this.newCategory = newCategory;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getNewCategory() { return newCategory; }
    public void setNewCategory(String newCategory) { this.newCategory = newCategory; }
}