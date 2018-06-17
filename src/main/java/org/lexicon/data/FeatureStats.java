package org.lexicon.data;

import org.lexicon.Sentiment;

import java.util.HashMap;
import java.util.Map;

public class FeatureStats {
    /**
     * total number of Observations
     */
    public int n;

    /**
     * It stores the co-occurrences of Feature and Category values
     */
    public Map<String, Map<Sentiment, Integer>> featureCategoryJointCount = new HashMap<>();

    /**
     * Measures how many times each category was found in the training dataset.
     */
    public Map<Sentiment, Integer> categoryCounts = new HashMap<>();

    public FeatureStats () {}

    public static FeatureStats generateFeatureStats(Document dataset) {
        FeatureStats stats = new FeatureStats();

        Integer categoryCount;
        Sentiment category;
        Integer featureCategoryCount;
        String feature;
        Map<Sentiment, Integer> featureCategoryCounts;
        for(AnnotatedText sentence : dataset.getData()) {
            ++stats.n; // increase the number of observations
            category = sentence.getCategory();

            // increase the category counter by one
            categoryCount = stats.categoryCounts.get(category);
            if (categoryCount == null) {
                stats.categoryCounts.put(category, 1);
            } else {
                stats.categoryCounts.put(category, categoryCount + 1);
            }

            for (AnnotatedText token : sentence.preprocessIntoWords()) {
                feature = token.getText();

                // get the counts of the feature in the categories
                featureCategoryCounts = stats.featureCategoryJointCount.get(feature);
                if (featureCategoryCounts == null) {
                    // initialize it if it does not exist
                    stats.featureCategoryJointCount.put(feature, new HashMap<>());
                }

                featureCategoryCount = stats.featureCategoryJointCount.get(feature).get(category);
                if (featureCategoryCount == null) {
                    featureCategoryCount = 0;
                }

                // increase the number of occurrences of the feature in the category
                stats.featureCategoryJointCount.get(feature).put(category, ++featureCategoryCount);
            }
        }
        return stats;
    }
}
