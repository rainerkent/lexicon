package org.lexicon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Lexicon {

    private static final String DEFAULT_EXCEL_NAME = "./files/Bisaya Lexicon.xls";

    private static int[] wordStats;
    private DataFormatter formatter = new DataFormatter();
    private Workbook workbook;

    public Lexicon() throws EncryptedDocumentException, InvalidFormatException, IOException {
        this(DEFAULT_EXCEL_NAME);
    }

    public Lexicon(String filePath) throws EncryptedDocumentException, InvalidFormatException, IOException {
        workbook = WorkbookFactory.create(new File(filePath));
    }

    public int countWordsWithMultipleTranslation() {
        return getWordStats()[2];
    }

    public int countWordsWithNoTranslation() {
        return getWordStats()[0];
    }

    public int countWordsWithSingleTranslation() {
        return getWordStats()[1];
    }

    private int[] getWordStats() {
        if (wordStats != null) {
            return wordStats;
        }
        else {
            ArrayList<String> none = new ArrayList<>();
            ArrayList<String> single = new ArrayList<>();
            ArrayList<String> multiple = new ArrayList<>();

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (formatter.formatCellValue(row.getCell(0)).isEmpty()) {
                    continue;
                }

                String word = formatter.formatCellValue(row.getCell(0)).trim();
                if (formatter.formatCellValue(row.getCell(2)).isEmpty()) {
                    none.add(word);
                }
                else if (single.contains(word)) {
                    single.remove(word);
                    multiple.add(word);
                }
                else if (!multiple.contains(word)) {
                    single.add(word);
                }
            }

            // for (int i = 1; i <= 13912; i++) {
            // String id = String.valueOf(i);

            // if (!none.contains(id) && !single.contains(id) && !multiple.contains(id))
            // System.out.println("Missing: " + id);
            // }

            wordStats = new int[] { none.size(), single.size(), multiple.size() };
            return wordStats;
        }
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }
}
