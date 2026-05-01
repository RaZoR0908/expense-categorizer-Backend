package com.expense.expense_categorizer.service;

import com.expense.expense_categorizer.model.Transaction;
import com.expense.expense_categorizer.parser.CsvStatementParser;
import com.expense.expense_categorizer.parser.ExcelStatementParser;
import com.expense.expense_categorizer.parser.PdfStatementParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ParserService {

    @Autowired
    private PdfStatementParser pdfParser;
    @Autowired
    private ExcelStatementParser excelParser;
    @Autowired
    private CsvStatementParser csvParser;

    public List<Transaction> parseFile(MultipartFile file, String password) throws Exception {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new Exception("Invalid file name");
        }

        if (fileName.endsWith(".pdf")) {
            return pdfParser.parse(file, password);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return excelParser.parse(file, password);
        } else if (fileName.endsWith(".csv")) {
            return csvParser.parse(file); // CSV has no password support
        } else {
            throw new Exception("Unsupported file format: " + fileName);
        }
    }
}