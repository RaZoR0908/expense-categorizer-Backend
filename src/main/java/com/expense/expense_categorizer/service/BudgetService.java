package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.model.Budget;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.BudgetRepository;
import com.expense.expense_categorizer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public static class BudgetStatusDTO {
        public Long id;
        public String category;
        public BigDecimal monthlyLimit;
        public BigDecimal actualSpent;
        public BigDecimal remaining;
        public Double percentageUsed;
        public String status; // ON_TRACK, WARNING, EXCEEDED

        public BudgetStatusDTO(Long id, String category, BigDecimal monthlyLimit,
                               BigDecimal actualSpent) {
            this.id = id;
            this.category = category;
            this.monthlyLimit = monthlyLimit;
            this.actualSpent = actualSpent;
            this.remaining = monthlyLimit.subtract(actualSpent);

            if (monthlyLimit.signum() != 0) {
                this.percentageUsed = actualSpent.divide(monthlyLimit, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            } else {
                this.percentageUsed = 0.0;
            }

            if (this.percentageUsed >= 100) {
                this.status = "EXCEEDED";
            } else if (this.percentageUsed >= 80) {
                this.status = "WARNING";
            } else {
                this.status = "ON_TRACK";
            }
        }

        public Long getId() { return id; }
        public String getCategory() { return category; }
        public BigDecimal getMonthlyLimit() { return monthlyLimit; }
        public BigDecimal getActualSpent() { return actualSpent; }
        public BigDecimal getRemaining() { return remaining; }
        public Double getPercentageUsed() { return percentageUsed; }
        public String getStatus() { return status; }
    }

    public static class BudgetSummaryDTO {
        public List<BudgetStatusDTO> budgets;
        public BigDecimal totalBudgeted;
        public BigDecimal totalSpent;
        public BigDecimal totalRemaining;
        public int exceededCount;
        public int warningCount;
        public int onTrackCount;
        public String yearMonth;

        public List<BudgetStatusDTO> getBudgets() { return budgets; }
        public BigDecimal getTotalBudgeted() { return totalBudgeted; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public BigDecimal getTotalRemaining() { return totalRemaining; }
        public int getExceededCount() { return exceededCount; }
        public int getWarningCount() { return warningCount; }
        public int getOnTrackCount() { return onTrackCount; }
        public String getYearMonth() { return yearMonth; }
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public BudgetSummaryDTO getBudgetStatus(User user, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findByUser(user);

        List<BudgetStatusDTO> statusList = budgets.stream().map(budget -> {
            Double spent = transactionRepository.getTotalByCategory(
                    user, budget.getCategory(), startDate, endDate);
            BigDecimal actualSpent = spent != null
                    ? BigDecimal.valueOf(spent).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new BudgetStatusDTO(
                    budget.getId(),
                    budget.getCategory(),
                    budget.getMonthlyLimit(),
                    actualSpent
            );
        }).collect(Collectors.toList());

        // Build summary
        BudgetSummaryDTO summary = new BudgetSummaryDTO();
        summary.budgets = statusList;
        summary.yearMonth = yearMonth.toString();
        summary.totalBudgeted = statusList.stream()
                .map(s -> s.monthlyLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.totalSpent = statusList.stream()
                .map(s -> s.actualSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.totalRemaining = summary.totalBudgeted.subtract(summary.totalSpent);
        summary.exceededCount = (int) statusList.stream()
                .filter(s -> s.status.equals("EXCEEDED")).count();
        summary.warningCount = (int) statusList.stream()
                .filter(s -> s.status.equals("WARNING")).count();
        summary.onTrackCount = (int) statusList.stream()
                .filter(s -> s.status.equals("ON_TRACK")).count();

        return summary;
    }
}