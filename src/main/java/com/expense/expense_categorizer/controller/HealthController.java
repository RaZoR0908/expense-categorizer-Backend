package com.expense.expense_categorizer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "message", "Expense Categorizer API is running"
        );
    }
}