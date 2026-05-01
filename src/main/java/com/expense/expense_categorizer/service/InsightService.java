package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.controller.InsightController.ComparisonResponseDTO;
import com.expense.expense_categorizer.dto.InsightResponseDTO;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InsightService {

    @Autowired
    private TransactionRepository transactionRepository;

    public InsightResponseDTO getMonthlyInsights(User user, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetween(user, startDate, endDate);

        return calculateInsights(transactions);
    }

    public InsightResponseDTO getYearlyInsights(User user, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetween(user, startDate, endDate);

        return calculateInsights(transactions);
    }

    public ComparisonResponseDTO compareMonths(User user, YearMonth current, YearMonth previous) {
        // Get current month total
        Double currentTotal = transactionRepository.getTotalSpentInRange(
                user,
                current.atDay(1),
                current.atEndOfMonth()
        );

        // Get previous month total
        Double previousTotal = transactionRepository.getTotalSpentInRange(
                user,
                previous.atDay(1),
                previous.atEndOfMonth()
        );

        BigDecimal currentBD = BigDecimal.valueOf(currentTotal != null ? currentTotal : 0);
        BigDecimal previousBD = BigDecimal.valueOf(previousTotal != null ? previousTotal : 0);

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
            String category = txn.getCategory() != null ? txn.getCategory() : "Uncategorized";
            categoryBreakdown.put(
                    category,
                    categoryBreakdown.getOrDefault(category, BigDecimal.ZERO).add(txn.getAmount())
            );
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
                percentageOfTop = (topAmount.doubleValue() / totalSpent.doubleValue()) * 100;
            }
        }

        List<String> aiInsights = generateInsights(transactions, categoryBreakdown, totalSpent);

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
                                         Map<String, BigDecimal> categoryBreakdown,
                                         BigDecimal totalSpent) {
        List<String> insights = new ArrayList<>();

        // Insight 1: Total spending
        insights.add("You spent ₹" + totalSpent.toString() + " this period");

        // Insight 2: Top category
        if (!categoryBreakdown.isEmpty()) {
            String topCategory = categoryBreakdown.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");
            insights.add(topCategory + " is your top spending category");
        }

        // Insight 3: Transaction count
        insights.add("You had " + transactions.size() + " transactions");

        // Insight 4: Average transaction
        if (!transactions.isEmpty()) {
            BigDecimal avgTransaction = totalSpent.divide(BigDecimal.valueOf(transactions.size()), 2, java.math.RoundingMode.HALF_UP);
            insights.add("Average transaction: ₹" + avgTransaction.toString());
        }

        return insights;
    }
}