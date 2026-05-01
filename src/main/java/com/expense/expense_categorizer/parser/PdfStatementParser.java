package com.expense.expense_categorizer.parser;

import com.expense.expense_categorizer.model.Transaction;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfStatementParser {

    private static final Pattern DATE_START = Pattern.compile(
        "^(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}" +
        "|\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4})",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DR_CR = Pattern.compile("\\b(DR|CR)\\b");

    private static final Pattern NUMBER = Pattern.compile(
        "\\b(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)\\b"
    );

    public List<Transaction> parse(MultipartFile file, String password) throws Exception {
        List<Transaction> transactions;

        try {
            byte[] fileBytes = file.getBytes();

            // Load with or without password
            PDDocument document = (password != null && !password.isBlank())
                ? Loader.loadPDF(fileBytes, password)
                : Loader.loadPDF(fileBytes);

            try (document) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String rawText = stripper.getText(document);

                BankFormat bank = ParserUtils.detectBank(rawText);
                transactions = parseText(rawText, bank);
            }
        } catch (Exception e) {
            throw new Exception("Failed to parse PDF: " + e.getMessage());
        }

        if (transactions.isEmpty()) {
            throw new Exception("No valid transactions found in PDF file");
        }

        return transactions;
    }

    // ── Core parser ───────────────────────────────────────────────────────────

    private List<Transaction> parseText(String text, BankFormat bank) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        String pendingLine = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isBlank()) continue;

            if (ParserUtils.isNoiseRow(line)) {
                if (pendingLine != null) {
                    Transaction txn = parseLine(pendingLine, bank);
                    if (txn != null) transactions.add(txn);
                    pendingLine = null;
                }
                continue;
            }

            if (DATE_START.matcher(line).find()) {
                if (pendingLine != null) {
                    Transaction txn = parseLine(pendingLine, bank);
                    if (txn != null) transactions.add(txn);
                }
                pendingLine = line;
            } else if (pendingLine != null) {
                pendingLine = pendingLine + " " + line;
            }
        }

        if (pendingLine != null) {
            Transaction txn = parseLine(pendingLine, bank);
            if (txn != null) transactions.add(txn);
        }

        return transactions;
    }

    private Transaction parseLine(String line, BankFormat bank) {
        return switch (bank) {
            case PNB, ICICI, KOTAK -> parseDrCrFormat(line);
            case HDFC, SBI, AXIS   -> parseSplitColumnFormat(line);
            default                -> tryBothFormats(line);
        };
    }

    private Transaction parseDrCrFormat(String line) {
        Matcher dateMatcher = DATE_START.matcher(line);
        if (!dateMatcher.find()) return null;
        LocalDate date = ParserUtils.parseDate(dateMatcher.group());
        if (date == null) return null;

        Matcher typeMatcher = DR_CR.matcher(line);
        if (!typeMatcher.find()) return null;
        String type = typeMatcher.group(1);

        if (type.equals("CR")) return null;

        String beforeType = line.substring(dateMatcher.end(), typeMatcher.start()).trim();
        List<BigDecimal> nums = extractNumbers(beforeType);
        if (nums.isEmpty()) return null;

        BigDecimal amount = nums.get(nums.size() - 1);

        String afterType = line.substring(typeMatcher.end()).trim();
        String merchant = extractMerchantAfterBalance(afterType);
        merchant = ParserUtils.cleanMerchant(merchant);
        if (merchant.isBlank()) return null;

        return buildTransaction(date, amount, merchant);
    }

    private Transaction parseSplitColumnFormat(String line) {
        Matcher dateMatcher = DATE_START.matcher(line);
        if (!dateMatcher.find()) return null;
        LocalDate date = ParserUtils.parseDate(dateMatcher.group());
        if (date == null) return null;

        String remainder = line.substring(dateMatcher.end()).trim();
        List<BigDecimal> nums = extractNumbers(remainder);
        if (nums.size() < 2) return null;

        BigDecimal amount = nums.get(nums.size() - 2);
        BigDecimal balance = nums.get(nums.size() - 1);

        if (amount.compareTo(balance.multiply(BigDecimal.TEN)) > 0) {
            if (nums.size() >= 3) {
                amount = nums.get(nums.size() - 3);
            } else {
                return null;
            }
        }

        String merchant = extractTextPortion(remainder);
        merchant = ParserUtils.cleanMerchant(merchant);
        if (merchant.isBlank()) return null;

        return buildTransaction(date, amount, merchant);
    }

    private Transaction tryBothFormats(String line) {
        Transaction txn = parseDrCrFormat(line);
        if (txn != null) return txn;
        return parseSplitColumnFormat(line);
    }

    private Transaction buildTransaction(LocalDate date, BigDecimal amount, String merchant) {
        if (date == null || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || merchant.isBlank()) return null;

        Transaction txn = new Transaction();
        txn.setDate(date);
        txn.setMerchant(merchant);
        txn.setAmount(amount.abs());
        txn.setCategory("Uncategorized");
        txn.setConfidenceScore(0.0);
        txn.setIsCorrected(false);
        return txn;
    }

    private List<BigDecimal> extractNumbers(String text) {
        List<BigDecimal> result = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            try {
                String clean = m.group(1).replace(",", "");
                BigDecimal val = new BigDecimal(clean);
                if (val.compareTo(BigDecimal.ONE) >= 0) result.add(val);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private String extractMerchantAfterBalance(String text) {
        Matcher m = NUMBER.matcher(text);
        if (m.find()) {
            String after = text.substring(m.end()).trim();
            return after.isBlank() ? text.trim() : after;
        }
        return text.trim();
    }

    private String extractTextPortion(String text) {
        String cleaned = text
            .replaceAll("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}", " ")
            .replaceAll("\\b\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?\\b", " ")
            .replaceAll("[|]{2,}", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();
        cleaned = cleaned.replaceAll("^[^a-zA-Z0-9₹/]+", "").trim();
        return cleaned;
    }
}