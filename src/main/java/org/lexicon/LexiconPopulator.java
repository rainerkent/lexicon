package org.lexicon;

import static org.lexicon.util.ResourceUtil.TRANSLATIONS_JSON;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.lexicon.data.Translation;
import org.lexicon.process.CebuanoNormalizer;
import org.lexicon.process.DataProcessor;
import org.lexicon.process.dictionary.CebuanoDictionary;
import org.lexicon.util.ResourceUtil;
import org.lexicon.util.WorkbookUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LexiconPopulator {

    public static final String BASE_EXCEL_FILE = "./files/Bisaya Lexicon.xls";
    public static final String OUTPUT_EXCEL_FILE = "./files/Output.xls";

    private static final int WORD_ID_COLUMN = 0;
    private static final int ENGLISH_WORD_COLUMN = 1;
    private static final int CEBUANO_WORD_COLUMN = 2;
    private static final int SCORE1_COLUMN = 3;
    private static final int SCORE2_COLUMN = 4;
    private static final int SCORE3_COLUMN = 5;
    private static final int POS_COLUMN = 7;

    private static JsonObject translations = null;
    private static CellStyle cellStyle = null;

    public static void copyRow(Row fromRow, Row toRow) {
        DataFormatter formatter = new DataFormatter();
        if (cellStyle == null) {
            cellStyle = toRow.getSheet().getWorkbook().createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.RIGHT);
        }

        toRow.createCell(WORD_ID_COLUMN).setCellValue(formatter.formatCellValue(fromRow.getCell(WORD_ID_COLUMN)));
        toRow.createCell(ENGLISH_WORD_COLUMN)
                .setCellValue(formatter.formatCellValue(fromRow.getCell(ENGLISH_WORD_COLUMN)));
        toRow.createCell(CEBUANO_WORD_COLUMN)
                .setCellValue(formatter.formatCellValue(fromRow.getCell(CEBUANO_WORD_COLUMN)));
        toRow.createCell(SCORE1_COLUMN).setCellValue(formatter.formatCellValue(fromRow.getCell(SCORE1_COLUMN)));
        toRow.createCell(SCORE2_COLUMN).setCellValue(formatter.formatCellValue(fromRow.getCell(SCORE2_COLUMN)));
        toRow.createCell(SCORE3_COLUMN).setCellValue(formatter.formatCellValue(fromRow.getCell(SCORE3_COLUMN)));
        toRow.createCell(POS_COLUMN).setCellValue(formatter.formatCellValue(fromRow.getCell(POS_COLUMN)));

        toRow.getCell(WORD_ID_COLUMN).setCellStyle(cellStyle);
        toRow.getCell(SCORE1_COLUMN).setCellStyle(cellStyle);
        toRow.getCell(SCORE2_COLUMN).setCellStyle(cellStyle);
        toRow.getCell(SCORE3_COLUMN).setCellStyle(cellStyle);
    }

    private static Translation getNextString(Set<Translation> from, Set<String> restrictions) {
        if (from == null)
            return null;

        while (from.iterator().hasNext()) {
            Translation translation = from.iterator().next();
            from.remove(translation);

            if (!restrictions.contains(translation.getWord())) { return translation; }
        }

        return null;
    }

    public static Translation getTranslation(String word, Set<Translation> set) {
        Translation translation = null;
        for (Translation t : set) {
            if (t.getWord() == word) {
                translation = t;
                break;
            }
        }

        if (translation != null) {
            set.remove(translation);
        }

        return translation;
    }

    public static Set<Translation> getTranslations(String word) {
        if (translations == null) {
            translations = ResourceUtil.parseJson(TRANSLATIONS_JSON).getAsJsonObject();
        }
        JsonArray cebuanoWords = translations.getAsJsonArray(word);

        Set<Translation> wordSet = new TreeSet<>();
        if (cebuanoWords != null) {
            for (int i = 0; i < cebuanoWords.size(); i++) {
                JsonObject jsonObj = cebuanoWords.get(i).getAsJsonObject();
                String cleanedWord = DataProcessor.clean(jsonObj.get("translation").getAsString());
                if (cleanedWord != null) {
                    wordSet.add(new Translation(cleanedWord, jsonObj.get("pos").getAsString()));
                    Set<String> otherForms = CebuanoDictionary.getOtherForms(cleanedWord);
                    for (String form : otherForms) {
                        wordSet.add(new Translation(form, jsonObj.get("pos").getAsString()));
                    }
                }
            }
        }
        else {
            System.out.println("Word not found: " + word);
        }

        return wordSet;
    }

    public static boolean populate(String baseFile, String resultFile) {
        try {
            Workbook wb = WorkbookFactory.create(new File(baseFile));
            Sheet sheet = wb.getSheetAt(0);

            Workbook wbOut = new HSSFWorkbook();
            Sheet sheetOut = wbOut.createSheet("ANEW-2013.csv");

            DataFormatter formatter = new DataFormatter();
            CellStyle styleRightAlign = sheet.getRow(1).getCell(3).getCellStyle();
            styleRightAlign.cloneStyleFrom(sheet.getRow(1).getCell(3).getCellStyle());
            styleRightAlign.setAlignment(HorizontalAlignment.RIGHT);

            String currentWord = "";
            int wordStartRow = 1;
            Set<String> existingCebuanoWords = new TreeSet<>();
            Set<Translation> cebuanoWordsToAdd = new TreeSet<>();

            int currentRowNum = 0;
            int currentRowOut = 0;
            while (currentRowNum < sheet.getLastRowNum()) {
                Row currentRow = sheet.getRow(currentRowNum);
                if (currentRow == null) {
                    currentRowNum++;
                    continue;
                }

                String currentRowWordId = formatter.formatCellValue(currentRow.getCell(WORD_ID_COLUMN));
                String currentRowWord = formatter.formatCellValue(currentRow.getCell(ENGLISH_WORD_COLUMN)).trim();

                // Clear pos cell
                if (currentRow.getCell(POS_COLUMN) != null)
                    currentRow.getCell(POS_COLUMN).setCellType(CellType.BLANK);

                if (currentRowWordId.isEmpty()) {
                    copyRow(currentRow, sheetOut.createRow(currentRowOut++));
                    currentRowNum++;
                }
                else if (currentWord == currentRowWord) {
                    if (wordStartRow == currentRowNum) {
                        cebuanoWordsToAdd = getTranslations(currentWord);
                    }

                    String currentCebuanoWord = CebuanoNormalizer
                            .normalize(formatter.formatCellValue(currentRow.getCell(CEBUANO_WORD_COLUMN)));

                    if (currentCebuanoWord.contains(",")) { // translations has comma-separated values
                        String[] wordsToAdd = currentCebuanoWord.replace(" ", "").split(",");
                        for (String word : wordsToAdd)
                            cebuanoWordsToAdd.add(new Translation(word, ""));
                        currentCebuanoWord = "";
                    }

                    if (currentCebuanoWord.isEmpty() && cebuanoWordsToAdd.size() > 0) { // CASE: No Translation
                        Translation cebuanoWord = getNextString(cebuanoWordsToAdd, existingCebuanoWords);
                        currentRow.createCell(CEBUANO_WORD_COLUMN).setCellValue(cebuanoWord.getWord());
                        currentRow.createCell(POS_COLUMN).setCellValue(cebuanoWord.getPOS());
                        cebuanoWordsToAdd.remove(cebuanoWord);
                        existingCebuanoWords.add(cebuanoWord.getWord());
                    }
                    else if (!currentCebuanoWord.isEmpty()) {
                        currentRow.getCell(CEBUANO_WORD_COLUMN).setCellValue(currentCebuanoWord);
                        currentRow.createCell(POS_COLUMN).setCellValue("");
                        existingCebuanoWords.add(currentCebuanoWord);

                        Set<String> otherForms = CebuanoDictionary.getOtherForms(currentCebuanoWord);
                        for (String form : otherForms) {
                            cebuanoWordsToAdd.add(new Translation(form, ""));
                        }
                    }

                    currentRowNum++;
                    copyRow(currentRow, sheetOut.createRow(currentRowOut++));
                }
                else {
                    Translation wordToAdd = getNextString(cebuanoWordsToAdd, existingCebuanoWords);

                    // Insert cebuano translation
                    if (wordToAdd != null) {
                        Row row = sheetOut.createRow(currentRowOut++);
                        Row baseRow = sheet.getRow(wordStartRow);

                        copyRow(baseRow, row);
                        row.createCell(CEBUANO_WORD_COLUMN).setCellValue(wordToAdd.getWord());
                        row.createCell(POS_COLUMN).setCellValue(wordToAdd.getPOS());
                    }
                    else { // New word found: reset variables
                        wordStartRow = currentRowNum;
                        currentWord = formatter.formatCellValue(currentRow.getCell(ENGLISH_WORD_COLUMN)).trim();
                        existingCebuanoWords = new TreeSet<>();
                        cebuanoWordsToAdd = null;

                        System.out.println("Word:\t" + currentRowWord);
                    }
                }
            }

            int numberOfColumns = 8;
            for (int i = 0; i < numberOfColumns; i++) {
                sheetOut.autoSizeColumn(i);
            }

            return WorkbookUtil.writeWorkbookToFile(wbOut, resultFile);
        }
        catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
