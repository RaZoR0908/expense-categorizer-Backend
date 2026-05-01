package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.dto.ChatResponseDTO;
import com.expense.expense_categorizer.model.ChatMessage;
import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.BudgetRepository;
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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetService budgetService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatResponseDTO processMessage(User user, String userMessage) {
        try {
            // Save user message
            ChatMessage userChatMessage = new ChatMessage();
            userChatMessage.setUser(user);
            userChatMessage.setRole("user");
            userChatMessage.setContent(userMessage);
            chatMessageRepository.save(userChatMessage);

            // Build context and prompt
            String context = buildTransactionContext(user);
            String prompt = buildChatPrompt(userMessage, context);
            String aiResponse = callGroqAPI(prompt);

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
                .map(msg -> new ChatResponseDTO(
                        msg.getId(),
                        msg.getRole(),
                        msg.getContent(),
                        msg.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void clearChatHistory(User user) {
        chatMessageRepository.deleteAll(chatMessageRepository.findByUser(user));
    }

    private String buildTransactionContext(User user) {
        // ── Transactions ──────────────────────────────────────────────────────
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
                        .append(" | Rs.").append(txn.getAmount())
                        .append(" | Category: ").append(txn.getCategory())
                        .append("\n");
            }
        }

        // ── Budget context ────────────────────────────────────────────────────
        context.append("\nUser's Monthly Budget Limits vs Actual Spending:\n");
        try {
            BudgetService.BudgetSummaryDTO budgetStatus =
                    budgetService.getBudgetStatus(user, YearMonth.now());

            if (budgetStatus.budgets.isEmpty()) {
                context.append("No budgets set yet.\n");
            } else {
                for (BudgetService.BudgetStatusDTO b : budgetStatus.budgets) {
                    context.append("- ").append(b.category)
                            .append(" | Limit: Rs.").append(b.monthlyLimit)
                            .append(" | Spent: Rs.").append(b.actualSpent)
                            .append(" | Remaining: Rs.").append(b.remaining)
                            .append(" | Status: ").append(b.status)
                            .append("\n");
                }
                context.append("Total Budgeted: Rs.").append(budgetStatus.totalBudgeted)
                        .append(" | Total Spent: Rs.").append(budgetStatus.totalSpent)
                        .append(" | Total Remaining: Rs.")
                        .append(budgetStatus.totalRemaining).append("\n");
            }
        } catch (Exception e) {
            context.append("Budget data unavailable.\n");
        }

        return context.toString();
    }

    private String buildChatPrompt(String userMessage, String context) {
        return "You are a helpful personal finance assistant named FinanceBot.\n"
                + "You have access to the user's transaction history and budget limits.\n"
                + "Always give specific numbers and actionable advice.\n\n"
                + context + "\n"
                + "User Question: " + userMessage + "\n\n"
                + "Provide a helpful, conversational response. Be concise but informative.";
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
        requestMap.put("temperature", 0.7);
        requestMap.put("max_tokens", 500);

        String requestBody = mapper.writeValueAsString(requestMap);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                GROQ_URL, request, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        return root.path("choices").path(0).path("message").path("content").asText();
    }
}