package org.lexicon;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.DataProcessor;
import org.lexicon.util.WorkbookUtil;

public class BagOfWords {
    public static final String DEFAULT_EXCEL_NAME = "./files/BOW.xls";
    private static final int WORD_COLUMN = 0;
    private static final int POSITIVE_COUNT_COLUMN = 1;
    private static final int POSITIVE_LIKELIHOOD_COLUMN = 2;
    private static final int NEGATIVE_COUNT_COLUMN = 3;
    private static final int NEGATIVE_LIKELIHOOD_COLUMN = 4;
    private static final int NEUTRAL_COUNT_COLUMN = 5;
    private static final int NEUTRAL_LIKELIHOOD_COLUMN = 6;

    private Map<String, Map<Sentiment, Integer>> wordCountMap = new TreeMap<>();
    private Map<String, Map<Sentiment, Double>> wordLikelihoodMap = new HashMap<>();

    private Map<Sentiment, Integer> wordsBySentimentCountMap = new EnumMap<>(Sentiment.class);
    private Map<Sentiment, Integer> sentencesBySentimentCountMap = new EnumMap<>(Sentiment.class);

    public BagOfWords(Document document) {
        List<AnnotatedText> sentences = document.getData();
        for (AnnotatedText sentence : sentences) {
            Sentiment sentiment = sentence.getCategory();
            for (String word : DataProcessor.preprocess(sentence.getText())) {
                if (!wordCountMap.containsKey(word)) {
                    wordCountMap.put(word, createCountMap(0, 0, 0));
                }

                Map<Sentiment, Integer> countMap = wordCountMap.get(word);
                int count = countMap.get(sentiment);
                countMap.put(sentiment, count + 1);
            }
        }
        sentencesBySentimentCountMap = document.getSentenceCountMap();
        calculateLikelihood();
    }

    public BagOfWords(String bowDocumentPath) throws EncryptedDocumentException, InvalidFormatException, IOException {
        if (bowDocumentPath == null) {
            bowDocumentPath = DEFAULT_EXCEL_NAME;
        }

        DataFormatter formatter = new DataFormatter();
        Workbook workbook = WorkbookFactory.create(new File(bowDocumentPath));
        Sheet sheet = workbook.getSheetAt(0);

        int currentRowNum;
        for (currentRowNum = 2; currentRowNum < sheet.getLastRowNum() - 1; currentRowNum++) { // Start at 2, assume 0-1 is header
            Row currentRow = sheet.getRow(currentRowNum);
            String word = formatter.formatCellValue(currentRow.getCell(WORD_COLUMN));
            Map<Sentiment, Integer> countMap = new EnumMap<>(Sentiment.class);
            Map<Sentiment, Double> likelihoodMap = new EnumMap<>(Sentiment.class);

            // Loop for every sentiment
            for (int i = 0; i < 3; i++) {
                Sentiment sentiment = Sentiment.values()[i];
                int wordCount = Integer.parseInt(formatter.formatCellValue(currentRow.getCell(i + 1)));
                double likelihood = Double.parseDouble(formatter.formatCellValue(currentRow.getCell(i + 2)));
                countMap.put(sentiment, wordCount);
                likelihoodMap.put(sentiment, likelihood);
            }
            wordCountMap.put(word, countMap);
            wordLikelihoodMap.put(word, likelihoodMap);
        }

        // Total Word count by class
        Row row = sheet.getRow(currentRowNum++);
        for (int i = 1; i < 6; i += 2) {
            int count = Integer.parseInt(formatter.formatCellValue(row.getCell(i + 1)));
            wordsBySentimentCountMap.put(Sentiment.values()[(i - 1) / 2], count);
        }

        // Total sentence count by class
        row = sheet.getRow(currentRowNum++);
        for (int i = 1; i < 6; i += 2) {
            int count = Integer.parseInt(formatter.formatCellValue(row.getCell(i + 1)));
            sentencesBySentimentCountMap.put(Sentiment.values()[(i - 1) / 2], count);
        }
    }

    private void calculateLikelihood() {
        for (Map.Entry<String, Map<Sentiment, Integer>> wordCountEntry : wordCountMap.entrySet()) {
            String word = wordCountEntry.getKey();
            Map<Sentiment, Double> likelihoodMap = new EnumMap<>(Sentiment.class);

            for (Sentiment sentiment : Sentiment.values()) {
                int wordCount = wordCountEntry.getValue().get(sentiment);
                int totalWordCountByClass = getWordsBySentimentCount(sentiment);
                double likelihood = ((double) wordCount + 1) / (totalWordCountByClass + getVocabulary().size());

                likelihoodMap.put(sentiment, likelihood);
            }
            wordLikelihoodMap.put(word, likelihoodMap);
        }
    }

