package com.expense.expense_categorizer.controller;

import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.UserRepository;
import com.expense.expense_categorizer.service.StatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/statements")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StatementController {

    @Autowired
    private StatementService statementService;
    @Autowired
    private UserRepository userRepository;

    // ✅ REMOVED: JwtUtil (no longer needed here)

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            Principal principal) { // ✅ CHANGED: from @RequestHeader token to Principal

        try {
            // ✅ CHANGED: get email directly from Spring Security context
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Not authenticated"));
            }

            String email = principal.getName();
            var user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("File is empty"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".pdf") && !fileName.endsWith(".csv") &&
                !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Only PDF, CSV, and Excel files are supported"));
            }

            UploadResponseDTO response = statementService.processStatement(file, password, foundUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("File upload failed: " + e.getMessage()));
        }
    }

    public static class UploadResponseDTO {
        public Long userId;
        public int transactionsProcessed;
        public int transactionsCategorized;
        public String message;
        public long timestamp;

        public UploadResponseDTO(Long userId, int processed, int categorized, String message) {
            this.userId = userId;
            this.transactionsProcessed = processed;
            this.transactionsCategorized = categorized;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public Long getUserId() { return userId; }
        public int getTransactionsProcessed() { return transactionsProcessed; }
        public int getTransactionsCategorized() { return transactionsCategorized; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
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