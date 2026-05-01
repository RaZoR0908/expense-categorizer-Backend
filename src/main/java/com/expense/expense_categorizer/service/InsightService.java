package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.controller.InsightController.ComparisonResponseDTO;
import com.expense.expense_categorizer.dto.InsightResponseDTO;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InsightService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetService budgetService;

    public InsightResponseDTO getMonthlyInsights(User user, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetween(user, startDate, endDate);

        InsightResponseDTO insights = calculateInsights(transactions);

        // ✅ Add budget comparison
        try {
            BudgetService.BudgetSummaryDTO budgetStatus =
                    budgetService.getBudgetStatus(user, yearMonth);

            List<String> aiInsights = new ArrayList<>(insights.getAiInsights());

            // Overall budget summary
            aiInsights.add("Total budgeted: ₹" + budgetStatus.totalBudgeted
                    + " | Total spent: ₹" + budgetStatus.totalSpent
                    + " | Remaining: ₹" + budgetStatus.totalRemaining);

            // Exceeded categories
            if (budgetStatus.exceededCount > 0) {
                aiInsights.add("⚠️ You exceeded budget in "
                        + budgetStatus.exceededCount + " categories!");
            }

            // Warning categories
            if (budgetStatus.warningCount > 0) {
                aiInsights.add("🔔 " + budgetStatus.warningCount
                        + " categories are close to their limit");
            }

            // Per category status
            for (BudgetService.BudgetStatusDTO b : budgetStatus.budgets) {
                if (b.status.equals("EXCEEDED")) {
                    aiInsights.add("❌ " + b.category
                            + ": Over budget by ₹" + b.remaining.abs()
                            + " (" + String.format("%.1f", b.percentageUsed) + "% used)");
                } else if (b.status.equals("WARNING")) {
                    aiInsights.add("⚠️ " + b.category
                            + ": ₹" + b.remaining + " remaining"
                            + " (" + String.format("%.1f", b.percentageUsed) + "% used)");
                } else {
                    aiInsights.add("✅ " + b.category
                            + ": ₹" + b.remaining + " remaining"
                            + " (" + String.format("%.1f", b.percentageUsed) + "% used)");
                }
            }

            insights.setAiInsights(aiInsights);
        } catch (Exception e) {
            // Return insights without budget if no budgets set
        }

        return insights;
    }

    public InsightResponseDTO getYearlyInsights(User user, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetween(user, startDate, endDate);

        return calculateInsights(transactions);
    }

    public ComparisonResponseDTO compareMonths(User user,
            YearMonth current, YearMonth previous) {
        Double currentTotal = transactionRepository.getTotalSpentInRange(
                user, current.atDay(1), current.atEndOfMonth());

        Double previousTotal = transactionRepository.getTotalSpentInRange(
                user, previous.atDay(1), previous.atEndOfMonth());

        BigDecimal currentBD = BigDecimal.valueOf(
                currentTotal != null ? currentTotal : 0);
        BigDecimal previousBD = BigDecimal.valueOf(
                previousTotal != null ? previousTotal : 0);

        return new ComparisonResponseDTO(currentBD, previousBD);
    }

    private InsightResponseDTO calculateInsights(List<Transaction> transactions) {
        // Total spent
        BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category breakdown
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        for (Transaction txn : transactions) {
            String category = txn.getCategory() != null
                    ? txn.getCategory() : "Uncategorized";
            categoryBreakdown.put(category,
                    categoryBreakdown.getOrDefault(category, BigDecimal.ZERO)
                            .add(txn.getAmount()));
        }

        // Top category
        String topCategory = "None";
        Double percentageOfTop = 0.0;
        if (!categoryBreakdown.isEmpty()) {
            topCategory = categoryBreakdown.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");

            BigDecimal topAmount = categoryBreakdown.get(topCategory);
            if (totalSpent.signum() > 0) {
                percentageOfTop = topAmount.divide(totalSpent, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }

        List<String> aiInsights = generateInsights(
                transactions, categoryBreakdown, totalSpent);

        return new InsightResponseDTO(
                totalSpent,
                BigDecimal.ZERO,
                topCategory,
                percentageOfTop,
                categoryBreakdown,
                aiInsights,
                BigDecimal.ZERO
        );
    }

    private List<String> generateInsights(List<Transaction> transactions,
            Map<String, BigDecimal> categoryBreakdown, BigDecimal totalSpent) {
        List<String> insights = new ArrayList<>();

        insights.add("You spent ₹" + totalSpent + " this period");

        if (!categoryBreakdown.isEmpty()) {
            String topCategory = categoryBreakdown.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");
            insights.add(topCategory + " is your top spending category");
        }

        insights.add("You had " + transactions.size() + " transactions");

        if (!transactions.isEmpty()) {
            BigDecimal avg = totalSpent.divide(
                    BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
            insights.add("Average transaction: ₹" + avg);
        }

        return insights;
    }
}