    public boolean writeFile(String filePath) {
        if (filePath == null) {
            filePath = DEFAULT_EXCEL_NAME;
        }
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("BOW");

        CellStyle centerAlignStyle = workbook.createCellStyle();
        centerAlignStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle leftAlignStyle = workbook.createCellStyle();
        leftAlignStyle.setAlignment(HorizontalAlignment.LEFT);

        // Header row 1
        Row row = sheet.createRow(0);
        for (int i = 1; i <= 6; i += 2) {
            Cell frequencyCell = row.createCell(i);
            frequencyCell.setCellValue("f");
            frequencyCell.setCellStyle(centerAlignStyle);

            Cell toWeightCell = row.createCell(i+1);
            toWeightCell.setCellValue("TO Weight");
            toWeightCell.setCellStyle(centerAlignStyle);

            sheet.addMergedRegion(new CellRangeAddress(1, 1, i, i + 1));
        }

        // Header row 2
        row = sheet.createRow(1);
        String[] symbols = {"+", "-", "N"};
        for (int i = 0; i < symbols.length; i++) {
            Cell sentimentCell = row.createCell(i * 2 + 1);
            sentimentCell.setCellValue(symbols[i]);
            sentimentCell.setCellStyle(centerAlignStyle);
        }

        // Word list starts here
        Sentiment[] sentimentValues = {Sentiment.POSITIVE, Sentiment. NEGATIVE, Sentiment.NEUTRAL};
        int currentRowNum = 2;
        for (Map.Entry<String, Map<Sentiment, Integer>> wordCountEntry : wordCountMap.entrySet()) {
            Row currentRow = sheet.createRow(currentRowNum++);
            String word = wordCountEntry.getKey();
            Map<Sentiment, Integer> countMap = wordCountEntry.getValue();
            Map<Sentiment, Double> likelihoodMap = wordLikelihoodMap.get(word);

            Cell wordCell = currentRow.createCell(0);
            wordCell.setCellValue(word);

            for (int i = 0; i < 3; i++) {
                Cell frequencyCell = currentRow.createCell(i * 2 + 1);
                frequencyCell.setCellValue(countMap.get(sentimentValues[i]));
                frequencyCell.setCellStyle(leftAlignStyle);

                Cell weightCell = currentRow.createCell(i * 2 + 2);
                weightCell.setCellValue(likelihoodMap.get(sentimentValues[i]));
                weightCell.setCellStyle(leftAlignStyle);
            }
        }

        // Total word count per class
        row = sheet.createRow(currentRowNum++);
        row.createCell(0).setCellValue("Total count:");
        row.createCell(1).setCellValue(wordsBySentimentCountMap.get(Sentiment.POSITIVE));
        row.createCell(3).setCellValue(wordsBySentimentCountMap.get(Sentiment.NEGATIVE));
        row.createCell(5).setCellValue(wordsBySentimentCountMap.get(Sentiment.NEUTRAL));

        // Total sentence count per class
        row = sheet.createRow(currentRowNum);
        row.createCell(0).setCellValue("Sentences:");
        row.createCell(1).setCellValue(sentencesBySentimentCountMap.get(Sentiment.POSITIVE));
        row.createCell(3).setCellValue(sentencesBySentimentCountMap.get(Sentiment.NEGATIVE));
        row.createCell(5).setCellValue(sentencesBySentimentCountMap.get(Sentiment.NEUTRAL));

        // Auto size columns
        for (int i = 0; i <= 6; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < sheet.getDefaultColumnWidth()) {
                sheet.setColumnWidth(i, sheet.getDefaultColumnWidth());
            }
        }

        return WorkbookUtil.writeWorkbookToFile(workbook, filePath);
    }

    public Set<String> getVocabulary() {
        return wordCountMap.keySet();
    }

    private static Map<Sentiment, Integer> createCountMap(int pos, int neg, int neut) {
        Map<Sentiment, Integer> countMap = new EnumMap<>(Sentiment.class);
        countMap.put(Sentiment.POSITIVE, pos);
        countMap.put(Sentiment.NEGATIVE, neg);
        countMap.put(Sentiment.NEUTRAL, neut);
        return countMap;
    }

    private static int getWordCount(Map<Sentiment, Integer> countMap) {
        int total = 0;
        for (int count : countMap.values()) {
            total += count;
        }
        return total;
    }

    private int getWordsBySentimentCount(Sentiment sentiment) {
        if (wordsBySentimentCountMap.containsKey(sentiment)) {
            return wordsBySentimentCountMap.get(sentiment);
        }

        int total = 0;
        for (Map<Sentiment, Integer> countMap : wordCountMap.values()) {
            total += countMap.get(sentiment);
        }
        wordsBySentimentCountMap.put(sentiment, total);
        return total;
    }
}
