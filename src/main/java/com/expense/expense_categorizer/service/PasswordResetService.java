package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenExpiry = new ConcurrentHashMap<>();

    public void initiateReset(String email) {
        userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, email);
        tokenExpiry.put(token, System.currentTimeMillis() + 15 * 60 * 1000);

        String resetLink = "https://finailytics.app/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    public void resetPassword(String token, String newPassword) {
        String email = tokenStore.get(token);
        if (email == null) throw new RuntimeException("Invalid or expired token");

        Long expiry = tokenExpiry.get(token);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            tokenStore.remove(token);
            tokenExpiry.remove(token);
            throw new RuntimeException("Token has expired. Please request a new reset link.");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenStore.remove(token);
        tokenExpiry.remove(token);
    }
}