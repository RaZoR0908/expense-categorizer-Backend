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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiCategorizationService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String[] CATEGORIES = {
        "Food & Dining", "Travel", "Shopping", "Utilities", "Healthcare",
        "Entertainment", "Finance & Banking", "Subscriptions", "Education", "Others"
    };

    public void categorizeTransaction(Transaction transaction) {
        try {
            String prompt = buildPrompt(transaction);
            String response = callGroqAPI(prompt);
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
        prompt.append("Amount: Rs.").append(transaction.getAmount()).append("\n");
        prompt.append("Merchant/Description: ").append(transaction.getMerchant()).append("\n\n");
        prompt.append("Respond in this EXACT format (only 3 lines):\n");
        prompt.append("CATEGORY: [exact category name from the list]\n");
        prompt.append("CONFIDENCE: [0.0 to 1.0]\n");
        prompt.append("REASON: [one line reason]\n\n");
        prompt.append("Do NOT include any other text.");
        return prompt.toString();
    }

    private String callGroqAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "llama-3.3-70b-versatile");
        requestMap.put("messages", List.of(message));
        requestMap.put("temperature", 0.3);
        requestMap.put("max_tokens", 100);

        String requestBody = mapper.writeValueAsString(requestMap);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Full error: " + e.getMessage());
            throw e;
        }
    }

    private void parseAndSetCategory(Transaction transaction, String apiResponse) {
        try {
            JsonNode root = mapper.readTree(apiResponse);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            String category = "Others";
            Double confidence = 0.5;

            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("CATEGORY:")) {
                    category = line.replace("CATEGORY:", "").trim();
                } else if (line.startsWith("CONFIDENCE:")) {
                    try {
                        confidence = Double.parseDouble(line.replace("CONFIDENCE:", "").trim());
                        confidence = Math.max(0.0, Math.min(1.0, confidence));
                    } catch (Exception e) {
                        confidence = 0.5;
                    }
                }
            }

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