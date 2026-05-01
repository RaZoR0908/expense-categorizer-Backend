package com.expense.expense_categorizer.controller;

import com.expense.expense_categorizer.dto.TransactionDTO;
import com.expense.expense_categorizer.dto.TransactionCorrectionDTO;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.TransactionRepository;
import com.expense.expense_categorizer.repository.UserRepository;
import com.expense.expense_categorizer.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAllTransactions(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            List<Transaction> transactions = transactionRepository.findByUser(foundUser);
            List<TransactionDTO> dtos = transactions.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized"));
        }
    }

    @GetMapping("/month/{yearMonth}")
    public ResponseEntity<?> getTransactionsByMonth(
            @PathVariable String yearMonth,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            YearMonth ym = YearMonth.parse(yearMonth);
            LocalDate startDate = ym.atDay(1);
            LocalDate endDate = ym.atEndOfMonth();

            User foundUser = user.get();
            List<Transaction> transactions = transactionRepository
                    .findByUserAndDateBetween(foundUser, startDate, endDate);
            List<TransactionDTO> dtos = transactions.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getTransactionsByCategory(
            @PathVariable String category,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            List<Transaction> transactions = transactionRepository
                    .findByUserAndCategory(foundUser, category);
            List<TransactionDTO> dtos = transactions.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized"));
        }
    }

    @PutMapping("/{id}/correct")
    public ResponseEntity<?> correctTransaction(
            @PathVariable Long id,
            @RequestBody TransactionCorrectionDTO correction,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            var transaction = transactionRepository.findById(id);
            if (transaction.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Transaction not found"));
            }

            Transaction txn = transaction.get();
            txn.setCategory(correction.getNewCategory());
            txn.setIsCorrected(true);

            Transaction updated = transactionRepository.save(txn);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error updating transaction: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            var transaction = transactionRepository.findById(id);
            if (transaction.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Transaction not found"));
            }

            return ResponseEntity.ok(convertToDTO(transaction.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized"));
        }
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getDate(),
                transaction.getAmount(),
                transaction.getMerchant(),
                transaction.getCategory(),
                transaction.getConfidenceScore(),
                transaction.getIsCorrected()
        );
    }

    public static class ErrorResponse {
        public String message;
        public long timestamp;

        public ErrorResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}