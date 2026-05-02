package com.expense.expense_categorizer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@finailytics.app");
        message.setTo(to);
        message.setSubject("Reset Your Finalytics Password");
        message.setText(
            "Hi,\n\n" +
            "You requested to reset your password.\n\n" +
            "Click the link below:\n" +
            resetLink + "\n\n" +
            "This link expires in 15 minutes.\n\n" +
            "If you didn't request this, ignore this email.\n\n" +
            "- Finalytics"
        );
        mailSender.send(message);
    }
}