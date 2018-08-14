package org.lexicon;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.DataProcessor;
import org.lexicon.util.ProgressBar;

public class HappinessIndex {

    private Map<String, LexiconWordDetail> lexiconWordMap = new HashMap<>();

    public void load(String lexiconFilePath) {
        lexiconWordMap = new HashMap<>();
        try (Workbook wb = WorkbookFactory.create(new File(lexiconFilePath))){
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Assume row[0] is header
                Row currentRow = sheet.getRow(i);
                if (currentRow != null) {
                    String bisayaWord = formatter.formatCellValue(currentRow.getCell(2)).trim();
                    
                    if (bisayaWord.length() != 0) {
                        LexiconWordDetail detail = lexiconWordMap.get(bisayaWord);
                        if (detail == null) {
                            detail = new LexiconWordDetail();
                        }
                        
                        double[] wordScores = getRowScores(currentRow);
                        if (wordScores == null) {
                            wordScores = detail.scores;
                        }
                        
                        detail.addScores(wordScores);
                        lexiconWordMap.put(bisayaWord, detail);
                    }
                }
            }
        }
        catch (EncryptedDocumentException | InvalidFormatException | IOException e) {}
    }
    

    public TestResult test(Document testDocument, int level) {
        Map<AnnotatedText, Sentiment> result = new LinkedHashMap<>();
        ProgressBar bar = new ProgressBar(testDocument.getData().size());
        for (AnnotatedText sentence : testDocument.getData()) {
            Sentiment prediction = predict(sentence.getText(), level);
            result.put(sentence, prediction);
            bar.step();
        }
        return new TestResult(result);
    }
        
    private Sentiment predict(String sentence, int level) {
        List<String> tokens = DataProcessor.preprocess(sentence);

        Sentiment maxSentiment = null;
        double maxScore = Double.NEGATIVE_INFINITY;

        for (Sentiment sentiment : Sentiment.values()) {
            double score = 0;
            
            for (String token : tokens) {
                if (lexiconWordMap.containsKey(token)) {
                    LexiconWordDetail detail = lexiconWordMap.get(token);
                    
                    if (sentiment == Sentiment.POSITIVE && detail.scores[0] > 5) {
                        score += getScore(detail.scores, level);
                    }
                    else if (sentiment == Sentiment.NEGATIVE && detail.scores[0] < 5) {
                        score += getScore(detail.scores, level);
                    }
                    else if (sentiment == Sentiment.NEUTRAL && detail.scores[0] == 5) {
                        score += getScore(detail.scores, level);
                    }
                }
            }

            if (maxScore < score) {
                maxSentiment = sentiment;
                maxScore = score;
            }
        }

        return maxSentiment;
    }


    private double[] getRowScores(Row row) {
        DataFormatter formatter = new DataFormatter();
        try {
            double[] scores = new double[3];
            for (int n = 0; n < 3; n++) {
                String scoreStr = formatter.formatCellValue(row.getCell(n + 3)).trim();
                scores[n] = new Double(scoreStr);
            }
            return scores;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    private double getScore(double[] scores, int level) {
        if (level < 1) {
            level = 1;
        }
        else if (level > 3) {
            level = 3;
        }
        
        double score = 0;
        for (int n = 0; n < level; n++) {
            score += scores[n];
        }
        return score;
    }

    private static class LexiconWordDetail {
        public int count = 0;
        public double[] scores =  { 0, 0, 0 };
        
        public void addScores(double[] scores) {
            for (int i = 0; i < this.scores.length; i++) {
                this.scores[i] = (this.scores[i] * count + scores[i]) / (count + 1);
            }
            count++;
        }
    }
}
