package org.lexicon.process;

import org.lexicon.Sentiment;
import org.lexicon.data.Document;
import org.lexicon.data.FeatureStats;

import java.util.HashMap;
import java.util.Map;

public class ChiSquare {
    // 0.016
    // 2.706
    // df1 x2 0.050 3.841
    private static double chisquareCriticalValue = 2.706;

    public static Map<String, Double> selectFeatures(Document dataset) {
        return selectFeatures(dataset, chisquareCriticalValue);
    }

    public static Map<String, Double> selectFeatures(Document dataset, double criticalLevel) {
        FeatureStats stats = FeatureStats.generateFeatureStats(dataset);
        Map<String, Double> selectedFeatures = new HashMap<>();

        String feature;
        Sentiment category;
        Map<Sentiment, Integer> categoryList;

        int N1dot, N0dot, N00, N01, N10, N11;
        double chiSquareScore;
        Double previousScore;
        for(Map.Entry<String, Map<Sentiment, Integer>> entry1 : stats.featureCategoryJointCount.entrySet()) {
            feature = entry1.getKey();
            categoryList = entry1.getValue();

            //calculate the N1. (number of documents that have the feature)
            N1dot = 0;
            for(Integer count : categoryList.values()) {
                N1dot += count;
            }

            //also the N0. (number of documents that DONT have the feature)
            N0dot = stats.n - N1dot;

            for(Map.Entry<Sentiment, Integer> entry2 : categoryList.entrySet()) {
                category = entry2.getKey();
                N11 = entry2.getValue(); // N11 is the number of documents that have the feature and belong on the specific category
                N01 = stats.categoryCounts.get(category) - N11; // N01 is the total number of documents that do not have the particular feature BUT they belong to the specific category

                N00 = N0dot - N01; // N00 counts the number of documents that don't have the feature and don't belong to the specific category
                N10 = N1dot - N11; // N10 counts the number of documents that have the feature and don't belong to the specific category

                // calculate the chisquare score based on the above statistics
                chiSquareScore = stats.n*Math.pow(N11*N00-N10*N01, 2)/((N11+N01)*(N11+N10)*(N10+N00)*(N01+N00));

                //if the score is larger than the critical value then add it in the list
                if (chiSquareScore >= criticalLevel) {
                    previousScore = selectedFeatures.get(feature);
                    if (previousScore == null || chiSquareScore > previousScore) {
                        selectedFeatures.put(feature, chiSquareScore);
                    }
                }
            }
        }
        System.out.println("selectedFeatures size: " + selectedFeatures.size());

        return selectedFeatures;
    }
}
