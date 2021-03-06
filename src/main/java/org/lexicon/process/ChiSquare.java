package org.lexicon.process;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.lexicon.NaiveBayesClassifier;
import org.lexicon.Sentiment;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.data.FeatureStats;

import java.util.HashMap;
import java.util.Map;

public class ChiSquare {

    private double df = 2;
    private double pValue = 0.05;

    public void setDegreesOfFreedom (double df) {
        this.df = df;
    }

    public void setPValue (double pValue) {
        this.pValue = pValue;
    }

    private double calculateCriticalValue () {
        ChiSquaredDistribution csd = new ChiSquaredDistribution(this.df);
        return csd.inverseCumulativeProbability(1 - this.pValue);
    }

    public Map<String, Double> selectFeatures(Document dataset) {
        int vocabularyNum = dataset.vocabularySetCache.size();
        FeatureStats stats = FeatureStats.generateFeatureStats(dataset);
        System.out.println("VocabularyNum: " + vocabularyNum);
        setDegreesOfFreedom((vocabularyNum - 1) * (Sentiment.values().length - 1));
        double criticalValue = calculateCriticalValue();
        System.out.println("CriticalValue: " + criticalValue);
        Map<String, Double> selectedFeatures = new HashMap<>();

        String feature;
        Sentiment category;
        Map<Sentiment, Integer> categoryList;

        int n1dot, n0dot, n00, n01, n10, n11;
        double chiSquareScore;
        Double previousScore;
        for(Map.Entry<String, Map<Sentiment, Integer>> entry1 : stats.featureCategoryJointCount.entrySet()) {
            feature = entry1.getKey();
            categoryList = entry1.getValue();

            //calculate the N1. (number of documents that have the feature)
            n1dot = 0;
            for(Integer count : categoryList.values()) {
                n1dot += count;
            }

            //also the N0. (number of documents that DONT have the feature)
            n0dot = stats.n - n1dot;

            for(Map.Entry<Sentiment, Integer> entry2 : categoryList.entrySet()) {
                category = entry2.getKey();
                n11 = entry2.getValue(); // N11 is the number of documents that have the feature and belong on the specific category
                n01 = stats.categoryCounts.get(category) - n11; // N01 is the total number of documents that do not have the particular feature BUT they belong to the specific category

                n00 = n0dot - n01; // N00 counts the number of documents that don't have the feature and don't belong to the specific category
                n10 = n1dot - n11; // N10 counts the number of documents that have the feature and don't belong to the specific category

                // calculate the chisquare score based on the above statistics
                chiSquareScore = stats.n*Math.pow(n11*n00-n10*n01, 2)/((n11+n01)*(n11+n10)*(n10+n00)*(n01+n00));

                //if the score is larger than the critical value then add it in the list
                if (chiSquareScore >= criticalValue) {
                    previousScore = selectedFeatures.get(feature);
                    if (previousScore == null || chiSquareScore > previousScore) {
                        // System.out.println("Selected: " + feature + "\nScore: " + chiSquareScore);
                        selectedFeatures.put(feature, chiSquareScore);
                    }
                }
                else {
                    // System.out.println("Not Selected: " + feature + "\nScore: " + chiSquareScore);
                }
            }
        }
        System.out.println("selectedFeatures size: " + selectedFeatures.size());

        return selectedFeatures;
    }


    public Map<String, Double> selectFeatures(NaiveBayesClassifier model) {
        int vocabularyNum = model.getVocabulary().size();

        System.out.println("VocabularyNum: " + vocabularyNum);

        setDegreesOfFreedom((vocabularyNum - 1) * (Sentiment.values().length - 1));
        double criticalValue = calculateCriticalValue();
        System.out.println("CriticalValue: " + criticalValue);
        Map<String, Double> selectedFeatures = new HashMap<>();

        Map<String, Double> wordScoreMap = new HashMap<>();
        Map<Sentiment, Double> categoryScoreMap = new HashMap<>();
        double observations = 0;
        for (Map.Entry<AnnotatedText, Double> modelScore : model.getLikelihoodMap().entrySet()) {
            String word = modelScore.getKey().getText();
            Sentiment category = modelScore.getKey().getCategory();
            double score = Math.exp(modelScore.getValue());
            observations += score;

            if (!wordScoreMap.containsKey(word)) {
                wordScoreMap.put(word, 0.0);
            }
            if (!categoryScoreMap.containsKey(category)) {
                categoryScoreMap.put(category, 0.0);
            }
            double currentScore = wordScoreMap.get(word);
            wordScoreMap.put(word, currentScore + score);

            double currentCategoryScore = categoryScoreMap.get(category);
            categoryScoreMap.put(category, currentCategoryScore + score);
        }
        System.out.println("Observations: " + observations);
        System.out.println("WordScoreMapSize: " + wordScoreMap.size());
        System.out.println("CategoryScoreMapSize: " + categoryScoreMap.size());


        String feature;
        Sentiment category;

        double n1dot, n0dot, n00, n01, n10, n11;
        double chiSquareScore;
        Double previousScore;
        for (Map.Entry<String, Double> entry1 : wordScoreMap.entrySet()) {
            feature = entry1.getKey();
            // categoryList = entry1.getValue();
            n1dot = entry1.getValue();

            //also the N0. (number of documents that DONT have the feature)
            n0dot = observations - n1dot;

            for (Sentiment sentiment : Sentiment.values()) {
                category = sentiment;
                n11 = model.getLikelihoodMap().get(new AnnotatedText(feature, category)); // N11 is the number of documents that have the feature and belong on the specific category
                n01 = categoryScoreMap.get(category) - n11; // N01 is the total number of documents that do not have the particular feature BUT they belong to the specific category

                n00 = n0dot - n01; // N00 counts the number of documents that don't have the feature and don't belong to the specific category
                n10 = n1dot - n11; // N10 counts the number of documents that have the feature and don't belong to the specific category

                // calculate the chisquare score based on the above statistics
                chiSquareScore = observations*Math.pow(n11*n00-n10*n01, 2)/((n11+n01)*(n11+n10)*(n10+n00)*(n01+n00));

                //if the score is larger than the critical value then add it in the list
                // System.out.printf("feature: " + feature + "%.2f %.2f %.2f %f \n", n00, n01, n10, n11);
                if (chiSquareScore >= criticalValue) {
                    previousScore = selectedFeatures.get(feature);
                    if (previousScore == null || chiSquareScore > previousScore) {
                        selectedFeatures.put(feature, chiSquareScore);
                    }
                }
                else {
                    System.out.println("Not Selected: " + feature + "\nScore: " + chiSquareScore);
                }
            }
        }
        System.out.println("selectedFeatures size: " + selectedFeatures.size());

        return selectedFeatures;
    }
}
