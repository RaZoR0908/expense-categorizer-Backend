package com.expense.expense_categorizer.repository;

import com.expense.expense_categorizer.model.ChatMessage;
import com.expense.expense_categorizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserOrderByCreatedAtDesc(User user);
    List<ChatMessage> findByUser(User user);
}