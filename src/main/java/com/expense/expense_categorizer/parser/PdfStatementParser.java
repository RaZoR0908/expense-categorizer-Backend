package com.expense.expense_categorizer.parser;

import com.expense.expense_categorizer.model.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Component
public class PdfStatementParser {
    public List<Transaction> parse(MultipartFile file) throws Exception {
        throw new UnsupportedOperationException("PDF parsing not yet implemented");
    }
}