package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.controller.StatementController.UploadResponseDTO;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class StatementService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ParserService parserService;
    @Autowired
    private AiCategorizationService aiCategorizationService;

    public UploadResponseDTO processStatement(MultipartFile file, String password, User user) {
        try {
            // Step 1: Parse the file
            List<Transaction> parsedTransactions = parserService.parseFile(file, password);

            // Step 2: Categorize each transaction
            int categorizedCount = 0;
            for (Transaction txn : parsedTransactions) {
                txn.setUser(user);
                try {
                    aiCategorizationService.categorizeTransaction(txn);
                    categorizedCount++;
                } catch (Exception e) {
                    txn.setCategory("Uncategorized");
                    txn.setConfidenceScore(0.0);
                }
            }

            // Step 3: Save all transactions
            transactionRepository.saveAll(parsedTransactions);

            return new UploadResponseDTO(
                    user.getId(),
                    parsedTransactions.size(),
                    categorizedCount,
                    "Successfully processed " + parsedTransactions.size() + " transactions"
            );
        } catch (Exception e) {
            throw new RuntimeException("Error processing statement: " + e.getMessage());
        }
    }
}