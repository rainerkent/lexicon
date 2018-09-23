package org.lexicon.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.lexicon.process.ChiSquare;
import org.lexicon.process.DataProcessor;
import org.lexicon.Sentiment;
import org.lexicon.util.ProgressBar;
import org.lexicon.util.WorkbookUtil;

public class Document implements Serializable {

    private static final long serialVersionUID = -4335694861861622949L;

    private List<AnnotatedText> sentences;

    // Sentence count per sentiment
    private Map<Sentiment, Integer> sentenceCountMapCache;

    // List of words found in document grouped by sentiment
    private Map<Sentiment, List<String>> wordListMapCache;

    // Set of words found in the vocabulary
    public Set<String> vocabularySetCache;

    public boolean useFeatureSelection;

    private boolean cacheValid = false;

    public Document(List<AnnotatedText> sentences, boolean useChiSquare) {
        if (sentences == null) {
            throw new NullPointerException();
        }
        this.sentences = sentences;
        this.useFeatureSelection = useChiSquare;
    }

    public List<AnnotatedText> getData() {
        return sentences;
    }

    public void invalidateCache() {
        cacheValid = false;
    }

    public Map<Sentiment, Integer> getSentenceCountMap() {
        if (!cacheValid) {
            generateCache();
        }
        return sentenceCountMapCache;
    }

    public Map<Sentiment, List<String>> getWordListMap() {
        if (!cacheValid) {
            generateCache();
        }
        return wordListMapCache;
    }

    public Set<String> getVocabulary() {
        if (!cacheValid) {
            generateCache();
        }
        return vocabularySetCache;
    }

    public Map<Sentiment, Map<String, Integer>> getWordCountByClassMap() {
        if (!cacheValid) {
            generateCache();
        }

        // Initialize Map
        Map<Sentiment, Map<String, Integer>> sentimentMap = new EnumMap<>(Sentiment.class);
        for (Sentiment s : Sentiment.values()) {
            sentimentMap.put(s, new TreeMap<>());
        }

        // Get individual word counts per classification
        for (Entry<Sentiment, List<String>> wordListMapEntry : wordListMapCache.entrySet()) {

            Map<String, Integer> wordCountMap = sentimentMap.get(wordListMapEntry.getKey());
            for (String word : wordListMapEntry.getValue()) {
                if (!wordCountMap.containsKey(word)) {
                    wordCountMap.put(word, 0);
                }

                int wordCount = wordCountMap.get(word);
                wordCountMap.put(word, wordCount + 1);
            }
        }

        return sentimentMap;
    }

    public Map<String, Map<String, Integer>> getIntersectionDataMap () {
        Map<Sentiment, Map<String, Integer>> sentimentMap = this.getWordCountByClassMap();

        Map<String, Map<String, Integer>> intersectionDataMap = new HashMap<>();
        Map<String, Integer> positiveWordCountMap = sentimentMap.get(Sentiment.POSITIVE);
        Map<String, Integer> negativeWordCountMap = sentimentMap.get(Sentiment.NEGATIVE);
        Map<String, Integer> neutralWordCountMap = sentimentMap.get(Sentiment.NEUTRAL);


        // Get words that exist in all Sentiments
        Map<String, Integer> wordCountMap = new TreeMap<>();
        Set<String> wordsToRemove = new HashSet<>();
        for (Entry<String, Integer> entry : positiveWordCountMap.entrySet()) {

            // Find out if word exists in other class
            String word = entry.getKey();
            if (negativeWordCountMap.containsKey(word) && neutralWordCountMap.containsKey(word)) {
                wordCountMap.put(word, positiveWordCountMap.get(word) + negativeWordCountMap.get(word) + neutralWordCountMap.get(word));

                wordsToRemove.add(word);
                negativeWordCountMap.remove(word);
                neutralWordCountMap.remove(word);
            }

        }
        for (String word : wordsToRemove) {
            positiveWordCountMap.remove(word);
        }
        wordsToRemove.clear();

        intersectionDataMap.put("POSITIVE-NEGATIVE-NEUTRAL", wordCountMap);
        Map<String, Integer> ponegmap =  new TreeMap<>();
        Map<String, Integer> poneumap =  new TreeMap<>();
        Map<String, Integer> negneumap =  new TreeMap<>();
        intersectionDataMap.put("POSITIVE-NEGATIVE", ponegmap);
        intersectionDataMap.put("POSITIVE-NEUTRAL", poneumap);
        intersectionDataMap.put("NEGATIVE-NEUTRAL", negneumap);

        for (Entry<String, Integer> entry : positiveWordCountMap.entrySet()) {
            String word = entry.getKey();
            if (negativeWordCountMap.containsKey(word)) {
                ponegmap.put(word, positiveWordCountMap.get(word) + negativeWordCountMap.get(word));
                wordsToRemove.add(word);
                negativeWordCountMap.remove(word);
            }
            if (neutralWordCountMap.containsKey(word)) {
                poneumap.put(word, positiveWordCountMap.get(word) + neutralWordCountMap.get(word));
                wordsToRemove.add(word);
                neutralWordCountMap.remove(word);
            }
        }
        for (String word : wordsToRemove) {
            positiveWordCountMap.remove(word);
        }
        wordsToRemove.clear();
        for (Entry<String, Integer> entry : negativeWordCountMap.entrySet()) {
            String word = entry.getKey();
            if (neutralWordCountMap.containsKey(word)) {
                negneumap.put(word, negativeWordCountMap.get(word) + neutralWordCountMap.get(word));
                wordsToRemove.add(word);
                neutralWordCountMap.remove(word);
            }
        }
        for (String word : wordsToRemove) {
            negativeWordCountMap.remove(word);
        }

        intersectionDataMap.put("POSITIVE", positiveWordCountMap);
        intersectionDataMap.put("NEGATIVE", negativeWordCountMap);
        intersectionDataMap.put("NEUTRAL", neutralWordCountMap);

        return intersectionDataMap;
    }

