package com.expense.expense_categorizer.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

public class InsightResponseDTO {
    private BigDecimal totalSpent;
    private BigDecimal totalSaved;
    private String topCategory;
    private Double percentageOfTopCategory;
    private Map<String, BigDecimal> categoryBreakdown;
    private List<String> aiInsights;
    private BigDecimal monthOnMonthDelta;

    public InsightResponseDTO() {}

    public InsightResponseDTO(BigDecimal totalSpent, BigDecimal totalSaved, String topCategory, Double percentageOfTopCategory, Map<String, BigDecimal> categoryBreakdown, List<String> aiInsights, BigDecimal monthOnMonthDelta) {
        this.totalSpent = totalSpent;
        this.totalSaved = totalSaved;
        this.topCategory = topCategory;
        this.percentageOfTopCategory = percentageOfTopCategory;
        this.categoryBreakdown = categoryBreakdown;
        this.aiInsights = aiInsights;
        this.monthOnMonthDelta = monthOnMonthDelta;
    }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getTotalSaved() { return totalSaved; }
    public void setTotalSaved(BigDecimal totalSaved) { this.totalSaved = totalSaved; }

    public String getTopCategory() { return topCategory; }
    public void setTopCategory(String topCategory) { this.topCategory = topCategory; }

    public Double getPercentageOfTopCategory() { return percentageOfTopCategory; }
    public void setPercentageOfTopCategory(Double percentageOfTopCategory) { this.percentageOfTopCategory = percentageOfTopCategory; }

    public Map<String, BigDecimal> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public List<String> getAiInsights() { return aiInsights; }
    public void setAiInsights(List<String> aiInsights) { this.aiInsights = aiInsights; }

    public BigDecimal getMonthOnMonthDelta() { return monthOnMonthDelta; }
    public void setMonthOnMonthDelta(BigDecimal monthOnMonthDelta) { this.monthOnMonthDelta = monthOnMonthDelta; }
}