package com.expense.expense_categorizer.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    public void sendPasswordResetEmail(String to, String resetLink) {
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from("onboarding@resend.dev")
            .to(to)
            .subject("Reset Your Finailytics Password")
            .html(
                "<h2>Reset Your Password</h2>" +
                "<p>Click the link below to reset your password:</p>" +
                "<a href='" + resetLink + "'>Reset Password</a>" +
                "<p>This link expires in 15 minutes.</p>" +
                "<p>If you didn't request this, ignore this email.</p>" +
                "<p>- Finailytics</p>"
            )
            .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}