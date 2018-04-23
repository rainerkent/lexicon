package org.lexicon.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.lexicon.Sentiment;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.util.WorkbookUtil;

public class DocumentHelper {

    public static final String DEFAULT_DOCUMENT_FILE = "./files/document.xlsx";
    public static final String DEFAULT_TEST_RESULT_FILE = "./files/test-result.xlsx";

    private static final int SENTENCE_COLUMN = 0;
    private static final int CATEGORY_COLUMN = 1;

    // static members only
    private DocumentHelper() {};

    public static Map<Sentiment, List<AnnotatedText>> loadDocument(String fileName) {
        Map<Sentiment, List<AnnotatedText>> document = new HashMap<>();

        // Initialize ArrayLists
        document.put(Sentiment.NEGATIVE, new ArrayList<>());
        document.put(Sentiment.NEUTRAL, new ArrayList<>());
        document.put(Sentiment.POSITIVE, new ArrayList<>());

        try {
            Sheet sheet = WorkbookFactory.create(new File(fileName)).getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // System.out.println("Last RowNum: " + sheet.getLastRowNum());
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Assume row0 is header
                Row currentRow = sheet.getRow(i);
                if (currentRow != null) {
                    String sentence = formatter.formatCellValue(currentRow.getCell(SENTENCE_COLUMN));
                    Sentiment sentiment = Sentiment
                            .getValue(formatter.formatCellValue(currentRow.getCell(CATEGORY_COLUMN)));
                    document.get(sentiment).add(new AnnotatedText(sentence, sentiment));
                }
            }
        }
        catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            return null;
        }

        adjustDocumentSize(document);
        return document;
    }

    /**
     * @param fileName
     * @return TrainingDocument or <code>null</code> if fileName is invalid
     */
    public static Document loadTrainingDocument(String fileName) {
        Map<Sentiment, List<AnnotatedText>> document = loadDocument(fileName);

        if (document != null) {
            // Get 70% of each for training data
            List<AnnotatedText> trainingDoc = new ArrayList<>();
            for (Map.Entry<Sentiment, List<AnnotatedText>> entry : document.entrySet()) {
                List<AnnotatedText> categoryDoc = entry.getValue();
                int endIndex = Math.round(categoryDoc.size() * 0.7f) - 1;
                trainingDoc.addAll(categoryDoc.subList(0, endIndex));
            }
            return new Document(trainingDoc);
        }
        else {
            return null;
        }
    }

    public static Document loadTestingDocument(String fileName) {
        Map<Sentiment, List<AnnotatedText>> document = loadDocument(fileName);

        if (document != null) {
            // Get 30% of each for testing data
            List<AnnotatedText> testingDoc = new ArrayList<>();
            for (Map.Entry<Sentiment, List<AnnotatedText>> entry : document.entrySet()) {
                List<AnnotatedText> categoryDoc = entry.getValue();
                int startIndex = Math.round(categoryDoc.size() * 0.7f);
                testingDoc.addAll(categoryDoc.subList(startIndex, categoryDoc.size() - 1));
            }

            return new Document(testingDoc);
        }
        else {
            return null;
        }
    }

    /**
     * Trims the size of the documents if there are size inequalities
     */
    private static void adjustDocumentSize(Map<Sentiment, List<AnnotatedText>> document) {
        // Get the minimum size for the three categories
        int minSize = Integer.MAX_VALUE;
        for (Map.Entry<Sentiment, List<AnnotatedText>> entry : document.entrySet()) {
            int categoryDocSize = entry.getValue().size();
            if (minSize > categoryDocSize)
                minSize = categoryDocSize;
        }

        // Trim each document to minSize
        for (Map.Entry<Sentiment, List<AnnotatedText>> entry : document.entrySet()) {
            List<AnnotatedText> list = entry.getValue();
            if (list.size() > minSize)
                entry.setValue(list.subList(0, minSize - 1));
        }
    }

    public static boolean writeTestResult(Map<AnnotatedText, Sentiment> testResult, String file) {
        final String[] headers = { "Sentence", "Classification", "Prediction" };

        Workbook resultWb = new HSSFWorkbook();
        Sheet sheet = resultWb.createSheet("Test Result");

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int correctPrediction = 0;
        int currentRowNum = 1;
        for (Map.Entry<AnnotatedText, Sentiment> entry : testResult.entrySet()) {
            String sentence = entry.getKey().getText();
            Sentiment classification = entry.getKey().getCategory();
            Sentiment prediction = entry.getValue();

            if (classification == prediction) {
                correctPrediction++;
            }

            // Write values
            Row currentRow = sheet.createRow(currentRowNum);
            currentRow.createCell(0).setCellValue(sentence);
            currentRow.createCell(1).setCellValue(classification.toString());
            currentRow.createCell(2).setCellValue(prediction.toString());

            currentRowNum++;
        }

        Row currentRow = sheet.createRow(currentRowNum);
        currentRow.createCell(0).setCellValue("Accuracy:");
        currentRow.createCell(1).setCellValue(String.format("%.2f%%", (double) correctPrediction / testResult.size() * 100));

        return WorkbookUtil.writeWorkbookToFile(resultWb, file);
    }
}
