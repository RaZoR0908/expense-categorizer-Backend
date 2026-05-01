package com.expense.expense_categorizer.parser;

import com.expense.expense_categorizer.model.Transaction;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ExcelStatementParser {

    public List<Transaction> parse(MultipartFile file, String password) throws Exception {
        String fileName = file.getOriginalFilename();
        boolean isXlsx = fileName != null && fileName.endsWith(".xlsx");

        Workbook workbook;
        try {
            byte[] fileBytes = file.getBytes();

            if (password != null && !password.isBlank()) {
                workbook = openWithPassword(fileBytes, password, isXlsx);
            } else {
                workbook = isXlsx
                    ? new XSSFWorkbook(new ByteArrayInputStream(fileBytes))
                    : new HSSFWorkbook(new ByteArrayInputStream(fileBytes));
            }
        } catch (Exception e) {
            throw new Exception("Failed to open Excel file: " + e.getMessage());
        }

        BankFormat bank = detectBankFromWorkbook(workbook);

        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            workbook.close();
            throw new Exception("Excel file has no sheets");
        }

        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex == -1) {
            workbook.close();
            throw new Exception("Could not find header row in Excel file");
        }

        ColumnMap cols = mapColumns(sheet.getRow(headerRowIndex));
        if (cols.dateCol == -1 || cols.descCol == -1) {
            workbook.close();
            throw new Exception("Excel missing required columns (date, description)");
        }

        List<Transaction> transactions = new ArrayList<>();

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                LocalDate date = parseDateCell(row.getCell(cols.dateCol));
                if (date == null) continue;

                String merchant = getCellString(row.getCell(cols.descCol)).trim();
                if (merchant.isBlank()) continue;

                BigDecimal amount = resolveDebitAmount(row, cols, bank);
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
                System.out.println("Skipping Excel row " + i + ": " + e.getMessage());
            }
        }

        workbook.close();

        if (transactions.isEmpty()) {
            throw new Exception("No valid transactions found in Excel file");
        }

        return transactions;
    }

    // ── Password handling ─────────────────────────────────────────────────────

    private Workbook openWithPassword(byte[] fileBytes, String password, boolean isXlsx)
            throws Exception {
        if (isXlsx) {
            // XLSX password decryption via POI EncryptionInfo
            POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(fileBytes));
            EncryptionInfo info = new EncryptionInfo(fs);
            Decryptor decryptor = Decryptor.getInstance(info);
            if (!decryptor.verifyPassword(password)) {
                throw new Exception("Incorrect password for Excel file");
            }
            return new XSSFWorkbook(decryptor.getDataStream(fs));
        } else {
            // XLS password via Biff8EncryptionKey
            Biff8EncryptionKey.setCurrentUserPassword(password);
            try {
                return new HSSFWorkbook(new ByteArrayInputStream(fileBytes));
            } finally {
                Biff8EncryptionKey.setCurrentUserPassword(null); // always clear
            }
        }
    }

    // ── Column mapping ────────────────────────────────────────────────────────

    private static class ColumnMap {
        int dateCol   = -1;
        int descCol   = -1;
        int amountCol = -1;
        int debitCol  = -1;
        @SuppressWarnings("unused")
        int creditCol = -1;
        int typeCol   = -1;
    }

    private ColumnMap mapColumns(Row headerRow) {
        ColumnMap map = new ColumnMap();
        if (headerRow == null) return map;

        for (Cell cell : headerRow) {
            String h = getCellString(cell).toLowerCase().trim();
            if (h.contains("date") && !h.contains("value"))  map.dateCol   = cell.getColumnIndex();
            else if (h.contains("narration") || h.contains("description")
                  || h.contains("particulars") || h.contains("remarks")
                  || h.contains("merchant"))                  map.descCol   = cell.getColumnIndex();
            else if (h.equals("amount(inr)") || h.equals("amount")
                  || h.contains("transaction amount"))        map.amountCol = cell.getColumnIndex();
            else if (h.contains("withdrawal") || h.contains("debit")
                  || h.equals("dr"))                         map.debitCol  = cell.getColumnIndex();
            else if (h.contains("deposit") || h.contains("credit")
                  || h.equals("cr"))                         map.creditCol = cell.getColumnIndex();
            else if (h.equals("type") || h.equals("dr/cr")
                  || h.equals("txn type"))                   map.typeCol   = cell.getColumnIndex();
        }
        return map;
    }

    private BigDecimal resolveDebitAmount(Row row, ColumnMap cols, BankFormat bank) {
        if (cols.typeCol != -1 && cols.amountCol != -1) {
            String type = getCellString(row.getCell(cols.typeCol)).trim().toUpperCase();
            if (type.equals("CR")) return null;
            return ParserUtils.parseAmount(getCellString(row.getCell(cols.amountCol)));
        }

        if (cols.amountCol != -1 && cols.debitCol == -1) {
            return ParserUtils.parseAmount(getCellString(row.getCell(cols.amountCol)));
        }

        if (cols.debitCol != -1) {
            BigDecimal debit = ParserUtils.parseAmount(
                getCellString(row.getCell(cols.debitCol)));
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) return debit;
        }

        return null;
    }

    // ── Sheet helpers ─────────────────────────────────────────────────────────

    private int findHeaderRow(Sheet sheet) {
        int limit = Math.min(10, sheet.getLastRowNum() + 1);
        for (int i = 0; i < limit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = getCellString(cell).toLowerCase();
                if (val.contains("date") || val.contains("narration")
                        || val.contains("description") || val.contains("particulars")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private BankFormat detectBankFromWorkbook(Workbook workbook) {
        StringBuilder sample = new StringBuilder();
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) return BankFormat.GENERIC;
        int limit = Math.min(10, sheet.getLastRowNum() + 1);
        for (int i = 0; i < limit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                sample.append(getCellString(cell)).append(" ");
            }
        }
        return ParserUtils.detectBank(sample.toString());
    }

    private LocalDate parseDateCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return ParserUtils.parseDate(getCellString(cell).trim());
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                : BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING  -> cell.getRichStringCellValue().getString();
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                default      -> "";
            };
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK
                    && !getCellString(cell).isBlank()) return false;
        }
        return true;
    }
}