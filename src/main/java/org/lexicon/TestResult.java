package org.lexicon;

import org.lexicon.data.AnnotatedText;

import java.util.HashMap;
import java.util.Map;

public class TestResult {

    public Map<AnnotatedText, Sentiment> resultMap;

    // Correctly identified results
    private Map<Sentiment, Integer> trueValues;

    // Falsely identified results
    private Map<Sentiment, Integer> falseValues;

    private Map<Sentiment, Integer> systemPredictCountMap = new HashMap<>();
    private int totalTrueCount = 0;
    private int totalFalseCount = 0;

    // Initializes member variables
    private TestResult () {
        trueValues = new HashMap<>();
        trueValues.put(Sentiment.POSITIVE, 0);
        trueValues.put(Sentiment.NEGATIVE, 0);
        trueValues.put(Sentiment.NEUTRAL, 0);

        falseValues = new HashMap<>();
        falseValues.put(Sentiment.NEGATIVE, 0);
        falseValues.put(Sentiment.POSITIVE, 0);
        falseValues.put(Sentiment.NEUTRAL, 0);

        systemPredictCountMap = new HashMap<>();
        systemPredictCountMap.put(Sentiment.NEGATIVE, 0);
        systemPredictCountMap.put(Sentiment.POSITIVE, 0);
        systemPredictCountMap.put(Sentiment.NEUTRAL, 0);
    }

    public TestResult(Map<AnnotatedText,Sentiment> result) {
        this();
        resultMap = result;
        for (Map.Entry<AnnotatedText, Sentiment> entry : result.entrySet()) {
            Sentiment value = entry.getKey().getCategory();
            Sentiment prediction = entry.getValue();

            int sentimentCount = systemPredictCountMap.get(prediction);
            systemPredictCountMap.put(prediction, sentimentCount + 1);

            if (value.equals(prediction)) {
                int count = trueValues.get(value);
                trueValues.put(value, count + 1);
                totalTrueCount++;
            }
            else {
                int count = falseValues.get(value);
                falseValues.put(value, count + 1);
                totalFalseCount++;
            }
        }
    }

    public Map<Sentiment, Integer> getTrueValues () {
        return trueValues;
    }

    public Map<Sentiment, Integer> getFalseValues () {
        return falseValues;
    }

    public Map<AnnotatedText, Sentiment> getResultMap () {
        return resultMap;
    }

    public double getAccuracy () {
        return (double) totalTrueCount / (totalTrueCount + totalFalseCount);
    }

    public Map<Sentiment, Double> getPrecision () {
        Map<Sentiment, Double> precisionMap = new HashMap<>();
        for (Sentiment sentiment : Sentiment.values()) {
            int trueValue = trueValues.get(sentiment);
            int falseValue = falseValues.get(sentiment);
            double sentimentPrecision = (double) trueValue / (trueValue + falseValue);
            precisionMap.put(sentiment, sentimentPrecision);
        }
        return precisionMap;
    }

    // Macro-average
    public double getOverallPrecision () {
        Map<Sentiment, Double> precisionMap = getPrecision();
        double total = 0;
        for (double precision : precisionMap.values()) {
            total += precision;
        }
        return total / precisionMap.size();
    }

    public Map<Sentiment, Double> getRecall () {
        Map<Sentiment, Double> recallMap = new HashMap<>();
        for (Sentiment sentiment : Sentiment.values()) {
            // count of correctly predicted sentence as `sentiment`
            int trueValue = trueValues.get(sentiment);

            // count of sentences predicted by system as `sentiment`
            int trueLabelCount = systemPredictCountMap.get(sentiment);

            double sentimentPrecision = (double) trueValue / trueLabelCount;
            recallMap.put(sentiment, sentimentPrecision);
        }
        return recallMap;
    }

    // Macro-average
    public double getOverallRecall () {
        Map<Sentiment, Double> recallMap = getRecall();
        double total = 0;
        for (double recall : recallMap.values()) {
            total += recall;
        }
        return total / recallMap.size();
    }

    public double getFMeasure (double beta) {
        double precision = getOverallPrecision();
        double recall = getOverallRecall();
        double betaSquared = Math.pow(beta, 2);
        return ((betaSquared + 1) * precision * recall) / (betaSquared * precision + recall);
    }

    public double getFMeasure () {
        return getFMeasure(1);
    }
}
