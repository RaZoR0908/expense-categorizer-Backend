package com.expense.expense_categorizer.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ParserUtils {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy"),
        DateTimeFormatter.ofPattern("d MMM yyyy"),
        DateTimeFormatter.ofPattern("MMM dd, yyyy"),
        DateTimeFormatter.ofPattern("MMM d, yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /** Try every known date format until one works */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /** Strip currency symbols and commas, return BigDecimal or null */
    public static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String clean = raw
                .replace("₹", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace(",", "")
                .trim();
            if (clean.isEmpty()) return null;
            BigDecimal val = new BigDecimal(clean);
            return val.compareTo(BigDecimal.ZERO) > 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Detect bank from raw text content.
     * Works for PDF raw text, Excel cell content, CSV header rows.
     */
    public static BankFormat detectBank(String content) {
        if (content == null) return BankFormat.GENERIC;
        String upper = content.toUpperCase();
        if (upper.contains("PUNJAB NATIONAL") || upper.contains("PNB"))    return BankFormat.PNB;
        if (upper.contains("HDFC BANK") || upper.contains("HDFCBANK"))     return BankFormat.HDFC;
        if (upper.contains("ICICI BANK") || upper.contains("ICICIBANKLTD"))return BankFormat.ICICI;
        if (upper.contains("STATE BANK") || upper.contains("SBI"))         return BankFormat.SBI;
        if (upper.contains("AXIS BANK"))                                    return BankFormat.AXIS;
        if (upper.contains("KOTAK"))                                        return BankFormat.KOTAK;
        return BankFormat.GENERIC;
    }

    /**
     * Clean UPI/NEFT/IMPS strings into readable merchant names.
     *
     * Examples:
     *   UPI/DR/827261891216/Google C/UTIB/googlecloud1.bd/ → "Google C"
     *   NEFT/ref/AMAZON SELLER/                            → "AMAZON SELLER"
     *   POS/SWIGGY*4829/YESB                               → "SWIGGY*4829"
     *   ATW/430006XXXXXX/HDFC ATM                          → "HDFC ATM"
     */
    public static String cleanMerchant(String raw) {
        if (raw == null || raw.isBlank()) return "";

        raw = raw.trim();
        String upper = raw.toUpperCase();

        // UPI: UPI/DR|CR/<refno>/<MerchantName>/<bank>/<vpa>/
        if (upper.startsWith("UPI/")) {
            String[] parts = raw.split("/");
            if (parts.length >= 4) return parts[3].trim();
            if (parts.length >= 2) return parts[parts.length - 1].trim();
        }

        // NEFT/IMPS/RTGS/NACH: TYPE/refno/MerchantName/...
        if (upper.matches("^(NEFT|IMPS|RTGS|NACH|ECS)/.*")) {
            String[] parts = raw.split("/");
            if (parts.length >= 3) return parts[2].trim();
        }

        // POS: POS/<MerchantName>/<bank>
        if (upper.startsWith("POS/")) {
            String[] parts = raw.split("/");
            if (parts.length >= 2) return parts[1].trim();
        }

        // ATM withdrawal
        if (upper.startsWith("ATW/") || upper.startsWith("ATM/")) {
            return "ATM Withdrawal";
        }

        // MMT/IRCTC etc — return first meaningful segment
        String[] parts = raw.split("[/\\-]");
        if (parts.length >= 2 && parts[0].length() <= 6) {
            return parts[1].trim();
        }

        // Fallback — trim to 60 chars
        return raw.length() > 60 ? raw.substring(0, 60).trim() : raw;
    }

    /** Returns true if the line is a header, footer, or summary line to skip */
    public static boolean isNoiseRow(String line) {
        if (line == null || line.isBlank()) return true;
        String lower = line.toLowerCase().trim();
        return lower.startsWith("date")
            || lower.startsWith("transaction")
            || lower.startsWith("narration")
            || lower.startsWith("opening balance")
            || lower.startsWith("closing balance")
            || lower.startsWith("statement")
            || lower.startsWith("account")
            || lower.startsWith("branch")
            || lower.startsWith("customer")
            || lower.startsWith("ifsc")
            || lower.startsWith("micr")
            || lower.startsWith("page ")
            || lower.startsWith("sr.")
            || lower.startsWith("s.no")
            || lower.startsWith("***")
            || lower.startsWith("unless")
            || lower.startsWith("computer generated")
            || lower.startsWith("------")
            || lower.startsWith("======")
            || lower.contains("generated through")
            || lower.matches(".*\\btotal\\b.*");
    }
}