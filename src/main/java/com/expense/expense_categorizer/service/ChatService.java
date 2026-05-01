package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.dto.ChatResponseDTO;
import com.expense.expense_categorizer.model.ChatMessage;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.ChatMessageRepository;
import com.expense.expense_categorizer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public ChatResponseDTO processMessage(User user, String userMessage) {
        try {
            // Save user message
            ChatMessage userChatMessage = new ChatMessage();
            userChatMessage.setUser(user);
            userChatMessage.setRole("user");
            userChatMessage.setContent(userMessage);
            chatMessageRepository.save(userChatMessage);

            // Build context from recent transactions
            String context = buildTransactionContext(user);

            // Build prompt with context
            String prompt = buildChatPrompt(userMessage, context);

            // Call Gemini API
            String aiResponse = callGeminiAPI(prompt);

            // Save AI response
            ChatMessage aiChatMessage = new ChatMessage();
            aiChatMessage.setUser(user);
            aiChatMessage.setRole("assistant");
            aiChatMessage.setContent(aiResponse);
            chatMessageRepository.save(aiChatMessage);

            return new ChatResponseDTO(
                    aiChatMessage.getId(),
                    "assistant",
                    aiResponse,
                    aiChatMessage.getCreatedAt()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error processing chat message: " + e.getMessage());
        }
    }

    public List<ChatResponseDTO> getChatHistory(User user) {
        List<ChatMessage> messages = chatMessageRepository.findByUser(user);
        return messages.stream()
                .map(msg -> new ChatResponseDTO(msg.getId(), msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void clearChatHistory(User user) {
        List<ChatMessage> messages = chatMessageRepository.findByUser(user);
        chatMessageRepository.deleteAll(messages);
    }

    private String buildTransactionContext(User user) {
        // Get last 30 days of transactions
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetween(user, startDate, endDate);

        StringBuilder context = new StringBuilder();
        context.append("User's Recent Spending (Last 30 days):\n");

        if (transactions.isEmpty()) {
            context.append("No transactions yet.\n");
        } else {
            for (Transaction txn : transactions) {
                context.append("- ").append(txn.getDate())
                        .append(" | ").append(txn.getMerchant())
                        .append(" | ₹").append(txn.getAmount())
                        .append(" | Category: ").append(txn.getCategory()).append("\n");
            }
        }

        return context.toString();
    }

    private String buildChatPrompt(String userMessage, String context) {
        return "You are a helpful personal finance assistant named FinanceBot. You help users understand their spending patterns and provide financial advice.\n\n"
                + context + "\n"
                + "User Question: " + userMessage + "\n\n"
                + "Provide a helpful, conversational response. Be concise but informative.";
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

        // Parse response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        
        String aiResponse = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        return aiResponse;
    }

    private String buildGeminiRequest(String prompt) throws Exception {
        return "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [{\n" +
                "      \"text\": \"" + escapeJson(prompt) + "\"\n" +
                "    }]\n" +
                "  }],\n" +
                "  \"generationConfig\": {\n" +
                "    \"temperature\": 0.7,\n" +
                "    \"maxOutputTokens\": 500,\n" +
                "    \"topP\": 0.8\n" +
                "  }\n" +
                "}";
    }

    private String escapeJson(String str) {
        return str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}