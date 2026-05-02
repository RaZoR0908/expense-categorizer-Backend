package com.expense.expense_categorizer.controller;

import com.expense.expense_categorizer.dto.AuthResponseDTO;
import com.expense.expense_categorizer.dto.LoginRequestDTO;
import com.expense.expense_categorizer.dto.RegisterRequestDTO;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.UserRepository;
import com.expense.expense_categorizer.security.JwtUtil;
import com.expense.expense_categorizer.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("Email already registered"));
            }
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullName(request.getFullName());
            User savedUser = userRepository.save(user);
            String token = jwtUtil.generateToken(savedUser.getEmail());
            AuthResponseDTO response = new AuthResponseDTO(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFullName(),
                    token
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            var user = userRepository.findByEmail(request.getEmail());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }
            User foundUser = user.get();
            if (!passwordEncoder.matches(request.getPassword(), foundUser.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }
            String token = jwtUtil.generateToken(foundUser.getEmail());
            AuthResponseDTO response = new AuthResponseDTO(
                    foundUser.getId(),
                    foundUser.getEmail(),
                    foundUser.getFullName(),
                    token
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }
            User foundUser = user.get();
            AuthResponseDTO response = new AuthResponseDTO(
                    foundUser.getId(),
                    foundUser.getEmail(),
                    foundUser.getFullName(),
                    token
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            passwordResetService.initiateReset(body.get("email"));
            return ResponseEntity.ok(Map.of("message", "Reset email sent"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            passwordResetService.resetPassword(body.get("token"), body.get("password"));
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
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