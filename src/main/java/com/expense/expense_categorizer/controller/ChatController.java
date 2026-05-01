package com.expense.expense_categorizer.controller;

import com.expense.expense_categorizer.dto.ChatRequestDTO;
import com.expense.expense_categorizer.dto.ChatResponseDTO;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.UserRepository;
import com.expense.expense_categorizer.security.JwtUtil;
import com.expense.expense_categorizer.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatRequestDTO request,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Message cannot be empty"));
            }

            User foundUser = user.get();
            ChatResponseDTO response = chatService.processMessage(foundUser, request.getMessage());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error processing message: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            List<ChatResponseDTO> history = chatService.getChatHistory(foundUser);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching chat history: " + e.getMessage()));
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearChatHistory(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            chatService.clearChatHistory(foundUser);
            return ResponseEntity.ok(new SuccessResponse("Chat history cleared"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error clearing chat history: " + e.getMessage()));
        }
    }

    public static class SuccessResponse {
        public String message;
        public long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

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