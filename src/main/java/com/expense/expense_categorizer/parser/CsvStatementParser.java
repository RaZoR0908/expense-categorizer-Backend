package com.expense.expense_categorizer.parser;

import com.expense.expense_categorizer.model.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvStatementParser {

    public List<Transaction> parse(MultipartFile file) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String headerLine = null;
            String line;

            // Skip blank/metadata rows until we find the header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isBlank()) continue;
                String lower = line.toLowerCase();
                if (lower.contains("date") && (lower.contains("narration")
                        || lower.contains("description") || lower.contains("amount")
                        || lower.contains("debit") || lower.contains("withdrawal"))) {
                    headerLine = line;
                    break;
                }
            }

            if (headerLine == null) {
                throw new Exception("Could not find header row in CSV file");
            }

            // Detect bank from header
            BankFormat bank = ParserUtils.detectBank(headerLine);

            // Map columns from header
            String[] headers = splitCsv(headerLine);
            CsvColumnMap cols = mapColumns(headers);

            if (cols.dateCol == -1 || cols.descCol == -1) {
                throw new Exception("CSV missing required columns (date, description)");
            }

            // Parse data rows
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isBlank()) continue;
                if (ParserUtils.isNoiseRow(line)) continue;

                String[] columns = splitCsv(line);
                if (columns.length <= Math.max(cols.dateCol, cols.descCol)) continue;

                try {
                    LocalDate date = ParserUtils.parseDate(
                        clean(columns[cols.dateCol]));
                    if (date == null) continue;

                    String merchant = clean(columns[cols.descCol]);
                    if (merchant.isBlank()) continue;

                    BigDecimal amount = resolveDebitAmount(columns, cols, bank);
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                    merchant = ParserUtils.cleanMerchant(merchant);

                    Transaction txn = new Transaction();
                    txn.setDate(date);
                    txn.setMerchant(merchant);
                    txn.setAmount(amount);
                    txn.setCategory("Uncategorized");
                    txn.setConfidenceScore(0.0);
                    txn.setIsCorrected(false);
                    transactions.add(txn);

                } catch (Exception e) {
                    System.out.println("Skipping CSV row: " + line + " | " + e.getMessage());
                }
            }
        }

        if (transactions.isEmpty()) {
            throw new Exception("No valid transactions found in CSV file");
        }

        return transactions;
    }

    // ── Column mapping ────────────────────────────────────────────────────────

    private static class CsvColumnMap {
        int dateCol   = -1;
        int descCol   = -1;
        int amountCol = -1;
        int debitCol  = -1;
        @SuppressWarnings("unused")
		int creditCol = -1;
        int typeCol   = -1;
    }

    private CsvColumnMap mapColumns(String[] headers) {
        CsvColumnMap map = new CsvColumnMap();
        for (int i = 0; i < headers.length; i++) {
            String h = clean(headers[i]).toLowerCase();
            if (h.contains("date") && !h.contains("value"))       map.dateCol   = i;
            else if (h.contains("narration") || h.contains("description")
                  || h.contains("particulars") || h.contains("remarks")
                  || h.contains("merchant"))                       map.descCol   = i;
            else if (h.equals("amount(inr)") || h.equals("amount")
                  || h.contains("transaction amount"))             map.amountCol = i;
            else if (h.contains("withdrawal") || h.contains("debit")
                  || h.equals("dr"))                              map.debitCol  = i;
            else if (h.contains("deposit") || h.contains("credit")
                  || h.equals("cr"))                              map.creditCol = i;
            else if (h.equals("type") || h.equals("dr/cr")
                  || h.equals("txn type"))                        map.typeCol   = i;
        }
        return map;
    }

    private BigDecimal resolveDebitAmount(String[] columns, CsvColumnMap cols, BankFormat bank) {
        // DR/CR type column present
        if (cols.typeCol != -1 && cols.typeCol < columns.length
                && cols.amountCol != -1 && cols.amountCol < columns.length) {
            String type = clean(columns[cols.typeCol]).toUpperCase();
            if (type.equals("CR")) return null;
            return ParserUtils.parseAmount(clean(columns[cols.amountCol]));
        }

        // Single amount column
        if (cols.amountCol != -1 && cols.amountCol < columns.length
                && cols.debitCol == -1) {
            return ParserUtils.parseAmount(clean(columns[cols.amountCol]));
        }

        // Split debit/credit — only read debit
        if (cols.debitCol != -1 && cols.debitCol < columns.length) {
            BigDecimal debit = ParserUtils.parseAmount(clean(columns[cols.debitCol]));
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) return debit;
        }

        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** RFC-4180 compliant CSV split — handles quoted fields with commas inside */
    private String[] splitCsv(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    /** Strip quotes and whitespace from a cell value */
    private String clean(String raw) {
        if (raw == null) return "";
        return raw.trim().replace("\"", "").trim();
    }
}