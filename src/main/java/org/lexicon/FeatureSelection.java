package org.lexicon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.lexicon.process.DataProcessor;

public class FeatureSelection {

    public static final String DEFAULT_DOCUMENT_FILE = "./files/document.xlsx";

    private static final int SENTENCE_COLUMN = 0;
    private static final int SENTIMENT_COLUMN = 1;

    // static members only
    private FeatureSelection() {}

    public static void displayTopWords(String lexiconFile, int num, Sentiment sentiment, boolean includeStopWords) {
        TreeMap<Integer, List<String>> topWords = getTopWords(lexiconFile, num, sentiment, includeStopWords);

        System.out.printf("Top %d words: \n", num);
        int n = 1;
        for (int key : topWords.descendingKeySet()) {
            System.out.printf("#%d", n++);
            for (String word : topWords.get(key)) {
                System.out.printf("\t%-20s\t%d\n", word, key);
            }
        }
        System.out.println();
    }

    public static Map<String, Map<String, Integer>> getDocumentWordCount(String lexiconFile, boolean removeStopWords) {
        try (Workbook workbook = WorkbookFactory.create(new File(lexiconFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Map<String, Integer>> sentimentMap = new HashMap<>();
            sentimentMap.put("ALL", new TreeMap<>());
            Map<String, Integer> totalCountMap = sentimentMap.get("ALL");

            // Assume index 0 is header row
            int currentRowNum = 1;
            while (currentRowNum < sheet.getLastRowNum()) {
                Row currentRow = sheet.getRow(currentRowNum++);
                if (currentRow == null) {
                    continue;
                }

                String sentence = formatter.formatCellValue(currentRow.getCell(SENTENCE_COLUMN));
                Sentiment sentiment = Sentiment
                        .getValue(formatter.formatCellValue(currentRow.getCell(SENTIMENT_COLUMN)));

                List<String> wordList = DataProcessor.preprocess(sentence, removeStopWords);
                if (sentimentMap.get(sentiment.getValue()) == null) {
                    sentimentMap.put(sentiment.getValue(), new TreeMap<>());
                }

                Map<String, Integer> wordCountMap = sentimentMap.get(sentiment.getValue());
                for (String word : wordList) {
                    // By Sentiment
                    if (wordCountMap.get(word) == null) {
                        wordCountMap.put(word, 1);
                    }
                    else {
                        int count = wordCountMap.get(word) + 1;
                        wordCountMap.put(word, count);
                    }

                    // All sentiments
                    if (totalCountMap.get(word) == null) {
                        totalCountMap.put(word, 1);
                    }
                    else {
                        int count = totalCountMap.get(word) + 1;
                        totalCountMap.put(word, count);
                    }
                }
            }

            return sentimentMap;
        }
        catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            System.err.println(e);
            return null;
        }
    }

    public static TreeMap<Integer, List<String>> getTopWords(String lexiconFile, int numOfWords, Sentiment sentiment, boolean removeStopWords) {
        String sentimentStr = sentiment != null ? sentiment.getValue() : "ALL";
        // Key -> word; Value -> count
        Map<String, Integer> wordCountMap = getDocumentWordCount(lexiconFile, removeStopWords).get(sentimentStr);

        TreeMap<Integer, List<String>> sortedWordsMap = new TreeMap<>();
        TreeMap<Integer, List<String>> topWordsMap = new TreeMap<>();

        for (Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            if (!sortedWordsMap.containsKey(entry.getValue())) {
                sortedWordsMap.put(entry.getValue(), new ArrayList<>(100));
            }
            sortedWordsMap.get(entry.getValue()).add(entry.getKey());
        }

        int n = 0;
        for (int key : sortedWordsMap.descendingKeySet()) { // Descending para mauna ang pinakadaku
            if (n++ >= numOfWords) {
                break;
            }
            topWordsMap.put(key, sortedWordsMap.get(key));
        }
        return topWordsMap;
    }

}
