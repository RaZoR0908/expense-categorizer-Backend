package com.expense.expense_categorizer.repository;

import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserAndCategory(User user, String category);
    List<Transaction> findByUser(User user);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.date BETWEEN :startDate AND :endDate")
    Double getTotalSpentInRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.date BETWEEN :startDate AND :endDate")
    Double getTotalByCategory(@Param("user") User user, @Param("category") String category, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}