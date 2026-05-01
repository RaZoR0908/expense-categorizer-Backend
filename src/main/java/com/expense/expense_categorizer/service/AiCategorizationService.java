package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

@Service
public class AiCategorizationService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String[] CATEGORIES = {
        "Food & Dining", "Travel", "Shopping", "Utilities", "Healthcare",
        "Entertainment", "Finance & Banking", "Subscriptions", "Education", "Others"
    };

    public void categorizeTransaction(Transaction transaction) {
        try {
            String prompt = buildPrompt(transaction);
            String response = callGeminiAPI(prompt);
            parseAndSetCategory(transaction, response);
        } catch (Exception e) {
            transaction.setCategory("Others");
            transaction.setConfidenceScore(0.0);
            System.err.println("Categorization error: " + e.getMessage());
        }
    }

    private String buildPrompt(Transaction transaction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial assistant. Classify this transaction into EXACTLY ONE category.\n\n");
        prompt.append("Available Categories: ").append(String.join(", ", CATEGORIES)).append("\n\n");
        prompt.append("Transaction Details:\n");
        prompt.append("Date: ").append(transaction.getDate()).append("\n");
        prompt.append("Amount: ₹").append(transaction.getAmount()).append("\n");
        prompt.append("Merchant/Description: ").append(transaction.getMerchant()).append("\n\n");
        prompt.append("Respond in this EXACT format (only 3 lines):\n");
        prompt.append("CATEGORY: [exact category name from the list]\n");
        prompt.append("CONFIDENCE: [0.0 to 1.0]\n");
        prompt.append("REASON: [one line reason]\n\n");
        prompt.append("Do NOT include any other text.");

        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build request body for Gemini API
        String requestBody = buildGeminiRequest(prompt);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Make API call with API key in URL
        String urlWithKey = geminiApiUrl + "?key=" + geminiApiKey;
        ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, request, String.class);

        return response.getBody();
    }

    private String buildGeminiRequest(String prompt) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        String jsonRequest = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [{\n" +
                "      \"text\": \"" + escapeJson(prompt) + "\"\n" +
                "    }]\n" +
                "  }],\n" +
                "  \"generationConfig\": {\n" +
                "    \"temperature\": 0.3,\n" +
                "    \"maxOutputTokens\": 100,\n" +
                "    \"topP\": 0.8\n" +
                "  }\n" +
                "}";

        return jsonRequest;
    }

    private String escapeJson(String str) {
        return str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void parseAndSetCategory(Transaction transaction, String apiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(apiResponse);

            // Navigate to the text content
            String content = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            // Parse response
            String category = "Others";
            Double confidence = 0.5;

            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("CATEGORY:")) {
                    category = line.replace("CATEGORY:", "").trim();
                } else if (line.startsWith("CONFIDENCE:")) {
                    try {
                        String confStr = line.replace("CONFIDENCE:", "").trim();
                        confidence = Double.parseDouble(confStr);
                        // Clamp between 0 and 1
                        confidence = Math.max(0.0, Math.min(1.0, confidence));
                    } catch (Exception e) {
                        confidence = 0.5;
                    }
                }
            }

            // Validate category
            boolean validCategory = false;
            for (String cat : CATEGORIES) {
                if (cat.equalsIgnoreCase(category)) {
                    category = cat;
                    validCategory = true;
                    break;
                }
            }
            if (!validCategory) {
                category = "Others";
                confidence = 0.3;
            }

            transaction.setCategory(category);
            transaction.setConfidenceScore(confidence);
            transaction.setIsCorrected(false);

        } catch (Exception e) {
            transaction.setCategory("Others");
            transaction.setConfidenceScore(0.0);
            System.err.println("Parse error: " + e.getMessage());
        }
    }
}