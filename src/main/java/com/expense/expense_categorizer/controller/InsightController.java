package com.expense.expense_categorizer.controller;

import com.expense.expense_categorizer.dto.InsightResponseDTO;
import com.expense.expense_categorizer.model.User;
import com.expense.expense_categorizer.repository.UserRepository;
import com.expense.expense_categorizer.security.JwtUtil;
import com.expense.expense_categorizer.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class InsightController {

    @Autowired
    private InsightService insightService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyInsights(
            @RequestParam(required = false) String month,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            YearMonth yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();

            InsightResponseDTO insights = insightService.getMonthlyInsights(foundUser, yearMonth);
            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching insights: " + e.getMessage()));
        }
    }

    @GetMapping("/yearly")
    public ResponseEntity<?> getYearlyInsights(
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            int targetYear = year != null ? year : java.time.Year.now().getValue();

            InsightResponseDTO insights = insightService.getYearlyInsights(foundUser, targetYear);
            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching yearly insights: " + e.getMessage()));
        }
    }

    @GetMapping("/comparison")
    public ResponseEntity<?> getMonthComparison(
            @RequestParam String currentMonth,
            @RequestParam String previousMonth,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            var user = userRepository.findByEmail(email);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User foundUser = user.get();
            YearMonth current = YearMonth.parse(currentMonth);
            YearMonth previous = YearMonth.parse(previousMonth);

            ComparisonResponseDTO comparison = insightService.compareMonths(foundUser, current, previous);
            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error comparing months: " + e.getMessage()));
        }
    }

    // Comparison Response DTO
    public static class ComparisonResponseDTO {
        public java.math.BigDecimal currentMonthSpent;
        public java.math.BigDecimal previousMonthSpent;
        public java.math.BigDecimal difference;
        public Double percentageChange;
        public String trend; // "UP", "DOWN", "NEUTRAL"

        public ComparisonResponseDTO(java.math.BigDecimal current, java.math.BigDecimal previous) {
            this.currentMonthSpent = current;
            this.previousMonthSpent = previous;
            this.difference = current.subtract(previous);
            
            if (previous.signum() != 0) {
                this.percentageChange = (this.difference.doubleValue() / previous.doubleValue()) * 100;
            } else {
                this.percentageChange = 0.0;
            }
            
            if (this.difference.signum() > 0) {
                this.trend = "UP";
            } else if (this.difference.signum() < 0) {
                this.trend = "DOWN";
            } else {
                this.trend = "NEUTRAL";
            }
        }

        public java.math.BigDecimal getCurrentMonthSpent() { return currentMonthSpent; }
        public java.math.BigDecimal getPreviousMonthSpent() { return previousMonthSpent; }
        public java.math.BigDecimal getDifference() { return difference; }
        public Double getPercentageChange() { return percentageChange; }
        public String getTrend() { return trend; }
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