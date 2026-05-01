package com.expense.expense_categorizer.parser;

import com.expense.expense_categorizer.model.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvStatementParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy")
    );

    public List<Transaction> parse(MultipartFile file) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (columns.length < 3) continue;

                try {
                    Transaction txn = new Transaction();

                    // Column 0: Date
                    txn.setDate(parseDate(columns[0].trim().replace("\"", "")));

                    // Column 1: Merchant/Description
                    txn.setMerchant(columns[1].trim().replace("\"", ""));

                    // Column 2: Amount
                    String amountStr = columns[2].trim()
                        .replace("\"", "")
                        .replace(",", "")
                        .replace("₹", "")
                        .replace("Rs.", "")
                        .replace("Rs", "")
                        .trim();

                    txn.setAmount(new BigDecimal(amountStr));
                    txn.setCategory("Uncategorized");
                    txn.setConfidenceScore(0.0);
                    txn.setIsCorrected(false);

                    transactions.add(txn);

                } catch (Exception e) {
                    // Skip malformed rows, continue parsing rest
                    System.out.println("Skipping malformed row: " + line + " | Error: " + e.getMessage());
                }
            }
        }

        if (transactions.isEmpty()) {
            throw new Exception("No valid transactions found in CSV file");
        }

        return transactions;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Cannot parse date: " + dateStr);
    }
}