    public boolean exportIntersectionData (String filename) {
        final String[] headers = { "WORD", "COUNT", "POSITIVE", "NEGATIVE", "NEUTRAL" };
        Map<Sentiment, Map<String, Integer>> wordCountByClassMap = this.getWordCountByClassMap();
        Map<String, Integer> positiveWordCountMap = wordCountByClassMap.get(Sentiment.POSITIVE);
        Map<String, Integer> negativeWordCountMap = wordCountByClassMap.get(Sentiment.NEGATIVE);
        Map<String, Integer> neutralWordCountMap = wordCountByClassMap.get(Sentiment.NEUTRAL);

        Map<String, Map<String, Integer>> intersectionDataMap = this.getIntersectionDataMap();

        Workbook wb = new HSSFWorkbook();
        for (Entry<String, Map<String, Integer>> intersectionDataEntry : intersectionDataMap.entrySet()) {
            Sheet sheet = wb.createSheet(intersectionDataEntry.getKey());

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int currentRow = 1;
            Map<String, Integer> wordCountMap = intersectionDataEntry.getValue();
            for (Entry<String, Integer> wordCountEntry : wordCountMap.entrySet()) {
                Row row = sheet.createRow(currentRow++);
                String word = wordCountEntry.getKey();

                int positiveCount = positiveWordCountMap.containsKey(word) ? positiveWordCountMap.get(word) : 0;
                int negativeCount = negativeWordCountMap.containsKey(word) ? negativeWordCountMap.get(word) : 0;
                int neutralCount = neutralWordCountMap.containsKey(word) ? neutralWordCountMap.get(word) : 0;

                row.createCell(0).setCellValue(wordCountEntry.getKey());
                row.createCell(1).setCellValue(wordCountEntry.getValue());
                row.createCell(2).setCellValue(positiveCount);
                row.createCell(3).setCellValue(negativeCount);
                row.createCell(4).setCellValue(neutralCount);
            }
            Row row = sheet.createRow(currentRow);
            row.createCell(0).setCellValue("TOTAL:");
            row.createCell(1).setCellFormula("SUM(B2:B" + (currentRow) + ")");

        }

        return WorkbookUtil.writeWorkbookToFile(wb, filename);
    }

    private void generateCache() {
        ProgressBar bar = new ProgressBar(sentences.size());
        Map<String, Double> selectedFeatures = new HashMap<>();
        if (useFeatureSelection) {
            ChiSquare cs = new ChiSquare();
            selectedFeatures = cs.selectFeatures(this);
        }

        initializeCache();
        for (AnnotatedText sentence : sentences) {
            Sentiment sentiment = sentence.getCategory();

            // sentence count
            int count = sentenceCountMapCache.get(sentiment);
            sentenceCountMapCache.put(sentiment, count + 1);

            // word list
            List<String> words = DataProcessor.preprocess(sentence.getText());
            for (String word : words) {
                if (!useFeatureSelection || selectedFeatures.get(word) != null) {
                    wordListMapCache.get(sentiment).add(word);
                    vocabularySetCache.add(word);
                }
            }
            bar.step();
        }
        cacheValid = true;
    }

    private void initializeCache() {
        sentenceCountMapCache = new HashMap<>();
        wordListMapCache = new HashMap<>();
        vocabularySetCache = new HashSet<>(5000);

        for (Sentiment sentiment : Sentiment.values()) {
            sentenceCountMapCache.put(sentiment, 0);
            wordListMapCache.put(sentiment, new ArrayList<>(5000));
        }
    }
}
